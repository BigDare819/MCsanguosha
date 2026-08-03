package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import net.minecraft.client.Minecraft;
import com.sanguosha.network.ActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** 选将界面:从 3 名武将中选择 1 名 */
public class HeroSelectScreen extends Screen {
    private static final int CARD_W = 110;
    private static final int CARD_H = 160;

    public HeroSelectScreen() {
        super(Component.literal("选择武将"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0101010);
        List<ClientGameState.CHero> opts = ClientGameState.heroOptions;
        if (opts.isEmpty()) {
            g.drawCenteredString(this.font, "等待服务器分配武将...", this.width / 2, this.height / 2, 0xFFFFFFFF);
            return;
        }
        g.drawCenteredString(this.font, "请选择你的武将", this.width / 2, 20, 0xFFFFD700);
        int totalW = opts.size() * (CARD_W + 20) - 20;
        int startX = (this.width - totalW) / 2;
        int y = this.height / 2 - CARD_H / 2 - 20;
        for (int i = 0; i < opts.size(); i++) {
            ClientGameState.CHero h = opts.get(i);
            int x = startX + i * (CARD_W + 20);
            // 卡片背景
            g.fill(x, y, x + CARD_W, y + CARD_H, 0xFF2B2B2B);
            int facColor = switch (h.faction) {
                case "WEI" -> 0xFFCC4444; case "SHU" -> 0xFF44CC44;
                case "WU" -> 0xFF4488CC; default -> 0xFFCCCC44;
            };
            g.fill(x, y, x + CARD_W, y + 4, facColor);
            // 势力+体力
            String facCn = switch (h.faction) {
                case "WEI" -> "魏"; case "SHU" -> "蜀"; case "WU" -> "吴"; default -> "群";
            };
            g.drawCenteredString(this.font, facCn + " · 体力 " + h.maxHp, x + CARD_W / 2, y + 10, 0xFFAAAAAA);
            // 名字
            // 武将立绘贴图
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/hero/" + h.id + ".png");
            if (Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()) {
                g.blit(tex, x + 5, y + 22, CARD_W - 10, CARD_H - 52, 0, 0, 1, 1, 1, 1);
            } else {
                g.drawCenteredString(this.font, h.name, x + CARD_W / 2, y + CARD_H / 2, 0xFFFFFFFF);
            }
            // 提示
            g.drawCenteredString(this.font, "点击选择", x + CARD_W / 2, y + CARD_H - 20, 0xFF88CC88);
            // 悬停边框
            if (mouseX >= x && mouseX <= x + CARD_W && mouseY >= y && mouseY <= y + CARD_H) {
                g.fill(x - 2, y - 2, x + CARD_W + 2, y, 0xFFFFFF00);
                g.fill(x - 2, y + CARD_H, x + CARD_W + 2, y + CARD_H + 2, 0xFFFFFF00);
                g.fill(x - 2, y, x, y + CARD_H, 0xFFFFFF00);
                g.fill(x + CARD_W, y, x + CARD_W + 2, y + CARD_H, 0xFFFFFF00);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<ClientGameState.CHero> opts = ClientGameState.heroOptions;
        int totalW = opts.size() * (CARD_W + 20) - 20;
        int startX = (this.width - totalW) / 2;
        int y = this.height / 2 - CARD_H / 2 - 20;
        for (int i = 0; i < opts.size(); i++) {
            int x = startX + i * (CARD_W + 20);
            if (mouseX >= x && mouseX <= x + CARD_W && mouseY >= y && mouseY <= y + CARD_H) {
                if (button == 1) {
                    // 右键:查看武将详情(技能)
                    Minecraft.getInstance().setScreen(new HeroDetailScreen(opts.get(i)));
                } else {
                    PacketDistributor.sendToServer(ActionPacket.hero(opts.get(i).id));
                    Minecraft.getInstance().setScreen(null);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}