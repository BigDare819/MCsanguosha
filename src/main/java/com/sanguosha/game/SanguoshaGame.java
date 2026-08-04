package com.sanguosha.game;

import com.sanguosha.SanguoshaMod;
import com.sanguosha.card.CardDefinition;
import com.sanguosha.card.CardSuit;
import com.sanguosha.card.Cards;
import com.sanguosha.game.effect.EffectRegistry;
import com.sanguosha.game.effect.EquipmentEffects;
import com.sanguosha.hero.HeroDefinition;
import com.sanguosha.hero.Heroes;
import com.sanguosha.skill.Skill;
import com.sanguosha.skill.SkillRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 三国杀 2v2 游戏状态机(服务器权威)。
 * 欢乐成双规则:座位 0-3,0/3 红方,1/2 蓝方;同队相邻距离视为 1。
 */
public class SanguoshaGame {
    public enum State { WAITING, HERO_SELECT, RUNNING, FINISHED }

    /** 选择请求:等待某玩家在多个选项中选一个(花色/改判/流离/观星等) */
    public static class ChoiceRequest {
        public final GamePlayer target;
        public final String prompt;
        public final String[] options;
        public final Consumer<Integer> handler;
        public ChoiceRequest(GamePlayer target, String prompt, String[] options, Consumer<Integer> handler) {
            this.target = target; this.prompt = prompt; this.options = options; this.handler = handler;
        }
    }

    /** 响应请求:等待某玩家打出指定类型的牌 */
    public static class ResponseRequest {
        public final GamePlayer target;
        public final String type;      // "jink" / "slash"
        public final Consumer<Boolean> handler; // true=打出了, false=放弃
        public ResponseRequest(GamePlayer target, String type, Consumer<Boolean> handler) {
            this.target = target; this.type = type; this.handler = handler;
        }
    }

    private final List<GamePlayer> players = new ArrayList<>();
    private final Map<UUID, GamePlayer> byUuid = new HashMap<>();
    private final List<CardDefinition> deck = new ArrayList<>();
    private final List<CardDefinition> discard = new ArrayList<>();

    private State state = State.WAITING;
    private int currentIdx = 0;
    private GamePhase phase = GamePhase.PREPARE;
    private Team winner;
    private String lastLog = "";
    private final List<String> logHistory = new ArrayList<>();

    private final Deque<ResponseRequest> responseQueue = new ArrayDeque<>();
    private ResponseRequest currentResponse;
    private ChoiceRequest pendingChoice;
    private final Deque<ChoiceRequest> choiceQueue = new ArrayDeque<>();

    private final Map<UUID, List<HeroDefinition>> heroOptions = new HashMap<>();
    private final Map<UUID, Boolean> heroChosen = new HashMap<>();
    private int heroReadyCount = 0;

    private GamePlayer pendingDiscardPlayer;   // 弃牌阶段等待的玩家
    private int pendingDiscardCount;           // 还需弃置的张数

    // 出牌动画事件(from/to/card 对全员广播;animSeq 递增防重复播放)
    private int lastAnimFrom = -1;
    private int lastAnimTo = -1;
    private String lastAnimCard = "";
    private int animSeq = 0;

    // ================= 加入 / 开始 =================

    public boolean join(ServerPlayer sp) {
        if (state != State.WAITING || players.size() >= 4) return false;
        if (byUuid.containsKey(sp.getUUID())) return false;
        GamePlayer p = new GamePlayer(sp.getUUID(), sp.getGameProfile().getName(), players.size());
        // 欢乐成双:seat 0/3 = RED, 1/2 = BLUE
        p.team = (p.seat == 0 || p.seat == 3) ? Team.RED : Team.BLUE;
        players.add(p);
        byUuid.put(p.uuid, p);
        log(p.name + " 加入游戏(" + p.team.cn + " 第" + (p.seat + 1) + "位)");
        return true;
    }

    public void leave(ServerPlayer sp) {
        GamePlayer p = byUuid.remove(sp.getUUID());
        if (p == null) return;
        players.remove(p);
        // 重排座位
        for (int i = 0; i < players.size(); i++) {
            players.get(i).seat = i;
            players.get(i).team = (i == 0 || i == 3) ? Team.RED : Team.BLUE;
        }
        log(sp.getGameProfile().getName() + " 离开游戏");
    }

    /** 开始选将(至少 1 人即可开始,便于单机测试;2v2 建议 4 人) */
    public void start() {
        if (state != State.WAITING) return;
        if (players.isEmpty()) { log("没有玩家,无法开始"); return; }
        state = State.HERO_SELECT;
        List<HeroDefinition> pool = new ArrayList<>(Heroes.all());
        Collections.shuffle(pool);
        // 每人 5 张,全局不重复(依次分发洗好的将池)
        int perPlayer = 5;
        int idx = 0;
        for (GamePlayer p : players) {
            List<HeroDefinition> opts = new ArrayList<>();
            for (int i = 0; i < perPlayer && idx < pool.size(); i++) opts.add(pool.get(idx++));
            heroOptions.put(p.uuid, opts);
            heroChosen.put(p.uuid, false);
        }
        log("选将开始!每人从 5 名武将中选择 1 名(全局不重复)");
    }

    public void selectHero(ServerPlayer sp, String heroId) {
        if (state != State.HERO_SELECT) return;
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || heroChosen.getOrDefault(p.uuid, false)) return;
        HeroDefinition hero = null;
        for (HeroDefinition h : heroOptions.getOrDefault(p.uuid, List.of())) {
            if (h.id.equals(heroId)) hero = h;
        }
        if (hero == null) return;
        p.setHero(hero);
        heroChosen.put(p.uuid, true);
        heroReadyCount++;
        log(p.name + " 选择了武将【" + hero.name + "】");
        if (heroReadyCount >= players.size()) {
            beginGame();
        }
    }

    private void beginGame() {
        state = State.RUNNING;
        buildDeck();
        for (GamePlayer p : players) drawCards(p, 4);
        currentIdx = 0;
        log("游戏开始!率先行动: " + players.get(0).name + " (" + players.get(0).team.cn + ")");
        beginTurn(players.get(0));
        syncAll();
    }

    // ================= 牌堆 =================

    private void buildDeck() {
        deck.clear();
        discard.clear();
        deck.addAll(Cards.all());
        Collections.shuffle(deck);
    }

    public CardDefinition drawTop() {
        if (deck.isEmpty()) {
            deck.addAll(discard);
            discard.clear();
            Collections.shuffle(deck);
        }
        if (deck.isEmpty()) return null;
        return deck.remove(deck.size() - 1);
    }

    public void drawCards(GamePlayer p, int n) {
        for (int i = 0; i < n; i++) {
            CardDefinition c = drawTop();
            if (c != null) p.hand.add(c);
        }
    }

    // ================= 回合流程 =================

    private void beginTurn(GamePlayer p) {
        if (state != State.RUNNING) return;
        currentIdx = players.indexOf(p);
        phase = GamePhase.PREPARE;
        p.slashUsedThisTurn = 0;
        p.drunk = false;
        p.skillsUsedThisTurn.clear();
        p.rendeGiven = 0;
        p.skipDraw = false;
        p.noSlashLimit = p.weapon != null && "crossbow".equals(p.weapon.effect);
        for (Skill s : SkillRegistry.of(p)) if (s.noSlashLimit()) p.noSlashLimit = true;
        log("===== " + p.name + " 的回合开始 =====");
        for (Skill s : SkillRegistry.of(p)) s.onTurnStart(this, p);
        // 观星:查看牌堆顶3张,选择放回顶部或底部
        if (SkillRegistry.has(p, "guanxing") && p.isAlive() && deck.size() >= 3) {
            CardDefinition t0 = deck.get(deck.size() - 1);
            CardDefinition t1 = deck.get(deck.size() - 2);
            CardDefinition t2 = deck.get(deck.size() - 3);
            String names = t0.name + "、" + t1.name + "、" + t2.name;
            awaitChoice(p, "【观星】牌堆顶:" + names + " 如何放置?", new String[]{"放回牌堆顶", "置于牌堆底"}, choice -> {
                if (choice == 1) {
                    deck.remove(deck.size() - 1);
                    deck.remove(deck.size() - 1);
                    deck.remove(deck.size() - 1);
                    deck.add(0, t2);
                    deck.add(0, t1);
                    deck.add(0, t0);
                    log(p.name + " 观星:将3张牌置于牌堆底");
                } else {
                    log(p.name + " 观星:保持牌序");
                }
                resolveJudgePhase(p);
            });
            return;
        }
        resolveJudgePhase(p);
    }

    /** 判定区结算 */
    private void resolveJudgePhase(GamePlayer p) {
        if (!p.isAlive()) { endTurn(p); return; }
        List<CardDefinition> judged = new ArrayList<>(p.judgedZone);
        if (judged.isEmpty()) { drawPhase(p); return; }
        resolveJudgedCard(p, judged, 0);
    }

    /** 逐张结算判定区(回调式,支持鬼才改判) */
    private void resolveJudgedCard(GamePlayer p, List<CardDefinition> judged, int idx) {
        CardDefinition j = judged.get(idx);
        p.judgedZone.remove(j);
        CardDefinition[] jc = new CardDefinition[1];
        jc[0] = drawTop();
        if (jc[0] == null) { nextJudge(p, judged, idx); return; }
        discard.add(jc[0]);
        for (Skill s : SkillRegistry.of(p)) s.onJudge(this, p, jc[0]);
        askGuicai(p, jc, () -> {
            if (settleJudge(p, j, jc[0])) nextJudge(p, judged, idx);
        });
    }

    private void nextJudge(GamePlayer p, List<CardDefinition> judged, int idx) {
        if (idx + 1 < judged.size()) resolveJudgedCard(p, judged, idx + 1);
        else drawPhase(p);
    }

    /** 结算一张判定牌,返回是否继续后续判定 */
    private boolean settleJudge(GamePlayer p, CardDefinition j, CardDefinition judgeCard) {
        switch (j.effect) {
            case "indulgence" -> {
                log(p.name + " 【乐不思蜀】判定:" + judgeCard.suit.cn + judgeCard.rankText());
                if (judgeCard.suit != CardSuit.HEART) {
                    log(p.name + " 判定失败,跳过出牌阶段!");
                    phase = GamePhase.DISCARD;
                    int need = p.hand.size() - p.maxHandSize();
                    if (need > 0) {
                        pendingDiscardPlayer = p;
                        pendingDiscardCount = need;
                        log(p.name + " 需要弃置 " + need + " 张手牌(点击手牌弃置)");
                        syncAll();
                    } else {
                        finishTurn(p);
                    }
                    return false;
                }
                log(p.name + " 判定成功,【乐不思蜀】被弃置");
            }
            case "bingliangcunduan" -> {
                log(p.name + " 【兵粮寸断】判定:" + judgeCard.suit.cn + judgeCard.rankText());
                if (judgeCard.suit != CardSuit.CLUB) {
                    log(p.name + " 判定失败,跳过摸牌阶段!");
                    p.skipDraw = true;
                } else {
                    log(p.name + " 判定成功,【兵粮寸断】被弃置");
                }
            }
            case "lightning" -> {
                log(p.name + " 【闪电】判定:" + judgeCard.suit.cn + judgeCard.rankText());
                if (judgeCard.suit == CardSuit.SPADE && judgeCard.rank >= 2 && judgeCard.rank <= 9) {
                    log(p.name + " 被【闪电】击中,受到 3 点雷电伤害!");
                    damage(null, p, 3, DamageType.THUNDER);
                } else {
                    log(p.name + " 躲过【闪电】,闪电传递给下家");
                    GamePlayer next = nextAliveAfter(p);
                    if (next != null) next.judgedZone.add(j);
                }
            }
            default -> {}
        }
        return true;
    }

    private void drawPhase(GamePlayer p) {
        if (!p.isAlive()) return;
        phase = GamePhase.DRAW;
        if (p.skipDraw) {
            p.skipDraw = false;
            log(p.name + " 因【兵粮寸断】跳过摸牌阶段");
            phase = GamePhase.PLAY;
            syncAll();
            return;
        }
        Skill.DrawInfo info = new Skill.DrawInfo(2);
        for (Skill s : SkillRegistry.of(p)) s.onDrawPhase(this, p, info);
        int drawN = Math.max(0, info.amount);
        drawCards(p, drawN);
        log(p.name + " 摸了 " + drawN + " 张牌");
        phase = GamePhase.PLAY;
        syncAll();
    }

    private void endTurn(GamePlayer p) {
        if (!p.isAlive()) return;
        Skill.TurnEndInfo tei = new Skill.TurnEndInfo();
        for (Skill s : SkillRegistry.of(p)) s.onTurnEnd(this, p, tei);
        if (tei.skipDiscard) {
            log(p.name + " 因【克己】跳过弃牌阶段");
            finishTurn(p);
            return;
        }
        int need = p.hand.size() - p.maxHandSize();
        if (need > 0) {
            // 进入弃牌阶段,等待玩家手动弃牌
            phase = GamePhase.DISCARD;
            pendingDiscardPlayer = p;
            pendingDiscardCount = need;
            log(p.name + " 需要弃置 " + need + " 张手牌(点击手牌弃置)");
            syncAll();
            return;
        }
        finishTurn(p);
    }

    private void finishTurn(GamePlayer p) {
        phase = GamePhase.END;
        syncAll();
        GamePlayer next = nextAliveAfter(p);
        if (next != null && state == State.RUNNING) beginTurn(next);
    }

    private void discardToLimit(GamePlayer p) {
        while (p.hand.size() > p.maxHandSize()) {
            CardDefinition c = p.hand.remove(p.hand.size() - 1);
            discard.add(c);
        }
    }

    private GamePlayer nextAliveAfter(GamePlayer p) {
        int n = players.size();
        for (int i = 1; i <= n; i++) {
            GamePlayer q = players.get((p.seat + i) % n);
            if (q.isAlive()) return q;
        }
        return null;
    }

    // ================= 出牌 =================

    public boolean playCard(ServerPlayer sp, int cardIndex, int targetSeat) {
        GamePlayer user = byUuid.get(sp.getUUID());
        if (user == null || state != State.RUNNING || phase != GamePhase.PLAY) return false;
        if (!user.isAlive() || user.seat != currentIdx) return false;
        if (cardIndex < 0 || cardIndex >= user.hand.size()) return false;

        CardDefinition card = user.hand.get(cardIndex);
        GamePlayer target = targetSeat >= 0 && targetSeat < players.size() ? players.get(targetSeat) : null;

        var effect = EffectRegistry.get(card);
        if (card.category.isEquip()) {
            useEquip(user, card);
            return true;
        }
        if (effect.requiresTarget() && target == null) return false;
        if (effect.requiresTarget() && (!target.isAlive() || !effect.canUse(this, user, target))) return false;
        if (target != null) {
            for (Skill s : SkillRegistry.of(target)) {
                if (s.isImmuneTo(this, target, card)) {
                    log(target.name + " 因【" + s.name() + "】免疫【" + card.name + "】");
                    return false;
                }
            }
        }
        if (effect.isMultiTarget()) {
            // 无目标锦囊
        } else if (!effect.requiresTarget()) {
            if (!effect.canUse(this, user, target == null ? user : target)) return false;
        }

        user.hand.remove(cardIndex);
        discard.add(card);
        lastAnimFrom = user.seat;
        lastAnimTo = target == null ? user.seat : target.seat;
        lastAnimCard = card.name;
        animSeq++;
        effect.use(this, user, target == null ? user : target, card);
        for (Skill s : SkillRegistry.of(user)) s.onCardUsed(this, user, card);
        checkLianying(user);
        syncAll();
        return true;
    }

    private void useEquip(GamePlayer p, CardDefinition card) {
        CardDefinition replaced = null;
        switch (card.category) {
            case EQUIP_WEAPON -> { replaced = p.weapon; p.weapon = card; }
            case EQUIP_ARMOR -> { replaced = p.armor; p.armor = card; }
            case EQUIP_HORSE_PLUS -> { replaced = p.horsePlus; p.horsePlus = card; }
            case EQUIP_HORSE_MINUS -> { replaced = p.horseMinus; p.horseMinus = card; }
            default -> {}
        }
        if (replaced != null) {
            discard.add(replaced);
            onLoseEquip(p, replaced);
        }
        p.hand.remove(card);
        checkLianying(p);
        var h = EffectRegistry.get(card);
        if (h instanceof EquipmentEffects.EquipHandler eq) eq.onEquip(this, p);
        if (replaced != null && EffectRegistry.get(replaced) instanceof EquipmentEffects.EquipHandler eq2) eq2.onUnequip(this, p);
        log(p.name + " 装备了【" + card.name + "】");
    }

    public void passTurn(ServerPlayer sp) {
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || state != State.RUNNING || phase != GamePhase.PLAY) return;
        if (p.seat != currentIdx) return;
        log(p.name + " 结束了出牌阶段");
        endTurn(p);
    }

    public boolean discardCard(ServerPlayer sp, int cardIndex) {
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || cardIndex < 0 || cardIndex >= p.hand.size()) return false;
        CardDefinition c = p.hand.remove(cardIndex);
        discard.add(c);
        log(p.name + " 弃置了【" + c.name + "】");
        checkLianying(p);
        if (pendingDiscardPlayer == p) {
            pendingDiscardCount--;
            if (pendingDiscardCount <= 0) {
                pendingDiscardPlayer = null;
                finishTurn(p);
                return true;
            }
            log(p.name + " 还需弃置 " + pendingDiscardCount + " 张");
        }
        syncAll();
        return true;
    }

    // ================= 响应机制 =================

    public void awaitJink(GamePlayer target, Runnable onHit) {
        awaitResponse(target, "jink", ok -> { if (!ok) onHit.run(); });
    }

    public void awaitResponse(GamePlayer target, String type, Consumer<Boolean> handler) {
        responseQueue.add(new ResponseRequest(target, type, handler));
        pumpResponses();
    }

    /** 请求玩家在选项中做选择(花色/改判/流离/观星) */
    public void awaitChoice(GamePlayer target, String prompt, String[] options, Consumer<Integer> handler) {
        choiceQueue.add(new ChoiceRequest(target, prompt, options, handler));
        pumpChoices();
    }

    private void pumpChoices() {
        if (pendingChoice == null && !choiceQueue.isEmpty()) {
            pendingChoice = choiceQueue.poll();
            syncAll();
        }
    }

    public ChoiceRequest pendingChoice() { return pendingChoice; }

    /** 玩家提交选择 */
    public boolean submitChoice(ServerPlayer sp, int optionIndex) {
        if (pendingChoice == null) return false;
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || pendingChoice.target.uuid != p.uuid) return false;
        if (optionIndex < 0 || optionIndex >= pendingChoice.options.length) return false;
        ChoiceRequest req = pendingChoice;
        pendingChoice = null;
        req.handler.accept(optionIndex);
        pumpChoices();
        syncAll();
        return true;
    }

    private void pumpResponses() {
        if (currentResponse == null && !responseQueue.isEmpty()) {
            currentResponse = responseQueue.poll();
            syncAll();
        }
    }

    public boolean hasPendingResponse() { return currentResponse != null; }
    public GamePlayer pendingResponder() { return currentResponse == null ? null : currentResponse.target; }
    public String pendingType() { return currentResponse == null ? null : currentResponse.type; }

    public boolean submitResponse(ServerPlayer sp, boolean responded, int cardIndex) {
        if (currentResponse == null) return false;
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || currentResponse.target.uuid != p.uuid) return false;

        CardDefinition card = null;
        if (responded) {
            if (cardIndex < 0 || cardIndex >= p.hand.size()) return false;
            card = p.hand.get(cardIndex);
            if (!isValidResponseCard(card, currentResponse.type)) return false;
            p.hand.remove(cardIndex);
            discard.add(card);
            checkLianying(p);
        }
        log(p.name + (responded ? " 打出了【" + card.name + "】" : " 放弃响应(" + (currentResponse.type.equals("jink") ? "闪" : "杀") + ")"));
        ResponseRequest req = currentResponse;
        currentResponse = null;
        req.handler.accept(responded);
        pumpResponses();
        syncAll();
        return true;
    }

    private boolean isValidResponseCard(CardDefinition card, String type) {
        return type.equals("jink") ? "jink".equals(card.effect) : "slash".equals(card.effect);
    }

    // ================= 伤害 / 死亡 =================

    /** 伤害类型 */
    public enum DamageType { NORMAL, FIRE, THUNDER }

    public void damage(GamePlayer source, GamePlayer target, int amount, DamageType type) {
        damage(source, target, amount, type, null);
    }

    public void damage(GamePlayer source, GamePlayer target, int amount, DamageType type, CardDefinition card) {
        if (!target.isAlive()) return;
        int finalAmount = amount;
        // 裸衣:杀/决斗伤害 +1
        if (source != null && source != target && SkillRegistry.has(source, "luoyi")
                && card != null && ("slash".equals(card.effect) || "duel".equals(card.effect))) {
            finalAmount += 1;
        }
        // 藤甲:火属性伤害 +1
        if (target.armor != null && "vine".equals(target.armor.effect) && type == DamageType.FIRE) {
            finalAmount += 1;
        }
        // 古锭刀:目标无手牌,杀伤害 +1
        if (source != null && source.weapon != null && "gudingdao".equals(source.weapon.effect)
                && target.handCount() == 0 && card != null && "slash".equals(card.effect)) {
            finalAmount += 1;
        }
        // 白银狮子:受到的伤害 -1(最少 1)
        if (target.armor != null && "baiyinshizi".equals(target.armor.effect) && finalAmount > 1) {
            finalAmount -= 1;
        }
        // 铁索连环:属性伤害传导给其他横置角色
        boolean chainedHit = type != DamageType.NORMAL && target.chained;
        applyDamage(source, target, finalAmount, type, card);
        if (chainedHit && target.isAlive()) {
            for (GamePlayer q : players) {
                if (q != target && q.isAlive() && q.chained) {
                    applyDamage(source, q, finalAmount, type, card);
                    log(q.name + " 被【铁索连环】传导,受到 " + finalAmount + " 点伤害");
                }
            }
        }
        syncAll();
    }

    private void applyDamage(GamePlayer source, GamePlayer target, int amount, DamageType type, CardDefinition card) {
        if (!target.isAlive()) return;
        // 受击技能触发(奸雄/反馈/刚烈/遗计/耀武)
        for (Skill s : SkillRegistry.of(target)) s.onDamageTaken(this, target, source, amount, card);
        target.hp -= amount;
        log(target.name + " 受到 " + amount + " 点伤害" + (source != null ? " (来源:" + source.name + ")" : "") + ",剩余体力 " + Math.max(0, target.hp));
        if (target.hp <= 0) {
            // 濒死:自动使用手牌中的桃自救
            if (tryRevive(target)) {
                log(target.name + " 从濒死中被救回");
            } else {
                onDeath(target);
            }
        }
    }

    private void onDeath(GamePlayer p) {
        p.alive = false;
        p.hp = 0;
        discard.addAll(p.hand);
        p.hand.clear();
        if (p.weapon != null) discard.add(p.weapon);
        if (p.armor != null) discard.add(p.armor);
        if (p.horsePlus != null) discard.add(p.horsePlus);
        if (p.horseMinus != null) discard.add(p.horseMinus);
        p.weapon = p.armor = p.horsePlus = p.horseMinus = null;
        log(p.name + " 阵亡了!" + "(" + p.team.cn + ")");
        checkWin();
    }

    private void checkWin() {
        boolean redAlive = false, blueAlive = false;
        for (GamePlayer p : players) {
            if (!p.isAlive()) continue;
            if (p.team == Team.RED) redAlive = true;
            else blueAlive = true;
        }
        if (!redAlive || !blueAlive) {
            state = State.FINISHED;
            winner = redAlive ? Team.RED : Team.BLUE;
            log("===== 游戏结束," + winner.cn + "获胜! =====");
            syncAll();
        }
    }

    // ================= 辅助 =================

    public List<GamePlayer> aliveOpponents(GamePlayer p) {
        List<GamePlayer> list = new ArrayList<>();
        for (GamePlayer q : players) if (q.isAlive() && q.team != p.team) list.add(q);
        return list;
    }

    public void discardOneFrom(GamePlayer target) {
        if (target.handCount() > 0) {
            CardDefinition c = target.hand.remove(target.hand.size() - 1);
            discard.add(c);
            log(target.name + " 的一张手牌被弃置");
            checkLianying(target);
        } else if (target.weapon != null) {
            CardDefinition lost = target.weapon;
            discard.add(target.weapon); target.weapon = null;
            onLoseEquip(target, lost);
        } else if (target.armor != null) {
            CardDefinition lost = target.armor;
            discard.add(target.armor); target.armor = null;
            onLoseEquip(target, lost);
        } else if (target.horsePlus != null) {
            CardDefinition lost = target.horsePlus;
            discard.add(target.horsePlus); target.horsePlus = null;
            onLoseEquip(target, lost);
        } else if (target.horseMinus != null) {
            CardDefinition lost = target.horseMinus;
            discard.add(target.horseMinus); target.horseMinus = null;
            onLoseEquip(target, lost);
        }
    }

    public void stealOneFrom(GamePlayer user, GamePlayer target) {
        if (target.handCount() > 0) {
            CardDefinition c = target.hand.remove(target.hand.size() - 1);
            user.hand.add(c);
            log(user.name + " 从 " + target.name + " 手中获得一张牌");
            checkLianying(target);
        } else if (target.weapon != null) {
            CardDefinition lost = target.weapon;
            user.hand.add(target.weapon); target.weapon = null;
            onLoseEquip(target, lost);
        } else if (target.armor != null) {
            CardDefinition lost = target.armor;
            user.hand.add(target.armor); target.armor = null;
            onLoseEquip(target, lost);
        } else if (target.horsePlus != null) {
            CardDefinition lost = target.horsePlus;
            user.hand.add(target.horsePlus); target.horsePlus = null;
            onLoseEquip(target, lost);
        } else if (target.horseMinus != null) {
            CardDefinition lost = target.horseMinus;
            user.hand.add(target.horseMinus); target.horseMinus = null;
            onLoseEquip(target, lost);
        }
    }

    // ================= 查询 =================

    public List<GamePlayer> players() { return players; }
    public State state() { return state; }
    public GamePhase phase() { return phase; }
    public Team winner() { return winner; }
    public GamePlayer currentPlayer() { return players.isEmpty() ? null : players.get(currentIdx); }
    public GamePlayer byUuid(UUID uuid) { return byUuid.get(uuid); }
    public List<HeroDefinition> heroOptionsFor(UUID uuid) { return heroOptions.getOrDefault(uuid, List.of()); }
    public String lastLog() { return lastLog; }
    public int deckCount() { return deck.size(); }
    public int discardCount() { return discard.size(); }

    /** 最近弃置的 n 张牌(弃牌堆展示) */
    public List<CardDefinition> recentDiscards(int n) {
        int size = discard.size();
        List<CardDefinition> list = new ArrayList<>();
        for (int i = Math.max(0, size - n); i < size; i++) list.add(discard.get(i));
        return list;
    }

    public int lastAnimFrom() { return lastAnimFrom; }
    public int lastAnimTo() { return lastAnimTo; }
    public String lastAnimCard() { return lastAnimCard; }
    public int animSeq() { return animSeq; }

    public void log(String msg) {
        lastLog = msg;
        logHistory.add(msg);
        if (logHistory.size() > 50) logHistory.remove(0);
        SanguoshaMod.LOGGER.info("[Sanguosha] {}", msg);
    }

    public List<String> logHistory() { return List.copyOf(logHistory); }

    // ================= 技能辅助方法 =================

    /** 判定:摸一张牌作为判定牌,入弃牌堆,触发天妒等技能 */
    public CardDefinition judgeFor(GamePlayer p) {
        CardDefinition j = drawTop();
        if (j == null) return null;
        discard.add(j);
        for (Skill s : SkillRegistry.of(p)) s.onJudge(this, p, j);
        return j;
    }

    /** 将一张牌从弃牌堆给到玩家手牌(天妒/奸雄/洛神) */
    public void giveToHand(GamePlayer p, CardDefinition card) {
        if (card == null) return;
        discard.remove(card);
        p.hand.add(card);
        if (p.isAlive()) checkLianying(p);
    }

    /** 弃牌入弃牌堆 */
    public void discardToPile(CardDefinition card) {
        discard.add(card);
    }

    /** 无懈可击询问:按座位顺序询问所有存活玩家,可打出无懈抵消瞬时锦囊(简化:不做连锁) */
    private void askNullification(GamePlayer user, CardDefinition trick, Runnable then) {
        List<GamePlayer> order = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            GamePlayer q = players.get((currentIdx + i) % players.size());
            if (q.isAlive()) order.add(q);
        }
        askNullificationStep(order, 0, user, trick, then);
    }

    private void askNullificationStep(List<GamePlayer> order, int idx, GamePlayer user, CardDefinition trick, Runnable then) {
        if (idx >= order.size()) { then.run(); return; }
        GamePlayer q = order.get(idx);
        CardDefinition nul = null;
        for (CardDefinition c : q.hand) {
            if ("nullification".equals(c.effect)) { nul = c; break; }
        }
        if (nul == null) { askNullificationStep(order, idx + 1, user, trick, then); return; }
        final CardDefinition nulCard = nul;
        awaitChoice(q, q.name + " 是否使用【无懈可击】抵消 " + user.name + " 的【" + trick.name + "】?", new String[]{"不使用", "使用【无懈可击】"}, choice -> {
            if (choice == 1) {
                q.hand.remove(nulCard);
                discard.add(nulCard);
                checkLianying(q);
                log(q.name + " 使用了【无懈可击】,抵消了【" + trick.name + "】的效果!");
            } else {
                askNullificationStep(order, idx + 1, user, trick, then);
            }
        });
    }

    /** 连营检查:失去最后手牌时摸牌 */
    public void checkLianying(GamePlayer p) {
        if (SkillRegistry.has(p, "lianying") && p.hand.isEmpty() && p.isAlive()) {
            drawCards(p, 1);
            log(p.name + " 触发【连营】,摸 1 张牌");
        }
    }

    /** 濒死自救:自动使用手牌中的桃,直到脱离濒死 */
    public boolean tryRevive(GamePlayer p) {
        // 1. 自己手牌中的桃
        while (p.hp <= 0 && p.isAlive()) {
            CardDefinition peach = null;
            for (CardDefinition c : p.hand) {
                if ("peach".equals(c.effect)) { peach = c; break; }
            }
            if (peach == null) break;
            p.hand.remove(peach);
            discard.add(peach);
            p.hp = Math.min(p.hp + 1, p.hero.maxHp);
            log(p.name + " 濒死时自动使用了【桃】,恢复 1 点体力");
        }
        // 1.5 急救:回合外红牌当桃自救
        if (p.hp <= 0 && SkillRegistry.has(p, "jijiu") && currentPlayer() != p) {
            CardDefinition red = null;
            for (CardDefinition c : p.hand) {
                if (c.suit.color == 1) { red = c; break; }
            }
            if (red != null) {
                p.hand.remove(red);
                discard.add(red);
                p.hp = Math.min(p.hp + 1, p.hero.maxHp);
                log(p.name + " 发动【急救】,将【" + red.name + "】当【桃】使用");
            }
        }
        // 2. 队友依次自动救援(有桃即出;华佗可红牌当桃)
        if (p.hp <= 0) {
            for (GamePlayer mate : players) {
                if (mate == p || !mate.isAlive() || mate.team != p.team) continue;
                CardDefinition peach = null;
                for (CardDefinition c : mate.hand) {
                    if ("peach".equals(c.effect)) { peach = c; break; }
                }
                boolean jijiuSave = false;
                if (peach == null && SkillRegistry.has(mate, "jijiu") && currentPlayer() != mate) {
                    for (CardDefinition c : mate.hand) {
                        if (c.suit.color == 1) { peach = c; jijiuSave = true; break; }
                    }
                }
                if (peach != null) {
                    mate.hand.remove(peach);
                    discard.add(peach);
                    p.hp = Math.min(p.hp + 1, p.hero.maxHp);
                    log(mate.name + (jijiuSave ? " 发动【急救】救援了 " : " 使用【桃】救援了 ") + p.name);
                    break;
                }
            }
        }
        return p.hp > 0;
    }

    // ================= 转换技能(武圣/龙胆/倾国/奇袭/国色) =================

    /** 将手牌当作另一张牌使用(转换技能) */
    public boolean playCardConverted(ServerPlayer sp, int cardIndex, int targetSeat, String asEffect) {
        GamePlayer user = byUuid.get(sp.getUUID());
        if (user == null || state != State.RUNNING) return false;
        boolean responding = currentResponse != null && currentResponse.target.uuid.equals(user.uuid);
        if (!responding && (phase != GamePhase.PLAY || user.seat != currentIdx)) return false;
        if (!user.isAlive()) return false;
        if (cardIndex < 0 || cardIndex >= user.hand.size()) return false;
        CardDefinition card = user.hand.get(cardIndex);
        if (!canConvert(user, card, asEffect)) return false;
        // 响应阶段转换(龙胆当闪/当杀、倾国当闪):直接作为响应打出
        if (responding && ("jink".equals(asEffect) || "slash".equals(asEffect))) {
            if (!asEffect.equals(currentResponse.type)) return false;
            user.hand.remove(cardIndex);
            discard.add(card);
            checkLianying(user);
            ResponseRequest req = currentResponse;
            currentResponse = null;
            req.handler.accept(true);
            pumpResponses();
            syncAll();
            return true;
        }
        GamePlayer target = targetSeat >= 0 && targetSeat < players.size() ? players.get(targetSeat) : null;
        if (target != null && !target.isAlive()) return false;
        if (target != null) {
            for (Skill s : SkillRegistry.of(target)) {
                if (s.isImmuneTo(this, target, card)) {
                    log(target.name + " 因【" + s.name() + "】免疫【" + card.name + "】");
                    return false;
                }
            }
        }
        user.hand.remove(cardIndex);
        discard.add(card);
        lastAnimFrom = user.seat;
        lastAnimTo = target == null ? user.seat : target.seat;
        lastAnimCard = card.name + "(当" + (asEffect.equals("slash") ? "杀" : asEffect.equals("jink") ? "闪" : asEffect.equals("dismantlement") ? "过河拆桥" : asEffect.equals("indulgence") ? "乐不思蜀" : asEffect) + ")";
        animSeq++;
        var effect = EffectRegistry.get(asEffect);
        if (effect.requiresTarget() && target == null) {
            user.hand.add(card);
            discard.remove(card);
            return false;
        }
        effect.use(this, user, target == null ? user : target, card);
        for (Skill s : SkillRegistry.of(user)) s.onCardUsed(this, user, card);
        checkLianying(user);
        syncAll();
        return true;
    }

    /** 转换合法性:技能 + 花色/牌型匹配 */
    public boolean canConvert(GamePlayer user, CardDefinition card, String asEffect) {
        if (user.hero == null) return false;
        if (SkillRegistry.has(user, "wusheng") && "slash".equals(asEffect) && card.suit.color == 1) return true;
        if (SkillRegistry.has(user, "longdan") && "slash".equals(asEffect) && "jink".equals(card.effect)) return true;
        if (SkillRegistry.has(user, "longdan") && "jink".equals(asEffect) && "slash".equals(card.effect)) return true;
        if (SkillRegistry.has(user, "qingguo") && "jink".equals(asEffect) && card.suit.color == 0) return true;
        if (SkillRegistry.has(user, "qixi") && "dismantlement".equals(asEffect) && card.suit.color == 0) return true;
        if (SkillRegistry.has(user, "guose") && "indulgence".equals(asEffect) && card.suit == CardSuit.DIAMOND) return true;
        // 朱雀羽扇:装备时可将杀当火杀
        if (user.weapon != null && "zhuqueyushan".equals(user.weapon.effect)
                && "slash".equals(card.effect) && "fire_slash".equals(asEffect)) return true;
        return false;
    }

    // ================= 主动技能(苦肉/青囊/制衡) =================

    public boolean useSkill(ServerPlayer sp, String skillId) {
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || state != State.RUNNING || phase != GamePhase.PLAY) return false;
        if (p.seat != currentIdx || !p.isAlive()) return false;
        if (p.hero == null || !p.hero.skills.contains(skillId)) return false;
        switch (skillId) {
            case "kuro" -> { // 苦肉:失去1点体力,摸两张牌(不限次数)
                p.hp -= 1;
                drawCards(p, 2);
                log(p.name + " 发动【苦肉】,失去1点体力,摸 2 张牌");
                if (p.hp <= 0) {
                    if (tryRevive(p)) log(p.name + " 从濒死中被救回");
                    else onDeath(p);
                }
                syncAll();
                return true;
            }
            case "qingnang" -> { // 青囊:弃置一张手牌,回复1点体力(限一次)
                if (p.hand.isEmpty() || p.hp >= p.hero.maxHp) return false;
                if (p.skillsUsedThisTurn.contains("qingnang")) return false;
                p.skillsUsedThisTurn.add("qingnang");
                CardDefinition c = p.hand.remove(p.hand.size() - 1);
                discard.add(c);
                p.hp = Math.min(p.hp + 1, p.hero.maxHp);
                log(p.name + " 发动【青囊】,弃置一张牌,回复 1 点体力");
                checkLianying(p);
                syncAll();
                return true;
            }
            case "zhiheng" -> { // 制衡(简化版):弃置一张牌,摸一张牌(限一次)
                if (p.hand.isEmpty()) return false;
                if (p.skillsUsedThisTurn.contains("zhiheng")) return false;
                p.skillsUsedThisTurn.add("zhiheng");
                CardDefinition c = p.hand.remove(p.hand.size() - 1);
                discard.add(c);
                drawCards(p, 1);
                log(p.name + " 发动【制衡】(简化版),弃置一张牌,摸一张牌");
                checkLianying(p);
                syncAll();
                return true;
            }
            default -> { return false; }
        }
    }

    // ================= 整理手牌 =================

    public boolean sortHand(ServerPlayer sp) {
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null) return false;
        Collections.shuffle(p.hand); // 无限次洗牌
        log(p.name + " 洗了手牌");
        syncAll();
        return true;
    }

    // ================= 铁索重铸 =================

    public boolean recastCard(ServerPlayer sp, int cardIndex) {
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || state != State.RUNNING || phase != GamePhase.PLAY) return false;
        if (p.seat != currentIdx || !p.isAlive()) return false;
        if (cardIndex < 0 || cardIndex >= p.hand.size()) return false;
        CardDefinition card = p.hand.get(cardIndex);
        if (!"iron_chain".equals(card.effect)) return false;
        p.hand.remove(cardIndex);
        discard.add(card);
        drawCards(p, 1);
        log(p.name + " 重铸了【铁索连环】,摸 1 张牌");
        checkLianying(p);
        syncAll();
        return true;
    }

    // ================= 离间(貂蝉) =================

    public boolean useLijian(ServerPlayer sp, int seatA, int seatB) {
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || state != State.RUNNING || phase != GamePhase.PLAY) return false;
        if (p.seat != currentIdx || !p.isAlive()) return false;
        if (p.hand.isEmpty()) return false;
        if (p.hero == null || !p.hero.skills.contains("lijian")) return false;
        if (p.skillsUsedThisTurn.contains("lijian")) return false;
        if (seatA < 0 || seatB < 0 || seatA >= players.size() || seatB >= players.size() || seatA == seatB) return false;
        GamePlayer a = players.get(seatA), b = players.get(seatB);
        if (!a.isAlive() || !b.isAlive()) return false;
        // MVP:所有非貂蝉角色视为男性
        if ("diaochan".equals(a.hero.id) || "diaochan".equals(b.hero.id)) return false;
        p.skillsUsedThisTurn.add("lijian");
        CardDefinition c = p.hand.remove(p.hand.size() - 1);
        discard.add(c);
        log(p.name + " 发动【离间】,弃置一张牌,令 " + a.name + " 与 " + b.name + " 决斗!");
        com.sanguosha.game.effect.TrickCardEffects.duelBetween(this, a, b);
        checkLianying(p);
        syncAll();
        return true;
    }

    // ================= 仁德(刘备) =================

    public boolean useRende(ServerPlayer sp, int cardIndex, int targetSeat) {
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || state != State.RUNNING || phase != GamePhase.PLAY) return false;
        if (p.seat != currentIdx || !p.isAlive()) return false;
        if (p.hero == null || !p.hero.skills.contains("rende")) return false;
        if (cardIndex < 0 || cardIndex >= p.hand.size()) return false;
        GamePlayer target = targetSeat >= 0 && targetSeat < players.size() ? players.get(targetSeat) : null;
        if (target == null || target == p || !target.isAlive()) return false;
        CardDefinition c = p.hand.remove(cardIndex);
        target.hand.add(c);
        log(p.name + " 发动【仁德】,将【" + c.name + "】交给 " + target.name);
        p.rendeGiven++;
        if (p.rendeGiven % 2 == 0 && p.hp < p.hero.maxHp) {
            p.hp = Math.min(p.hp + 1, p.hero.maxHp);
            log(p.name + " 【仁德】回复 1 点体力");
        }
        checkLianying(p);
        syncAll();
        return true;
    }

    // ================= 反间(周瑜) =================

    public boolean useFanjian(ServerPlayer sp, int targetSeat) {
        GamePlayer p = byUuid.get(sp.getUUID());
        if (p == null || state != State.RUNNING || phase != GamePhase.PLAY) return false;
        if (p.seat != currentIdx || !p.isAlive()) return false;
        if (p.hand.isEmpty()) return false;
        if (p.hero == null || !p.hero.skills.contains("fanjian")) return false;
        if (p.skillsUsedThisTurn.contains("fanjian")) return false;
        GamePlayer target = targetSeat >= 0 && targetSeat < players.size() ? players.get(targetSeat) : null;
        if (target == null || !target.isAlive()) return false;
        p.skillsUsedThisTurn.add("fanjian");
        CardDefinition card = p.hand.get(0); // 反间牌(目标不可见)
        awaitChoice(target, target.name + ",请选择一种花色(周瑜【反间】)", new String[]{"黑桃", "红桃", "梅花", "方块"}, choice -> {
            p.hand.remove(card);
            target.hand.add(card);
            CardSuit chosen = CardSuit.values()[choice];
            log(target.name + " 猜了" + chosen.cn + ",抽到的牌是【" + card.name + "】" + card.suit.cn + card.rankText());
            if (chosen != card.suit) {
                damage(p, target, 1, DamageType.NORMAL);
                log(target.name + " 猜错花色,受到 1 点伤害!");
            } else {
                log(target.name + " 猜中花色,无事发生");
            }
        });
        syncAll();
        return true;
    }

    // ================= 鬼才(司马懿)改判 =================

    /** 判定前询问第一个存活司马懿是否改判(judgeCard[0] 可变引用) */
    private void askGuicai(GamePlayer owner, CardDefinition[] judgeCard, Runnable then) {
        GamePlayer simayi = null;
        for (GamePlayer q : players) {
            if (q.isAlive() && q.hero != null && q.hero.skills.contains("guicai") && !q.hand.isEmpty()) {
                simayi = q;
                break;
            }
        }
        if (simayi == null) { then.run(); return; }
        final GamePlayer sm = simayi;
        String[] options = new String[sm.hand.size() + 1];
        options[0] = "不改判";
        for (int i = 0; i < sm.hand.size(); i++) options[i + 1] = sm.hand.get(i).name;
        awaitChoice(sm, sm.name + " 是否发动【鬼才】改判?", options, choice -> {
            if (choice > 0 && choice <= sm.hand.size()) {
                CardDefinition newCard = sm.hand.remove(choice - 1);
                sm.hand.add(judgeCard[0]); // 原判定牌交给司马懿(简化)
                judgeCard[0] = newCard;
                log(sm.name + " 发动【鬼才】,用【" + newCard.name + "】替换判定牌");
            }
            then.run();
        });
    }

    // ================= 流离(大乔) =================

    /** 被杀时询问大乔是否流离(转移给其他角色) */
    public void askLiuli(GamePlayer user, GamePlayer target, Runnable onNo, Consumer<GamePlayer> onTransfer) {
        if (!SkillRegistry.has(target, "liuli") || !target.isAlive()) { onNo.run(); return; }
        List<GamePlayer> candidates = new ArrayList<>();
        for (GamePlayer q : players) {
            if (q.isAlive() && q != target && q != user) candidates.add(q);
        }
        if (candidates.isEmpty()) { onNo.run(); return; }
        String[] options = new String[candidates.size() + 1];
        options[0] = "不发动";
        for (int i = 0; i < candidates.size(); i++) options[i + 1] = "转移给 " + candidates.get(i).name;
        awaitChoice(target, target.name + " 是否发动【流离】?", options, choice -> {
            if (choice == 0 || (target.handCount() == 0 && target.weapon == null && target.armor == null)) {
                onNo.run();
                return;
            }
            discardOneFrom(target);
            log(target.name + " 发动【流离】,弃置一张牌");
            onTransfer.accept(candidates.get(choice - 1));
        });
    }

    /** 失去装备触发枭姬/白银狮子回血 */
    public void onLoseEquip(GamePlayer p, CardDefinition lost) {
        if (lost != null && "baiyinshizi".equals(lost.effect) && p.isAlive() && p.hp < p.hero.maxHp) {
            p.hp = Math.min(p.hp + 1, p.hero.maxHp);
            log(p.name + " 失去【白银狮子】,回复 1 点体力");
        }
        if (SkillRegistry.has(p, "xiaoji") && p.isAlive()) {
            drawCards(p, 2);
            log(p.name + " 触发【枭姬】,摸 2 张牌");
        }
    }

    /** 强制同步给所有玩家(由 Networking 层调用) */
    public void syncAll() {
    }
}