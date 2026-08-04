package com.sanguosha.block;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 手持物品右键牌盒/将盒:MC 交互链会先走 item.useOn(手持木板等 BlockItem 直接尝试放置,
 * 返回 SUCCESS 吞掉方块交互),导致蹲下右键打不开剩余 UI、非蹲下也摸不了牌。
 * 此事件在物品放置之前拦截,目标方块是牌盒/将盒时强制执行方块交互逻辑(不区分手持物品)。
 */
public final class CardBoxInteractEvents {
    private CardBoxInteractEvents() {}

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.isCanceled()) return; // 已被其他监听器处理(如手持牌布铺布),不重复
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.getBlock() instanceof CardBoxBlock box) {
            event.setCanceled(true);
            box.useWithoutItem(state, event.getLevel(), event.getPos(), event.getEntity(), event.getHitVec());
        }
    }
}
