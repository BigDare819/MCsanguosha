package com.sanguosha.item;

import com.sanguosha.block.CardMatBlock;
import com.sanguosha.block.ModBlocks;
import com.sanguosha.client.ClientHudText;
import com.sanguosha.entity.CardEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 牌布上方牌面统计显示:定期扫描牌布(4x3)区域上方的 CardEntity,汇总牌名计数,
 * 在牌布中央上方用常驻 TextDisplay 显示(如"杀×2 闪×1"),内容变化时重建实体,
 * 牌布被挖/无牌时移除显示。
 */
@EventBusSubscriber(modid = "sanguosha", bus = EventBusSubscriber.Bus.GAME)
public final class CardMatScanner {
    private static final int SCAN_INTERVAL = 10; // tick,0.5 秒扫一次
    private static final int RANGE = 24;          // 水平扫描半径(格)
    /** 牌布起点 -> 常驻 TextDisplay */
    private static final Map<BlockPos, Display.TextDisplay> ACTIVE = new HashMap<>();
    /** 牌布起点 -> 上次显示文字(避免无变化重建闪烁) */
    private static final Map<BlockPos, String> LAST_TEXT = new HashMap<>();

    private CardMatScanner() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long tick = event.getServer().getTickCount();
        if (tick % SCAN_INTERVAL != 0) return;
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;
        // 收集在线玩家附近的完整牌布起点
        Set<BlockPos> mats = new HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            collectMats(level, player, mats);
        }
        // 清理已不存在的牌布显示(被挖/玩家远离)
        // 先收集再删除:迭代器遍历期间调用 removeMat(内部会 ACTIVE.remove)会改 modCount,
        // 再 it.remove() 会抛 ConcurrentModificationException(退出世界收尾 tick 崩溃)
        java.util.ArrayList<BlockPos> stale = new java.util.ArrayList<>();
        for (Map.Entry<BlockPos, Display.TextDisplay> e : ACTIVE.entrySet()) {
            if (!mats.contains(e.getKey())) stale.add(e.getKey());
        }
        for (BlockPos key : stale) {
            removeMat(level, key);
        }
        // 清理孤儿文字实体:重进后实体变回原版 TextDisplay(类型丢失),但 CustomName 标记会保留。
        // 扫描玩家附近所有 TextDisplay:标记指向的牌布已不存在 → discard;无标记的旧实体
        // (历史版本)若位置下方已无牌布 → discard。
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            sweepOrphans(level, player);
        }
        // 更新每张牌布的文字
        for (BlockPos start : mats) {
            updateMat(level, start);
        }
    }

    /** 清理玩家附近所有"所属牌布已不存在"的统计文字实体(含重进残留的孤儿) */
    private static void sweepOrphans(ServerLevel level, ServerPlayer player) {
        BlockPos c = player.blockPosition();
        AABB area = new AABB(c.getX() - RANGE, c.getY() - 8, c.getZ() - RANGE,
                c.getX() + RANGE, c.getY() + 8, c.getZ() + RANGE);
        for (net.minecraft.world.entity.Display.TextDisplay e : level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, area)) {
            if (e.isRemoved()) continue;
            BlockPos ms = ClientHudText.parseMarker(e);
            if (ms != null) {
                if (!isMatComplete(level, ms)) {
                    e.discard();
                    ACTIVE.remove(ms);
                    LAST_TEXT.remove(ms);
                }
                continue;
            }
            // 临时漂浮文字(牌盒摸牌/发将提示):重进后 STANDS(内存)清空,实体变回原版类型,
            // isTmpTracked=false → 判定为残留,清掉;正常游戏中 STANDS 仍在跟踪 → 保留
            if (ClientHudText.isTmpMarker(e)) {
                if (!ClientHudText.isTmpTracked(e)) {
                    e.discard();
                }
                continue;
            }
            // 无标记(旧版本实体):若下方既无牌布也无牌盒 → 孤儿,清掉
            // (旧版本牌盒临时文字无标记且下方有牌盒,会被保留——历史残留,新版本已打标记)
            if (!hasAnchorBelow(level, e.blockPosition())) {
                e.discard();
            }
        }
    }

    /** 检查某列坐标下方 1~4 格内是否有牌布或牌盒方块(两者都是统计文字/临时文字的锚点) */
    private static boolean hasAnchorBelow(ServerLevel level, BlockPos at) {
        for (int dy = 1; dy <= 4; dy++) {
            var b = level.getBlockState(at.below(dy)).getBlock();
            if (b == ModBlocks.CARD_MAT.get() || b == com.sanguosha.block.ModBlocks.DECK_BOX.get()
                    || b == com.sanguosha.block.ModBlocks.HERO_DECK_BOX.get()) return true;
        }
        return false;
    }

    /** 扫描玩家周围找牌布分片,反推整张布的起点 */
    private static void collectMats(ServerLevel level, ServerPlayer player, Set<BlockPos> out) {
        BlockPos c = player.blockPosition();
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dz = -RANGE; dz <= RANGE; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() != ModBlocks.CARD_MAT.get()) continue;
                    BlockPos start = matStart(level, p);
                    if (start != null && isMatComplete(level, start)) out.add(start);
                }
            }
        }
    }

    /** 由一片牌布反推整张布的起点(与 CardMatBlock.onDestroyedByPlayer 同逻辑) */
    private static BlockPos matStart(Level level, BlockPos piece) {
        BlockState st = level.getBlockState(piece);
        if (st.getBlock() != ModBlocks.CARD_MAT.get()) return null;
        Direction dir = st.getValue(CardMatBlock.FACING);
        Direction right = dir.getClockWise();
        int mx = st.getValue(CardMatBlock.MAT_X);
        int my = st.getValue(CardMatBlock.MAT_Y);
        int dz0 = 2 - my; // 被扫片在布中的行(mat_y = 2 - dz)
        return piece.offset(-right.getStepX() * mx - dir.getStepX() * dz0,
                0,
                -right.getStepZ() * mx - dir.getStepZ() * dz0);
    }

    /** 检查 4x3 区域 12 片是否都是同一张牌布(facing 一致);public 供 TextDisplay 实体自检 */
    public static boolean isMatComplete(Level level, BlockPos start) {
        BlockState s0 = level.getBlockState(start);
        if (s0.getBlock() != ModBlocks.CARD_MAT.get()) return false;
        Direction dir = s0.getValue(CardMatBlock.FACING);
        Direction right = dir.getClockWise();
        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos p = start.offset(right.getStepX() * dx + dir.getStepX() * dz,
                        0,
                        right.getStepZ() * dx + dir.getStepZ() * dz);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() != ModBlocks.CARD_MAT.get() || s.getValue(CardMatBlock.FACING) != dir) return false;
            }
        }
        return true;
    }

    /** 汇总牌布上方实体牌的名字计数,更新/移除显示 */
    private static void updateMat(ServerLevel level, BlockPos start) {
        BlockState s0 = level.getBlockState(start);
        Direction dir = s0.getValue(CardMatBlock.FACING);
        Direction right = dir.getClockWise();
        // 布面之上 0.02 ~ 1.2 格内的卡牌实体
        // 布实际覆盖 start ~ start+right*3+dir*2 的方块(4x3 分片),每块占据 [pos, pos+1)。
        // AABB 边界 = 布对角方块起点 + 1.0(区域上界):布内最远边缘格(如 right 方向第 4 格
        // [bx, bx+1))的牌中心在 bx+0.5、bbox 从 bx+0.225 起,必然 < bx+1 → 相交不漏检;
        // 布外一格的牌 bbox 从 bx+1.225 起 > bx+1 → 不相交不误检。四朝向对称,无需外扩。
        double bx = start.getX() + right.getStepX() * 3.0 + dir.getStepX() * 2.0;
        double bz = start.getZ() + right.getStepZ() * 3.0 + dir.getStepZ() * 2.0;
        double minX = Math.min(start.getX(), bx);
        double maxX = Math.max(start.getX(), bx) + 1.0;
        double minZ = Math.min(start.getZ(), bz);
        double maxZ = Math.max(start.getZ(), bz) + 1.0;
        AABB box = new AABB(minX, start.getY() + 0.02, minZ, maxX, start.getY() + 1.2, maxZ);
        List<CardEntity> cards = level.getEntitiesOfClass(CardEntity.class, box);
        if (cards.isEmpty()) {
            removeMat(level, start);
            return;
        }
        Map<String, Integer> counts = new TreeMap<>();
        for (CardEntity e : cards) {
            String info = e.getCardInfo();
            if (info == null || info.isEmpty()) continue;
            String[] parts = info.split("\\|");
            String name;
            if (parts.length > 0 && parts[0].startsWith("\u6b66\u5c06:")) {
                // 武将牌:"武将:id|名字",显示名字
                name = parts.length > 1 ? parts[1] : parts[0].substring(3);
            } else {
                // 普通牌:"牌名|花色|点数",显示牌名
                name = parts.length > 0 ? parts[0] : info;
            }
            counts.merge(name, 1, Integer::sum);
        }
        if (counts.isEmpty()) {
            removeMat(level, start);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> en : counts.entrySet()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(en.getKey());
            if (en.getValue() > 1) sb.append("\u00d7").append(en.getValue());
        }
        String text = sb.toString();
        if (text.equals(LAST_TEXT.get(start))) return; // 内容没变,不重建
        // 牌布中心上方 2.6 格(提高 2 格,避免被牌挡住)
        double cx = start.getX() + 0.5 + right.getStepX() * 1.5 + dir.getStepX() * 1.5;
        double cz = start.getZ() + 0.5 + right.getStepZ() * 1.5 + dir.getStepZ() * 1.5;
        updateDisplay(level, start, new Vec3(cx, start.getY() + 2.6, cz), text);
    }

    /** 重建常驻 TextDisplay(1.21.1 setText 为 private,只能经 NBT 创建) */
    private static void updateDisplay(ServerLevel level, BlockPos start, Vec3 pos, String text) {
        // 清理旧实体:内存 map 里的 + 世界里的(重进后 ACTIVE 为空,存档里的旧 TextDisplay 会
        // 变回原版类型,只能按 CustomName 标记匹配清理,否则新旧文字叠加)
        Display.TextDisplay old = ACTIVE.remove(start);
        if (old != null && !old.isRemoved()) old.discard();
        AABB clear = new AABB(pos.x - 3, pos.y - 3, pos.z - 3, pos.x + 3, pos.y + 3, pos.z + 3);
        for (net.minecraft.world.entity.Display.TextDisplay e : level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, clear)) {
            if (e.isRemoved()) continue;
            BlockPos ms = ClientHudText.parseMarker(e);
            if (ms != null && ms.equals(start)) {
                e.discard();
                continue;
            }
            // 旧版本无标记实体:位置与目标文字重叠(统计文字就在 pos 处)→ 清
            if (ms == null && Math.abs(e.getX() - pos.x) < 1.5 && Math.abs(e.getZ() - pos.z) < 1.5
                    && Math.abs(e.getY() - pos.y) < 1.0) {
                e.discard();
            }
        }
        ClientHudText.HudTextDisplay s = new ClientHudText.HudTextDisplay(EntityType.TEXT_DISPLAY, level);
        s.setPos(pos.x, pos.y, pos.z);
        CompoundTag tag = new CompoundTag();
        tag.putString("text", Component.Serializer.toJson(
                Component.literal(text).withStyle(Style.EMPTY.withColor(0xFFFFE040).withBold(true)),
                level.registryAccess()));
        tag.putString("billboard", "center");
        tag.putBoolean("shadow", true);
        tag.putInt("background", 0x66000000); // 半透明黑底,提高可读性
        s.loadData(tag);
        // CustomName 标记(持久化,重进后实体变回原版 TextDisplay 也能识别清理);不显示名字
        s.setCustomName(ClientHudText.markerFor(start));
        s.setCustomNameVisible(false);
        level.addFreshEntity(s);
        ACTIVE.put(start, s);
        LAST_TEXT.put(start, text);
    }

    /** 移除牌布显示(清理内存 map + 世界里的残留 TextDisplay;也被方块 onRemove 调用) */
    public static void removeMat(ServerLevel level, BlockPos start) {
        Display.TextDisplay old = ACTIVE.remove(start);
        if (old != null && !old.isRemoved()) old.discard();
        if (level != null) {
            // 覆盖整张牌布(4x3)上方区域,按 CustomName 标记匹配 start 清理
            AABB clear = new AABB(start.getX() - 3, start.getY() - 3, start.getZ() - 3,
                    start.getX() + 7, start.getY() + 6, start.getZ() + 6);
            for (net.minecraft.world.entity.Display.TextDisplay e : level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class, clear)) {
                if (e.isRemoved()) continue;
                BlockPos ms = ClientHudText.parseMarker(e);
                if (ms != null && ms.equals(start)) {
                    e.discard();
                    continue;
                }
                // 旧版本无标记实体:高度接近统计文字(布面+2.6)→ 清
                if (ms == null && e.getY() > start.getY() + 1.8 && e.getY() < start.getY() + 3.4) {
                    e.discard();
                }
            }
        }
        LAST_TEXT.remove(start);
    }
}
