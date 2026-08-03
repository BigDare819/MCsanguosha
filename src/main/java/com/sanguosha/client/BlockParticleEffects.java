package com.sanguosha.client;

import com.sanguosha.block.CardBoxBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 牌盒/将盒踩踏粒子(客户端):玩家踩在盒上时,脚下冒原版方块碎片粒子
 * (ParticleTypes.BLOCK,纹理取方块模型的 particle 字段,原版风格)。
 * 破坏粒子为方块默认行为(levelEvent 2001),无需额外代码。
 */
@EventBusSubscriber(modid = "sanguosha", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class BlockParticleEffects {
    private static int stepTicks = 0;

    private BlockParticleEffects() {}

    /** 玩家踩在盒上时,脚下每 8 tick 冒一粒原版方块碎屑 */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.screen != null) return;
        Player p = mc.player;
        if (!p.onGround()) return;
        BlockPos below = p.getBlockPosBelowThatAffectsMyMovement();
        BlockState state = mc.level.getBlockState(below);
        if (!(state.getBlock() instanceof CardBoxBlock)) return;
        if (++stepTicks < 8) return;
        stepTicks = 0;
        RandomSource rnd = mc.level.random;
        double x = p.getX() + (rnd.nextDouble() - 0.5) * 0.6;
        double y = below.getY() + 0.05;
        double z = p.getZ() + (rnd.nextDouble() - 0.5) * 0.6;
        mc.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 0, 0.05, 0);
    }
}
