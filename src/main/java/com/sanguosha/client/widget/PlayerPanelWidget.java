package com.sanguosha.client.widget;

import com.sanguosha.client.ClientGameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 武将卡风格玩家面板(反哺网页版 UI):
 * 势力色条 / 竖排武将名 / 体力珠 / 阵营角标 / 装备标记 / 铁索特效 / 出牌绿色呼吸高亮
 */
public final class PlayerPanelWidget {
    private PlayerPanelWidget() {}

    public static int weaponRange(String weapon) {
        return switch (weapon) {
            case "诸葛连弩" -> 1;
            case "青釭剑", "雌雄双股剑", "丈八蛇矛", "古锭刀" -> 2;
            case "青龙偃月刀", "贯石斧", "方天画戟" -> 3;
            case "朱雀羽扇" -> 4;
            case "麒麟弓" -> 5;
            default -> 0;
        };
    }

    public static int teamColor(String team) {
        return "RED".equals(team) ? 0xFFCC4444 : "BLUE".equals(team) ? 0xFF4488CC : 0xFF888888;
    }

    /** 出牌高亮呼吸强度 0-255 */
    private static int pulseAlpha() {
        long t = System.currentTimeMillis() % 1600;
        double s = Math.sin(t / 1600.0 * Math.PI * 2);
        return (int) (160 + 95 * s);
    }

    public static void render(GuiGraphics g, Font font, int x, int y, int w, int h,
                              ClientGameState.CPlayer p, boolean isCurrent, boolean isMine) {
        // 卡底(深棕)
        g.fill(x, y, x + w, y + h, 0xF0332B21);
        g.fill(x, y, x + w, y + 2, 0xFF241E16);
        // 卡面内侧金线
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x55C8AA6E);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x55C8AA6E);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, 0x55C8AA6E);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0x55C8AA6E);

        int teamC = teamColor(p.team);
        // 顶部势力色条
        g.fill(x, y, x + w, y + 4, teamC);
        // 左侧浅色区(信息区)
        int textW = w - 84; // 右侧 84px 给头像

        // 名字(白)
        String title = p.name + (isMine ? " (你)" : "");
        g.drawString(font, font.plainSubstrByWidth(title, textW - 8), x + 5, y + 8, 0xFFFFFFFF);

        // 武将名(金色大字,竖排两字一行)
        if (!p.hero.isEmpty()) {
            String hero = p.hero;
            int heroColor = p.alive ? 0xFFFFD700 : 0xFF666666;
            if (hero.length() >= 2) {
                // 竖排:一个字一行
                int hy = y + 22;
                g.drawString(font, hero.substring(0, 1), x + 5, hy, heroColor);
                g.drawString(font, hero.substring(1, 2), x + 5, hy + 12, heroColor);
            } else {
                g.drawString(font, hero, x + 5, y + 24, heroColor);
            }
            // 体力文本
            g.drawString(font, "♥" + p.hp + "/" + p.maxHp, x + 26, y + 24, p.hp <= 1 && p.alive ? 0xFFFF5555 : 0xFF7EE08A);
        } else {
            g.drawString(font, "未选将", x + 5, y + 24, 0xFF888888);
        }

        // 手牌数
        g.drawString(font, "手牌:" + p.handCount, x + 5, y + 40, 0xFFAAAAAA);

        // 装备/标记(两行)
        StringBuilder eq = new StringBuilder();
        if (!p.weapon.isEmpty()) eq.append(p.weapon).append("[").append(weaponRange(p.weapon)).append("]");
        if (!p.armor.isEmpty()) eq.append(" ").append(p.armor);
        if (!p.horsePlus.isEmpty()) eq.append(" ").append(p.horsePlus);
        if (!p.horseMinus.isEmpty()) eq.append(" ").append(p.horseMinus);
        g.drawString(font, font.plainSubstrByWidth(eq.toString(), textW - 8), x + 5, y + 52, 0xFFE8D9B0);
        if (!p.judged.isEmpty()) {
            g.drawString(font, font.plainSubstrByWidth("标记:" + String.join(",", p.judged), textW - 8), x + 5, y + 63, 0xFFCC66FF);
        }

        // 右侧武将头像
        if (!p.heroId.isEmpty()) {
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/hero/" + p.heroId + ".png");
            if (Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()) {
                int ax = x + w - 78;
                int ay = y + 6;
                g.blit(tex, ax, ay, 74, 100, 0, 0, 1, 1, 1, 1);
            }
        }

        // 阵营角标(右上,圆形近似)
        int tagX = x + w - 14, tagY = y - 4;
        g.fill(tagX - 5, tagY - 5, tagX + 9, tagY + 9, 0xFFFFFFFF); // 白边
        g.fill(tagX - 3, tagY - 3, tagX + 7, tagY + 7, teamC);      // 队色

        // 体力珠(头像左侧竖排)
        if (p.maxHp > 0) {
            int bx = x + w - 88, by = y + 10;
            for (int i = 0; i < p.maxHp; i++) {
                int color;
                if (!p.alive) color = 0xFF333333;
                else if (i >= p.hp) color = 0xFF1C1C1C;          // 已失去:黑
                else if (p.hp == 1 && i == p.hp - 1) color = 0xFFD32F2F; // 最后1点:红
                else color = 0xFF2F9E44;                         // 正常:绿
                g.fill(bx, by + i * 8, bx + 6, by + i * 8 + 6, color);
                g.fill(bx, by + i * 8, bx + 6, by + i * 8 + 1, 0x558EF09A); // 高光
            }
        }

        // 铁索连环特效(紫色覆盖 + 标记)
        if (p.chained) {
            g.fill(x, y, x + w, y + h, 0x40502A78);
            g.drawString(font, "⛓", x + w - 30, y + h - 16, 0xFFCC88FF);
            g.drawString(font, "铁索", x + w - 34, y + 12, 0xFFCC88FF);
        }

        // 出牌绿色呼吸高亮
        if (isCurrent && p.alive) {
            int a = pulseAlpha();
            int c = (a << 24) | 0x4CD964;
            g.fill(x - 2, y - 2, x + w + 2, y, c);
            g.fill(x - 2, y + h, x + w + 2, y + h + 2, c);
            g.fill(x - 2, y, x, y + h, c);
            g.fill(x + w, y, x + w + 2, y + h, c);
        }

        // 阵亡遮罩
        if (!p.alive) {
            g.fill(x, y, x + w, y + h, 0xA0000000);
            g.drawString(font, "阵亡", x + w / 2 - 8, y + h / 2 - 4, 0xFFFF4444);
        }
    }
}