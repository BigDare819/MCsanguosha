package com.sanguosha.block;

import com.sanguosha.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** 三国杀方块注册 */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("sanguosha");

    public static final DeferredBlock<Block> DECK_BOX = BLOCKS.register("deck_box_block",
            () -> new CardBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.8F).noOcclusion(), false));

    public static final DeferredBlock<Block> HERO_DECK_BOX = BLOCKS.register("hero_deck_box_block",
            () -> new CardBoxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.8F).noOcclusion(), true));

    public static final DeferredBlock<Block> CARD_MAT = BLOCKS.register("card_mat",
            () -> new CardMatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8F).noOcclusion()));

    /** 方块对应的物品(可在物品栏持有并放置) */
    public static Supplier<Item> itemFor(DeferredBlock<Block> block) {
        return () -> new BlockItem(block.get(), new Item.Properties());
    }
}