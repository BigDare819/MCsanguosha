package com.sanguosha.item;

import com.sanguosha.block.CardMatBlock;
import com.sanguosha.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 牌布放置:右键方块时自动铺 4x3 整张(12 片,分片按相对位置) */
public final class CardMatEvents {
    private CardMatEvents() {}

    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getItemStack().getItem() != ModItems.CARD_MAT.get()) return;
        Level level = event.getLevel();
        BlockPos start = event.getPos().relative(event.getFace());
        // 布局方向:布的纵深(3 格)朝向玩家面朝方向,横向(4 格)为右手方向
        net.minecraft.core.Direction dir = event.getEntity().getDirection();
        net.minecraft.core.Direction right = dir.getClockWise();
        int fX = dir.getStepX(), fZ = dir.getStepZ();
        int rX = right.getStepX(), rZ = right.getStepZ();
        // 检查 4x3 区域全部可放
        boolean ok = true;
        for (int dx = 0; dx < 4 && ok; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos p = start.offset(rX * dx + fX * dz, 0, rZ * dx + fZ * dz);
                if (!level.getBlockState(p).canBeReplaced()) { ok = false; break; }
            }
        }
        if (!ok) {
            // 4x3 无法完全展开:禁止放置(取消默认放置,不消耗物品)
            event.setCanceled(true);
            if (event.getEntity() != null) {
                event.getEntity().displayClientMessage(
                        net.minecraft.network.chat.Component.literal("\u7a7a\u95f4\u4e0d\u8db3,\u65e0\u6cd5\u94fa\u5f00\u724c\u5e03"), true);
            }
            return;
        }
        // 铺 12 片:facing=玩家朝向(blockstate 模型 y 旋转保证图案方向),分片 = 相对位置
        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos p = start.offset(rX * dx + fX * dz, 0, rZ * dx + fZ * dz);
                BlockState st = ModBlocks.CARD_MAT.get().defaultBlockState()
                        .setValue(CardMatBlock.FACING, dir)
                        .setValue(CardMatBlock.MAT_X, dx)
                        .setValue(CardMatBlock.MAT_Y, 2 - dz);
                level.setBlock(p, st, 3);
            }
        }
        // 消耗物品 + 音效
        if (!event.getEntity().isCreative()) {
            event.getItemStack().shrink(1);
        }
        level.playSound(null, start, SoundType.WOOL.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        event.setCanceled(true);
    }
}