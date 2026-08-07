package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import com.sanguosha.client.ModKeybinds;
import com.sanguosha.network.ActionPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * X 键开启的血量面板:集中显示血量 + 血量上限。
 * V/B 血量±1,F/G 上限±1(键位跟随按键绑定,可任意更改),X/ESC 关闭。
 */
public class HpUiScreen extends Screen {
    public HpUiScreen() {
        super(Component.literal("血量面板"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // X 或 ESC 关闭面板
        if (keyCode == ModKeybinds.OPEN_HP_UI.getKey().getValue() || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == ModKeybinds.HP_UP.getKey().getValue()) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.HP_UP, 0, 0, false, ""));
            return true;
        }
        if (keyCode == ModKeybinds.HP_DOWN.getKey().getValue()) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.HP_DOWN, 0, 0, false, ""));
            return true;
        }
        if (keyCode == ModKeybinds.MAX_HP_UP.getKey().getValue()) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.MAX_HP_UP, 0, 0, false, ""));
            return true;
        }
        if (keyCode == ModKeybinds.MAX_HP_DOWN.getKey().getValue()) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.MAX_HP_DOWN, 0, 0, false, ""));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        int w = g.guiWidth(), h = g.guiHeight();

        // 半透明遮罩
        g.fill(0, 0, w, h, 0x66000000);

        // 中央面板
        int pw = 300, ph = 170;
        int px = w / 2 - pw / 2, py = h / 2 - ph / 2;
        g.fill(px, py, px + pw, py + ph, 0xCC000000);

        // 标题
        String title = "♥ 血量面板";
        int tw = mc.font.width(title);
        g.drawString(mc.font, title, w / 2 - tw / 2, py + 12, 0xFFE8C15A);

        // 当前玩家血量 + 血量上限(血量在前,上限紧跟其后)
        String name = mc.player.getName().getString();
        int hp = ClientGameState.HP_MAP.getOrDefault(name, 4);
        int maxHp = ClientGameState.MAX_HP_MAP.getOrDefault(name, 4);
        String hpText = "血量: " + hp;
        String maxText = "血量上限: " + maxHp;
        int htw = mc.font.width(hpText);
        int mtw = mc.font.width(maxText);
        g.drawString(mc.font, hpText, w / 2 - htw / 2, py + 50, 0xFFFF5555);
        g.drawString(mc.font, maxText, w / 2 - mtw / 2, py + 72, 0xFF55AFFF);

        // 按键提示(键名动态读取,跟随玩家改绑)
        String[] tips = {
            keyName(ModKeybinds.HP_UP) + "/" + keyName(ModKeybinds.HP_DOWN) + ": 血量±1",
            keyName(ModKeybinds.MAX_HP_UP) + "/" + keyName(ModKeybinds.MAX_HP_DOWN) + ": 血量上限±1",
            keyName(ModKeybinds.OPEN_HP_UI) + "/ESC: 关闭"
        };
        int ty = py + 100;
        for (String tip : tips) {
            int tipw = mc.font.width(tip);
            g.drawString(mc.font, tip, w / 2 - tipw / 2, ty, 0xFFAAAAAA);
            ty += 12;
        }
    }

    private static String keyName(KeyMapping km) {
        var k = km.getKey();
        if (k == com.mojang.blaze3d.platform.InputConstants.UNKNOWN) return "未绑定";
        return k.getDisplayName().getString();
    }
}
