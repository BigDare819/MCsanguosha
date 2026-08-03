package com.sanguosha.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** 服务器 -> 客户端:牌盒/将盒剩余列表(type: "deck" 牌盒 / "hero" 将盒) */
public record RemainSyncPacket(int posX, int posY, int posZ, String boxType, List<String> names) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("sanguosha", "remain_sync");
    public static final Type<RemainSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, RemainSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RemainSyncPacket::posX,
            ByteBufCodecs.VAR_INT, RemainSyncPacket::posY,
            ByteBufCodecs.VAR_INT, RemainSyncPacket::posZ,
            ByteBufCodecs.STRING_UTF8, RemainSyncPacket::boxType,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), RemainSyncPacket::names,
            RemainSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}