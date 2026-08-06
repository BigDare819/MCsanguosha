package com.sanguosha.item;

import com.sanguosha.entity.CardEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** 桌面卡牌交互:右键翻面/摸牌;Shift+右键:牌盒/将盒=查看剩余,普通牌=捡起 */
public final class CardPlaceEvents {
    private static final java.util.Map<java.util.UUID, Long> lastDraw = new java.util.HashMap<>();

    private CardPlaceEvents() {}

    @SubscribeEvent
    public static void onRightClickEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof CardEntity disp && !disp.level().isClientSide()) {
            Player p = event.getEntity();
            String info = disp.getCardInfo();
            if (p.isShiftKeyDown()) {
                // 蹲下右键:牌盒/将盒 -> 查看剩余卡牌 UI;普通牌 -> 捡起
                if ("\u724c\u76d2:".equals(info) || "\u5c06\u76d2:".equals(info)) {
                    com.sanguosha.network.ServerPayloadHandler.sendRemain((net.minecraft.server.level.ServerPlayer) p,
                            disp.blockPosition(), "\u724c\u76d2:".equals(info) ? "deck" : "hero");
                    p.displayClientMessage(Component.literal("\u67e5\u770b\u5269\u4f59\u5361\u724c"), true);
                } else {
                    ItemStack s = disp.toItemStack();
                    if (p.getInventory().add(s)) {
                        disp.discard();
                        p.displayClientMessage(Component.literal("\u5df2\u62fe\u8d77"), true);
                        com.sanguosha.network.ServerPayloadHandler.syncHpList(((net.minecraft.server.level.ServerLevel) disp.level()).getServer());
                    }
                }
            } else {
                long now = System.currentTimeMillis();
                Long prev = lastDraw.get(p.getUUID());
                boolean cooldown = prev != null && now - prev < 200;
                if (("\u724c\u76d2:".equals(info) || "\u5c06\u76d2:".equals(info)) && !cooldown) {
                    lastDraw.put(p.getUUID(), now);
                }
                if ("\u724c\u76d2:".equals(info)) {
                    // 摸一张手牌:直接进背包(点一下一张,有冷却防连发)
                    if (cooldown) { event.setCanceled(true); return; }
                    com.sanguosha.card.CardDefinition c = com.sanguosha.item.CardDeck.next();
                    ItemStack card = new ItemStack(ModItems.CARD.get());
                    String cardInfo = c.name + "|" + c.suit.cn + "|" + c.rankText();
                    card.set(CardData.CARD_INFO, cardInfo);
                    card.set(DataComponents.ITEM_NAME, Component.literal("\u3010" + c.name + "\u3011"));
                    card.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(CardModelIds.idOf(c.name)));
                    if (p.getInventory().add(card)) {
                        // 牌盒上方显示花色符号(红=红字,黑=黑字),5 秒后消失
                        int color = c.suit.color == 1 ? 0xFFE04040 : 0xFF303030;
                        net.minecraft.network.chat.Style st = net.minecraft.network.chat.Style.EMPTY.withColor(color).withBold(true);
                        disp.showNameTemporarily(Component.literal(c.suit.symbol + " " + c.rankText()).withStyle(st));
                        com.sanguosha.network.ServerPayloadHandler.syncHpList(((net.minecraft.server.level.ServerLevel) disp.level()).getServer());
                    }
                } else if ("\u5c06\u76d2:".equals(info)) {
                    // 发武将牌:直接进背包(点一下一张)
                    if (cooldown) { event.setCanceled(true); return; }
                    com.sanguosha.hero.HeroDefinition h = com.sanguosha.item.HeroDeckBox.next();
                    ItemStack heroCard = new ItemStack(ModItems.CARD.get());
                    String heroInfo = "\u6b66\u5c06:" + h.id + "|" + h.name;
                    heroCard.set(CardData.CARD_INFO, heroInfo);
                    heroCard.set(DataComponents.ITEM_NAME, Component.literal("\u3010" + h.name + "\u3011"));
                    heroCard.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(CardModelIds.heroIdOf(h.id)));
                    if (p.getInventory().add(heroCard)) {
                        p.displayClientMessage(Component.literal("\u53d1\u5c06: " + h.name + " (\u5269 " + com.sanguosha.item.HeroDeckBox.remaining() + " \u5f20)"), true);
                        com.sanguosha.network.ServerPayloadHandler.syncHpList(((net.minecraft.server.level.ServerLevel) disp.level()).getServer());
                    } else {
                        p.displayClientMessage(Component.literal("\u80cc\u5305\u5df2\u6ee1,\u65e0\u6cd5\u53d1\u5c06!"), true);
                    }
                } else {
                    // 右键:旋转 45
                    disp.setCardRotation(disp.getCardRotation() + 45.0F);
                }
            }
            event.setCanceled(true);
        }
    }
}