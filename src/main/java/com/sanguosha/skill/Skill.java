package com.sanguosha.skill;

import com.sanguosha.card.CardDefinition;
import com.sanguosha.game.GamePlayer;
import com.sanguosha.game.SanguoshaGame;

/** 武将技能接口(各触发点默认空实现) */
public interface Skill {
    String id();
    String name();
    String description();

    /** 摸牌阶段:可修改摸牌数量 */
    default void onDrawPhase(SanguoshaGame game, GamePlayer p, DrawInfo info) {}

    /** 回合开始(准备阶段) */
    default void onTurnStart(SanguoshaGame game, GamePlayer p) {}

    /** 回合结束 */
    default void onTurnEnd(SanguoshaGame game, GamePlayer p, TurnEndInfo info) {}

    /** 使用一张牌后 */
    default void onCardUsed(SanguoshaGame game, GamePlayer p, CardDefinition card) {}

    /** 受到伤害后(扣血前调用) */
    default void onDamageTaken(SanguoshaGame game, GamePlayer p, GamePlayer source, int amount, CardDefinition card) {}

    /** 一次判定结算后 */
    default void onJudge(SanguoshaGame game, GamePlayer p, CardDefinition judgeCard) {}

    /** 失去手牌后(remaining 为剩余手牌数) */
    default void onHandCardLost(SanguoshaGame game, GamePlayer p, int remaining) {}

    /** 对该牌免疫(不能成为目标) */
    default boolean isImmuneTo(SanguoshaGame game, GamePlayer p, CardDefinition card) { return false; }

    /** 距离修正(马术 -1) */
    default int distanceModifier(GamePlayer p) { return 0; }

    /** 出杀无次数限制(咆哮/诸葛连弩) */
    default boolean noSlashLimit() { return false; }

    /** 摸牌阶段是否跳过(张辽突袭等,第二批实现) */
    default boolean skipDrawPhase() { return false; }

    /** 弃牌阶段是否跳过(克己) */
    default boolean skipDiscardPhase() { return false; }

    /** 摸牌阶段摸牌数量信息 */
    class DrawInfo {
        public int amount;
        public DrawInfo(int amount) { this.amount = amount; }
    }

    /** 回合结束信息 */
    class TurnEndInfo {
        public boolean skipDiscard = false;
    }
}