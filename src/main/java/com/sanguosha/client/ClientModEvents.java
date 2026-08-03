package com.sanguosha.client;

import com.sanguosha.entity.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 客户端事件:注册卡牌实体渲染器 */
@EventBusSubscriber(modid = "sanguosha", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    @net.neoforged.bus.api.SubscribeEvent
    public static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.HP_UP);
        event.register(ModKeybinds.HP_DOWN);
        event.register(ModKeybinds.TOGGLE_UI);
        event.register(ModKeybinds.PLACE_CARD);
        event.register(ModKeybinds.OPEN_TABLE);
        event.register(ModKeybinds.DROP_CARD);
        event.register(ModKeybinds.CLEAR_CARDS);
    }
    private ClientModEvents() {}

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CARD.get(), com.sanguosha.client.render.CardEntityRenderer::new);
    }
}