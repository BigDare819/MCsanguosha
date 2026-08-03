package com.sanguosha.item;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** 按方块位置管理的独立牌堆:每个牌盒/将盒方块各有一份,互不影响 */
public final class BoxDeckManager {
    private static final Map<BlockPos, LocalCardDeck> CARD_DECKS = new HashMap<>();
    private static final Map<BlockPos, LocalHeroDeck> HERO_DECKS = new HashMap<>();

    private BoxDeckManager() {}

    public static LocalCardDeck cardDeck(BlockPos pos) {
        return CARD_DECKS.computeIfAbsent(pos, p -> new LocalCardDeck());
    }

    public static LocalHeroDeck heroDeck(BlockPos pos) {
        return HERO_DECKS.computeIfAbsent(pos, p -> new LocalHeroDeck());
    }

    /** 方块被破坏时清理(可选调用) */
    public static void remove(BlockPos pos) {
        CARD_DECKS.remove(pos);
        HERO_DECKS.remove(pos);
    }
}