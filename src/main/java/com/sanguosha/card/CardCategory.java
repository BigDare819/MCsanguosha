package com.sanguosha.card;

/** 卡牌类别 */
public enum CardCategory {
    BASIC("基本牌"),
    TRICK_INSTANT("非延时锦囊"),
    TRICK_DELAYED("延时锦囊"),
    EQUIP_WEAPON("武器"),
    EQUIP_ARMOR("防具"),
    EQUIP_HORSE_PLUS("防御马"),
    EQUIP_HORSE_MINUS("进攻马");

    public final String cn;
    CardCategory(String cn) { this.cn = cn; }

    public boolean isTrick() { return this == TRICK_INSTANT || this == TRICK_DELAYED; }
    public boolean isEquip() { return this == EQUIP_WEAPON || this == EQUIP_ARMOR || this == EQUIP_HORSE_PLUS || this == EQUIP_HORSE_MINUS; }
}