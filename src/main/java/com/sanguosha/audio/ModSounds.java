package com.sanguosha.audio;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** 三国杀音效注册(与 assets/sanguosha/sounds.json 对应) */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, "sanguosha");

    public static final Supplier<SoundEvent> CARD_USE = register("card_use");
    public static final Supplier<SoundEvent> CARD_DRAW = register("card_draw");
    public static final Supplier<SoundEvent> DAMAGE = register("damage");
    public static final Supplier<SoundEvent> DEATH = register("death");
    public static final Supplier<SoundEvent> WIN = register("win");
    public static final Supplier<SoundEvent> BUTTON = register("button");
    public static final Supplier<SoundEvent> JUDGE = register("judge");
    public static final Supplier<SoundEvent> SLASH_MALE = register("slash_male");
    public static final Supplier<SoundEvent> SLASH_FEMALE = register("slash_female");
    public static final Supplier<SoundEvent> JINK_MALE = register("jink_male");
    public static final Supplier<SoundEvent> JINK_FEMALE = register("jink_female");
    public static final Supplier<SoundEvent> PEACH = register("peach");
    public static final Supplier<SoundEvent> COMBO1 = register("combo1");
    public static final Supplier<SoundEvent> COMBO2 = register("combo2");
    public static final Supplier<SoundEvent> COMBO3 = register("combo3");
    public static final Supplier<SoundEvent> EXNIHILO_MALE = register("exnihilo_male");
    public static final Supplier<SoundEvent> EXNIHILO_FEMALE = register("exnihilo_female");
    public static final Supplier<SoundEvent> SNATCH_MALE = register("snatch_male");
    public static final Supplier<SoundEvent> SNATCH_FEMALE = register("snatch_female");
    public static final Supplier<SoundEvent> DUEL_MALE = register("duel_male");
    public static final Supplier<SoundEvent> DUEL_FEMALE = register("duel_female");
    public static final Supplier<SoundEvent> ANALEPTIC_MALE = register("analeptic_male");
    public static final Supplier<SoundEvent> SAVAGE_MALE = register("savage_male");
    public static final Supplier<SoundEvent> ARCHERY_MALE = register("archery_male");
    public static final Supplier<SoundEvent> ARCHERY_FEMALE = register("archery_female");
    public static final Supplier<SoundEvent> BOUNTIFUL_MALE = register("bountiful_male");
    public static final Supplier<SoundEvent> NULLIFICATION_MALE = register("nullification_male");
    public static final Supplier<SoundEvent> IRON_CHAIN_FEMALE = register("iron_chain_female");
    public static final Supplier<SoundEvent> LIGHTNING = register("lightning");

    private static Supplier<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("sanguosha", name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}