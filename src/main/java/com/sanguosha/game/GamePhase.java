package com.sanguosha.game;

/** 回合阶段 */
public enum GamePhase {
    PREPARE("准备阶段"),
    DRAW("摸牌阶段"),
    PLAY("出牌阶段"),
    DISCARD("弃牌阶段"),
    END("结束阶段"),
    FINISHED("游戏结束");

    public final String cn;
    GamePhase(String cn) { this.cn = cn; }
}