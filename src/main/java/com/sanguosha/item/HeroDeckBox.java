package com.sanguosha.item;

import com.sanguosha.hero.HeroDefinition;
import com.sanguosha.hero.Heroes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 武将牌堆:洗牌后按顺序发武将牌 */
public final class HeroDeckBox {
    private static List<HeroDefinition> shuffled = new ArrayList<>();
    private static int index = 0;

    private HeroDeckBox() {}

    public static void reset() {
        shuffled = new ArrayList<>(Heroes.all());
        Collections.shuffle(shuffled);
        index = 0;
    }

    public static HeroDefinition next() {
        if (shuffled.isEmpty() || index >= shuffled.size()) reset();
        return shuffled.get(index++);
    }

    public static int remaining() { return shuffled.isEmpty() ? 0 : shuffled.size() - index; }

    /** 剩余武将列表(未发出的部分) */
    public static List<HeroDefinition> remainingList() {
        if (shuffled.isEmpty() || index >= shuffled.size()) return java.util.List.of();
        return shuffled.subList(index, shuffled.size());
    }

    /** 从剩余堆抽出第 pos 个武将(0 基);返回 null 表示空堆 */
    public static HeroDefinition take(int pos) {
        java.util.List<HeroDefinition> rem = remainingList();
        if (rem.isEmpty()) reset();
        rem = remainingList();
        if (rem.isEmpty()) return null;
        if (pos < 0 || pos >= rem.size()) pos = 0;
        return rem.remove(pos);
    }
}