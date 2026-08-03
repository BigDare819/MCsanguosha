package com.sanguosha.game;

import com.sanguosha.card.CardDefinition;
import com.sanguosha.hero.HeroDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 一名玩家的游戏状态(服务器权威) */
public class GamePlayer {
    public final UUID uuid;
    public final String name;
    public int seat;          // 座位 0-3
    public Team team;

    public HeroDefinition hero;
    public int hp;
    public boolean alive = true;

    public final List<CardDefinition> hand = new ArrayList<>();
    public CardDefinition weapon;
    public CardDefinition armor;
    public CardDefinition horsePlus;  // 防御马
    public CardDefinition horseMinus; // 进攻马
    public final List<CardDefinition> judgedZone = new ArrayList<>(); // 判定区

    public int slashUsedThisTurn = 0;
    public boolean noSlashLimit = false;   // 诸葛连弩/咆哮
    public boolean chained = false;        // 铁索连环:横置
    public boolean drunk = false;          // 酒:本回合下一张杀伤害+1
public final java.util.Set<String> skillsUsedThisTurn = new java.util.HashSet<>(); // 本回合已使用的限一次技能
public int rendeGiven = 0; // 仁德:本回合给出牌数
public boolean skipDraw = false; // 兵粮寸断:跳过摸牌阶段

    public GamePlayer(UUID uuid, String name, int seat) {
        this.uuid = uuid;
        this.name = name;
        this.seat = seat;
    }

    public void setHero(HeroDefinition hero) {
        this.hero = hero;
        this.hp = hero.maxHp;
    }

    public boolean isAlive() { return alive; }
    public boolean isDying() { return hp <= 0 && alive; }

    public int handCount() { return hand.size(); }
    public int maxHandSize() { return hero == null ? 0 : hp; }

    /** 计算到目标玩家之间的距离(2v2 欢乐成双:同队相邻距离为 1) */
    public int distanceTo(GamePlayer other, SanguoshaGame game) {
        int n = game.players().size();
        if (n == 0 || !alive || !other.alive) return Integer.MAX_VALUE;
        int diff = Math.abs(seat - other.seat);
        int dist = Math.min(diff, n - diff);
        if (this.team == other.team) dist = Math.min(dist, 1); // 同队相邻
        // 马术技能:距离 -1
        for (com.sanguosha.skill.Skill s : com.sanguosha.skill.SkillRegistry.of(this)) {
            dist = Math.max(1, dist + s.distanceModifier(this));
        }
        // 进攻马: 自己对他人距离 -1
        if (horseMinus != null) dist = Math.max(1, dist - 1);
        // 防御马: 他人对自己距离 +1
        if (other.horsePlus != null) dist += 1;
        // 武器范围限制最大攻击距离
        int range = weapon == null ? 1 : weaponRange(weapon.name);
        return Math.min(dist, Math.max(1, range));
    }

    private static int weaponRange(String name) {
        return switch (name) {
            case "诸葛连弩" -> 1;
            case "青釭剑", "雌雄双股剑", "丈八蛇矛", "古锭刀" -> 2;
            case "青龙偃月刀", "贯石斧", "方天画戟" -> 3;
            case "朱雀羽扇" -> 4;
            case "麒麟弓" -> 5;
            default -> 1;
        };
    }
}