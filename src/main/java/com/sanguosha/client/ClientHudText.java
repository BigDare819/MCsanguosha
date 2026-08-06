package com.sanguosha.client;

import com.sanguosha.item.CardMatScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 方块上方漂浮文字(TextDisplay 实体:原生无碰撞箱不挡右键、不渲染实体本体;1.5 秒后消失)。
 * 文字以实体坐标为中心,无需高度补偿。
 * 牌布统计文字(CardMatScanner)复用本类实体,持久化牌布起点 matStart,
 * tick 自检:牌布被挖/存档恢复后牌布不存在 → 实体自动销毁,防止重进残留。
 */
@EventBusSubscriber(modid = "sanguosha", bus = EventBusSubscriber.Bus.GAME)
public final class ClientHudText {
    private static final Map<Display, Long> STANDS = new HashMap<>();

    private ClientHudText() {}

    /** 牌布统计文字的 CustomName 标记前缀。实体存档重进后变回原版 TextDisplay(类型丢失),
     *  但 CustomName 会持久化,靠它精确识别/清理牌布统计文字 */
    private static final String MARKER_PREFIX = "\u00a7\u00a7sgsmat:";

    /** 临时漂浮文字(牌盒摸牌/发将提示)的 CustomName 标记前缀 */
    private static final String TMP_MARKER_PREFIX = "\u00a7\u00a7sgstmp:";

    /** 生成牌布统计文字标记:§§sgsmat:x,y,z */
    public static net.minecraft.network.chat.Component markerFor(BlockPos start) {
        return Component.literal(MARKER_PREFIX + start.getX() + "," + start.getY() + "," + start.getZ());
    }

    /** 生成临时漂浮文字标记:§§sgstmp:<实体uuid前8位> */
    public static net.minecraft.network.chat.Component tmpMarkerFor(java.util.UUID uuid) {
        return Component.literal(TMP_MARKER_PREFIX + uuid.toString().substring(0, 8));
    }

    /** 解析标记:是牌布统计文字则返回牌布起点,否则 null */
    public static BlockPos parseMarker(net.minecraft.world.entity.Display.TextDisplay e) {
        net.minecraft.network.chat.Component c = e.getCustomName();
        if (c == null) return null;
        String s = c.getString();
        if (!s.startsWith(MARKER_PREFIX)) return null;
        String[] p = s.substring(MARKER_PREFIX.length()).split(",");
        if (p.length != 3) return null;
        try {
            return new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** 是否临时漂浮文字(§§sgstmp: 标记) */
    public static boolean isTmpMarker(net.minecraft.world.entity.Display.TextDisplay e) {
        net.minecraft.network.chat.Component c = e.getCustomName();
        return c != null && c.getString().startsWith(TMP_MARKER_PREFIX);
    }

    /** 临时文字是否仍在 STANDS 跟踪中(正常显示中);重进后 STANDS 清空 → false */
    public static boolean isTmpTracked(net.minecraft.world.entity.Display.TextDisplay e) {
        return STANDS.containsKey(e);
    }

    /** 子类包装:readAdditionalSaveData 为 protected;setText/DATA_TEXT_ID 在 1.21.1 均为 private,只能经 NBT 设置 */
    public static class HudTextDisplay extends Display.TextDisplay {
        private BlockPos matStart = null; // 所属牌布起点(null = 临时漂浮文字,不参与自检)
        private int selfCheck = 0;

        public HudTextDisplay(EntityType<? extends Display.TextDisplay> type, Level level) { super(type, level); }
        public void loadData(CompoundTag tag) { this.readAdditionalSaveData(tag); }

        /** 记录所属牌布起点(牌布统计文字用) */
        public void setMatStart(BlockPos p) { this.matStart = p; }

        /** 所属牌布起点(供 CardMatScanner 扫描孤儿实体时判断) */
        public BlockPos getMatStart() { return matStart; }

        @Override
        public void readAdditionalSaveData(CompoundTag tag) {
            super.readAdditionalSaveData(tag);
            if (tag.contains("MatStartX") && tag.contains("MatStartY") && tag.contains("MatStartZ")) {
                matStart = new BlockPos(tag.getInt("MatStartX"), tag.getInt("MatStartY"), tag.getInt("MatStartZ"));
            }
        }

        @Override
        public void addAdditionalSaveData(CompoundTag tag) {
            super.addAdditionalSaveData(tag);
            if (matStart != null) {
                tag.putInt("MatStartX", matStart.getX());
                tag.putInt("MatStartY", matStart.getY());
                tag.putInt("MatStartZ", matStart.getZ());
            }
        }

        @Override
        public void tick() {
            super.tick();
            // 牌布统计文字:每 20 tick(1s)检查所属牌布是否还在,没了就自毁(兜底清理)
            if (matStart != null && !level().isClientSide && ++selfCheck % 20 == 0) {
                if (!CardMatScanner.isMatComplete(level(), matStart)) {
                    discard();
                }
            }
        }
    }

    public static void show(ServerLevel level, Vec3 pos, Component text) {
        // 最多只共存一个临时文字:先清掉所有正在显示的旧文字,再创建新的
        java.util.Iterator<Map.Entry<Display, Long>> it = STANDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Display, Long> en = it.next();
            Display d = en.getKey();
            if (d != null && !d.isRemoved()) d.discard();
            it.remove();
        }
        HudTextDisplay s = new HudTextDisplay(EntityType.TEXT_DISPLAY, level);
        s.setPos(pos.x, pos.y, pos.z);              // 文字中心落在目标坐标
        CompoundTag tag = new CompoundTag();
        tag.putString("text", Component.Serializer.toJson(text, level.registryAccess()));
        tag.putString("billboard", "center");        // 始终面向玩家
        s.loadData(tag);
        // 临时文字也打 CustomName 标记(持久化):重进后实体变回原版 TextDisplay,
        // 靠标记识别 + STANDS 不在跟踪中 → 判定为残留并清理(与牌布统计文字同思路)
        s.setCustomName(tmpMarkerFor(java.util.UUID.randomUUID()));
        s.setCustomNameVisible(false);
        level.addFreshEntity(s);
        STANDS.put(s, (long) (level.getServer().getTickCount() + 30));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        STANDS.entrySet().removeIf(e -> {
            if (now > e.getValue()) {
                e.getKey().discard();
                return true;
            }
            return false;
        });
    }
}
