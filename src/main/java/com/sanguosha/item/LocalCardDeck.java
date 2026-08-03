package com.sanguosha.item;

import com.sanguosha.card.CardDefinition;
import com.sanguosha.card.Cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 单个牌盒的独立牌堆:洗牌后取牌,取空返回 null(不自动重洗) */
public class LocalCardDeck {
    private final List<CardDefinition> cards = new ArrayList<>();

    public LocalCardDeck() { shuffle(); }

    /** 重新洗牌 */
    public void shuffle() {
        cards.clear();
        cards.addAll(Cards.all());
        Collections.shuffle(cards);
    }

    /** 取顶牌;空堆返回 null */
    public CardDefinition next() {
        return cards.isEmpty() ? null : cards.remove(0);
    }

    /** 抽指定位置(0 基);空堆/越界返回 null */
    public CardDefinition take(int pos) {
        if (cards.isEmpty() || pos < 0 || pos >= cards.size()) return null;
        return cards.remove(pos);
    }

    /** 观星:查看牌堆顶前 n 张(不取出);不足则返回全部 */
    public List<CardDefinition> peekTop(int n) {
        if (n <= 0 || cards.isEmpty()) return List.of();
        return List.copyOf(cards.subList(0, Math.min(n, cards.size())));
    }

    /**
     * 观星:按玩家选择的顺序重排牌堆顶前 n 张。
     * order:新顺序(0 基,对应 peekTop 返回的列表下标);未列入的牌放回牌堆底。
     */
    public void reorderTop(int n, List<Integer> order) {
        if (n <= 0 || cards.isEmpty()) return;
        int takeN = Math.min(n, cards.size());
        List<CardDefinition> top = new ArrayList<>(cards.subList(0, takeN));
        for (int i = 0; i < takeN; i++) cards.remove(0);
        // 按 order 顺序放回牌堆顶(先放最后的,再放最前的 → 逆序插入)
        for (int i = order.size() - 1; i >= 0; i--) {
            int idx = order.get(i);
            if (idx >= 0 && idx < top.size()) {
                cards.add(0, top.get(idx));
            }
        }
        // 未选中的牌放到牌堆底
        for (int i = 0; i < top.size(); i++) {
            if (!order.contains(i)) {
                cards.add(top.get(i));
            }
        }
    }

    public List<CardDefinition> remainingList() { return List.copyOf(cards); }
    public int remaining() { return cards.size(); }
}