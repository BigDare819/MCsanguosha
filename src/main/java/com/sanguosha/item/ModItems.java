package com.sanguosha.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** 三国杀实体卡牌物品注册 */
public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("sanguosha");

    /** 单张卡牌(通过 NBT 区分牌与武将,配合 custom_model_data 切换模型) */
    public static final DeferredItem<Item> CARD = ITEMS.register("card",
            () -> new CardItem(new Item.Properties().stacksTo(1)));

    /** 牌盒方块物品(右键放置为方块) */
    public static final DeferredItem<Item> DECK_BOX = ITEMS.register("deck_box",
            () -> new net.minecraft.world.item.BlockItem(com.sanguosha.block.ModBlocks.DECK_BOX.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx,
                                            java.util.List<net.minecraft.network.chat.Component> tooltip,
                                            net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(net.minecraft.network.chat.Component.literal(
                            "\u5efa\u8bae\u624b\u6301\u6728\u68cd\u6216\u4e0d\u80fd\u653e\u7f6e\u7684\u4e1c\u897f\u6e38\u73a9")
                            .withStyle(s -> s.withColor(0xFFFFE0B0)));
                }
            });

    /** 牌布方块物品(右键放置为方块) */
    public static final DeferredItem<Item> CARD_MAT = ITEMS.register("card_mat", com.sanguosha.block.ModBlocks.itemFor(com.sanguosha.block.ModBlocks.CARD_MAT));

    /** 弃牌布方块物品(右键放置为方块,牌放上去自动清除并累计记录) */
    public static final DeferredItem<Item> DISCARD_MAT = ITEMS.register("discard_mat", com.sanguosha.block.ModBlocks.itemFor(com.sanguosha.block.ModBlocks.DISCARD_MAT));

    public static final DeferredItem<Item> HERO_DECK_BOX = ITEMS.register("hero_deck_box",
            () -> new net.minecraft.world.item.BlockItem(com.sanguosha.block.ModBlocks.HERO_DECK_BOX.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx,
                                            java.util.List<net.minecraft.network.chat.Component> tooltip,
                                            net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(net.minecraft.network.chat.Component.literal(
                            "\u5efa\u8bae\u624b\u6301\u6728\u68cd\u6216\u4e0d\u80fd\u653e\u7f6e\u7684\u4e1c\u897f\u6e38\u73a9")
                            .withStyle(s -> s.withColor(0xFFFFE0B0)));
                }
            });

    /** 三国杀创造物品栏 */
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "sanguosha");

    public static final Supplier<CreativeModeTab> SG_TAB = TABS.register("sanguosha", () -> CreativeModeTab.builder()
            .title(Component.literal("三国杀"))
            .icon(() -> new ItemStack(DECK_BOX.get()))
            .displayItems((params, output) -> {
                output.accept(new ItemStack(DECK_BOX.get()));
                output.accept(new ItemStack(HERO_DECK_BOX.get()));
                output.accept(new ItemStack(com.sanguosha.block.ModBlocks.DECK_BOX.get().asItem()));
                output.accept(new ItemStack(com.sanguosha.block.ModBlocks.HERO_DECK_BOX.get().asItem()));
                output.accept(new ItemStack(CARD_MAT.get()));
                output.accept(new ItemStack(DISCARD_MAT.get()));
            })
            .build());
}