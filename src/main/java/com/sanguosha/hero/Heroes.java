package com.sanguosha.hero;

import java.util.ArrayList;
import java.util.List;

/**
 * 武将登记:标准版 25 将 + 扩展(袁术、华雄)+ 自定义(奶龙、张雪峰、界徐盛、
 * 神曹操、神赵云、李典、吕岱、留赞、龙羽飞)。技能实现逐步完善,此处登记技能名称。
 */
public final class Heroes {
    private static final List<HeroDefinition> ALL = new ArrayList<>();
    private Heroes() {}

    private static void add(String name, Faction f, int hp, String... skills) {
        String id = nameToId(name);
        ALL.add(new HeroDefinition(id, name, f, hp, List.of(skills)));
    }

    public static String nameToId(String name) {
        return switch (name) {
            case "曹操" -> "caocao";
            case "司马懿" -> "simayi"; case "夏侯惇" -> "xiahoudun"; case "张辽" -> "zhangliao";
            case "许褚" -> "xuchu"; case "郭嘉" -> "guojia"; case "甄姬" -> "zhenji";
            case "刘备" -> "liubei"; case "关羽" -> "guanyu"; case "张飞" -> "zhangfei";
            case "诸葛亮" -> "zhugeliang"; case "赵云" -> "zhaoyun"; case "马超" -> "machao";
            case "黄月英" -> "huangyueying"; case "孙权" -> "sunquan"; case "甘宁" -> "ganning";
            case "吕蒙" -> "lvmeng"; case "黄盖" -> "huanggai"; case "周瑜" -> "zhouyu";
            case "大乔" -> "daqiao"; case "陆逊" -> "luxun"; case "孙尚香" -> "sunshangxiang";
            case "华佗" -> "huatuo"; case "吕布" -> "lvbu"; case "貂蝉" -> "diaochan";
            case "袁术" -> "yuanshu"; case "华雄" -> "huaxiong";
            case "\u5976\u9f99" -> "nailong"; case "\u5f20\u96ea\u5cf0" -> "zhangxuefeng";
            case "\u754c\u5f90\u76db" -> "jiexusheng";
            case "\u795e\u66f9\u64cd" -> "shencaocao";
            case "\u795e\u8d75\u4e91" -> "shenzhaoyun";
            case "\u674e\u5178" -> "lidian";
            case "\u5415\u5cb1" -> "lvdai";
            case "\u7559\u8d5e" -> "liuzan";
            case "\u9f99\u7fbd\u98de" -> "longyufei";
            default -> name;
        };
    }

    static {
        // ===== 魏 7 =====
        add("曹操", Faction.WEI, 4, "jianxiong", "hujia");
        add("司马懿", Faction.WEI, 3, "fankui", "guicai");
        add("夏侯惇", Faction.WEI, 4, "ganglie");
        add("张辽", Faction.WEI, 4, "tuxi");
        add("许褚", Faction.WEI, 4, "luoyi");
        add("郭嘉", Faction.WEI, 3, "tiandu", "yiji");
        add("甄姬", Faction.WEI, 3, "qingguo", "luoshen");
        // ===== 蜀 7 =====
        add("刘备", Faction.SHU, 4, "rende", "jijiang");
        add("关羽", Faction.SHU, 4, "wusheng");
        add("张飞", Faction.SHU, 4, "paoxiao");
        add("诸葛亮", Faction.SHU, 3, "guanxing", "kongcheng");
        add("赵云", Faction.SHU, 4, "longdan");
        add("马超", Faction.SHU, 4, "mashu", "tieqi");
        add("黄月英", Faction.SHU, 3, "jizhi", "qicai");
        // ===== 吴 8 =====
        add("孙权", Faction.WU, 4, "zhiheng", "jiuyuan");
        add("甘宁", Faction.WU, 4, "qixi");
        add("吕蒙", Faction.WU, 4, "keji");
        add("黄盖", Faction.WU, 4, "kuro");
        add("周瑜", Faction.WU, 3, "yingzi", "fanjian");
        add("大乔", Faction.WU, 3, "guose", "liuli");
        add("陆逊", Faction.WU, 3, "qianxun", "lianying");
        add("孙尚香", Faction.WU, 3, "jieyin", "xiaoji");
        // ===== 群 3 =====
        add("华佗", Faction.QUN, 3, "jijiu", "qingnang");
        add("吕布", Faction.QUN, 4, "wushuang");
        add("貂蝉", Faction.QUN, 3, "lijian", "biyue");
        // ===== 扩展 2 =====
        add("袁术", Faction.QUN, 4, "yongsi", "weidi");
        add("华雄", Faction.QUN, 6, "yaowu");
        // ===== 新武将(仅将盒数据,技能待实现) =====
        add("\u5976\u9f99", Faction.SHEN, 4, "juhua", "zhenglong");
        add("\u5f20\u96ea\u5cf0", Faction.LAO, 5, "qiaolezi", "zuichunfazi", "lajinmiwu");
        add("\u754c\u5f90\u76db", Faction.WU, 4);
        add("\u795e\u66f9\u64cd", Faction.SHEN, 3);
        add("\u795e\u8d75\u4e91", Faction.SHEN, 2);
        add("\u674e\u5178", Faction.WEI, 3);
        add("\u5415\u5cb1", Faction.WU, 4);
        add("\u7559\u8d5e", Faction.WU, 4);
        add("\u9f99\u7fbd\u98de", Faction.SHU, 3);
    }

    public static List<HeroDefinition> all() { return List.copyOf(ALL); }
    public static int count() { return ALL.size(); }
    public static HeroDefinition byId(String id) {
        for (HeroDefinition h : ALL) if (h.id.equals(id)) return h;
        return ALL.get(0);
    }
}