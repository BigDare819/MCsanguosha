package com.sanguosha.card;

/** 卡牌定义(数据驱动,一张牌一个实例) */
public class CardDefinition {
    public final String id;
    public final String name;      // 中文名,如 "杀"
    public final CardCategory category;
    public final CardSuit suit;
    public final int rank;         // 点数 1-13 (A=1, J=11, Q=12, K=13)
    /** 效果处理器 key, 如 "slash"/"jink"/"peach"/"duel", 由 game.effect.EffectRegistry 解析 */
    public final String effect;

    public CardDefinition(String id, String name, CardCategory category, CardSuit suit, int rank, String effect) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.suit = suit;
        this.rank = rank;
        this.effect = effect;
    }

    /** 显示点数文本 */
    public String rankText() {
        return switch (rank) {
            case 1 -> "A";
            case 11 -> "J";
            case 12 -> "Q";
            case 13 -> "K";
            default -> String.valueOf(rank);
        };
    }

    /** 贴图资源路径:同名卡牌共用一张贴图,按牌名映射 */
    public String texturePath() {
        return "sanguosha:textures/card/" + textureKey() + ".png";
    }

    private String textureKey() {
        return switch (name) {
            case "杀" -> "slash";      case "闪" -> "jink";       case "桃" -> "peach";
            case "火杀" -> "fire_slash"; case "雷杀" -> "thunder_slash"; case "酒" -> "analeptic";
            case "铁索连环" -> "iron_chain"; case "藤甲" -> "vine";
            case "决斗" -> "duel";     case "过河拆桥" -> "dismantlement"; case "顺手牵羊" -> "snatch";
            case "无中生有" -> "exnihilo"; case "借刀杀人" -> "collateral"; case "无懈可击" -> "nullification";
            case "南蛮入侵" -> "savage"; case "万箭齐发" -> "archery"; case "桃园结义" -> "amazing";
            case "五谷丰登" -> "bountiful"; case "乐不思蜀" -> "indulgence"; case "闪电" -> "lightning";
            case "诸葛连弩" -> "crossbow"; case "青釭剑" -> "qinggang"; case "雌雄双股剑" -> "double_sword";
            case "青龙偃月刀" -> "green_dragon"; case "丈八蛇矛" -> "spear"; case "贯石斧" -> "axe";
            case "方天画戟" -> "halberd"; case "麒麟弓" -> "kylin"; case "八卦阵" -> "bagua";
            case "仁王盾" -> "renwang"; case "的卢" -> "dilu"; case "绝影" -> "jueying";
            case "爪黄飞电" -> "zhuahuang"; case "赤兔" -> "chitu"; case "大宛" -> "dayuan";
            case "紫骍" -> "zixun"; case "惊帆" -> "jingfan"; case "骅骝" -> "hualiu";
            default -> "unknown";
        };
    }
}