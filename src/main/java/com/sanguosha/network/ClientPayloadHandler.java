package com.sanguosha.network;

import com.sanguosha.client.ClientGameState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 客户端处理服务器同步包 */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    public static void handleRemain(RemainSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if ("discard".equals(packet.boxType())) {
                // 弃牌布记录:直接打开弃牌记录 UI(箭头顺序 + 点击拿取)
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.sanguosha.client.screen.DiscardRecordScreen(packet.posX(), packet.posY(), packet.posZ(), packet.names()));
                return;
            }
            // 蹲下右键牌盒:先打开上级界面(查看剩余 / 观星),不再直接进剩余列表
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.sanguosha.client.screen.BoxMenuScreen(packet.posX(), packet.posY(), packet.posZ(), packet.boxType(), packet.names()));
        });
    }

    public static void handleGuanXing(GuanXingSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.sanguosha.client.screen.GuanXingScreen(packet.posX(), packet.posY(), packet.posZ(), packet.names()));
        });
    }

    public static void handleSync(GameSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientGameState.update(packet.json());
        });
    }
}