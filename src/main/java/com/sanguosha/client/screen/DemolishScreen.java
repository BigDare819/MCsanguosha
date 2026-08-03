package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import com.sanguosha.network.ActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** 过河拆桥/顺手牵羊:选择目标玩家 + 选择要拆/顺的手牌 */
public class DemolishScreen extends Screen {
    private final String cardName;
    private final List<String> playerNames = new ArrayList<>();
    private String selectedPlayer = null;

    public DemolishScreen(String cardName) {
        super(Component.literal(cardName + " - 选择目标"));
        this.cardName = cardName;
        String me = Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getName().getString();
        for (String n : ClientGameState.HP_MAP.keySet()) {
            if (!n.equals(me)) playerNames.add(n);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        String verb = cardName.contains("过河") ? "拆" : "顺";
        String title = selectedPlayer == null ? cardName + " - 选择目标玩家" : cardName + " - 选择要" + verb + "的手牌";
        g.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        int y = 50;
        if (selectedPlayer == null) {
            if (playerNames.isEmpty()) {
                g.drawCenteredString(font, "没有其他玩家!", width / 2, y, 0xFFE04040);
                return;
            }
            for (String n : playerNames) {
                int hc = ClientGameState.HAND_MAP.getOrDefault(n, 0);
                int col = (mx > width/2 - 100 && mx < width/2 + 100 && my >= y - 8 && my <= y + 8) ? 0xFFFFFF00 : 0xFFFFAA00;
                g.drawCenteredString(font, n + " (手牌" + hc + ")", width / 2, y, col);
                y += 22;
            }
        } else {
            int hc = ClientGameState.HAND_MAP.getOrDefault(selectedPlayer, 0);
            if (hc <= 0) {
                g.drawCenteredString(font, "目标没有手牌,无法" + verb + "!", width / 2, y, 0xFFE04040);
                return;
            }
            for (int i = 0; i < hc; i++) {
                int col = (mx > width/2 - 100 && mx < width/2 + 100 && my >= y - 8 && my <= y + 8) ? 0xFFFFFF00 : 0xFFFFFFFF;
                g.drawCenteredString(font, "第" + (i + 1) + "张 (背面)", width / 2, y, col);
                y += 22;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int y = 50;
        if (selectedPlayer == null) {
            for (String n : playerNames) {
                if (my >= y - 8 && my <= y + 8 && mx > width/2 - 100 && mx < width/2 + 100) {
                    selectedPlayer = n;
                    return true;
                }
                y += 22;
            }
        } else {
            int hc = ClientGameState.HAND_MAP.getOrDefault(selectedPlayer, 0);
            for (int i = 0; i < hc; i++) {
                if (my >= y - 8 && my <= y + 8 && mx > width/2 - 100 && mx < width/2 + 100) {
                    boolean isShun = cardName.contains("顺手");
                    PacketDistributor.sendToServer(new ActionPacket(ActionPacket.DEMOLISH, i, 0, isShun, selectedPlayer));
                    this.onClose();
                    return true;
                }
                y += 22;
            }
        }
        return super.mouseClicked(mx, my, button);
    }
}