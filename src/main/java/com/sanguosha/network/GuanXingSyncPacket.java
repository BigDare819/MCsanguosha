package com.sanguosha.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** 服务器 -> 客户端:观星牌堆顶列表(pos 牌盒位置, names 每张"牌名|花色|点数") */
public record GuanXingSyncPacket(int posX, int posY, int posZ, List<String> names) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("sanguosha", "guanxing_sync");
    public static final Type<GuanXingSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, GuanXingSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GuanXingSyncPacket::posX,
            ByteBufCodecs.VAR_INT, GuanXingSyncPacket::posY,
            ByteBufCodecs.VAR_INT, GuanXingSyncPacket::posZ,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), GuanXingSyncPacket::names,
            GuanXingSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
