package com.sanguosha.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sanguosha.item.CardData;
import com.sanguosha.item.CardModelIds;
import com.sanguosha.item.ModItems;
import com.sanguosha.network.ActionPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** 实体卡牌 HUD:右上角血量 + 炉石式手牌(替代物品栏),H 开关,J/K 血量,滚轮/数字键选牌 */
@EventBusSubscriber(modid = "sanguosha", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class SanguoshaHud {
    private SanguoshaHud() {}

    /** 按键:J=血+1 K=血-1 H=开关 数字1-9=选牌 */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (ModKeybinds.HP_UP.isDown()) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.HP_UP, 0, 0, false, ""));
        } else if (ModKeybinds.HP_DOWN.isDown()) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.HP_DOWN, 0, 0, false, ""));
        } else if (ModKeybinds.TOGGLE_UI.isDown()) {
            ClientGameState.showOverlay = !ClientGameState.showOverlay;
            com.sanguosha.SanguoshaMod.LOGGER.info("[UI] H toggled -> {}", ClientGameState.showOverlay);
        } else if (event.getKey() >= GLFW.GLFW_KEY_1 && event.getKey() <= GLFW.GLFW_KEY_9 && ClientGameState.showOverlay) {
            ClientGameState.selectedHand = event.getKey() - GLFW.GLFW_KEY_1;
        } else if (ModKeybinds.PLACE_CARD.isDown() && ClientGameState.showOverlay && ClientGameState.selectedHand >= 0) {
            // 过河拆桥/顺手牵羊:先放到桌上,再弹选择界面
            String selInfo = handInfoAt(mc, ClientGameState.selectedHand);
            if (selInfo != null && (selInfo.startsWith("过河拆桥") || selInfo.startsWith("顺手牵羊"))) {
                PacketDistributor.sendToServer(new ActionPacket(ActionPacket.PLACE_CARD, ClientGameState.selectedHand, 0, false, ""));
                mc.setScreen(new com.sanguosha.client.screen.DemolishScreen(selInfo.split("\\|")[0]));
            } else {
                PacketDistributor.sendToServer(new ActionPacket(ActionPacket.PLACE_CARD, ClientGameState.selectedHand, 0, false, ""));
            }
        } else if (ModKeybinds.DROP_CARD.isDown() && ClientGameState.showOverlay && ClientGameState.selectedHand >= 0) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.DROP_CARD, ClientGameState.selectedHand, 0, false, ""));
        } else if (ModKeybinds.CLEAR_CARDS.isDown()) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.CLEAR_CARDS, 0, 0, false, ""));
        }
    }

    /** 隐藏原版物品栏层(炉石式替换) */
    @SubscribeEvent
    public static void onGuiLayer(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Pre event) {
        if (ClientGameState.showOverlay) {
            String path = event.getName().getPath();
            if (path.contains("hotbar") || path.contains("health") || path.contains("food") || path.contains("experience") || path.contains("air")) {
                event.setCanceled(true);
            }
        }
    }

    /** 鼠标滚轮选牌(HUD 模式下取消原版物品栏滚轮切换) */
    @SubscribeEvent
    public static void onMouseScroll(MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientGameState.showOverlay) return;
        int n = handCount(mc);
        if (n <= 0) return;
        int sel = ClientGameState.selectedHand;
        if (sel < 0 || sel >= n) sel = 0;
        int delta = event.getScrollDeltaY() > 0 ? -1 : 1; // 上滚=向左
        sel = Math.max(0, Math.min(n - 1, sel + delta));
        ClientGameState.selectedHand = sel;
        event.setCanceled(true); // 阻止原版热键槽切换
    }

    /**
     * HUD 模式下抢先消费原版数字键热键槽点击(Minecraft.handleKeybinds 之前触发),
     * 使 1-9 只用于选牌、不再切换原版物品栏选中槽。
     */
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !ClientGameState.showOverlay) return;
        for (int i = 0; i < 9; i++) {
            KeyMapping km = mc.options.keyHotbarSlots[i];
            if (km.isDown()) km.consumeClick();
        }
    }

    private static String handInfoAt(Minecraft mc, int index) {
        int found = 0;
        for (net.minecraft.world.item.ItemStack s : mc.player.getInventory().items) {
            if (s != null && !s.isEmpty() && s.is(com.sanguosha.item.ModItems.CARD.get())) {
                if (found == index) return s.get(com.sanguosha.item.CardData.CARD_INFO);
                found++;
            }
        }
        return null;
    }

    private static String suitSymbol(String cn) {
        if (cn == null) return "";
        if (cn.contains("黑桃")) return "♠";
        if (cn.contains("红桃")) return "♥";
        if (cn.contains("梅花")) return "♣";
        if (cn.contains("方块")) return "♦";
        return cn;
    }

    private static int handCount(Minecraft mc) {
        int n = 0;
        for (ItemStack s : mc.player.getInventory().items) {
            if (s != null && !s.isEmpty() && s.is(ModItems.CARD.get())) n++;
        }
        return n;
    }

    private static boolean hudLogged = false;

    /** 取 KeyMapping 当前绑定的键名(玩家改绑后自动跟随;未绑定时提示"未绑定") */
    private static String keyName(KeyMapping km) {
        InputConstants.Key k = km.getKey();
        if (k == InputConstants.UNKNOWN) return "未绑定";
        return k.getDisplayName().getString();
    }

    private static float lastPartial = -1.0F;
    private static boolean hpLogged = false;

    /** 任意层 Post 渲染 UI(帧标志保证每帧只画一次;hotbar 被取消所以不用它的 Post) */
    @SubscribeEvent
    public static void onGuiLayerPost(net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post event) {
        if (!ClientGameState.showOverlay) return;
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (pt == lastPartial) return;
        lastPartial = pt;
        renderOverlay(event.getGuiGraphics());
    }

    private static void renderOverlay(GuiGraphics g) {
        if (!hudLogged) {
            hudLogged = true;
            com.sanguosha.SanguoshaMod.LOGGER.info("[UI] renderOverlay firing");
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int w = g.guiWidth(), h = g.guiHeight();
        // 右上角血量
        if (!hpLogged) {
            hpLogged = true;
            com.sanguosha.SanguoshaMod.LOGGER.info("[UI] renderOverlay name={} HP_MAP={}", mc.player.getName().getString(), ClientGameState.HP_MAP);
        }
        // 左侧按键提示(键名从 KeyMapping 动态读取,跟随玩家改绑)
        String[] tips = {
            keyName(ModKeybinds.HP_UP) + "/" + keyName(ModKeybinds.HP_DOWN) + ": 血量±1",
            keyName(ModKeybinds.PLACE_CARD) + ": 放置选中牌",
            keyName(ModKeybinds.DROP_CARD) + ": 丢出选中牌",
            "1-9/滚轮: 选牌",
            keyName(ModKeybinds.CLEAR_CARDS) + ": 清理地上卡牌",
            keyName(ModKeybinds.TOGGLE_UI) + ": 关闭UI"
        };
        int ty = 10;
        for (String tip : tips) {
            g.fill(4, ty - 2, 4 + mc.font.width(tip) + 6, ty + 8, 0x88000000);
            g.drawString(mc.font, tip, 7, ty, 0xFFFFFFFF);
            ty += 11;
        }
        int myHp = ClientGameState.HP_MAP.getOrDefault(mc.player.getName().getString(), 4);
        String hpText = "♥ 血量: " + myHp;
        int tw = mc.font.width(hpText);
        g.fill(w - tw - 26, 8, w - 8, 26, 0xAA000000);
        g.drawString(mc.font, hpText, w - tw - 18, 12, 0xFFE8C15A);
        // 底部手牌(炉石式弧形,中间高两侧低)
        List<ItemStack> hand = new ArrayList<>();
        for (ItemStack s : mc.player.getInventory().items) {
            if (s != null && !s.isEmpty() && s.is(ModItems.CARD.get())) hand.add(s);
        }
        int n = hand.size();
        if (n == 0) return;
        if (ClientGameState.selectedHand < 0 || ClientGameState.selectedHand >= n) ClientGameState.selectedHand = 0;
        for (int i = 0; i < n; i++) {
            ItemStack s = hand.get(i);
            String info = s.get(CardData.CARD_INFO);
            String key;
            if (info != null && info.startsWith("武将:")) {
                key = "hero_" + info.split("\\|")[0].substring(3);
            } else if (info != null) {
                key = CardModelIds.keyOf(info.split("\\|")[0]);
            } else {
                key = "back";
            }
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/item/" + key + ".png");
            float t = n == 1 ? 0.5F : (float) i / (n - 1);
            float ang = (t - 0.5F) * 1.2F; // 弧度:中间0,两侧±
            boolean sel = i == ClientGameState.selectedHand;
            float anim = (float) Math.sin(mc.level.getGameTime() * 0.15F);
            int cw = sel ? 48 : 44, ch = sel ? 66 : 60;
            int x = w / 2 + (int) ((t - 0.5F) * (n * 30)) - cw / 2;
            int y = h - 18 - (int) ((1.0F - Math.abs(ang)) * 52) - ch; // 中间高两侧低
            if (sel) y -= 10 + (int) (anim * 4); // 选中呼吸上浮
            g.pose().pushPose();
            g.pose().translate(x + cw / 2.0F, y + ch / 2.0F, 0);
            g.pose().mulPose(new Quaternionf().rotationZ(ang));
            g.pose().translate(-cw / 2.0F, -ch / 2.0F, 0);
            if (sel) {
                // 呼吸高亮:金色 ↔ 亮白脉动
                int glowR = 212 + (int) (anim * 30);
                int glowG = 175 + (int) (anim * 50);
                int glowB = 55 + (int) (anim * 40);
                int glowCol = (0xFF << 24) | (glowR << 16) | (glowG << 8) | glowB;
                g.fill(-3, -3, cw + 3, ch + 3, glowCol);
                g.fill(-1, -1, cw + 1, ch + 1, 0xFF000000);
            }
            g.blit(tex, 0, 0, cw, ch, 0, 0, 1, 1, 1, 1);
            // 牌上显示花色+点数(放大 1.8 倍 + 黑色描边 + 亮色,保证醒目)
            if (info != null && !info.startsWith("武将:")) {
                String[] parts = info.split("\\|");
                String sym = suitSymbol(parts[1]);
                int sCol = (parts[1].contains("红桃") || parts[1].contains("方块")) ? 0xFFFF5555 : 0xFFFFFFFF;
                String txt = sym + parts[2];
                g.pose().pushPose();
                g.pose().translate(4, 4, 0);
                g.drawString(mc.font, txt, -1, -1, 0xFF000000);
                g.drawString(mc.font, txt, 1, -1, 0xFF000000);
                g.drawString(mc.font, txt, -1, 1, 0xFF000000);
                g.drawString(mc.font, txt, 1, 1, 0xFF000000);
                g.drawString(mc.font, txt, 0, 0, sCol);
                g.pose().popPose();
            }
            g.pose().popPose();
        }
    }
}