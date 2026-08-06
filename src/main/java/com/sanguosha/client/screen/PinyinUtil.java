package com.sanguosha.client.screen;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 牌名/武将名 -> 小写拼音(无调),用于剩余 UI 拼音搜索 */
public final class PinyinUtil {
    private static final Map<String, String> CARD_PINYIN = new HashMap<>();
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
    }

    private PinyinUtil() {}

    /** 取名字核心(去掉花色/点数前缀)并转拼音;未知字符原样小写返回 */
    public static String toPinyin(String name) {
        String core = name;
        int sp = core.lastIndexOf(' ');
        if (sp >= 0 && sp < core.length() - 1) core = core.substring(sp + 1);
        String p = CARD_PINYIN.get(core);
        if (p == null) p = com.sanguosha.hero.Heroes.nameToId(core);
        return p != null ? p : name.toLowerCase(Locale.ROOT);
    }
}
