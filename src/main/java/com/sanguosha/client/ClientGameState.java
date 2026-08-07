package com.sanguosha.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** 客户端缓存的游戏状态(由服务器同步包更新) */
public final class ClientGameState {
    private static final Gson GSON = new Gson();

    // 玩家信息
    public static class CPlayer {
        public String name = "";
        public int seat;
        public String team = "";
        public String hero = "";
        public String heroId = "";
        public boolean chained = false;
        public boolean drunk = false;
        public int hp;
        public int maxHp;
        public boolean alive = true;
        public int handCount;
        public String weapon = "", armor = "", horsePlus = "", horseMinus = "";
        public List<String> judged = new ArrayList<>();
        public final List<String> skills = new ArrayList<>();
        public int slashUsed = 0;
        public boolean noSlashLimit = false;
    }

    // 手牌
    public static class CCard {
        public String id = "";
        public String name = "";
        public String suit = "";
        public int rank;
        public String cat = "";
        public String effect = "";
    }

    // 选将
    public static class CSkill {
        public String name = "";
        public String desc = "";
    }

    public static class CHero {
        public String id = "";
        public String name = "";
        public String faction = "";
        public int maxHp;
        public final List<CSkill> skills = new ArrayList<>();
    }

    public static String state = "";
    public static String phase = "";
    public static String winner = "";
    public static int currentSeat = -1;
    public static String lastLog = "";
    public static final List<String> recentLogs = new ArrayList<>();
    public static int deckCount;
    public static int discardCount;
    public static String prompt = "";
    public static int mySeat = -1;
    public static String choicePrompt = "";
    public static final List<String> choiceOptions = new ArrayList<>();
    public static final List<String> discardTop = new ArrayList<>();
    public static int animFrom = -1;
    public static boolean showOverlay = false;
    public static final java.util.Map<String, Integer> HP_MAP = new java.util.HashMap<>();
    public static final java.util.Map<String, Integer> MAX_HP_MAP = new java.util.HashMap<>();
    public static final java.util.Map<String, Integer> HAND_MAP = new java.util.HashMap<>();
    public static int selectedHand = -1;
    public static int animTo = -1;
    public static String animCard = "";
    public static int animSeq = 0;
    public static boolean inGame = false;

    public static final List<CPlayer> players = new ArrayList<>();
    public static final List<CCard> hand = new ArrayList<>();
    public static final List<CHero> heroOptions = new ArrayList<>();

    private ClientGameState() {}

    public static void update(String json) {
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            state = str(root, "state");
            phase = str(root, "phase");
            winner = str(root, "winner");
            currentSeat = intOr(root, "currentSeat", -1);
            lastLog = str(root, "lastLog");
            recentLogs.clear();
            if (root.has("recentLogs") && root.get("recentLogs").isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray("recentLogs")) recentLogs.add(e.getAsString());
            }
            deckCount = intOr(root, "deckCount", 0);
            discardCount = intOr(root, "discardCount", 0);
            prompt = str(root, "prompt");
            mySeat = intOr(root, "mySeat", -1);
            choicePrompt = str(root, "choicePrompt");
            choiceOptions.clear();
            if (root.has("choiceOptions") && root.get("choiceOptions").isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray("choiceOptions")) choiceOptions.add(e.getAsString());
            }
            discardTop.clear();
            if (root.has("discardTop") && root.get("discardTop").isJsonArray()) {
                for (JsonElement e : root.getAsJsonArray("discardTop")) discardTop.add(e.getAsString());
            }
            animFrom = intOr(root, "animFrom", -1);
            animTo = intOr(root, "animTo", -1);
            animCard = str(root, "animCard");
            animSeq = intOr(root, "animSeq", 0);
            inGame = !state.isEmpty() && !"WAITING".equals(state);

            players.clear();
            JsonArray pa = root.getAsJsonArray("players");
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
            if (pa != null) {
                for (JsonElement e : pa) {
                    JsonObject o = e.getAsJsonObject();
                    CPlayer p = new CPlayer();
                    p.name = str(o, "name");
                    p.seat = intOr(o, "seat", 0);
                    p.team = str(o, "team");
                    p.hero = str(o, "hero");
                    p.heroId = str(o, "heroId");
                    p.chained = o.has("chained") && o.get("chained").getAsBoolean();
                    p.drunk = o.has("drunk") && o.get("drunk").getAsBoolean();
                    p.hp = intOr(o, "hp", 0);
                    p.maxHp = intOr(o, "maxHp", 0);
                    p.alive = o.has("alive") && o.get("alive").getAsBoolean();
                    p.handCount = intOr(o, "handCount", 0);
                    p.weapon = str(o, "weapon");
                    p.armor = str(o, "armor");
                    p.horsePlus = str(o, "horsePlus");
                    p.horseMinus = str(o, "horseMinus");
                    p.skills.clear();
                    if (o.has("skills") && o.get("skills").isJsonArray()) {
                        for (JsonElement se : o.getAsJsonArray("skills")) p.skills.add(se.getAsString());
                    }
                    p.slashUsed = intOr(o, "slashUsed", 0);
                    p.noSlashLimit = o.has("noSlashLimit") && o.get("noSlashLimit").getAsBoolean();
                    if (o.has("judged")) {
                        for (JsonElement j : o.getAsJsonArray("judged")) p.judged.add(j.getAsString());
                    }
                    players.add(p);
                }
            }

            hand.clear();
            JsonArray ha = root.getAsJsonArray("hand");
            if (ha != null) {
                for (JsonElement e : ha) {
                    JsonObject o = e.getAsJsonObject();
                    CCard c = new CCard();
                    c.id = str(o, "id");
                    c.name = str(o, "name");
                    c.suit = str(o, "suit");
                    c.rank = intOr(o, "rank", 0);
                    c.cat = str(o, "cat");
                    c.effect = str(o, "effect");
                    hand.add(c);
                }
            }

            heroOptions.clear();
            JsonArray ho = root.getAsJsonArray("heroOptions");
            if (ho != null) {
                for (JsonElement e : ho) {
                    JsonObject o = e.getAsJsonObject();
                    CHero h = new CHero();
                    h.id = str(o, "id");
                    h.name = str(o, "name");
                    h.faction = str(o, "faction");
                    h.maxHp = intOr(o, "maxHp", 0);
                    if (o.has("skills") && o.get("skills").isJsonArray()) {
                        for (JsonElement se : o.getAsJsonArray("skills")) {
                            JsonObject so = se.getAsJsonObject();
                            CSkill sk = new CSkill();
                            sk.name = str(so, "name");
                            sk.desc = str(so, "desc");
                            h.skills.add(sk);
                        }
                    }
                    heroOptions.add(h);
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
    }

    /** 我方是否当前回合 */
    public static boolean isMyTurn() {
        return players.stream().anyMatch(p -> p.seat == currentSeat && !isSpectator());
    }

    /** 简化:当前回合者是否为"自己"(通过 seat 匹配本地玩家不可靠,MVP 用 prompt/phase 判断 UI 按钮) */
    public static boolean isSpectator() { return !inGame; }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
    private static int intOr(JsonObject o, String key, int def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : def;
    }
}