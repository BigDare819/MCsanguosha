package com.sanguosha.game.effect;

import com.sanguosha.card.CardDefinition;

import java.util.HashMap;
import java.util.Map;

/** 效果注册表:effect key -> 处理器 */
public final class EffectRegistry {
    private static final Map<String, CardEffect> EFFECTS = new HashMap<>();
    private EffectRegistry() {}

    public static void register(String key, CardEffect effect) { EFFECTS.put(key, effect); }

    public static CardEffect get(CardDefinition card) {
        return get(card.effect);
    }

    /** 按效果 key 获取处理器(转换技能用) */
    public static CardEffect get(String effectKey) {
        CardEffect e = EFFECTS.get(effectKey);
        return e == null ? new BasicCardEffects.PassEffect() : e;
    }

    public static boolean has(String key) { return EFFECTS.containsKey(key); }

    /** 初始化所有效果 */
    public static void init() {
        BasicCardEffects.init();
        TrickCardEffects.init();
        EquipmentEffects.init();
    }
}