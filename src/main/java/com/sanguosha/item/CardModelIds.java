package com.sanguosha.item;

/** 卡牌贴图 key -> custom_model_data 序号(与生成的模型文件对应) */
public final class CardModelIds {
    private static final String[] KEYS = {
        "amazing","analeptic","archery","axe","back","bagua","baiyinshizi","bingliangcunduan",
        "bountiful","chitu","collateral","crossbow","dayuan","dilu","dismantlement","double_sword",
        "duel","exnihilo","fire_slash","green_dragon","gudingdao","halberd","hualiu","huogong",
        "indulgence","iron_chain","jingfan","jink","jueying","kylin","lightning","nullification",
        "peach","qinggang","renwang","savage","slash","snatch","spear","thunder_slash",
        "vine","zhuahuang","zhuqueyushan","zixun"
    };

    private static final String[] HERO_KEYS = {
        "caocao","daqiao","diaochan","ganning","guanyu","guojia","huanggai","huatuo",
        "huangyueying","huaxiong","liubei","lvbu","lvmeng","luxun","machao","simayi",
        "sunquan","sunshangxiang","xiahoudun","xuchu","yuanshu","zhangfei","zhangliao",
        "zhaoyun","zhenji","zhugeliang","zhouyu","nailong","zhangxuefeng","jiexusheng","shencaocao","shenzhaoyun","lidian","lvdai","liuzan","longyufei"
    };

    private CardModelIds() {}

    /** 全部贴图 key(供图集注册使用) */
    public static String[] allKeys() { return KEYS; }

    /** 武将 id -> custom_model_data(45-71) */
    public static int heroIdOf(String heroId) {
        for (int i = 0; i < HERO_KEYS.length; i++) {
            if (HERO_KEYS[i].equals(heroId)) return 45 + i;
        }
        return 45;
    }

    /** 牌名 -> 贴图 key */
    public static String keyOf(String name) {
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
            default -> "back";
        };
    }

    /** 牌名 -> custom_model_data 序号 */
    public static int idOf(String cardName) {
        String key = keyOf(cardName);
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return i + 1;
        }
        return 1;
    }
}