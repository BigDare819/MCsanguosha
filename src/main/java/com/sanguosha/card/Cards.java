package com.sanguosha.card;

import java.util.ArrayList;
import java.util.List;

/**
 * 三国杀标准版牌堆 108 张。
 * 构成:基本牌53(杀30/闪15/桃8)+ 锦囊牌35 + 装备牌20。
 * 花色点数为通行标准版近似值(个别与官方牌表略有出入,待贴图到位后校对微调)。
 */
public final class Cards {
    private static final List<CardDefinition> ALL = new ArrayList<>();
    private static int seq = 0;

    private Cards() {}

    private static void add(String name, CardCategory cat, CardSuit suit, int rank, String effect) {
        ALL.add(new CardDefinition("card_" + (++seq), name, cat, suit, rank, effect));
    }

    static {
        // ========== 基本牌 53 ==========
        // 杀 30
        int[] spadeSlash = {7,8,8,9,9,10,10};
        int[] heartSlash = {10,10};
        int[] clubSlash  = {2,3,4,5,6,7,8,8,9,9,10,10,11,11};
        int[] diamondSlash = {6,7,8,9,10,13,13};
        for (int r : spadeSlash) add("杀", CardCategory.BASIC, CardSuit.SPADE, r, "slash");
        for (int r : heartSlash) add("杀", CardCategory.BASIC, CardSuit.HEART, r, "slash");
        for (int r : clubSlash)  add("杀", CardCategory.BASIC, CardSuit.CLUB, r, "slash");
        for (int r : diamondSlash) add("杀", CardCategory.BASIC, CardSuit.DIAMOND, r, "slash");
        // 闪 15
        add("闪", CardCategory.BASIC, CardSuit.HEART, 2, "jink");
        add("闪", CardCategory.BASIC, CardSuit.HEART, 2, "jink");
        add("闪", CardCategory.BASIC, CardSuit.HEART, 13, "jink");
        int[] diamondJink = {2,2,3,4,5,6,7,8,9,10,11,11};
        for (int r : diamondJink) add("闪", CardCategory.BASIC, CardSuit.DIAMOND, r, "jink");
        // 桃 8
        int[] heartPeach = {3,4,6,7,8};
        for (int r : heartPeach) add("桃", CardCategory.BASIC, CardSuit.HEART, r, "peach");
        add("桃", CardCategory.BASIC, CardSuit.DIAMOND, 12, "peach");
        add("桃", CardCategory.BASIC, CardSuit.DIAMOND, 12, "peach");
        add("桃", CardCategory.BASIC, CardSuit.DIAMOND, 12, "peach");

        // ========== 锦囊牌 35 ==========
        // 决斗 3
        add("决斗", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 1, "duel");
        add("决斗", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 1, "duel");
        add("决斗", CardCategory.TRICK_INSTANT, CardSuit.DIAMOND, 1, "duel");
        // 过河拆桥 6
        add("过河拆桥", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 3, "dismantlement");
        add("过河拆桥", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 4, "dismantlement");
        add("过河拆桥", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 12, "dismantlement");
        add("过河拆桥", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 3, "dismantlement");
        add("过河拆桥", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 4, "dismantlement");
        add("过河拆桥", CardCategory.TRICK_INSTANT, CardSuit.HEART, 12, "dismantlement");
        // 顺手牵羊 5
        add("顺手牵羊", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 3, "snatch");
        add("顺手牵羊", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 4, "snatch");
        add("顺手牵羊", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 11, "snatch");
        add("顺手牵羊", CardCategory.TRICK_INSTANT, CardSuit.DIAMOND, 3, "snatch");
        add("顺手牵羊", CardCategory.TRICK_INSTANT, CardSuit.DIAMOND, 4, "snatch");
        // 无中生有 4
        add("无中生有", CardCategory.TRICK_INSTANT, CardSuit.HEART, 7, "exnihilo");
        add("无中生有", CardCategory.TRICK_INSTANT, CardSuit.HEART, 8, "exnihilo");
        add("无中生有", CardCategory.TRICK_INSTANT, CardSuit.HEART, 9, "exnihilo");
        add("无中生有", CardCategory.TRICK_INSTANT, CardSuit.HEART, 11, "exnihilo");
        // 借刀杀人 2
        add("借刀杀人", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 12, "collateral");
        add("借刀杀人", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 13, "collateral");
        // 无懈可击 3
        add("无懈可击", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 13, "nullification");
        add("无懈可击", CardCategory.TRICK_INSTANT, CardSuit.HEART, 1, "nullification");
        add("无懈可击", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 13, "nullification");
        // 南蛮入侵 3
        add("南蛮入侵", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 7, "savage");
        add("南蛮入侵", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 7, "savage");
        add("南蛮入侵", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 13, "savage");
        // 万箭齐发 1
        add("万箭齐发", CardCategory.TRICK_INSTANT, CardSuit.HEART, 1, "archery");
        // 桃园结义 1
        add("桃园结义", CardCategory.TRICK_INSTANT, CardSuit.HEART, 1, "amazing");
        // 五谷丰登 2
        add("五谷丰登", CardCategory.TRICK_INSTANT, CardSuit.HEART, 3, "bountiful");
        add("五谷丰登", CardCategory.TRICK_INSTANT, CardSuit.HEART, 4, "bountiful");
        // 乐不思蜀 3
        add("乐不思蜀", CardCategory.TRICK_DELAYED, CardSuit.CLUB, 6, "indulgence");
        add("乐不思蜀", CardCategory.TRICK_DELAYED, CardSuit.HEART, 6, "indulgence");
        add("乐不思蜀", CardCategory.TRICK_DELAYED, CardSuit.SPADE, 6, "indulgence");
        // 闪电 2
        add("闪电", CardCategory.TRICK_DELAYED, CardSuit.SPADE, 1, "lightning");
        add("闪电", CardCategory.TRICK_DELAYED, CardSuit.CLUB, 1, "lightning");

        // ========== 装备牌 20 ==========
        // 武器 9: 诸葛连弩x2, 青釭剑, 雌雄双股剑, 青龙偃月刀, 丈八蛇矛, 贯石斧, 方天画戟, 麒麟弓
        add("诸葛连弩", CardCategory.EQUIP_WEAPON, CardSuit.CLUB, 1, "crossbow");
        add("诸葛连弩", CardCategory.EQUIP_WEAPON, CardSuit.DIAMOND, 1, "crossbow");
        add("青釭剑", CardCategory.EQUIP_WEAPON, CardSuit.SPADE, 6, "qinggang");
        add("雌雄双股剑", CardCategory.EQUIP_WEAPON, CardSuit.SPADE, 2, "double_sword");
        add("青龙偃月刀", CardCategory.EQUIP_WEAPON, CardSuit.SPADE, 5, "green_dragon");
        add("丈八蛇矛", CardCategory.EQUIP_WEAPON, CardSuit.CLUB, 12, "spear");
        add("贯石斧", CardCategory.EQUIP_WEAPON, CardSuit.DIAMOND, 5, "axe");
        add("方天画戟", CardCategory.EQUIP_WEAPON, CardSuit.DIAMOND, 12, "halberd");
        add("麒麟弓", CardCategory.EQUIP_WEAPON, CardSuit.SPADE, 13, "kylin");
        // 防具 3: 八卦阵x2, 仁王盾
        add("八卦阵", CardCategory.EQUIP_ARMOR, CardSuit.SPADE, 2, "bagua");
        add("八卦阵", CardCategory.EQUIP_ARMOR, CardSuit.CLUB, 2, "bagua");
        add("仁王盾", CardCategory.EQUIP_ARMOR, CardSuit.CLUB, 3, "renwang");
        // 坐骑 8: 防御马(的卢/绝影/爪黄飞电), 进攻马(赤兔/大宛/紫骍), 另 2 张凑足 108 可调
        add("的卢", CardCategory.EQUIP_HORSE_PLUS, CardSuit.CLUB, 5, "horse_plus");
        add("绝影", CardCategory.EQUIP_HORSE_PLUS, CardSuit.SPADE, 5, "horse_plus");
        add("爪黄飞电", CardCategory.EQUIP_HORSE_PLUS, CardSuit.HEART, 13, "horse_plus");
        add("赤兔", CardCategory.EQUIP_HORSE_MINUS, CardSuit.HEART, 5, "horse_minus");
        add("大宛", CardCategory.EQUIP_HORSE_MINUS, CardSuit.SPADE, 13, "horse_minus");
        add("紫骍", CardCategory.EQUIP_HORSE_MINUS, CardSuit.DIAMOND, 13, "horse_minus");
        add("惊帆", CardCategory.EQUIP_HORSE_MINUS, CardSuit.HEART, 12, "horse_minus");
        add("骅骝", CardCategory.EQUIP_HORSE_PLUS, CardSuit.DIAMOND, 12, "horse_plus");

        // ========== 军争扩展:火杀/雷杀/酒/铁索连环/藤甲(27 张) ==========
        // 火杀 5
        add("火杀", CardCategory.BASIC, CardSuit.HEART, 4, "fire_slash");
        add("火杀", CardCategory.BASIC, CardSuit.HEART, 7, "fire_slash");
        add("火杀", CardCategory.BASIC, CardSuit.DIAMOND, 4, "fire_slash");
        add("火杀", CardCategory.BASIC, CardSuit.DIAMOND, 5, "fire_slash");
        add("火杀", CardCategory.BASIC, CardSuit.DIAMOND, 5, "fire_slash");
        // 雷杀 9
        int[] thunderSpade = {4, 5, 6, 7, 8, 9};
        int[] thunderClub = {5, 6, 7};
        for (int r : thunderSpade) add("雷杀", CardCategory.BASIC, CardSuit.SPADE, r, "thunder_slash");
        for (int r : thunderClub) add("雷杀", CardCategory.BASIC, CardSuit.CLUB, r, "thunder_slash");
        // 酒 5
        add("酒", CardCategory.BASIC, CardSuit.SPADE, 3, "analeptic");
        add("酒", CardCategory.BASIC, CardSuit.SPADE, 9, "analeptic");
        add("酒", CardCategory.BASIC, CardSuit.CLUB, 3, "analeptic");
        add("酒", CardCategory.BASIC, CardSuit.CLUB, 9, "analeptic");
        add("酒", CardCategory.BASIC, CardSuit.DIAMOND, 9, "analeptic");
        // 铁索连环 6
        add("铁索连环", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 11, "iron_chain");
        add("铁索连环", CardCategory.TRICK_INSTANT, CardSuit.SPADE, 12, "iron_chain");
        add("铁索连环", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 11, "iron_chain");
        add("铁索连环", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 11, "iron_chain");
        add("铁索连环", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 12, "iron_chain");
        add("铁索连环", CardCategory.TRICK_INSTANT, CardSuit.CLUB, 13, "iron_chain");
        // 藤甲 2
        add("藤甲", CardCategory.EQUIP_ARMOR, CardSuit.SPADE, 2, "vine");
        add("藤甲", CardCategory.EQUIP_ARMOR, CardSuit.CLUB, 2, "vine");

        // ========== 军争扩展2:火攻/古锭刀/朱雀羽扇/白银狮子/兵粮寸断 ==========
        // 火攻 3
        add("火攻", CardCategory.TRICK_INSTANT, CardSuit.HEART, 2, "huogong");
        add("火攻", CardCategory.TRICK_INSTANT, CardSuit.HEART, 3, "huogong");
        add("火攻", CardCategory.TRICK_INSTANT, CardSuit.DIAMOND, 12, "huogong");
        // 古锭刀(武器,范围2)
        add("古锭刀", CardCategory.EQUIP_WEAPON, CardSuit.SPADE, 1, "gudingdao");
        // 朱雀羽扇(武器,范围4)
        add("朱雀羽扇", CardCategory.EQUIP_WEAPON, CardSuit.DIAMOND, 1, "zhuqueyushan");
        // 白银狮子(防具)
        add("白银狮子", CardCategory.EQUIP_ARMOR, CardSuit.CLUB, 1, "baiyinshizi");
        // 兵粮寸断 2
        add("兵粮寸断", CardCategory.TRICK_DELAYED, CardSuit.SPADE, 10, "bingliangcunduan");
        add("兵粮寸断", CardCategory.TRICK_DELAYED, CardSuit.CLUB, 4, "bingliangcunduan");
    }

    public static List<CardDefinition> all() { return List.copyOf(ALL); }
    public static int count() { return ALL.size(); }
}