package com.sanguosha.item;

import com.sanguosha.hero.HeroDefinition;
import com.sanguosha.hero.Heroes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 单个将盒的独立武将牌堆:洗牌后取将,取空返回 null(不自动重洗) */
public class LocalHeroDeck {
    private final List<HeroDefinition> heroes = new ArrayList<>();

    public LocalHeroDeck() { shuffle(); }

    public void shuffle() {
        heroes.clear();
        heroes.addAll(Heroes.all());
        Collections.shuffle(heroes);
    }

    public HeroDefinition next() {
        return heroes.isEmpty() ? null : heroes.remove(0);
    }

    public HeroDefinition take(int pos) {
        if (heroes.isEmpty() || pos < 0 || pos >= heroes.size()) return null;
        return heroes.remove(pos);
    }

    public List<HeroDefinition> remainingList() { return List.copyOf(heroes); }
    public int remaining() { return heroes.size(); }
}