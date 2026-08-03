package com.sanguosha.game.effect;

import com.sanguosha.card.CardDefinition;
import com.sanguosha.game.GamePlayer;
import com.sanguosha.game.SanguoshaGame.DamageType;
import com.sanguosha.game.SanguoshaGame;

import java.util.List;

/** 锦囊牌效果 */
public final class TrickCardEffects {
    private TrickCardEffects() {}

    public static void init() {
        // ============ 决斗 ============
        EffectRegistry.register("duel", new CardEffect() {
            @Override public boolean requiresTarget() { return true; }
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                return user != target;
            }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                game.log(user.name + " 对 " + target.name + " 使用了【决斗】");
                duelLoop(game, user, target, true);
            }
        });

        // ============ 过河拆桥(自己选弃哪张) ============
        EffectRegistry.register("dismantlement", new CardEffect() {
            @Override public boolean requiresTarget() { return true; }
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                return user != target && (target.handCount() > 0 || target.weapon != null || target.armor != null
                        || target.horsePlus != null || target.horseMinus != null || !target.judgedZone.isEmpty());
            }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                java.util.List<String> opts = new java.util.ArrayList<>();
                java.util.List<Runnable> acts = new java.util.ArrayList<>();
                if (target.handCount() > 0) {
                    opts.add("手牌(随机)"); acts.add(() -> game.discardOneFrom(target));
                }
                if (target.weapon != null) {
                    CardDefinition lost = target.weapon;
                    opts.add("装备:" + lost.name);
                    acts.add(() -> { target.weapon = null; game.discardToPile(lost); game.onLoseEquip(target, lost); });
                }
                if (target.armor != null) {
                    CardDefinition lost = target.armor;
                    opts.add("装备:" + lost.name);
                    acts.add(() -> { target.armor = null; game.discardToPile(lost); game.onLoseEquip(target, lost); });
                }
                if (target.horsePlus != null) {
                    CardDefinition lost = target.horsePlus;
                    opts.add("装备:" + lost.name);
                    acts.add(() -> { target.horsePlus = null; game.discardToPile(lost); game.onLoseEquip(target, lost); });
                }
                if (target.horseMinus != null) {
                    CardDefinition lost = target.horseMinus;
                    opts.add("装备:" + lost.name);
                    acts.add(() -> { target.horseMinus = null; game.discardToPile(lost); game.onLoseEquip(target, lost); });
                }
                if (!target.judgedZone.isEmpty()) {
                    CardDefinition j = target.judgedZone.get(0);
                    opts.add("判定区:" + j.name);
                    acts.add(() -> { target.judgedZone.remove(j); game.discardToPile(j); });
                }
                game.awaitChoice(user, "选择要弃置 " + target.name + " 的哪张牌", opts.toArray(new String[0]), choice -> {
                    if (choice >= 0 && choice < acts.size()) {
                        acts.get(choice).run();
                        game.log(user.name + " 弃置了 " + target.name + " 的" + opts.get(choice));
                    }
                });
            }
        });

        // ============ 顺手牵羊(自己选拿哪张) ============
        EffectRegistry.register("snatch", new CardEffect() {
            @Override public boolean requiresTarget() { return true; }
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                if (user == target) return false;
                if (user.distanceTo(target, game) > 1) return false;
                return target.handCount() > 0 || target.weapon != null || target.armor != null
                        || target.horsePlus != null || target.horseMinus != null || !target.judgedZone.isEmpty();
            }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                java.util.List<String> opts = new java.util.ArrayList<>();
                java.util.List<Runnable> acts = new java.util.ArrayList<>();
                if (target.handCount() > 0) {
                    opts.add("手牌(随机)"); acts.add(() -> game.stealOneFrom(user, target));
                }
                if (target.weapon != null) {
                    CardDefinition lost = target.weapon;
                    opts.add("装备:" + lost.name);
                    acts.add(() -> { target.weapon = null; user.hand.add(lost); game.onLoseEquip(target, lost); });
                }
                if (target.armor != null) {
                    CardDefinition lost = target.armor;
                    opts.add("装备:" + lost.name);
                    acts.add(() -> { target.armor = null; user.hand.add(lost); game.onLoseEquip(target, lost); });
                }
                if (target.horsePlus != null) {
                    CardDefinition lost = target.horsePlus;
                    opts.add("装备:" + lost.name);
                    acts.add(() -> { target.horsePlus = null; user.hand.add(lost); game.onLoseEquip(target, lost); });
                }
                if (target.horseMinus != null) {
                    CardDefinition lost = target.horseMinus;
                    opts.add("装备:" + lost.name);
                    acts.add(() -> { target.horseMinus = null; user.hand.add(lost); game.onLoseEquip(target, lost); });
                }
                if (!target.judgedZone.isEmpty()) {
                    CardDefinition j = target.judgedZone.get(0);
                    opts.add("判定区:" + j.name);
                    acts.add(() -> { target.judgedZone.remove(j); user.hand.add(j); });
                }
                game.awaitChoice(user, "选择要获得 " + target.name + " 的哪张牌", opts.toArray(new String[0]), choice -> {
                    if (choice >= 0 && choice < acts.size()) {
                        acts.get(choice).run();
                        game.log(user.name + " 获得了 " + target.name + " 的" + opts.get(choice));
                    }
                });
            }
        });

        // ============ 无中生有 ============
        EffectRegistry.register("exnihilo", new CardEffect() {
            @Override public boolean isMultiTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                game.drawCards(user, 2);
                game.log(user.name + " 使用了【无中生有】,摸 2 张牌");
            }
        });

        // ============ 南蛮入侵 ============
        EffectRegistry.register("savage", new CardEffect() {
            @Override public boolean isMultiTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                game.log(user.name + " 使用了【南蛮入侵】");
                List<GamePlayer> enemies = game.aliveOpponents(user);
                for (GamePlayer t : enemies) {
                    game.awaitResponse(t, "slash", ok -> {
                        if (!ok && !BasicCardEffects.vineImmune(game, t, user)) {
                            game.damage(user, t, 1, DamageType.NORMAL);
                        } else if (!ok) {
                            game.log(t.name + " 装备【藤甲】,【南蛮入侵】无效");
                        }
                    });
                }
            }
        });

        // ============ 万箭齐发 ============
        EffectRegistry.register("archery", new CardEffect() {
            @Override public boolean isMultiTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                game.log(user.name + " 使用了【万箭齐发】");
                List<GamePlayer> enemies = game.aliveOpponents(user);
                for (GamePlayer t : enemies) {
                    game.awaitResponse(t, "jink", ok -> {
                        if (!ok && !BasicCardEffects.vineImmune(game, t, user)) {
                            game.damage(user, t, 1, DamageType.NORMAL);
                        } else if (!ok) {
                            game.log(t.name + " 装备【藤甲】,【万箭齐发】无效");
                        }
                    });
                }
            }
        });

        // ============ 桃园结义 ============
        EffectRegistry.register("amazing", new CardEffect() {
            @Override public boolean isMultiTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                game.log(user.name + " 使用了【桃园结义】");
                for (GamePlayer p : game.players()) {
                    if (p.isAlive()) {
                        p.hp = Math.min(p.hp + 1, p.hero.maxHp);
                    }
                }
            }
        });

        // ============ 五谷丰登(简化:每人摸 1 张) ============
        EffectRegistry.register("bountiful", new CardEffect() {
            @Override public boolean isMultiTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                game.log(user.name + " 使用了【五谷丰登】(简化版:每人摸 1 张)");
                for (GamePlayer p : game.players()) {
                    if (p.isAlive()) game.drawCards(p, 1);
                }
            }
        });

        // ============ 无懈可击(MVP:占位,完整响应链后续实现) ============
        EffectRegistry.register("nullification", new CardEffect() {
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                game.log(user.name + " 使用了【无懈可击】(完整响应链后续版本实现)");
            }
        });

        // ============ 借刀杀人(MVP:简化跳过) ============
        EffectRegistry.register("collateral", new CardEffect() {
            @Override public boolean requiresTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                game.log(user.name + " 使用了【借刀杀人】(完整效果后续版本实现)");
            }
        });

        // ============ 乐不思蜀 ============
        EffectRegistry.register("indulgence", new CardEffect() {
            @Override public boolean requiresTarget() { return true; }
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                return user != target;
            }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                target.judgedZone.add(card);
                game.log(user.name + " 对 " + target.name + " 使用了【乐不思蜀】(进入判定区)");
            }
        });

        // ============ 兵粮寸断(判定区,判定非梅花跳过摸牌) ============
        EffectRegistry.register("bingliangcunduan", new CardEffect() {
            @Override public boolean requiresTarget() { return true; }
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                return user != target && user.distanceTo(target, game) <= 1;
            }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                target.judgedZone.add(card);
                game.log(user.name + " 对 " + target.name + " 使用了【兵粮寸断】(进入判定区)");
            }
        });

        // ============ 铁索连环(横置/重置一名角色) ============
        EffectRegistry.register("iron_chain", new CardEffect() {
            @Override public boolean requiresTarget() { return true; }
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                return target.isAlive(); // 可横置/重置任意存活角色(含自己)
            }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                target.chained = !target.chained;
                game.log(target.name + (target.chained ? " 被横置(铁索连环)" : " 被重置(铁索连环)"));
            }
        });

        // ============ 火攻 ============
        EffectRegistry.register("huogong", new CardEffect() {
            @Override public boolean requiresTarget() { return true; }
            @Override public boolean canUse(SanguoshaGame game, GamePlayer user, GamePlayer target) {
                return target.isAlive() && target.handCount() > 0;
            }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                CardDefinition shown = target.hand.get(0); // 目标展示一张手牌
                game.log(target.name + " 展示手牌【" + shown.name + "】" + shown.suit.cn + shown.rankText());
                for (CardDefinition c : new java.util.ArrayList<>(user.hand)) {
                    if (c.suit == shown.suit) {
                        user.hand.remove(c);
                        game.discardToPile(c);
                        game.log(user.name + " 弃置【" + c.name + "】,对 " + target.name + " 造成 1 点火焰伤害!");
                        game.damage(user, target, 1, SanguoshaGame.DamageType.FIRE, card);
                        return;
                    }
                }
                game.log(user.name + " 没有相同花色的手牌,【火攻】未造成伤害");
            }
        });

        // ============ 闪电 ============
        EffectRegistry.register("lightning", new CardEffect() {
            @Override public boolean isMultiTarget() { return true; }
            @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {
                user.judgedZone.add(card);
                game.log(user.name + " 使用了【闪电】(进入判定区)");
            }
        });
    }

    /** 公开决斗入口(离间等技能使用):视为 a 对 b 使用决斗 */
    public static void duelBetween(SanguoshaGame game, GamePlayer a, GamePlayer b) {
        duelLoop(game, a, b, true);
    }

    /** 决斗循环:当前需要出杀的玩家不出则受伤(吕布无双需两张杀) */
    private static void duelLoop(SanguoshaGame game, GamePlayer a, GamePlayer b, boolean bActsNow) {
        GamePlayer current = bActsNow ? b : a;
        GamePlayer other = bActsNow ? a : b;
        boolean wushuang = com.sanguosha.skill.SkillRegistry.has(current, "wushuang");
        game.awaitResponse(current, "slash", ok -> {
            if (!ok) {
                game.damage(other, current, 1, DamageType.NORMAL);
            } else if (wushuang) {
                game.log(current.name + "【无双】,需再打出一张杀");
                game.awaitResponse(current, "slash", ok2 -> {
                    if (!ok2) {
                        game.damage(other, current, 1, DamageType.NORMAL);
                    } else {
                        duelLoop(game, other, current, false);
                    }
                });
            } else {
                duelLoop(game, other, current, false);
            }
        });
    }
}