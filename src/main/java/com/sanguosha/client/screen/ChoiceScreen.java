package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import com.sanguosha.network.ActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** 通用选择弹窗:花色猜测(反间)/鬼才改判/流离/观星等 */
public class ChoiceScreen extends Screen {
    private final String prompt;
    private final List<String> options;

    public ChoiceScreen(String prompt, List<String> options) {
        super(Component.literal("选择"));
        this.prompt = prompt;
        this.options = options;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xA0100A04);
        g.drawCenteredString(this.font, prompt, this.width / 2, this.height / 2 - 60, 0xFFFFD700);
        if (options.isEmpty()) {
            g.drawCenteredString(this.font, "等待...", this.width / 2, this.height / 2, 0xFFAAAAAA);
            return;
        }
        int bw = 150, bh = 30;
        int startY = this.height / 2 - (options.size() * (bh + 10)) / 2;
        for (int i = 0; i < options.size(); i++) {
            int y = startY + i * (bh + 10);
            boolean hover = mouseX >= this.width / 2 - bw / 2 && mouseX <= this.width / 2 + bw / 2
                    && mouseY >= y && mouseY <= y + bh;
            g.fill(this.width / 2 - bw / 2, y, this.width / 2 + bw / 2, y + bh, hover ? 0xFF5A4A28 : 0xFF3A3020);
            g.fill(this.width / 2 - bw / 2, y, this.width / 2 + bw / 2, y + 1, 0xFFC8AA6E);
            g.drawCenteredString(this.font, options.get(i), this.width / 2, y + (bh - 8) / 2, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bw = 150, bh = 30;
        int startY = this.height / 2 - (options.size() * (bh + 10)) / 2;
        for (int i = 0; i < options.size(); i++) {
            int y = startY + i * (bh + 10);
            if (mouseX >= this.width / 2 - bw / 2 && mouseX <= this.width / 2 + bw / 2
                    && mouseY >= y && mouseY <= y + bh) {
                PacketDistributor.sendToServer(new ActionPacket(ActionPacket.CHOICE, i, -1, false, ""));
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}