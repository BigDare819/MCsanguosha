package com.sanguosha.client;

import com.sanguosha.SanguoshaMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** 客户端事件:头顶血量显示(线下实体卡牌模式) */
@EventBusSubscriber(modid = SanguoshaMod.MODID, value = Dist.CLIENT)
public final class SanguoshaClientEvents {
    @net.neoforged.bus.api.SubscribeEvent
    public static void onNameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player p) {
            String name = p.getName().getString();
            int hp = ClientGameState.HP_MAP.getOrDefault(name, -1);
            if (hp > 0) {
                // 头顶血量显示为 当前/上限(x/y):x=血量面板的当前血量,y=血量面板的血量上限
                int maxHp = ClientGameState.MAX_HP_MAP.getOrDefault(name, 4);
                int hc = ClientGameState.HAND_MAP.getOrDefault(name, 0);
                event.setContent(net.minecraft.network.chat.Component.literal(name + " ♥" + hp + "/" + maxHp + "  手牌" + hc));
            }
        }
    }

    private SanguoshaClientEvents() {}
}
