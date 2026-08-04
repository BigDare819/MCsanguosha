package com.sanguosha.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 三国杀卡牌物品:悬停显示牌名。右键放置已禁用(会卡进牌布下面),放置用 R 键 */
public class CardItem extends Item {
    public CardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new com.sanguosha.client.render.CardItemExtensions());
    }





    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // 右键放置卡牌已禁用:准星命中牌布(1px 薄片)时,实体牌会卡进/掉到牌布下面。
        // 放置请用 R 键(PLACE_CARD)走网络包 ServerPayloadHandler.placeSelectedCard。
        // 摸牌/翻转/捡起等实体交互由 CardPlaceEvents(EntityInteract 事件)处理,不受影响。
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String cardInfo = stack.get(com.sanguosha.item.CardData.CARD_INFO);
        if (cardInfo == null) cardInfo = "未知牌";
        tooltipComponents.add(Component.literal("三国杀 · " + cardInfo));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}