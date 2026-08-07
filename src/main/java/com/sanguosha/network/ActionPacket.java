package com.sanguosha.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 -> 服务器:统一动作包 */
public record ActionPacket(String action, int cardIndex, int targetSeat, boolean responded, String heroId) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("sanguosha", "action");
    public static final Type<ActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, ActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ActionPacket::action,
            ByteBufCodecs.VAR_INT, ActionPacket::cardIndex,
            ByteBufCodecs.VAR_INT, ActionPacket::targetSeat,
            ByteBufCodecs.BOOL, ActionPacket::responded,
            ByteBufCodecs.STRING_UTF8, ActionPacket::heroId,
            ActionPacket::new);

    // 动作常量(线下实体卡牌模式)
    public static final String HP_UP = "hp_up";
    public static final String HP_DOWN = "hp_down";
    public static final String MAX_HP_UP = "max_hp_up";
    public static final String MAX_HP_DOWN = "max_hp_down";
    public static final String PLACE_CARD = "place_card";
    public static final String DROP_CARD = "drop_card";
    public static final String DEMOLISH = "demolish";
    public static final String CLEAR_CARDS = "clear_cards";
    public static final String REMAIN_TAKE = "remain_take";
    public static final String REMAIN_SHUFFLE = "remain_shuffle";
    public static final String GUANXING_VIEW = "guanxing_view";
    public static final String GUANXING_CONFIRM = "guanxing_confirm";
    public static final String DISCARD_VIEW = "discard_view";
    public static final String DISCARD_TAKE = "discard_take";
    public static final String DISCARD_CLEAR = "discard_clear";

    public static ActionPacket of(String action) { return new ActionPacket(action, -1, -1, false, ""); }
    public static ActionPacket of(String action, int cardIndex) { return new ActionPacket(action, cardIndex, -1, false, ""); }
    public static ActionPacket of(String action, int cardIndex, int targetSeat) { return new ActionPacket(action, cardIndex, targetSeat, false, ""); }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}