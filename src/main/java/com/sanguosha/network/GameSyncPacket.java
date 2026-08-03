package com.sanguosha.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务器 -> 客户端:游戏状态全量同步(JSON) */
public record GameSyncPacket(String json) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("sanguosha", "sync");
    public static final Type<GameSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, GameSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GameSyncPacket::json,
            GameSyncPacket::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}