package com.sanguosha.game;

/** 2v2 阵营 */
public enum Team {
    RED("红方"),
    BLUE("蓝方");

    public final String cn;
    Team(String cn) { this.cn = cn; }

    public Team opposite() { return this == RED ? BLUE : RED; }
}