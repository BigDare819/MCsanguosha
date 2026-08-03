package com.sanguosha.client.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** 卡牌渲染:有贴图渲染贴图,没有则画文字卡片 */
public final class CardWidget {
    private CardWidget() {}

    private static final int CARD_BG = 0xFF2B2B2B;
    private static final int CARD_BORDER = 0xFF8B7355;

    public static int categoryColor(String cat) {
        return switch (cat) {
            case "BASIC" -> 0xFFE8E8E8;        // 基本:白
            case "TRICK_INSTANT", "TRICK_DELAYED" -> 0xFF4AA8FF; // 锦囊:蓝
            case "EQUIP_WEAPON" -> 0xFFFFB84A;  // 武器:橙
            case "EQUIP_ARMOR" -> 0xFF4AD94A;   // 防具:绿
            case "EQUIP_HORSE_PLUS", "EQUIP_HORSE_MINUS" -> 0xFFB04AE8; // 马:紫
            default -> 0xFFAAAAAA;
        };
    }

    public static String suitSymbol(String suit) {
        return switch (suit) {
            case "SPADE" -> "♠"; case "HEART" -> "♥";
            case "CLUB" -> "♣"; case "DIAMOND" -> "♦";
            default -> "";
        };
    }

    public static int suitColor(String suit) {
        return (suit.equals("HEART") || suit.equals("DIAMOND")) ? 0xFFFF5555 : 0xFF555555;
    }

    public static String rankText(int rank) {
        return switch (rank) {
            case 1 -> "A"; case 11 -> "J"; case 12 -> "Q"; case 13 -> "K";
            default -> String.valueOf(rank);
        };
    }

    /** 渲染一张卡牌 */
    public static void render(GuiGraphics g, Font font, int x, int y, int w, int h,
                              String name, String suit, int rank, String cat, boolean highlight) {
        String texKey = textureKey(name);
        ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/card/" + texKey + ".png");
        boolean hasTex = Minecraft.getInstance().getResourceManager().getResource(tex).isPresent();

        if (hasTex) {
            g.blit(tex, x, y, w, h, 0, 0, 1, 1, 1, 1);
        } else {
            g.fill(x, y, x + w, y + h, CARD_BG);
            int border = categoryColor(cat);
            g.fill(x, y, x + w, y + 2, border);
            g.fill(x, y + h - 2, x + w, y + h, border);
            g.fill(x, y, x + 2, y + h, border);
            g.fill(x + w - 2, y, x + w, y + h, border);
            // 左上角花色点数
            String suitRank = suitSymbol(suit) + rankText(rank);
            g.drawString(font, suitRank, x + 4, y + 4, suitColor(suit));
            // 中间牌名(竖排拆字)
            int cx = x + w / 2;
            int cy = y + h / 2 - 8;
            for (int i = 0; i < name.length(); i++) {
                String ch = String.valueOf(name.charAt(i));
                g.drawString(font, ch, cx - font.width(ch) / 2, cy + i * 10, 0xFFFFFFFF);
            }
            // 底部类别
            String catCn = switch (cat) {
                case "BASIC" -> "基本牌";
                case "TRICK_INSTANT" -> "锦囊";
                case "TRICK_DELAYED" -> "延时锦囊";
                case "EQUIP_WEAPON" -> "武器";
                case "EQUIP_ARMOR" -> "防具";
                case "EQUIP_HORSE_PLUS" -> "防御马";
                case "EQUIP_HORSE_MINUS" -> "进攻马";
                default -> "";
            };
            g.drawString(font, catCn, x + 4, y + h - 10, 0xFFAAAAAA);
        }
        // 选中高亮边框(金色呼吸闪烁)
        if (highlight) {
            long t = System.currentTimeMillis() % 1000;
            int a = 180 + (int) (75 * Math.sin(t / 1000.0 * Math.PI * 2));
            int c = (a << 24) | 0xFFD700;
            g.fill(x - 2, y - 2, x + w + 2, y, c);
            g.fill(x - 2, y + h, x + w + 2, y + h + 2, c);
            g.fill(x - 2, y, x, y + h, c);
            g.fill(x + w, y, x + w + 2, y + h, c);
        }
    }

    /** 牌名 -> 贴图文件名 */
    public static String textureKey(String name) {
        return switch (name) {
            case "杀" -> "slash"; case "闪" -> "jink"; case "桃" -> "peach";
            case "火杀" -> "fire_slash"; case "雷杀" -> "thunder_slash"; case "酒" -> "analeptic";
            case "铁索连环" -> "iron_chain"; case "藤甲" -> "vine";
            case "决斗" -> "duel"; case "过河拆桥" -> "dismantlement"; case "顺手牵羊" -> "snatch";
            case "无中生有" -> "exnihilo"; case "借刀杀人" -> "collateral"; case "无懈可击" -> "nullification";
            case "南蛮入侵" -> "savage"; case "万箭齐发" -> "archery"; case "桃园结义" -> "amazing";
            case "五谷丰登" -> "bountiful"; case "乐不思蜀" -> "indulgence"; case "闪电" -> "lightning";
            case "诸葛连弩" -> "crossbow"; case "青釭剑" -> "qinggang"; case "雌雄双股剑" -> "double_sword";
            case "青龙偃月刀" -> "green_dragon"; case "丈八蛇矛" -> "spear"; case "贯石斧" -> "axe";
            case "方天画戟" -> "halberd"; case "麒麟弓" -> "kylin"; case "八卦阵" -> "bagua";
            case "仁王盾" -> "renwang"; case "的卢" -> "dilu"; case "绝影" -> "jueying";
            case "爪黄飞电" -> "zhuahuang"; case "赤兔" -> "chitu"; case "大宛" -> "dayuan";
            case "紫骍" -> "zixun"; case "惊帆" -> "jingfan"; case "骅骝" -> "hualiu";
            case "火攻" -> "huogong"; case "古锭刀" -> "gudingdao"; case "朱雀羽扇" -> "zhuqueyushan";
            case "白银狮子" -> "baiyinshizi"; case "兵粮寸断" -> "bingliangcunduan";
            default -> "unknown";
        };
    }
}