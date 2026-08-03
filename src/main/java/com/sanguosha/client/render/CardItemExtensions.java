package com.sanguosha.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** 卡牌物品扩展:提供自定义渲染器(每次获取,避免初始化时序问题) */
public class CardItemExtensions implements IClientItemExtensions {
    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        Minecraft mc = Minecraft.getInstance();
        return new CardItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModels());
    }
}