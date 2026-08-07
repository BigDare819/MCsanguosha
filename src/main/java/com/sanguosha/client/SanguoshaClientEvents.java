package com.sanguosha.client;

import com.sanguosha.SanguoshaMod;
import com.sanguosha.audio.AudioManager;
import com.sanguosha.audio.ModSounds;
import com.sanguosha.client.screen.ChoiceScreen;
import com.sanguosha.client.screen.HeroSelectScreen;
import com.sanguosha.client.screen.SanguoshaScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** 客户端事件:按键、HUD、选将弹窗 */
@EventBusSubscriber(modid = SanguoshaMod.MODID, value = Dist.CLIENT)
public final class SanguoshaClientEvents {
    @net.neoforged.bus.api.SubscribeEvent
    public static void onNameTag(net.neoforged.neoforge.client.event.RenderNameTagEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player p) {
            String name = p.getName().getString();
            int hp = ClientGameState.HP_MAP.getOrDefault(name, -1);
            if (hp > 0) {
                // 头顶血量显示为 当前/上限(x/y):x=血量面板的当前血量,y=血量面板的血量上限
                int maxHp = ClientGameState.MAX_HP_MAP.getOrDefault(name, 4);
                int hc = ClientGameState.HAND_MAP.getOrDefault(name, 0);
                event.setContent(net.minecraft.network.chat.Component.literal(name + " ♥" + hp + "/" + maxHp + "  手牌" + hc));
            }
        }
    }

    private static boolean heroScreenShown = false;
    private static int lastAnimSfxSeq = -1;
    private static boolean lastRunning = false;
    private static boolean lobbyAutoShown = false;

    private SanguoshaClientEvents() {}


    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AudioManager.tick();
        // 出牌配音:按牌类型 + 出牌者性别(男/女,序号变化才播)
        if (ClientGameState.animSeq != lastAnimSfxSeq && !ClientGameState.animCard.isEmpty()) {
            lastAnimSfxSeq = ClientGameState.animSeq;
            String card = ClientGameState.animCard;
            String heroId = "";
            for (ClientGameState.CPlayer p : ClientGameState.players) {
                if (p.seat == ClientGameState.animFrom) { heroId = p.heroId; break; }
            }
            boolean female = heroId.equals("daqiao") || heroId.equals("diaochan")
                    || heroId.equals("sunshangxiang") || heroId.equals("zhenji")
                    || heroId.equals("huangyueying");
            if (card.contains("杀")) {
                AudioManager.play(mc, female ? ModSounds.SLASH_FEMALE.get() : ModSounds.SLASH_MALE.get());
            } else if (card.contains("闪")) {
                AudioManager.play(mc, female ? ModSounds.JINK_FEMALE.get() : ModSounds.JINK_MALE.get());
            } else if (card.contains("无中生有")) {
                AudioManager.play(mc, female ? ModSounds.EXNIHILO_FEMALE.get() : ModSounds.EXNIHILO_MALE.get());
            } else if (card.contains("顺手牵羊")) {
                AudioManager.play(mc, female ? ModSounds.SNATCH_FEMALE.get() : ModSounds.SNATCH_MALE.get());
            } else if (card.contains("决斗")) {
                AudioManager.play(mc, female ? ModSounds.DUEL_FEMALE.get() : ModSounds.DUEL_MALE.get());
            } else if (card.contains("酒")) {
                AudioManager.play(mc, ModSounds.ANALEPTIC_MALE.get());
            } else if (card.contains("南蛮入侵")) {
                AudioManager.play(mc, ModSounds.SAVAGE_MALE.get());
            } else if (card.contains("万箭齐发")) {
                AudioManager.play(mc, female ? ModSounds.ARCHERY_FEMALE.get() : ModSounds.ARCHERY_MALE.get());
            } else if (card.contains("五谷丰登")) {
                AudioManager.play(mc, ModSounds.BOUNTIFUL_MALE.get());
            } else if (card.contains("无懈可击")) {
                AudioManager.play(mc, ModSounds.NULLIFICATION_MALE.get());
            } else if (card.contains("铁索连环")) {
                AudioManager.play(mc, ModSounds.IRON_CHAIN_FEMALE.get());
            }
        }
        // 按 G 打开/关闭牌桌
        if (ModKeybinds.OPEN_TABLE.consumeClick()) {
            if (mc.screen instanceof SanguoshaScreen) {
                mc.setScreen(null);
            } else if (ClientGameState.inGame) {
                mc.setScreen(new SanguoshaScreen());
            }
        }
        // 自动弹大厅已禁用(实体卡牌线下模式不需要大厅)
        // 对局开始(选将完成)自动进入牌桌
        boolean running = "RUNNING".equals(ClientGameState.state);
        if (running && !lastRunning && mc.screen == null) {
            mc.setScreen(new SanguoshaScreen());
        }
        lastRunning = running;
        // 选择弹窗(反间/鬼才/流离/观星)
        if (!ClientGameState.choicePrompt.isEmpty() && !ClientGameState.choiceOptions.isEmpty()) {
            if (!(mc.screen instanceof ChoiceScreen)) {
                mc.setScreen(new ChoiceScreen(ClientGameState.choicePrompt, ClientGameState.choiceOptions));
            }
        } else if (mc.screen instanceof ChoiceScreen) {
            mc.setScreen(null);
        }
        // 选将阶段自动弹出
        if ("HERO_SELECT".equals(ClientGameState.state) && !ClientGameState.heroOptions.isEmpty()) {
            if (!heroScreenShown && mc.screen == null) {
                heroScreenShown = true;
                mc.setScreen(new HeroSelectScreen());
            }
        } else if (!"HERO_SELECT".equals(ClientGameState.state)) {
            heroScreenShown = false;
        }
    }

    @SubscribeEvent
    public static void onRenderHud(RenderGuiEvent.Post event) {
        if (!ClientGameState.inGame || ClientGameState.lastLog.isEmpty()) return;
        var g = event.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        // 顶部中央显示日志
        g.drawCenteredString(font, ClientGameState.lastLog, event.getGuiGraphics().guiWidth() / 2, 4, 0xFFFFFF00);
        // 需要响应时提示
        if (!ClientGameState.prompt.isEmpty() && !(Minecraft.getInstance().screen instanceof SanguoshaScreen)) {
            String tip = ClientGameState.prompt.equals("jink") ? "【闪】" : "【杀】";
            String openKey = ModKeybinds.OPEN_TABLE.getKey().getDisplayName().getString();
            g.drawCenteredString(font, "你需要打出 " + tip + " !按 " + openKey + " 打开牌桌", event.getGuiGraphics().guiWidth() / 2, 16, 0xFFFF4444);
        }
    }
}