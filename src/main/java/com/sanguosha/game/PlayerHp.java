package com.sanguosha.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 线下实体玩法独立血量(默认 4,可按键调整)+血量上限(默认 4,可按键调整) */
public final class PlayerHp {
    private static final Map<UUID, Integer> HP = new HashMap<>();
    private static final Map<UUID, Integer> MAX_HP = new HashMap<>();

    private PlayerHp() {}

    public static int get(UUID id) { return HP.getOrDefault(id, 4); }

    public static int adjust(UUID id, int delta) {
        int v = Math.max(0, Math.min(20, get(id) + delta));
        HP.put(id, v);
        return v;
    }

    public static int getMax(UUID id) { return MAX_HP.getOrDefault(id, 4); }

    public static int adjustMax(UUID id, int delta) {
        int v = Math.max(0, Math.min(20, getMax(id) + delta));
        MAX_HP.put(id, v);
        return v;
    }

    public static void reset(UUID id) { HP.remove(id); MAX_HP.remove(id); }
}