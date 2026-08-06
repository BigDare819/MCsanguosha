package com.sanguosha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** 牌盒方块:右键摸一张牌(或发武将),0.8x1x0.5 碰撞箱 */
public class CardBoxBlock extends Block {
    private final boolean heroBox;
    private static final VoxelShape SHAPE = Shapes.box(0.1, 0.0, 0.25, 0.9, 0.3, 0.75); // 长0.8 宽0.5 高0.3

    public CardBoxBlock(Properties properties, boolean heroBox) {
        super(properties);
        this.heroBox = heroBox;
    }

    /** 摸牌冷却:玩家 UUID -> 上次摸牌时刻(0.2s = 4 tick) */
    private static final java.util.Map<java.util.UUID, Long> LAST_DRAW = new java.util.HashMap<>();

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                // sneak-right-click: view remaining cards UI
                com.sanguosha.network.ServerPayloadHandler.sendRemain((net.minecraft.server.level.ServerPlayer) player,
                        pos, heroBox ? "hero" : "deck");
                return InteractionResult.sidedSuccess(true);
            }
            ServerLevel sl = (ServerLevel) level;
            long now = level.getGameTime();
            Long prev = LAST_DRAW.get(player.getUUID());
            if (prev != null && now - prev < 4) { // 0.2s 冷却
                return InteractionResult.sidedSuccess(true);
            }
            LAST_DRAW.put(player.getUUID(), now);
            Vec3 top = new Vec3(pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5);
            if (heroBox) {
                // 将盒:发武将牌
                com.sanguosha.hero.HeroDefinition h = com.sanguosha.item.BoxDeckManager.heroDeck(pos).next();
                if (h == null) {
                    player.displayClientMessage(Component.literal("\u5c06\u76d2\u5df2\u7a7a"), true);
                    return InteractionResult.sidedSuccess(true);
                }
                ItemStack card = new ItemStack(com.sanguosha.item.ModItems.CARD.get());
                String heroInfo = "武将:" + h.id + "|" + h.name;
                card.set(com.sanguosha.item.CardData.CARD_INFO, heroInfo);
                card.set(net.minecraft.core.component.DataComponents.ITEM_NAME, Component.literal("【" + h.name + "】"));
                card.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(com.sanguosha.item.CardModelIds.heroIdOf(h.id)));
                if (player.getInventory().add(card)) {
                    com.sanguosha.client.ClientHudText.show(sl, top, Component.literal(h.name).withStyle(s -> s.withColor(0xFFE8C15A).withBold(true)));
                    player.displayClientMessage(Component.literal("发将: " + h.name + " (剩 " + com.sanguosha.item.BoxDeckManager.heroDeck(pos).remaining() + " 张)"), true);
                    com.sanguosha.network.ServerPayloadHandler.syncHpList(sl.getServer());
                } else {
                    player.displayClientMessage(Component.literal("背包已满,无法发将!"), true);
                }
            } else {
                // 牌盒:摸一张手牌
                com.sanguosha.card.CardDefinition c = com.sanguosha.item.BoxDeckManager.cardDeck(pos).next();
                if (c == null) {
                    player.displayClientMessage(Component.literal("\u724c\u76d2\u5df2\u7a7a"), true);
                    return InteractionResult.sidedSuccess(true);
                }
                ItemStack card = new ItemStack(com.sanguosha.item.ModItems.CARD.get());
                String cardInfo = c.name + "|" + c.suit.cn + "|" + c.rankText();
                card.set(com.sanguosha.item.CardData.CARD_INFO, cardInfo);
                card.set(net.minecraft.core.component.DataComponents.ITEM_NAME, Component.literal("【" + c.name + "】"));
                card.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(com.sanguosha.item.CardModelIds.idOf(c.name)));
                if (player.getInventory().add(card)) {
                    int color = c.suit.color == 1 ? 0xFFE04040 : 0xFF303030;
                    com.sanguosha.client.ClientHudText.show(sl, top, Component.literal(c.suit.symbol + " " + c.rankText()).withStyle(s -> s.withColor(color).withBold(true)));
                    player.displayClientMessage(Component.literal(c.suit.symbol + " " + c.rankText()).withStyle(s -> s.withColor(color).withBold(true)), true);
                    // 摸牌后同步手牌数(名字旁显示),否则 HAND_MAP 不更新
                    com.sanguosha.network.ServerPayloadHandler.syncHpList(sl.getServer());
                } else {
                    player.displayClientMessage(Component.literal("背包已满,无法摸牌!"), true);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

}