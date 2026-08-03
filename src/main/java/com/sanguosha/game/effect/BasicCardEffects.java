package com.sanguosha.game.effect;

import com.sanguosha.card.CardDefinition;
import com.sanguosha.game.GamePlayer;
import com.sanguosha.game.SanguoshaGame;
import com.sanguosha.game.SanguoshaGame.DamageType;

/** 基本牌效果:杀 / 火杀 / 雷杀 / 闪 / 桃 / 酒 */
public final class BasicCardEffects {
    private BasicCardEffects() {}

    /** 藤甲免疫判断:目标装备藤甲且来源无青釭剑时,普通杀/南蛮/万箭无效 */
    public static boolean vineImmune(SanguoshaGame game, GamePlayer target, GamePlayer source) {
        if (target.armor == null || !"vine".equals(target.armor.effect)) return false;
        // 青釭剑无视防具
        return !(source.weapon != null && "qinggang".equals(source.weapon.effect));
    }

    public static void init() {
        // ============ 杀(普通) ============
        EffectRegistry.register("slash", new CardEffect() {
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                if (user == target) return false;
                if (user.team == target.team) return false;
                if (user.slashUsedThisTurn >= 1 && !user.noSlashLimit) return false;
                return user.distanceTo(target, game) <= 1 || user.weapon != null;
            }
            @Override public boolean requiresTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                user.slashUsedThisTurn++;
                game.log(user.name + " 对 " + target.name + " 使用了【杀】");
                slashResolve(game, user, target, card, DamageType.NORMAL, true);
            }
        });

        // ============ 火杀 ============
        EffectRegistry.register("fire_slash", new CardEffect() {
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                if (user == target) return false;
                if (user.team == target.team) return false;
                if (user.slashUsedThisTurn >= 1 && !user.noSlashLimit) return false;
                return user.distanceTo(target, game) <= 1 || user.weapon != null;
            }
            @Override public boolean requiresTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                user.slashUsedThisTurn++;
                game.log(user.name + " 对 " + target.name + " 使用了【火杀】");
                slashResolve(game, user, target, card, DamageType.FIRE, false);
            }
        });

        // ============ 雷杀 ============
        EffectRegistry.register("thunder_slash", new CardEffect() {
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                if (user == target) return false;
                if (user.team == target.team) return false;
                if (user.slashUsedThisTurn >= 1 && !user.noSlashLimit) return false;
                return user.distanceTo(target, game) <= 1 || user.weapon != null;
            }
            @Override public boolean requiresTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                user.slashUsedThisTurn++;
                game.log(user.name + " 对 " + target.name + " 使用了【雷杀】");
                slashResolve(game, user, target, card, DamageType.THUNDER, false);
            }
        });

        // ============ 闪(响应牌) ============
        EffectRegistry.register("jink", new CardEffect() {
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                return false; // 闪不能主动使用
            }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {}
        });

        // ============ 桃 ============
        EffectRegistry.register("peach", new CardEffect() {
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                if (user.hero == null) return false;
                return user == target && (user.hp < user.hero.maxHp || user.isDying());
            }
            @Override public boolean requiresTarget() { return false; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                user.hp = Math.min(user.hp + 1, user.hero.maxHp);
                game.log(user.name + " 使用了【桃】,恢复 1 点体力");
            }
        });

        // ============ 酒 ============
        EffectRegistry.register("analeptic", new CardEffect() {
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                if (user.hero == null) return false;
                if (user.isDying()) return true;                     // 濒死当桃
                return !user.drunk && game.phase() == com.sanguosha.game.GamePhase.PLAY; // 出牌阶段限一次
            }
            @Override public boolean requiresTarget() { return false; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                if (user.isDying()) {
                    user.hp = Math.min(user.hp + 1, user.hero.maxHp);
                    game.log(user.name + " 濒死时使用了【酒】,恢复 1 点体力");
                } else {
                    user.drunk = true;
                    game.log(user.name + " 使用了【酒】,本回合下一张杀伤害+1");
                }
            }
        });
    }

    /** 杀结算:流离转移 + 铁骑判定 + 无双双闪 */
    private static void slashResolve(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card, DamageType type, boolean vineCheck) {
        game.askLiuli(user, target, () -> slashCore(game, user, target, card, type, vineCheck), newTarget -> {
            game.log(target.name + " 将【杀】转移给 " + newTarget.name);
            slashCore(game, user, newTarget, card, type, vineCheck);
        });
    }

    /** 杀核心结算(铁骑/无双/伤害) */
    private static void slashCore(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card, DamageType type, boolean vineCheck) {
        if (!target.isAlive()) return;
        boolean wushuang = com.sanguosha.skill.SkillRegistry.has(user, "wushuang");
        // 铁骑:判定,非红桃则目标不能出闪
        if (com.sanguosha.skill.SkillRegistry.has(user, "tieqi")) {
            CardDefinition judge = game.judgeFor(user);
            if (judge != null && judge.suit != com.sanguosha.card.CardSuit.HEART) {
                game.log(user.name + "【铁骑】判定:" + judge.suit.cn + judge.rankText() + ",目标不能出闪");
                doSlashDamage(game, user, target, card, type, vineCheck);
                return;
            }
            game.log(user.name + "【铁骑】判定:" + judge.suit.cn + judge.rankText() + ",目标可出闪");
        }
        if (!wushuang) {
            game.awaitJink(target, () -> doSlashDamage(game, user, target, card, type, vineCheck));
        } else {
            game.log(user.name + "【无双】,目标需打出两张闪");
            game.awaitResponse(target, "jink", ok1 -> {
                if (!ok1) doSlashDamage(game, user, target, card, type, vineCheck);
                else game.awaitResponse(target, "jink", ok2 -> {
                    if (!ok2) doSlashDamage(game, user, target, card, type, vineCheck);
                });
            });
        }
    }

    /** 杀命中结算(酒加成/藤甲免疫) */
    private static void doSlashDamage(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card, DamageType type, boolean vineCheck) {
        int dmg = user.drunk ? 2 : 1;
        user.drunk = false;
        if (vineCheck && vineImmune(game, target, user)) {
            game.log(target.name + " 装备【藤甲】,【杀】无效");
            return;
        }
        game.damage(user, target, dmg, type, card);
    }

    /** 无效果占位牌 */
    public static class PassEffect implements CardEffect {
        @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
            game.log(user.name + " 使用了【" + card.name + "】(效果尚未实现)");
        }
    }
}