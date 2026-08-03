package com.sanguosha.hero;

import java.util.List;

/** 武将定义 */
public class HeroDefinition {
    public final String id;
    public final String name;      // 中文名,如 "关羽"
    public final Faction faction;
    public final int maxHp;
    public final List<String> skills; // 技能 key 列表(对应 game.skill 包)
    public final String texturePath;  // 贴图资源路径

    public HeroDefinition(String id, String name, Faction faction, int maxHp, List<String> skills) {
        this.id = id;
        this.name = name;
        this.faction = faction;
        this.maxHp = maxHp;
        this.skills = List.copyOf(skills);
        this.texturePath = "sanguosha:textures/hero/" + id + ".png";
    }
}