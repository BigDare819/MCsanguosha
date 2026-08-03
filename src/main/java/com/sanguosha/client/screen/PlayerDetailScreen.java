package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 对局中右键玩家面板:查看武将属性/装备/技能 */
public class PlayerDetailScreen extends Screen {
    private final ClientGameState.CPlayer p;
    private final List<String> lines = new ArrayList<>();

    public PlayerDetailScreen(ClientGameState.CPlayer p) {
        super(Component.literal("武将属性"));
        this.p = p;
        String team = "RED".equals(p.team) ? "龙队" : "蓝队";
        lines.add(p.name + " · " + team + " · 座位" + (p.seat + 1));
        lines.add("武将: " + (p.hero.isEmpty() ? "未选将" : p.hero) + "  体力 " + p.hp + "/" + p.maxHp + "  手牌 " + p.handCount);
        StringBuilder eq = new StringBuilder("装备: ");
        if (!p.weapon.isEmpty()) eq.append(p.weapon);
        if (!p.armor.isEmpty()) eq.append(" ").append(p.armor);
        if (!p.horsePlus.isEmpty()) eq.append(" ").append(p.horsePlus);
        if (!p.horseMinus.isEmpty()) eq.append(" ").append(p.horseMinus);
        if (p.weapon.isEmpty() && p.armor.isEmpty() && p.horsePlus.isEmpty() && p.horseMinus.isEmpty()) eq.append("无");
        lines.add(eq.toString());
        lines.add(p.chained ? "状态: 横置(铁索连环)" : "状态: 正常");
        lines.add(" ");
        lines.add("—— 技能 ——");
        if (p.skills.isEmpty()) {
            lines.add("(无)");
        } else {
            for (String s : p.skills) {
                int sep = s.indexOf('\u0001');
                if (sep > 0) {
                    lines.add("【" + s.substring(0, sep) + "】");
                    lines.add(s.substring(sep + 1));
                } else {
                    lines.add("【" + s + "】");
                }
            }
        }
        lines.add(" ");
        lines.add("点击任意处关闭");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int w = Math.min(300, this.width - 40);
        int h = Math.min(40 + lines.size() * 11, this.height - 40);
        int x = (this.width - w) / 2, y = (this.height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xF0201810);
        g.fill(x, y, x + w, y + 1, 0xFFC8AA6E);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFC8AA6E);
        int ty = y + 12;
        for (int i = 0; i < lines.size() && ty < y + h - 8; i++) {
            String line = lines.get(i);
            int color = 0xFFE8D9B0;
            if (line.startsWith("——")) color = 0xFFC8AA6E;
            else if (line.startsWith("【")) color = 0xFFFFD700;
            else if (line.contains("·")) color = 0xFFFFFFFF;
            else if (line.equals(" ") || line.equals("点击任意处关闭")) color = 0xFF888888;
            g.drawString(this.font, line, x + 12, ty, color);
            ty += 11;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}