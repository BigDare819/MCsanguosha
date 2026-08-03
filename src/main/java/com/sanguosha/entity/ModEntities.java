package com.sanguosha.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** 实体注册 */
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, "sanguosha");

    /** 桌面卡牌实体(固定位置渲染 3D 卡牌) */
    public static final Supplier<EntityType<CardEntity>> CARD = ENTITIES.register("card_entity",
            () -> EntityType.Builder.<CardEntity>of((type, level) -> new CardEntity(type, level), MobCategory.MISC)
                    .sized(0.6F, 0.08F)
                    .clientTrackingRange(10)
                    .build("card_entity"));
}