package com.sanguosha.hero;

/** 势力 */
public enum Faction {
    WEI("魏", 0xFF4A4A),
    SHU("蜀", 0x4AD94A),
    WU("吴", 0x4AA8FF),
    QUN("\u7fa4", 0xE0C341),
    SHEN("\u795e", 0xC9A6FF),
    LAO("\u7262", 0x9E9E9E);

    public final String cn;
    public final int color;
    Faction(String cn, int color) { this.cn = cn; this.color = color; }
}