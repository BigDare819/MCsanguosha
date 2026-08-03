package com.sanguosha.skill;

import com.sanguosha.game.GamePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 技能注册表 */
public final class SkillRegistry {
    private static final Map<String, Skill> SKILLS = new HashMap<>();
    private SkillRegistry() {}

    public static void register(Skill s) { SKILLS.put(s.id(), s); }

    public static Skill get(String id) { return SKILLS.get(id); }

    public static boolean has(GamePlayer p, String id) {
        if (p.hero == null) return false;
        return p.hero.skills.contains(id);
    }

    /** 玩家拥有的所有技能实例 */
    public static List<Skill> of(GamePlayer p) {
        List<Skill> list = new ArrayList<>();
        if (p.hero == null) return list;
        for (String id : p.hero.skills) {
            Skill s = SKILLS.get(id);
            if (s != null) list.add(s);
        }
        return list;
    }

    public static void init() { StandardSkills.init(); }
}