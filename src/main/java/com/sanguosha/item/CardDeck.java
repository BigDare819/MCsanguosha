package com.sanguosha.item;

import com.sanguosha.card.CardDefinition;
import com.sanguosha.card.Cards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 线下牌堆:洗牌后按顺序发牌 */
public final class CardDeck {
    private static List<CardDefinition> shuffled = new ArrayList<>();
    private static int index = 0;

    private CardDeck() {}

    /** 重新洗牌 */
    public static void reset() {
        shuffled = new ArrayList<>(Cards.all());
        Collections.shuffle(shuffled);
        index = 0;
    }

    /** 取下一张牌(发完自动重新洗牌) */
    public static CardDefinition next() {
        if (shuffled.isEmpty() || index >= shuffled.size()) reset();
        return shuffled.get(index++);
    }

    /** 剩余张数 */
    public static int remaining() { return shuffled.isEmpty() ? 0 : shuffled.size() - index; }

    /** 剩余牌列表(未发出的部分) */
    public static List<CardDefinition> remainingList() {
        if (shuffled.isEmpty() || index >= shuffled.size()) return java.util.List.of();
        return shuffled.subList(index, shuffled.size());
    }

    /** 从剩余堆抽出第 pos 张(0 基);返回 null 表示空堆 */
    public static CardDefinition take(int pos) {
        java.util.List<CardDefinition> rem = remainingList();
        if (rem.isEmpty()) reset();
        rem = remainingList();
        if (rem.isEmpty()) return null;
        if (pos < 0 || pos >= rem.size()) pos = 0;
        return rem.remove(pos);
    }
}