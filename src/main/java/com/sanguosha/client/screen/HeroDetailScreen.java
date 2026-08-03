package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 武将详情弹窗(右键将牌查看:势力/体力/技能名与描述) */
public class HeroDetailScreen extends Screen {
    private final ClientGameState.CHero hero;
    private final List<String> lines = new ArrayList<>();

    public HeroDetailScreen(ClientGameState.CHero hero) {
        super(Component.literal("武将详情"));
        this.hero = hero;
        lines.add(hero.name + " · " + factionCn(hero.faction) + " · 体力 " + hero.maxHp);
        lines.add(" ");
        for (ClientGameState.CSkill s : hero.skills) {
            lines.add("【" + s.name + "】");
            lines.add(s.desc);
            lines.add(" ");
        }
        if (hero.skills.isEmpty()) {
            lines.add("(无技能)");
        }
        lines.add(" ");
        lines.add("点击任意处关闭");
    }

    private static String factionCn(String f) {
        return switch (f) {
            case "WEI" -> "魏";
            case "SHU" -> "蜀";
            case "WU" -> "吴";
            case "QUN" -> "群";
            default -> f;
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int w = Math.min(300, this.width - 40);
        int h = Math.min(30 + lines.size() * 10, this.height - 40);
        int x = (this.width - w) / 2, y = (this.height - h) / 2;
        g.fill(x, y, x + w, y + h, 0xF0201810);
        g.fill(x, y, x + w, y + 1, 0xFFC8AA6E);
        g.fill(x, y + h - 1, x + w, y + h, 0xFFC8AA6E);
        int ty = y + 12;
        for (int i = 0; i < lines.size() && ty < y + h - 8; i++) {
            String line = lines.get(i);
            int color = 0xFFE8D9B0;
            if (line.startsWith("【")) color = 0xFFFFD700;
            else if (line.contains("·")) color = 0xFFFFFFFF;
            else if (line.equals(" ") || line.equals("点击任意处关闭")) color = 0xFF888888;
            g.drawString(this.font, line, x + 12, ty, color);
            ty += 10;
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