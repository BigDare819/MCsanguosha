package com.sanguosha.game.effect;

import com.sanguosha.card.CardDefinition;
import com.sanguosha.game.GamePlayer;
import com.sanguosha.game.SanguoshaGame;

/** 卡牌效果处理器 */
public interface CardEffect {
    /** 该牌是否可以由 user 对 target 使用(距离/限制检查) */
    default boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) { return true; }

    /** 执行效果。调用时该牌已从手牌移除。 */
    void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card);

    /** 是否必须有目标才能使用(杀/决斗等单目标牌) */
    default boolean requiresTarget() { return false; }

    /** 是否可以选择任意目标数量(无中/桃园等) */
    default boolean isMultiTarget() { return false; }
}