package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import com.sanguosha.network.ActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** 准备大厅:显示已加入的玩家(等待开始) */
public class LobbyScreen extends Screen {
    public LobbyScreen() {
        super(Component.literal("三国杀大厅"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        // 古风背景
        g.fill(0, 0, this.width, this.height, 0xFFB5A68D);
        g.fill(0, 0, this.width, this.height, 0x24000000);
        g.fill(0, 0, this.width, 6, 0x55302820);
        g.fill(0, this.height - 6, this.width, this.height, 0x55302820);

        g.drawCenteredString(this.font, "三 国 杀 · 2v2 大 厅", this.width / 2, 34, 0xFF3A2A08);
        int count = ClientGameState.players.size();
        g.drawCenteredString(this.font, "已加入: " + count + " / 4", this.width / 2, 62, 0xFF5A4A28);

        String[] seats = {"一号位", "二号位", "三号位", "四号位"};
        int cw = Math.min(260, this.width - 60), ch = 48;
        int startY = 100;
        for (int s = 0; s < 4; s++) {
            int x = (this.width - cw) / 2;
            int y = startY + s * (ch + 12);
            ClientGameState.CPlayer p = null;
            for (ClientGameState.CPlayer q : ClientGameState.players) {
                if (q.seat == s) { p = q; break; }
            }
            g.fill(x, y, x + cw, y + ch, 0xFF3A3228);
            g.fill(x, y, x + cw, y + 1, 0xFFC8AA6E);
            g.fill(x, y, x + 2, y + ch, 0xFFC8AA6E);
            g.drawString(this.font, seats[s], x + 12, y + 9, 0xFFC8AA6E);
            if (p != null) {
                boolean red = "RED".equals(p.team);
                String team = red ? "龙队" : "蓝队";
                int tc = red ? 0xFFCC4444 : 0xFF4488CC;
                g.drawString(this.font, p.name, x + 80, y + 8, 0xFFFFFFFF);
                g.drawString(this.font, "[" + team + "]", x + 80 + this.font.width(p.name) + 8, y + 8, tc);
                g.drawString(this.font, p.hero.isEmpty() ? "尚未选将" : "武将: " + p.hero, x + 80, y + 28, 0xFFFFD700);
            } else {
                g.drawString(this.font, "空位(等待加入)", x + 80, y + 16, 0xFF888888);
            }
        }
        g.drawCenteredString(this.font, "输入 /sanguosha join 加入游戏", this.width / 2, this.height - 56, 0xFFE8D9B0);
        // 开始按钮
        int bx = (this.width - 160) / 2, by = this.height - 106;
        boolean hover = mouseX >= bx && mouseX <= bx + 160 && mouseY >= by && mouseY <= by + 36;
        g.fill(bx, by, bx + 160, by + 36, hover ? 0xFFE8C84A : 0xFFC9A227);
        g.fill(bx, by, bx + 160, by + 2, 0xFFFFE98A);
        g.fill(bx, by + 34, bx + 160, by + 36, 0xFF8A6D1F);
        g.drawCenteredString(this.font, "⚔ 开始游戏", this.width / 2, by + 13, 0xFF3A2A08);
        g.drawCenteredString(this.font, "开始后将自动进入选将", this.width / 2, this.height - 30, 0xFF888888);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int bx = (this.width - 160) / 2, by = this.height - 106;
        if (mouseX >= bx && mouseX <= bx + 160 && mouseY >= by && mouseY <= by + 36) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.START, -1, -1, false, ""));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}