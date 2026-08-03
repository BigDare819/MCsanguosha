package com.sanguosha.skill;

import com.sanguosha.card.CardCategory;
import com.sanguosha.card.CardDefinition;
import com.sanguosha.game.GamePlayer;
import com.sanguosha.game.SanguoshaGame;

/**
 * 标准版武将技能。
 * 第一批:自动生效的被动技能;转换/主动技能(武圣/制衡/仁德/苦肉等)占位,第二批实现。
 */
public final class StandardSkills {
    private StandardSkills() {}

    private static void register(Skill s) { SkillRegistry.register(s); }

    public static void init() {
        // ===== 张飞:咆哮 =====
        register(new Skill() {
            public String id() { return "paoxiao"; }
            public String name() { return "咆哮"; }
            public String description() { return "锁定技,你使用【杀】无次数限制。"; }
            public boolean noSlashLimit() { return true; }
        });

        // ===== 马超:马术 =====
        register(new Skill() {
            public String id() { return "mashu"; }
            public String name() { return "马术"; }
            public String description() { return "锁定技,你计算与其他角色的距离-1。"; }
            public int distanceModifier(GamePlayer p) { return -1; }
        });

        // ===== 周瑜:英姿 =====
        register(new Skill() {
            public String id() { return "yingzi"; }
            public String name() { return "英姿"; }
            public String description() { return "摸牌阶段,你多摸一张牌。"; }
            public void onDrawPhase(SanguoshaGame game, GamePlayer p, DrawInfo info) { info.amount += 1; }
        });

        // ===== 许褚:裸衣 =====
        register(new Skill() {
            public String id() { return "luoyi"; }
            public String name() { return "裸衣"; }
            public String description() { return "摸牌阶段少摸一张牌,本回合你使用【杀】或【决斗】造成的伤害+1。"; }
            public void onDrawPhase(SanguoshaGame game, GamePlayer p, DrawInfo info) { info.amount -= 1; }
        });

        // ===== 黄月英:集智 =====
        register(new Skill() {
            public String id() { return "jizhi"; }
            public String name() { return "集智"; }
            public String description() { return "当你使用一张非延时锦囊牌时,你摸一张牌。"; }
            public void onCardUsed(SanguoshaGame game, GamePlayer p, CardDefinition card) {
                if (card.category == CardCategory.TRICK_INSTANT) {
                    game.drawCards(p, 1);
                    game.log(p.name + " 触发【集智】,摸 1 张牌");
                }
            }
        });

        // ===== 郭嘉:天妒 =====
        register(new Skill() {
            public String id() { return "tiandu"; }
            public String name() { return "天妒"; }
            public String description() { return "当你的判定牌生效后,你获得之。"; }
            public void onJudge(SanguoshaGame game, GamePlayer p, CardDefinition judgeCard) {
                if (judgeCard != null) {
                    game.giveToHand(p, judgeCard);
                    game.log(p.name + " 触发【天妒】,获得判定牌【" + judgeCard.name + "】");
                }
            }
        });

        // ===== 郭嘉:遗计 =====
        register(new Skill() {
            public String id() { return "yiji"; }
            public String name() { return "遗计"; }
            public String description() { return "当你受到1点伤害后,你摸两张牌。"; }
            public void onDamageTaken(SanguoshaGame game, GamePlayer p, GamePlayer source, int amount, CardDefinition card) {
                game.drawCards(p, 2);
                game.log(p.name + " 触发【遗计】,摸 2 张牌");
            }
        });

        // ===== 曹操:奸雄 =====
        register(new Skill() {
            public String id() { return "jianxiong"; }
            public String name() { return "奸雄"; }
            public String description() { return "当你受到伤害后,你获得造成此伤害的牌。"; }
            public void onDamageTaken(SanguoshaGame game, GamePlayer p, GamePlayer source, int amount, CardDefinition card) {
                if (card != null) {
                    game.giveToHand(p, card);
                    game.log(p.name + " 触发【奸雄】,获得【" + card.name + "】");
                }
            }
        });

        // ===== 司马懿:反馈 =====
        register(new Skill() {
            public String id() { return "fankui"; }
            public String name() { return "反馈"; }
            public String description() { return "当你受到伤害后,你可以获得伤害来源的一张牌。"; }
            public void onDamageTaken(SanguoshaGame game, GamePlayer p, GamePlayer source, int amount, CardDefinition card) {
                if (source != null && source.isAlive()) {
                    game.stealOneFrom(p, source);
                    game.log(p.name + " 触发【反馈】,从 " + source.name + " 处获得一张牌");
                }
            }
        });

        // ===== 夏侯惇:刚烈 =====
        register(new Skill() {
            public String id() { return "ganglie"; }
            public String name() { return "刚烈"; }
            public String description() { return "当你受到伤害后,你可以判定,若结果不为红桃,伤害来源失去1点体力。"; }
            public void onDamageTaken(SanguoshaGame game, GamePlayer p, GamePlayer source, int amount, CardDefinition card) {
                if (source == null || !source.isAlive()) return;
                CardDefinition j = game.judgeFor(p);
                if (j != null && j.suit != com.sanguosha.card.CardSuit.HEART) {
                    game.log(p.name + " 【刚烈】判定:" + j.suit.cn + j.rankText() + ",触发反噬!");
                    game.damage(p, source, 1, com.sanguosha.game.SanguoshaGame.DamageType.NORMAL);
                } else {
                    game.log(p.name + " 【刚烈】判定为红桃,无事发生");
                }
            }
        });

        // ===== 吕蒙:克己 =====
        register(new Skill() {
            public String id() { return "keji"; }
            public String name() { return "克己"; }
            public String description() { return "若你于出牌阶段未使用或打出过【杀】,你可以跳过弃牌阶段。"; }
            public void onTurnEnd(SanguoshaGame game, GamePlayer p, TurnEndInfo info) {
                if (p.slashUsedThisTurn == 0) info.skipDiscard = true;
            }
        });

        // ===== 陆逊:连营 =====
        register(new Skill() {
            public String id() { return "lianying"; }
            public String name() { return "连营"; }
            public String description() { return "当你失去最后的手牌时,你摸一张牌。"; }
        });

        // ===== 陆逊:谦逊 =====
        register(new Skill() {
            public String id() { return "qianxun"; }
            public String name() { return "谦逊"; }
            public String description() { return "锁定技,你不能成为【顺手牵羊】和【乐不思蜀】的目标。"; }
            public boolean isImmuneTo(SanguoshaGame game, GamePlayer p, CardDefinition card) {
                return "snatch".equals(card.effect) || "indulgence".equals(card.effect);
            }
        });

        // ===== 貂蝉:闭月 =====
        register(new Skill() {
            public String id() { return "biyue"; }
            public String name() { return "闭月"; }
            public String description() { return "结束阶段,你摸一张牌。"; }
            public void onTurnEnd(SanguoshaGame game, GamePlayer p, TurnEndInfo info) {
                game.drawCards(p, 1);
                game.log(p.name + " 触发【闭月】,摸 1 张牌");
            }
        });

        // ===== 华雄:耀武 =====
        register(new Skill() {
            public String id() { return "yaowu"; }
            public String name() { return "耀武"; }
            public String description() { return "锁定技,当你受到红色【杀】造成的伤害后,伤害来源摸一张牌。"; }
            public void onDamageTaken(SanguoshaGame game, GamePlayer p, GamePlayer source, int amount, CardDefinition card) {
                if (card != null && "slash".equals(card.effect) && card.suit.color == 1 && source != null && source.isAlive()) {
                    game.drawCards(source, 1);
                    game.log(source.name + " 因【耀武】摸 1 张牌");
                }
            }
        });

        // ===== 诸葛亮:空城 =====
        register(new Skill() {
            public String id() { return "kongcheng"; }
            public String name() { return "空城"; }
            public String description() { return "锁定技,当你没有手牌时,你不能成为【杀】或【决斗】的目标。"; }
            public boolean isImmuneTo(SanguoshaGame game, GamePlayer p, CardDefinition card) {
                return p.hand.isEmpty() && ("slash".equals(card.effect) || "duel".equals(card.effect));
            }
        });

        // ===== 甄姬:洛神 =====
        register(new Skill() {
            public String id() { return "luoshen"; }
            public String name() { return "洛神"; }
            public String description() { return "准备阶段,你可以判定,若结果为黑色,你获得之(简化版:判定一次)。"; }
            public void onTurnStart(SanguoshaGame game, GamePlayer p) {
                CardDefinition j = game.judgeFor(p);
                if (j != null && j.suit.color == 0) {
                    game.giveToHand(p, j);
                    game.log(p.name + " 【洛神】判定:" + j.suit.cn + j.rankText() + ",获得之");
                } else if (j != null) {
                    game.log(p.name + " 【洛神】判定:" + j.suit.cn + j.rankText() + ",非黑色,结束");
                }
            }
        });

        // ===== 袁术:庸肆 =====
        register(new Skill() {
            public String id() { return "yongsi"; }
            public String name() { return "庸肆"; }
            public String description() { return "摸牌阶段多摸一张牌,弃牌阶段额外弃置一张牌(简化版)。"; }
            public void onDrawPhase(SanguoshaGame game, GamePlayer p, DrawInfo info) { info.amount += 1; }
            public void onTurnEnd(SanguoshaGame game, GamePlayer p, TurnEndInfo info) {
                if (p.hand.size() > p.maxHandSize()) {
                    CardDefinition c = p.hand.remove(p.hand.size() - 1);
                    game.discardToPile(c);
                    game.log(p.name + " 因【庸肆】额外弃置一张牌");
                }
            }
        });

        // ===== 华佗:急救 =====
        register(new Skill() {
            public String id() { return "jijiu"; }
            public String name() { return "急救"; }
            public String description() { return "你的回合外,你可以将一张红色牌当【桃】使用(濒死救援,后续版本完善)。"; }
        });

        // ===== 华佗:青囊 =====
        register(new Skill() {
            public String id() { return "qingnang"; }
            public String name() { return "青囊"; }
            public String description() { return "出牌阶段限一次,你可以弃置一张手牌并回复1点体力(交互技能,待实现)。"; }
        });

        // ===== 关羽:武圣 =====
        register(new Skill() {
            public String id() { return "wusheng"; }
            public String name() { return "武圣"; }
            public String description() { return "你可以将一张红色牌当【杀】使用(转换技能,待实现)。"; }
        });

        // ===== 赵云:龙胆 =====
        register(new Skill() {
            public String id() { return "longdan"; }
            public String name() { return "龙胆"; }
            public String description() { return "你可以将【杀】当【闪】、【闪】当【杀】使用(转换技能,待实现)。"; }
        });

        // ===== 甄姬:倾国 =====
        register(new Skill() {
            public String id() { return "qingguo"; }
            public String name() { return "倾国"; }
            public String description() { return "你可以将一张黑色手牌当【闪】使用或打出(转换技能,待实现)。"; }
        });

        // ===== 甘宁:奇袭 =====
        register(new Skill() {
            public String id() { return "qixi"; }
            public String name() { return "奇袭"; }
            public String description() { return "你可以将一张黑色牌当【过河拆桥】使用(转换技能,待实现)。"; }
        });

        // ===== 大乔:国色 =====
        register(new Skill() {
            public String id() { return "guose"; }
            public String name() { return "国色"; }
            public String description() { return "你可以将一张方块牌当【乐不思蜀】使用(转换技能,待实现)。"; }
        });

        // ===== 大乔:流离 =====
        register(new Skill() {
            public String id() { return "liuli"; }
            public String name() { return "流离"; }
            public String description() { return "当你成为【杀】的目标时,你可以弃置一张牌并将此【杀】转移给你攻击范围内的一名角色(交互技能,待实现)。"; }
        });

        // ===== 刘备:仁德 =====
        register(new Skill() {
            public String id() { return "rende"; }
            public String name() { return "仁德"; }
            public String description() { return "出牌阶段,你可以将任意张手牌交给其他角色,每阶段给出第二张时回复1点体力(交互技能,待实现)。"; }
        });

        // ===== 孙权:制衡 =====
        register(new Skill() {
            public String id() { return "zhiheng"; }
            public String name() { return "制衡"; }
            public String description() { return "出牌阶段限一次,你可以弃置任意张牌,然后摸等量的牌(交互技能,待实现)。"; }
        });

        // ===== 黄盖:苦肉 =====
        register(new Skill() {
            public String id() { return "kuro"; }
            public String name() { return "苦肉"; }
            public String description() { return "出牌阶段,你可以失去1点体力,然后摸两张牌(交互技能,待实现)。"; }
        });

        // ===== 貂蝉:离间 =====
        register(new Skill() {
            public String id() { return "lijian"; }
            public String name() { return "离间"; }
            public String description() { return "出牌阶段限一次,你可以弃置一张牌并选择两名男性角色,视为其中一名对另一名使用【决斗】(交互技能,待实现)。"; }
        });

        // ===== 周瑜:反间 =====
        register(new Skill() {
            public String id() { return "fanjian"; }
            public String name() { return "反间"; }
            public String description() { return "出牌阶段限一次,你可以令一名角色选择一种花色并抽取你的一张手牌,若花色不同则受到1点伤害(交互技能,待实现)。"; }
        });

        // ===== 马超:铁骑 =====
        register(new Skill() {
            public String id() { return "tieqi"; }
            public String name() { return "铁骑"; }
            public String description() { return "当你使用【杀】指定目标后,你可以进行判定,若结果为红色,目标不能使用【闪】响应。"; }
        });

        // ===== 吕布:无双 =====
        register(new Skill() {
            public String id() { return "wushuang"; }
            public String name() { return "无双"; }
            public String description() { return "锁定技,你使用【杀】需两张【闪】响应,你使用【决斗】对方需连续打出两张【杀】。"; }
        });

        // ===== 司马懿:鬼才 =====
        register(new Skill() {
            public String id() { return "guicai"; }
            public String name() { return "鬼才"; }
            public String description() { return "在一名角色的判定牌生效前,你可以打出一张手牌代替之(交互技能,待实现)。"; }
        });

        // ===== 诸葛亮:观星 =====
        register(new Skill() {
            public String id() { return "guanxing"; }
            public String name() { return "观星"; }
            public String description() { return "准备阶段,你可以观看牌堆顶的X张牌(X为存活角色数),然后以任意顺序置于牌堆顶或牌堆底(交互技能,待实现)。"; }
        });

        // ===== 黄月英:奇才 =====
        register(new Skill() {
            public String id() { return "qicai"; }
            public String name() { return "奇才"; }
            public String description() { return "锁定技,你使用锦囊牌无距离限制(锦囊距离限制简化后无影响)。"; }
        });

        // ===== 孙尚香:枭姬 =====
        register(new Skill() {
            public String id() { return "xiaoji"; }
            public String name() { return "枭姬"; }
            public String description() { return "当你失去装备区里的牌后,你摸两张牌(装备区变化监听,待实现)。"; }
        });

        // ===== 孙尚香:结姻 =====
        register(new Skill() {
            public String id() { return "jieyin"; }
            public String name() { return "结姻"; }
            public String description() { return "出牌阶段限一次,你可以弃置两张手牌并选择一名已受伤的男性角色,你与其各回复1点体力(交互技能,待实现)。"; }
        });

        // ===== 主公技占位(2v2 无主公,仅注册名称) =====
        register(new Skill() {
            public String id() { return "hujia"; }
            public String name() { return "护驾"; }
            public String description() { return "主公技(2v2 模式不生效)。"; }
        });
        register(new Skill() {
            public String id() { return "jijiang"; }
            public String name() { return "激将"; }
            public String description() { return "主公技(2v2 模式不生效)。"; }
        });
        register(new Skill() {
            public String id() { return "jiuyuan"; }
            public String name() { return "救援"; }
            public String description() { return "主公技(2v2 模式不生效)。"; }
        });
                // ===== 奶龙 =====
        register(new Skill() {
            public String id() { return "juhua"; }
            public String name() { return "\u5de8\u5316"; }
            public String description() { return "\u51fa\u724c\u9636\u6bb5\u9650\u4e00\u6b21,\u4e27\u7f6e\u4efb\u610f\u5f20\u624b\u724c\u5e76\u558a\u51fa\u300c\u6211\u662f\u5976\u9f99\u300d,\u589e\u52a0\u7b49\u91cf\u4f53\u529b\u4e0a\u9650;\u56de\u5408\u7ed3\u675f\u65f6\u56de\u590d\u672c\u56de\u5408\u9020\u6210\u4f24\u5bb3\u503c\u4f53\u529b\u6216\u6478\u4e24\u5f20\u724c(\u5f85\u5b9e\u73b0)\u3002"; }
        });
        register(new Skill() {
            public String id() { return "zhenglong"; }
            public String name() { return "\u4e89\u9f99"; }
            public String description() { return "\u9501\u5b9a\u6280,\u6bcf\u56de\u5408\u624b\u724c\u6570\u9996\u6b21\u53d8\u4e3a1\u65f6\u89e6\u53d1\u8bed\u97f3\u62a2\u7b54\u300c\u6211\u624d\u662f\u5976\u9f99\u300d(\u7ebf\u4e0b\u73a9\u6cd5,\u5f85\u5b9e\u73b0)\u3002"; }
        });
        // ===== 张雪峰 =====
        register(new Skill() {
            public String id() { return "qiaolezi"; }
            public String name() { return "\u5de7\u4e50\u5179"; }
            public String description() { return "\u5403\u4e00\u4e2a\u6843\u56de\u590d\u4e24\u70b9\u4f53\u529b(\u5f85\u5b9e\u73b0)\u3002"; }
        });
        register(new Skill() {
            public String id() { return "zuichunfazi"; }
            public String name() { return "\u5634\u5507\u53d1\u7d2b"; }
            public String description() { return "\u9501\u5b9a\u6280,\u56de\u5408\u7ed3\u675f\u65f6\u82e5\u672c\u56de\u5408\u51fa\u8fc7\u724c,\u51cf\u4e00\u70b9\u4f53\u529b(\u5f85\u5b9e\u73b0)\u3002"; }
        });
        register(new Skill() {
            public String id() { return "lajinmiwu"; }
            public String name() { return "\u62c9\u8fdb\u8ff7\u96fe"; }
            public String description() { return "\u6bcf\u56de\u5408\u9650\u4e00\u6b21,\u4e27\u7f6e\u4efb\u610f\u4e00\u5f20\u724c,\u4ee4\u4e00\u540d\u5176\u4ed6\u6b66\u5c06\u7ffb\u9762(\u5f85\u5b9e\u73b0)\u3002"; }
        });
        register(new Skill() {
            public String id() { return "weidi"; }
            public String name() { return "伪帝"; }
            public String description() { return "主公技(2v2 模式不生效)。"; }
        });
    }
}