package com.sanguosha.card;

/** 花色 */
public enum CardSuit {
    SPADE("黑桃", "♠", 0),
    HEART("红桃", "♥", 1),
    CLUB("梅花", "♣", 0),
    DIAMOND("方块", "♦", 1);

    public final String cn;
    public final String symbol;
    /** 0=黑, 1=红 */
    public final int color;

    CardSuit(String cn, String symbol, int color) {
        this.cn = cn;
        this.symbol = symbol;
        this.color = color;
    }
}