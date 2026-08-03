package com.sanguosha.audio;

import com.sanguosha.client.ClientGameState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

import java.util.HashSet;
import java.util.Set;

/** 客户端音效播放:逐条处理最近日志(去重),多事件不再丢失 */
public final class AudioManager {
    private static final Set<String> processedLogs = new HashSet<>();
    private static int lastSeat = -1;
    private static int comboCount = 0;

    private AudioManager() {}

    /** 每 tick 调用,处理新日志并播放对应音效 */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        // 回合切换重置连杀计数
        if (ClientGameState.currentSeat != lastSeat) {
            lastSeat = ClientGameState.currentSeat;
            comboCount = 0;
        }
        if (processedLogs.size() > 300) processedLogs.clear();
        for (String log : ClientGameState.recentLogs) {
            if (log == null || log.isEmpty()) continue;
            if (!processedLogs.add(log)) continue; // 已处理过
            if (log.contains("被【闪电】击中")) {
                play(mc, ModSounds.LIGHTNING.get());
            } else if (log.contains("使用了【桃】") || log.contains("【急救】")) {
                play(mc, ModSounds.PEACH.get());
            } else if (log.contains("摸了")) {
                play(mc, ModSounds.CARD_DRAW.get());
            } else if (log.contains("受到") || log.contains("击中")) {
                play(mc, ModSounds.DAMAGE.get());
            } else if (log.contains("阵亡")) {
                play(mc, ModSounds.DEATH.get());
                comboCount++;
                if (comboCount == 1) play(mc, ModSounds.COMBO1.get());
                else if (comboCount == 2) play(mc, ModSounds.COMBO2.get());
                else play(mc, ModSounds.COMBO3.get());
            } else if (log.contains("获胜")) {
                play(mc, ModSounds.WIN.get());
            } else if (log.contains("判定")) {
                play(mc, ModSounds.JUDGE.get());
            }
        }
    }

    public static void playButton() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) play(mc, ModSounds.BUTTON.get());
    }

    public static void play(Minecraft mc, SoundEvent sound) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }
}