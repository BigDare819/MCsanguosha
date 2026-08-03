package com.sanguosha.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sanguosha.entity.CardEntity;
import com.sanguosha.item.CardModelIds;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** 卡牌实体渲染:卡牌/武将牌平面 + 牌盒/将盒 3D 长方体 */
public class CardEntityRenderer extends EntityRenderer<CardEntity> {
    public CardEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CardEntity entity, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        super.render(entity, yaw, partialTick, pose, buffer, light);
        String info = entity.getCardInfo();
        if (info.isEmpty()) return;
        ResourceLocation tex;
        boolean isBox = info.equals("牌盒:") || info.equals("将盒:");
        if (isBox) {
            tex = info.equals("牌盒:")
                    ? ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/item/box.png")
                    : ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/item/herobox.png");
            renderBox(entity, pose, buffer, light, tex);
            return;
        }
        if (!entity.isFaceUp()) {
            tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/card/back.png");
        } else if (info.startsWith("武将:")) {
            String id = info.split("\\|")[0].substring(3);
            tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/hero/" + id + ".png");
        } else {
            String name = info.split("\\|")[0];
            String key = CardModelIds.keyOf(name);
            tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/card/" + key + ".png");
        }
        pose.pushPose();
        pose.translate(0, 0.03F + (entity.getId() % 10) * 0.003F, 0);
        pose.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(entity.getCardRotation()), 0, 1, 0)));
        pose.mulPose(new Quaternionf(new AxisAngle4f((float) -Math.PI / 2, 1, 0, 0)));
        float hw = 0.45F, hh = 0.62F;
        // 牌面左上角显示花色+点数
        if (entity.isFaceUp() && info != null && !info.startsWith("武将:") && !isBox) {
            String[] parts = info.split("\\|");
            String sym = parts.length > 1 ? suitSymbol(parts[1]) : "";
            String rk = parts.length > 2 ? parts[2] : "";
                        int sCol = (parts.length > 1 && (parts[1].contains("\u7ea2\u6843") || parts[1].contains("\u65b9\u5757"))) ? 0xFFFF5555 : 0xFFFFFFFF;
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            pose.pushPose();
            pose.translate(-0.32F, 0.28F, 0.02F);
            pose.scale(0.026F, -0.026F, 0.026F);
            String txt = sym + rk;
            org.joml.Matrix4f mtx = pose.last().pose();
            net.minecraft.client.gui.Font.DisplayMode disp = net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET;
            mc.font.drawInBatch(txt, 0, 0, sCol, true, mtx, buffer, disp, 0, light);
            pose.popPose();
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(tex));
        Matrix4f m = pose.last().pose();
        int overlay = OverlayTexture.NO_OVERLAY;
        vc.addVertex(m, -hw, -hh, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, -hw, hh, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, hw, hh, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, hw, -hh, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        pose.popPose();
    }

    /** 牌盒:3D 长方体(顶面牌背图案,侧面深色) */
    private void renderBox(CardEntity entity, PoseStack pose, MultiBufferSource buffer, int light, ResourceLocation tex) {
        pose.pushPose();
        pose.translate(0, 0.03F + (entity.getId() % 10) * 0.003F, 0);
        pose.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(entity.getCardRotation()), 0, 1, 0)));
        float hw = 0.4F, hh = 0.5F, hd = 0.25F; // 半宽/半高/半深(0.8x1.0x0.5 格方块)
        int overlay = OverlayTexture.NO_OVERLAY;
        // 顶面 + 底面(牌背图案)
        VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(tex));
        Matrix4f m = pose.last().pose();
        vc.addVertex(m, -hw, hh, -hd).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, -hw, hh, hd).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, hw, hh, hd).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        vc.addVertex(m, hw, hh, -hd).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 1.0F, 0.0F);
        // 底面
        vc.addVertex(m, -hw, 0.0F, -hd).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, -1.0F, 0.0F);
        vc.addVertex(m, hw, 0.0F, -hd).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, -1.0F, 0.0F);
        vc.addVertex(m, hw, 0.0F, hd).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, -1.0F, 0.0F);
        vc.addVertex(m, -hw, 0.0F, hd).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, -1.0F, 0.0F);
        // 四个侧面(深色)
        VertexConsumer side = buffer.getBuffer(RenderType.entityTranslucent(
                ResourceLocation.withDefaultNamespace("textures/block/gold_block.png")));
        Matrix4f sm = pose.last().pose();
        // 前 z+
        side.addVertex(sm, -hw, 0.0F, hd).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        side.addVertex(sm, -hw, hh, hd).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        side.addVertex(sm, hw, hh, hd).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        side.addVertex(sm, hw, 0.0F, hd).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, 1.0F);
        // 后 z-
        side.addVertex(sm, -hw, 0.0F, -hd).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, -1.0F);
        side.addVertex(sm, hw, 0.0F, -hd).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, -1.0F);
        side.addVertex(sm, hw, hh, -hd).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, -1.0F);
        side.addVertex(sm, -hw, hh, -hd).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(0.0F, 0.0F, -1.0F);
        // 左 x-
        side.addVertex(sm, -hw, 0.0F, -hd).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(-1.0F, 0.0F, 0.0F);
        side.addVertex(sm, -hw, hh, -hd).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(-1.0F, 0.0F, 0.0F);
        side.addVertex(sm, -hw, hh, hd).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(-1.0F, 0.0F, 0.0F);
        side.addVertex(sm, -hw, 0.0F, hd).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(-1.0F, 0.0F, 0.0F);
        // 右 x+
        side.addVertex(sm, hw, 0.0F, -hd).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(1.0F, 0.0F, 0.0F);
        side.addVertex(sm, hw, 0.0F, hd).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setOverlay(overlay).setLight(light).setNormal(1.0F, 0.0F, 0.0F);
        side.addVertex(sm, hw, hh, hd).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(1.0F, 0.0F, 0.0F);
        side.addVertex(sm, hw, hh, -hd).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setOverlay(overlay).setLight(light).setNormal(1.0F, 0.0F, 0.0F);
        pose.popPose();
    }

    private static String suitSymbol(String cn) {
        if (cn == null) return "";
        if (cn.contains("黑桃")) return "♠";
        if (cn.contains("红桃")) return "♥";
        if (cn.contains("梅花")) return "♣";
        if (cn.contains("方块")) return "♦";
        return cn;
    }

    @Override
    public ResourceLocation getTextureLocation(CardEntity entity) {
        return ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/card/back.png");
    }
}