package com.sanguosha.client.screen;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 牌名/武将名 -> 小写拼音(无调),用于剩余 UI 拼音搜索 */
public final class PinyinUtil {
    private static final Map<String, String> CARD_PINYIN = new HashMap<>();
    private static final Map<String, String> HERO_PINYIN = new HashMap<>();
    static {
        CARD_PINYIN.put("\u94c1\u7d22\u8fde\u73af", "tiesuolianhuan");
        CARD_PINYIN.put("\u96f7\u6740", "leisha");
        CARD_PINYIN.put("\u96cc\u96c4\u53cc\u80a1\u5251", "cixiongshuanggu");
        CARD_PINYIN.put("\u8bf8\u845b\u8fde\u5f29", "zhugeliannu");
        CARD_PINYIN.put("\u51b3\u6597", "juedou");
        CARD_PINYIN.put("\u6731\u96c0\u7fbd\u6247", "zhuqueyushan");
        CARD_PINYIN.put("\u9a85\u9a9d", "hualiu");
        CARD_PINYIN.put("\u5357\u86ee\u5165\u4fb5", "nanmanruqin");
        CARD_PINYIN.put("\u4e07\u7bad\u9f50\u53d1", "wanjianqifa");
        CARD_PINYIN.put("\u6843\u56ed\u7ed3\u4e49", "taoyuanjieyi");
        CARD_PINYIN.put("\u9752\u9f99\u5043\u6708\u5200", "qinglongyanyuedao");
        CARD_PINYIN.put("\u7d2b\u9a8d", "zixun");
        CARD_PINYIN.put("\u706b\u6740", "huosha");
        CARD_PINYIN.put("\u5927\u5b9b", "dayuan");
        CARD_PINYIN.put("\u8d64\u5154", "chitu");
        CARD_PINYIN.put("\u95ea", "shan");
        CARD_PINYIN.put("\u9e92\u9e9f\u5f13", "qilingong");
        CARD_PINYIN.put("\u4e08\u516b\u86c7\u77db", "zhangbashemao");
        CARD_PINYIN.put("\u53e4\u952d\u5200", "gudingdao");
        CARD_PINYIN.put("\u9752\u91ed\u5251", "qinggangjian");
        CARD_PINYIN.put("\u6740", "sha");
        CARD_PINYIN.put("\u8fc7\u6cb3\u62c6\u6865", "guohechaiqiao");
        CARD_PINYIN.put("\u516b\u5366\u9635", "baguazhen");
        CARD_PINYIN.put("\u4e50\u4e0d\u601d\u8700", "lebusishu");
        CARD_PINYIN.put("\u85e4\u7532", "tengjia");
        CARD_PINYIN.put("\u987a\u624b\u7275\u7f8a", "shunshouqianyang");
        CARD_PINYIN.put("\u65b9\u5929\u753b\u621f", "fangtianhuaji");
        CARD_PINYIN.put("\u65e0\u61c8\u53ef\u51fb", "wuxiekeji");
        CARD_PINYIN.put("\u8d2f\u77f3\u65a7", "guanshifu");
        CARD_PINYIN.put("\u65e0\u4e2d\u751f\u6709", "wuzhongshengyou");
        CARD_PINYIN.put("\u7edd\u5f71", "jueying");
        CARD_PINYIN.put("\u767d\u94f6\u72ee\u5b50", "baiyinshizi");
        CARD_PINYIN.put("\u5175\u7cae\u5bf8\u65ad", "bingliangcunduan");
        CARD_PINYIN.put("\u501f\u5200\u6740\u4eba", "jiedaosharen");
        CARD_PINYIN.put("\u722a\u9ec4\u98de\u7535", "zhuahuangfeidian");
        CARD_PINYIN.put("\u4e94\u8c37\u4e30\u767b", "wugufengdeng");
        CARD_PINYIN.put("\u706b\u653b", "huogong");
        CARD_PINYIN.put("\u6843", "tao");
        CARD_PINYIN.put("\u95ea\u7535", "shandian");
        CARD_PINYIN.put("\u7684\u5362", "dilu");
        CARD_PINYIN.put("\u9152", "jiu");
        CARD_PINYIN.put("\u4ec1\u738b\u76fe", "renwangdun");
        CARD_PINYIN.put("\u60ca\u5e06", "jingfan");
        HERO_PINYIN.put("\u8bf8\u845b\u4eae", "zhugeliang");
        HERO_PINYIN.put("\u8bb8\u891a", "xuchu");
        HERO_PINYIN.put("\u534e\u4f57", "huatuo");
        HERO_PINYIN.put("\u9ec4\u6708\u82f1", "huangyueying");
        HERO_PINYIN.put("\u5b59\u6743", "sunquan");
        HERO_PINYIN.put("\u5f20\u8fbd", "zhangliao");
        HERO_PINYIN.put("\u590f\u4faf\u60c7", "xiahoudun");
        HERO_PINYIN.put("\u5927\u4e54", "daqiao");
        HERO_PINYIN.put("\u7504\u59ec", "zhenji");
        HERO_PINYIN.put("\u5f20\u96ea\u5cf0", "zhangxuefeng");
        HERO_PINYIN.put("\u5b59\u5c1a\u9999", "sunshangxiang");
        HERO_PINYIN.put("\u66f9\u64cd", "caocao");
        HERO_PINYIN.put("\u5173\u7fbd", "guanyu");
        HERO_PINYIN.put("\u5f20\u98de", "zhangfei");
        HERO_PINYIN.put("\u9646\u900a", "luxun");
        HERO_PINYIN.put("\u5415\u8499", "lvmeng");
        HERO_PINYIN.put("\u8d75\u4e91", "zhaoyun");
        HERO_PINYIN.put("\u5415\u5e03", "lvbu");
        HERO_PINYIN.put("\u5218\u5907", "liubei");
        HERO_PINYIN.put("\u8c82\u8749", "diaochan");
        HERO_PINYIN.put("\u9a6c\u8d85", "machao");
        HERO_PINYIN.put("\u534e\u96c4", "huaxiong");
        HERO_PINYIN.put("\u5468\u745c", "zhouyu");
        HERO_PINYIN.put("\u53f8\u9a6c\u61ff", "simayi");
        HERO_PINYIN.put("\u9ec4\u76d6", "huanggai");
        HERO_PINYIN.put("\u8881\u672f", "yuanshu");
        HERO_PINYIN.put("\u5976\u9f99", "nailong");
        HERO_PINYIN.put("\u7518\u5b81", "ganning");
        HERO_PINYIN.put("\u90ed\u5609", "guojia");
    }

    private PinyinUtil() {}

    /** 取名字核心(去掉花色/点数前缀)并转拼音;未知字符原样小写返回 */
    public static String toPinyin(String name) {
        String core = name;
        int sp = core.lastIndexOf(' ');
        if (sp >= 0 && sp < core.length() - 1) core = core.substring(sp + 1);
        String p = CARD_PINYIN.get(core);
        if (p == null) p = HERO_PINYIN.get(core);
        if (p == null) p = com.sanguosha.hero.Heroes.nameToId(core);
        return p != null ? p : name.toLowerCase(Locale.ROOT);
    }
}
