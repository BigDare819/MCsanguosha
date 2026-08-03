package com.sanguosha.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 三国杀卡牌物品:悬停显示牌名;右键放置到桌面(CardEntity,可捡回) */
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
        // 准星对着卡牌/牌盒实体时,不放置(让给实体交互:摸牌/旋转/捡起)
        if (!level.isClientSide) {
            net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
            net.minecraft.world.phys.Vec3 end = eye.add(player.getLookAngle().scale(5.0));
            net.minecraft.world.phys.AABB ray = new net.minecraft.world.phys.AABB(eye, end).inflate(0.5);
            for (net.minecraft.world.entity.Entity e : level.getEntities(player, ray,
                    e2 -> e2 instanceof com.sanguosha.entity.CardEntity)) {
                if (e.getBoundingBox().inflate(0.1).clip(eye, end).isPresent()) {
                    return InteractionResultHolder.pass(player.getItemInHand(hand));
                }
            }
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            HitResult hit = player.pick(5.0, 0.0F, false);
            Vec3 pos;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                Vec3 loc = hit.getLocation();
                pos = new Vec3(Math.floor(loc.x) + 0.5, Math.floor(loc.y) + 0.02, Math.floor(loc.z) + 0.5);
            } else {
                Vec3 eye = player.getEyePosition();
                Vec3 look = player.getLookAngle();
                pos = new Vec3(eye.x + look.x * 2.5, eye.y + look.y * 2.5, eye.z + look.z * 2.5);
            }
            float rot = 180.0F - player.getYRot(); // 牌顶朝玩家面朝方向
            String info = stack.get(CardData.CARD_INFO);
            com.sanguosha.SanguoshaMod.LOGGER.info("[Card] use: info='{}' pos=({},{},{})", info, pos.x, pos.y, pos.z);
            com.sanguosha.entity.CardEntity disp = new com.sanguosha.entity.CardEntity(level, pos.x, pos.y, pos.z, info == null ? "" : info, rot);
            level.addFreshEntity(disp);
            player.displayClientMessage(Component.literal("卡牌已放置在桌面(右键旋转,左键拿回)"), true);
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String cardInfo = stack.get(com.sanguosha.item.CardData.CARD_INFO);
        if (cardInfo == null) cardInfo = "未知牌";
        tooltipComponents.add(Component.literal("三国杀 · " + cardInfo));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}