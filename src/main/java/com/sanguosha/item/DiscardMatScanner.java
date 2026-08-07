package com.sanguosha.item;

import com.sanguosha.block.DiscardMatBlock;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 弃牌布记录显示:定期扫描 4x4 弃牌布区域上方的 CardEntity/ItemEntity,发现即清除(discard)并累计记录,
 * 在弃牌布中心上方用常驻 TextDisplay 显示最近弃的 5 张牌(每行 "♠A 杀",红桃/方块红字、黑桃/梅花黑字),
 * 内容变化时重建实体,弃牌布被挖时移除显示。
 *
 * 与 CardMatScanner 的区别:牌布是"统计当前牌面",弃牌布是"清除 + 累计历史记录"。
 */
@EventBusSubscriber(modid = "sanguosha", bus = EventBusSubscriber.Bus.GAME)
public final class DiscardMatScanner {
    private static final int SCAN_INTERVAL = 10; // tick,0.5 秒扫一次
    private static final int RANGE = 24;         // 水平扫描半径(格)
    /** 弃牌布起点 -> 常驻 TextDisplay */
    private static final Map<BlockPos, Display.TextDisplay> ACTIVE = new HashMap<>();
    /** 弃牌布起点 -> 上次显示文字(避免无变化重建闪烁) */
    private static final Map<BlockPos, String> LAST_TEXT = new HashMap<>();
    /** 弃牌布浮空文字的 CustomName 标记前缀(与牌布的 sgsmat 区分) */
    private static final String MARKER_PREFIX = "\u00a7\u00a7sgsdiscard:";
    /** 浮空文本显示最近弃的牌数 */
    private static final int DISPLAY_LAST = 5;

    private DiscardMatScanner() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long tick = event.getServer().getTickCount();
        if (tick % SCAN_INTERVAL != 0) return;
        ServerLevel level = event.getServer().overworld();
        if (level == null) return;
        // 收集在线玩家附近的完整弃牌布起点
        Set<BlockPos> mats = new HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            collectMats(level, player, mats);
        }
        // 清理已不存在的弃牌布显示(被挖/玩家远离)
        ArrayList<BlockPos> stale = new ArrayList<>();
        for (BlockPos key : ACTIVE.keySet()) {
            if (!mats.contains(key)) stale.add(key);
        }
        for (BlockPos key : stale) {
            removeDisplay(level, key);
        }
        // 清理已自动淡出销毁的字幕(ACTIVE 残留;LAST_TEXT 保留 → updateMat 内容没变会保持隐藏,
        // 不会每 10 tick 自动重建;只有新弃牌/内容变化才重新显示)
        ACTIVE.entrySet().removeIf(en -> en.getValue() == null || en.getValue().isRemoved());
        // 更新每张弃牌布的记录
        for (BlockPos start : mats) {
            updateMat(level, start);
        }
    }

    /** 扫描玩家周围找弃牌布分片,反推整张布的起点 */
    private static void collectMats(ServerLevel level, ServerPlayer player, Set<BlockPos> out) {
        BlockPos c = player.blockPosition();
        for (int dx = -RANGE; dx <= RANGE; dx++) {
            for (int dz = -RANGE; dz <= RANGE; dz++) {
                for (int dy = -4; dy <= 4; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (level.getBlockState(p).getBlock() != ModBlocks.DISCARD_MAT.get()) continue;
                    BlockPos start = matStart(level, p);
                    if (start != null && isMatComplete(level, start)) out.add(start);
                }
            }
        }
    }

    /** 由一片弃牌布反推整张布的起点(与 DiscardMatBlock.onDestroyedByPlayer 同逻辑) */
    private static BlockPos matStart(Level level, BlockPos piece) {
        BlockState st = level.getBlockState(piece);
        if (st.getBlock() != ModBlocks.DISCARD_MAT.get()) return null;
        Direction dir = st.getValue(DiscardMatBlock.FACING);
        Direction right = dir.getClockWise();
        int mx = st.getValue(DiscardMatBlock.MAT_X);
        int my = st.getValue(DiscardMatBlock.MAT_Y);
        int dz0 = 3 - my; // 被扫片在布中的行(mat_y = 3 - dz)
        return piece.offset(-right.getStepX() * mx - dir.getStepX() * dz0,
                0,
                -right.getStepZ() * mx - dir.getStepZ() * dz0);
    }

    /** 检查 4x4 区域 16 片是否都是同一张弃牌布(facing 一致) */
    private static boolean isMatComplete(Level level, BlockPos start) {
        BlockState s0 = level.getBlockState(start);
        if (s0.getBlock() != ModBlocks.DISCARD_MAT.get()) return false;
        Direction dir = s0.getValue(DiscardMatBlock.FACING);
        Direction right = dir.getClockWise();
        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 4; dz++) {
                BlockPos p = start.offset(right.getStepX() * dx + dir.getStepX() * dz,
                        0,
                        right.getStepZ() * dx + dir.getStepZ() * dz);
                BlockState s = level.getBlockState(p);
                if (s.getBlock() != ModBlocks.DISCARD_MAT.get() || s.getValue(DiscardMatBlock.FACING) != dir) return false;
            }
        }
        return true;
    }

    /** 扫描弃牌布上方 CardEntity:清除并累计计数,更新显示 */
    private static void updateMat(ServerLevel level, BlockPos start) {
        BlockState s0 = level.getBlockState(start);
        Direction dir = s0.getValue(DiscardMatBlock.FACING);
        Direction right = dir.getClockWise();
        // 4x4 区域上方 0.02 ~ 1.2 格内的卡牌实体
        double bx = start.getX() + right.getStepX() * 3.0 + dir.getStepX() * 3.0;
        double bz = start.getZ() + right.getStepZ() * 3.0 + dir.getStepZ() * 3.0;
        double minX = Math.min(start.getX(), bx);
        double maxX = Math.max(start.getX(), bx) + 1.0;
        double minZ = Math.min(start.getZ(), bz);
        double maxZ = Math.max(start.getZ(), bz) + 1.0;
        AABB box = new AABB(minX, start.getY() + 0.02, minZ, maxX, start.getY() + 1.2, maxZ);
        boolean changed = false;
        // 实体卡牌(R 键放置的 CardEntity)
        for (CardEntity e : level.getEntitiesOfClass(CardEntity.class, box)) {
            String info = e.getCardInfo();
            if (info == null || info.isEmpty()) continue;
            if (formatLine(info) == null) continue;
            // 记录完整牌信息到有序弃牌列表(UI 可查看/拿取)
            DiscardDeckManager.add(start, info);
            e.discard();                        // 清除牌实体
            changed = true;
        }
        // 凋落物(Q 键丢出的原版物品,ItemEntity 内含卡牌 ItemStack)
        for (net.minecraft.world.entity.item.ItemEntity it : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            net.minecraft.world.item.ItemStack st = it.getItem();
            if (st.isEmpty() || !st.is(com.sanguosha.item.ModItems.CARD.get())) continue;
            String info = st.get(com.sanguosha.item.CardData.CARD_INFO);
            if (info == null || info.isEmpty()) continue;
            if (formatLine(info) == null) continue;
            DiscardDeckManager.add(start, info);
            it.discard();
            changed = true;
        }
        if (DiscardDeckManager.size(start) == 0) {
            removeDisplay(level, start);
            return;
        }
        // 汇总显示文字:最近弃的 DISPLAY_LAST 张,每张一行(第 1 行在顶部最远,最后一行贴近布面),
        // 每行显示 "花色符号 点数 牌名"(如 "♠A 杀"),红桃/方块红字、黑桃/梅花黑字,武将牌黄字
        List<String> recent = DiscardDeckManager.recent(start, DISPLAY_LAST);
        net.minecraft.network.chat.MutableComponent text = Component.literal("");
        boolean first = true;
        for (int i = 0; i < recent.size(); i++) {
            Component line = formatLine(recent.get(i));
            if (line == null) continue;
            if (!first) text.append("\n");
            text.append(line);
            first = false;
        }
        int lineCount = recent.size();
        String plain = text.getString();
        if (!changed && plain.equals(LAST_TEXT.get(start))) return; // 内容没变,不重建
        // 弃牌布中心上方:最后一行中心离布面 0.5 格,第 1 行在最远(行高 0.25 格,实体为文本中心)
        double cx = start.getX() + 0.5 + right.getStepX() * 1.5 + dir.getStepX() * 1.5;
        double cz = start.getZ() + 0.5 + right.getStepZ() * 1.5 + dir.getStepZ() * 1.5;
        double y = start.getY() + 0.5 + (lineCount - 1) * 0.125;
        updateDisplay(level, start, new Vec3(cx, y, cz), text);
    }

    /** 解析卡牌信息为显示行:普通牌 "♠A 杀"(红桃/方块红字、黑桃/梅花黑字),武将牌黄字 "名字" */
    private static Component formatLine(String info) {
        String[] parts = info.split("\\|");
        if (parts.length == 0) return null;
        if (parts[0].startsWith("武将:")) {
            return parts.length > 1
                    ? Component.literal(parts[1]).withStyle(s -> s.withColor(0xFFFFE040).withBold(true))
                    : null;
        }
        if (parts.length < 3) return Component.literal(parts[0]);
        return Component.literal(suitSymbol(parts[1]) + parts[2] + " " + parts[0])
                .withStyle(s -> s.withColor(suitColor(parts[1])).withBold(true));
    }

    /** 花色中文名 -> 显示颜色:红桃/方块红(0xFFE04040),黑桃/梅花黑(0xFF303030);未知黄 */
    private static int suitColor(String cn) {
        for (com.sanguosha.card.CardSuit s : com.sanguosha.card.CardSuit.values()) {
            if (s.cn.equals(cn)) return s.color == 1 ? 0xFFE04040 : 0xFF303030;
        }
        return 0xFFFFE040;
    }

    /** 花色中文名 -> 符号(黑桃→♠ 红桃→♥ 梅花→♣ 方块→♦);未知返回空 */
    private static String suitSymbol(String cn) {
        for (com.sanguosha.card.CardSuit s : com.sanguosha.card.CardSuit.values()) {
            if (s.cn.equals(cn)) return s.symbol;
        }
        return "";
    }

    /** 重建常驻 TextDisplay(复用 ClientHudText.HudTextDisplay 的 NBT 创建方式) */
    private static void updateDisplay(ServerLevel level, BlockPos start, Vec3 loc, Component text) {
        Display.TextDisplay old = ACTIVE.remove(start);
        if (old != null && !old.isRemoved()) old.discard();
        // 清理世界里的旧实体(按弃牌布标记匹配,防重进残留叠加)
        AABB clear = new AABB(loc.x - 3, loc.y - 3, loc.z - 3, loc.x + 3, loc.y + 3, loc.z + 3);
        for (Display.TextDisplay e : level.getEntitiesOfClass(Display.TextDisplay.class, clear)) {
            if (e.isRemoved()) continue;
            if (isDiscardMarker(e, start)) e.discard();
        }
        ClientHudText.HudTextDisplay s = new ClientHudText.HudTextDisplay(EntityType.TEXT_DISPLAY, level);
        s.setPos(loc.x, loc.y, loc.z);
        CompoundTag tag = new CompoundTag();
        // 整块文字统一加粗(每行颜色已在 formatLine 里设置),billboard 面向玩家
        tag.putString("text", Component.Serializer.toJson(text.copy().withStyle(Style.EMPTY.withBold(true)), level.registryAccess()));
        tag.putString("billboard", "center");
        tag.putBoolean("shadow", true);
        tag.putInt("background", 0x66000000);
        s.loadData(tag);
        s.setCustomName(markerFor(start)); // CustomName 标记(持久化,重进后也能识别清理)
        s.setCustomNameVisible(false);
        // 弃牌布字幕:出现 4 秒后逐渐淡出并自动销毁(不挡玩家对视);新弃牌重建时重新计时
        s.setFadeOut(true);
        s.setBaseTag(tag);
        level.addFreshEntity(s);
        ACTIVE.put(start, s);
        LAST_TEXT.put(start, text.getString());
    }

    /** 弃牌布浮空文字标记:§§sgsdiscard:x,y,z */
    private static Component markerFor(BlockPos pos) {
        return Component.literal(MARKER_PREFIX + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    /** 判断 TextDisplay 是否属于弃牌布记录文字(供 CardMatScanner 清理时跳过,防误杀) */
    public static boolean isDiscardMatText(Display.TextDisplay e) {
        Component c = e.getCustomName();
        if (c == null) return false;
        return c.getString().startsWith(MARKER_PREFIX);
    }

    /** 判断 TextDisplay 是否属于指定弃牌布的记录文字 */
    private static boolean isDiscardMarker(Display.TextDisplay e, BlockPos pos) {
        Component c = e.getCustomName();
        if (c == null) return false;
        String s = c.getString();
        return s.startsWith(MARKER_PREFIX + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    /** 移除弃牌布显示(内存 map + 世界里的 TextDisplay);也被 DiscardMatBlock.onRemove 调用 */
    public static void removeDisplay(ServerLevel level, BlockPos start) {
        Display.TextDisplay old = ACTIVE.remove(start);
        if (old != null && !old.isRemoved()) old.discard();
        if (level != null) {
            // 覆盖整张弃牌布(4x4)上方区域,按 CustomName 标记匹配 start 清理
            AABB clear = new AABB(start.getX() - 3, start.getY() - 3, start.getZ() - 3,
                    start.getX() + 7, start.getY() + 6, start.getZ() + 7);
            for (Display.TextDisplay e : level.getEntitiesOfClass(Display.TextDisplay.class, clear)) {
                if (e.isRemoved()) continue;
                if (isDiscardMarker(e, start)) e.discard();
            }
        }
        LAST_TEXT.remove(start);
    }

    /** 弃牌布被移除时完整清理(显示 + 记录);由 DiscardMatBlock 在整张布销毁时调用 */
    public static void removeMat(ServerLevel level, BlockPos start) {
        removeDisplay(level, start);
        DiscardDeckManager.remove(start);
    }

    /** 外部(如拿取记录后)强制刷新某张弃牌布的浮空文本 */
    public static void refreshDisplay(ServerLevel level, BlockPos start) {
        LAST_TEXT.remove(start); // 强制重建
        if (DiscardDeckManager.size(start) == 0) {
            removeDisplay(level, start);
            return;
        }
        updateMat(level, start);
    }
}
