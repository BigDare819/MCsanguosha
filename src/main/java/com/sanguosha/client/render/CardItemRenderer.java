package com.sanguosha.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sanguosha.item.CardData;
import com.sanguosha.item.CardModelIds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** 手持卡牌渲染:entityTranslucent + 直接贴图(与地面实体完全相同的已验证代码) */
public class CardItemRenderer extends BlockEntityWithoutLevelRenderer {

    public CardItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
        super(dispatcher, models);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        String info = stack.get(CardData.CARD_INFO);
        String name = info == null ? "" : info.split("\\|")[0];
        String key;
        ResourceLocation tex;
        if (name.startsWith("\u6b66\u5c06:")) {
            // 武将牌:手持用 textures/item/hero_<id>.png
            key = "hero_" + name.substring(3);
            tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/item/" + key + ".png");
        } else {
            key = CardModelIds.keyOf(name);
            tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/card/" + key + ".png");
        }
        Minecraft.getInstance().getTextureManager().getTexture(tex); // 预加载
        float hw, hh;
        pose.pushPose();
        if (ctx == ItemDisplayContext.GUI) {
            hw = 0.55F; hh = 0.75F;
            pose.translate(0, 0.15, 0);
        } else if (ctx == ItemDisplayContext.GROUND || ctx == ItemDisplayContext.FIXED) {
            hw = 0.9F; hh = 1.25F;
            pose.mulPose(new Quaternionf(new AxisAngle4f((float) -Math.PI / 2, 1, 0, 0)));
            pose.translate(0, 0, 0.1F);
        } else {
            hw = 0.6F; hh = 0.84F;
            pose.translate(0, 0.2F, 0);
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(tex));
        Matrix4f m = pose.last().pose();
        int ov = OverlayTexture.NO_OVERLAY;
        vc.addVertex(m, -hw, -hh, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(ov).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, -hw, hh, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(ov).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, hw, hh, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(ov).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, hw, -hh, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(ov).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        pose.popPose();
    }
}