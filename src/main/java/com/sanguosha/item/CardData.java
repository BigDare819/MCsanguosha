package com.sanguosha.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** 卡牌自定义数据:牌名|花色|点数 组合字符串(如 杀|红桃|7) */
public final class CardData {
    private CardData() {}

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "sanguosha");

    public static final Supplier<DataComponentType<String>> CARD_INFO =
            COMPONENTS.register("card_info", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}