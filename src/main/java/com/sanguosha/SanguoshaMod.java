package com.sanguosha;

import com.mojang.logging.LogUtils;
import com.sanguosha.audio.ModSounds;
import com.sanguosha.command.SanguoshaCommand;
import com.sanguosha.item.CardData;
import com.sanguosha.item.CardPlaceEvents;
import com.sanguosha.entity.ModEntities;
import com.sanguosha.item.ModItems;
import com.sanguosha.game.effect.EffectRegistry;
import com.sanguosha.skill.SkillRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

@Mod(SanguoshaMod.MODID)
public class SanguoshaMod {
    public static final String MODID = "sanguosha";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SanguoshaMod(IEventBus modEventBus) {
        EffectRegistry.init();
        SkillRegistry.init();
        ModSounds.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        com.sanguosha.block.ModBlocks.BLOCKS.register(modEventBus);
        ModItems.TABS.register(modEventBus);
        CardData.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(SanguoshaMod::registerCommands);
        NeoForge.EVENT_BUS.addListener(CardPlaceEvents::onRightClickEntity);
        NeoForge.EVENT_BUS.addListener(com.sanguosha.item.CardMatEvents::onUse);
        LOGGER.info("[Sanguosha] 三国杀模组加载完成");
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        SanguoshaCommand.register(event.getDispatcher());
    }
}