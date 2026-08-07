package com.sanguosha.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** 客户端缓存的状态(由服务器同步包更新) */
public final class ClientGameState {
    private static final Gson GSON = new Gson();

    // 线下实体卡牌模式:HUD 开关与选牌
    public static boolean showOverlay = false;
    public static int selectedHand = -1;

    // 血量/手牌数映射(头顶血量、血量面板、拆迁界面共用)
    public static final java.util.Map<String, Integer> HP_MAP = new java.util.HashMap<>();
    public static final java.util.Map<String, Integer> MAX_HP_MAP = new java.util.HashMap<>();
    public static final java.util.Map<String, Integer> HAND_MAP = new java.util.HashMap<>();

    private ClientGameState() {}

    public static void update(String json) {
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            HP_MAP.clear();
            MAX_HP_MAP.clear();
            if (root.has("hpList")) {
                for (var he : root.getAsJsonArray("hpList")) {
                    var ho = he.getAsJsonObject();
                    HP_MAP.put(ho.get("name").getAsString(), ho.get("hp").getAsInt());
                    if (ho.has("maxHp")) MAX_HP_MAP.put(ho.get("name").getAsString(), ho.get("maxHp").getAsInt());
                    if (ho.has("handCount")) HAND_MAP.put(ho.get("name").getAsString(), ho.get("handCount").getAsInt());
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
    private static int intOr(JsonObject o, String key, int def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : def;
    }
}
