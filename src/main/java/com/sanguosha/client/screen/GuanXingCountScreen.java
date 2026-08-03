package com.sanguosha.client.screen;

import com.sanguosha.network.ActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** 观星牌数选择子界面:选择看牌堆顶几张(1~5,诸葛亮观星 X 张规则) */
public class GuanXingCountScreen extends Screen {
    private final int posX, posY, posZ;
    private static final int MAX = 5;

    public GuanXingCountScreen(int posX, int posY, int posZ) {
        super(Component.literal("\u89c2\u661f\u6570\u91cf"));
        this.posX = posX; this.posY = posY; this.posZ = posZ;
    }

    /** 第 n(1~5)个数量按钮命中检测 */
    private boolean countBtnHit(double mx, double my, int n) {
        int cx = this.width / 2;
        int y = this.height / 2 - 60 + (n - 1) * 30;
        return mx >= cx - 60 && mx <= cx + 60 && my >= y && my <= y + 22;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0100A04);
        g.drawCenteredString(this.font, "\u89c2\u661f - \u9009\u62e9\u770b\u724c\u5806\u9876\u51e0\u5f20", this.width / 2, 24, 0xFFFFD700);
        int cx = this.width / 2;
        for (int n = 1; n <= MAX; n++) {
            int y = this.height / 2 - 60 + (n - 1) * 30;
            boolean h = countBtnHit(mouseX, mouseY, n);
            g.fill(cx - 60, y, cx + 60, y + 22, h ? 0xFF6A5A2A : 0xFF3A3020);
            g.drawCenteredString(this.font, n + " \u5f20", cx, y + 6, 0xFFFFFFFF);
        }
        g.drawCenteredString(this.font, "ESC \u53d6\u6d88", cx, this.height - 30, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int n = 1; n <= MAX; n++) {
            if (countBtnHit(mouseX, mouseY, n)) {
                // 带数量请求观星:"x,y,z,deck|n"
                String enc = posX + "," + posY + "," + posZ + ",deck|" + n;
                PacketDistributor.sendToServer(new ActionPacket(ActionPacket.GUANXING_VIEW, 0, -1, false, enc));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
