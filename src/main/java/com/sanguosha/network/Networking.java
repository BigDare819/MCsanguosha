package com.sanguosha.network;

import com.sanguosha.SanguoshaMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = SanguoshaMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Networking {
    private Networking() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("sanguosha").versioned("1").optional();
        registrar.playToClient(GameSyncPacket.TYPE, GameSyncPacket.STREAM_CODEC, ClientPayloadHandler::handleSync);
        registrar.playToClient(RemainSyncPacket.TYPE, RemainSyncPacket.STREAM_CODEC, ClientPayloadHandler::handleRemain);
        registrar.playToClient(GuanXingSyncPacket.TYPE, GuanXingSyncPacket.STREAM_CODEC, ClientPayloadHandler::handleGuanXing);
        registrar.playToServer(ActionPacket.TYPE, ActionPacket.STREAM_CODEC, ServerPayloadHandler::handleAction);
        SanguoshaMod.LOGGER.info("[Sanguosha] 网络通道注册完成");
    }
}