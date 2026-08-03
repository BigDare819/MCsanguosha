package com.sanguosha.game;

/** 游戏管理器:当前服务器单局游戏 */
public final class GameManager {
    private static SanguoshaGame game;

    private GameManager() {}

    public static SanguoshaGame get() {
        if (game == null) game = new SanguoshaGame();
        return game;
    }

    public static void reset() { game = new SanguoshaGame(); }
}