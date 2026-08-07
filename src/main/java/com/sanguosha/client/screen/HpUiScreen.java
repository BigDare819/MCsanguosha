package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import com.sanguosha.client.ModKeybinds;
import com.sanguosha.network.ActionPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * X 键开启的血量面板:集中显示血量 + 血量上限,用 UI 按钮控制加减(无键盘加减键位)。
 * 血量 -/+ 与上限 -/+ 四个按钮,点击发 ActionPacket 到服务端,同步后实时刷新。
 * X/ESC 关闭。
 */
public class HpUiScreen extends Screen {
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 170;

    public HpUiScreen() {
        super(Component.literal("血量面板"));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        int pw = PANEL_W, ph = PANEL_H;
        int px = width / 2 - pw / 2, py = height / 2 - ph / 2;
        int btnW = 24, btnH = 20;
        // 血量行:[-] 血量 [-]  (按钮在面板内,文字渲染时居中于两按钮之间)
        addRenderableWidget(Button.builder(Component.literal("-"), b -> sendAction(ActionPacket.HP_DOWN))
                .bounds(px + 40, py + 42, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> sendAction(ActionPacket.HP_UP))
                .bounds(px + pw - 40 - btnW, py + 42, btnW, btnH).build());
        // 上限行:[-] 血量上限 [+]
        addRenderableWidget(Button.builder(Component.literal("-"), b -> sendAction(ActionPacket.MAX_HP_DOWN))
                .bounds(px + 40, py + 64, btnW, btnH).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> sendAction(ActionPacket.MAX_HP_UP))
                .bounds(px + pw - 40 - btnW, py + 64, btnW, btnH).build());
    }

    private static void sendAction(String action) {
        PacketDistributor.sendToServer(new ActionPacket(action, 0, 0, false, ""));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // X 或 ESC 关闭面板
        if (keyCode == ModKeybinds.OPEN_HP_UI.getKey().getValue() || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
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
        int pw = PANEL_W, ph = PANEL_H;
        int px = w / 2 - pw / 2, py = h / 2 - ph / 2;
        g.fill(px, py, px + pw, py + ph, 0xCC000000);

        // 标题
        String title = "♥ 血量面板";
        int tw = mc.font.width(title);
        g.drawString(mc.font, title, w / 2 - tw / 2, py + 12, 0xFFE8C15A);

        // 当前玩家血量 + 血量上限(居中于两按钮之间)
        String name = mc.player.getName().getString();
        int hp = ClientGameState.HP_MAP.getOrDefault(name, 4);
        int maxHp = ClientGameState.MAX_HP_MAP.getOrDefault(name, 4);
        int center = px + pw / 2;
        String hpText = "血量: " + hp;
        String maxText = "血量上限: " + maxHp;
        g.drawString(mc.font, hpText, center - mc.font.width(hpText) / 2, py + 46, 0xFFFF5555);
        g.drawString(mc.font, maxText, center - mc.font.width(maxText) / 2, py + 68, 0xFF55AFFF);

        // 提示(关闭键名动态读取,跟随玩家改绑)
        String[] tips = {
            "点击 - / + 按钮调整血量与上限",
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
