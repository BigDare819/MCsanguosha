package com.sanguosha.client.screen;

import com.sanguosha.network.ActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** 清除卡牌确认界面:V 键触发,防止误触清空桌面(确认后才发 CLEAR_CARDS) */
public class ClearConfirmScreen extends Screen {
    private static final int BTN_W = 120, BTN_H = 30;

    public ClearConfirmScreen() {
        super(Component.literal("\u6e05\u9664\u786e\u8ba4")); // 清除确认
    }

    private boolean confirmHit(double mx, double my) {
        return mx >= this.width / 2 - BTN_W - 10 && mx <= this.width / 2 - 10
                && my >= this.height / 2 - BTN_H / 2 && my <= this.height / 2 + BTN_H / 2;
    }

    private boolean cancelHit(double mx, double my) {
        return mx >= this.width / 2 + 10 && mx <= this.width / 2 + BTN_W + 10
                && my >= this.height / 2 - BTN_H / 2 && my <= this.height / 2 + BTN_H / 2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0100A04);
        g.drawCenteredString(this.font, "\u786e\u8ba4\u6e05\u9664\u5730\u4e0a\u6240\u6709\u5361\u724c?", this.width / 2, this.height / 2 - 60, 0xFFFFD700);
        // 确认清除按钮(红)
        boolean ch = confirmHit(mouseX, mouseY);
        g.fill(this.width / 2 - BTN_W - 10, this.height / 2 - BTN_H / 2, this.width / 2 - 10, this.height / 2 + BTN_H / 2, ch ? 0xFFAA3A2A : 0xFF8A2A1A);
        g.drawCenteredString(this.font, "\u786e\u8ba4\u6e05\u9664", this.width / 2 - BTN_W / 2 - 10, this.height / 2 - 8, 0xFFFFFFFF);
        // 取消按钮
        boolean bh = cancelHit(mouseX, mouseY);
        g.fill(this.width / 2 + 10, this.height / 2 - BTN_H / 2, this.width / 2 + BTN_W + 10, this.height / 2 + BTN_H / 2, bh ? 0xFF4A4A5A : 0xFF3A3A4A);
        g.drawCenteredString(this.font, "\u53d6\u6d88", this.width / 2 + BTN_W / 2 + 10, this.height / 2 - 8, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "ESC \u53d6\u6d88", this.width / 2, this.height - 30, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmHit(mouseX, mouseY)) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.CLEAR_CARDS, 0, 0, false, ""));
            this.onClose();
            return true;
        }
        if (cancelHit(mouseX, mouseY)) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
