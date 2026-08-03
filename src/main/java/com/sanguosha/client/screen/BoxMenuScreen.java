package com.sanguosha.client.screen;

import com.sanguosha.network.ActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** 牌盒/将盒上级界面:查看剩余 / 观星(仅牌盒)入口 */
public class BoxMenuScreen extends Screen {
    private final int posX, posY, posZ;
    private final String type;
    private final List<String> remainNames;

    public BoxMenuScreen(int posX, int posY, int posZ, String type, List<String> remainNames) {
        super(Component.literal("\u724c\u76d2\u83dc\u5355"));
        this.posX = posX; this.posY = posY; this.posZ = posZ;
        this.type = type;
        this.remainNames = remainNames;
    }

    private boolean remainBtnHit(double mx, double my) {
        return mx >= this.width / 2 - 80 && mx <= this.width / 2 + 80
                && my >= this.height / 2 - 50 && my <= this.height / 2 - 26;
    }

    private boolean guanxingBtnHit(double mx, double my) {
        return mx >= this.width / 2 - 80 && mx <= this.width / 2 + 80
                && my >= this.height / 2 - 10 && my <= this.height / 2 + 14;
    }

    private boolean backBtnHit(double mx, double my) {
        return mx >= this.width / 2 - 80 && mx <= this.width / 2 + 80
                && my >= this.height / 2 + 30 && my <= this.height / 2 + 54;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0100A04);
        boolean hero = "hero".equals(type);
        String title = hero ? "\u5c06\u76d2" : "\u724c\u76d2";
        g.drawCenteredString(this.font, title + " \u83dc\u5355", this.width / 2, 24, 0xFFFFD700);

        boolean rh = remainBtnHit(mouseX, mouseY);
        g.fill(this.width / 2 - 80, this.height / 2 - 50, this.width / 2 + 80, this.height / 2 - 26, rh ? 0xFF6A5A2A : 0xFF3A3020);
        g.drawCenteredString(this.font, "\u67e5\u770b\u5269\u4f59", this.width / 2, this.height / 2 - 45, 0xFFFFFFFF);

        if (!hero) {
            boolean gh = guanxingBtnHit(mouseX, mouseY);
            g.fill(this.width / 2 - 80, this.height / 2 - 10, this.width / 2 + 80, this.height / 2 + 14, gh ? 0xFF6A5A2A : 0xFF3A3020);
            g.drawCenteredString(this.font, "\u89c2\u661f(\u8bf8\u845b\u4eae)", this.width / 2, this.height / 2 - 5, 0xFFFFFFFF);
        }

        boolean bh = backBtnHit(mouseX, mouseY);
        g.fill(this.width / 2 - 80, this.height / 2 + 30, this.width / 2 + 80, this.height / 2 + 54, bh ? 0xFF6A5A2A : 0xFF3A3020);
        g.drawCenteredString(this.font, "\u8fd4\u56de\u6e38\u620f", this.width / 2, this.height / 2 + 35, 0xFFFFFFFF);

        g.drawCenteredString(this.font, "ESC \u5173\u95ed", this.width / 2, this.height - 30, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (remainBtnHit(mouseX, mouseY)) {
            // 打开剩余界面
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new RemainBoxScreen(posX, posY, posZ, type, remainNames));
            return true;
        }
        if (!"hero".equals(type) && guanxingBtnHit(mouseX, mouseY)) {
            // 先选观星牌数,再请求牌堆顶
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new GuanXingCountScreen(posX, posY, posZ));
            return true;
        }
        if (backBtnHit(mouseX, mouseY)) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
