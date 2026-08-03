package com.sanguosha.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.sanguosha.game.GameManager;
import com.sanguosha.game.SanguoshaGame;
import com.sanguosha.network.ServerPayloadHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /sanguosha 指令 */
public final class SanguoshaCommand {
    private SanguoshaCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sanguosha")
            .then(Commands.literal("join").executes(ctx -> join(ctx.getSource())))
            .then(Commands.literal("start").executes(ctx -> start(ctx.getSource())))
            .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
            .then(Commands.literal("reset").executes(ctx -> reset(ctx.getSource())))
            .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
            .then(Commands.literal("card").executes(ctx -> card(ctx.getSource())))
            .then(Commands.literal("deck").executes(ctx -> deck(ctx.getSource())))
            .then(Commands.literal("table").executes(ctx -> table(ctx.getSource())))
            .then(Commands.literal("clear").executes(ctx -> clear(ctx.getSource()))));
    }

    private static int join(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        SanguoshaGame game = GameManager.get();
        if (game.join(sp)) {
            src.sendSuccess(() -> Component.literal("[三国杀] 加入成功,等待开始"), false);
        } else {
            src.sendFailure(Component.literal("[三国杀] 加入失败(游戏已开始或人数已满)"));
        }
        ServerPayloadHandler.syncAll(game, sp.server);
        return 1;
    }

    private static int start(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        SanguoshaGame game = GameManager.get();
        game.start();
        ServerPayloadHandler.syncAll(game, sp.server);
        return 1;
    }

    private static int leave(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        SanguoshaGame game = GameManager.get();
        game.leave(sp);
        ServerPayloadHandler.syncAll(game, sp.server);
        src.sendSuccess(() -> Component.literal("[三国杀] 已离开"), false);
        return 1;
    }

    private static int reset(CommandSourceStack src) throws CommandSyntaxException {
        GameManager.reset();
        src.sendSuccess(() -> Component.literal("[三国杀] 游戏已重置"), false);
        return 1;
    }

    private static int card(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.world.item.ItemStack box = new net.minecraft.world.item.ItemStack(com.sanguosha.item.ModItems.DECK_BOX.get());
        box.set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("三国杀牌盒"));
        sp.getInventory().add(box);
        src.sendSuccess(() -> Component.literal("[三国杀] 已获得牌盒,手持右键逐张发牌"), false);
        return 1;
    }

    private static int deck(CommandSourceStack src) throws CommandSyntaxException {
        com.sanguosha.item.CardDeck.reset();
        src.sendSuccess(() -> Component.literal("[三国杀] 牌堆已重新洗牌(108 张)"), false);
        return 1;
    }

    private static int table(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        net.minecraft.server.level.ServerLevel level = sp.serverLevel();
        net.minecraft.world.phys.Vec3 dir = sp.getLookAngle().normalize();
        net.minecraft.core.BlockPos base = sp.blockPosition().above();
        int[][] tableBlocks = new int[][] {
            {0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1},
            {2,0},{-2,0},{0,2},{0,-2},{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}
        };
        net.minecraft.world.level.block.Block plank = net.minecraft.world.level.block.Blocks.OAK_PLANKS;
        net.minecraft.world.level.block.Block edge = net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS;
        net.minecraft.world.level.block.Block under = net.minecraft.world.level.block.Blocks.STONE;
        int bx = sp.getBlockX(), by = sp.getBlockY() - 1, bz = sp.getBlockZ();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean border = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                level.setBlock(new net.minecraft.core.BlockPos(bx + dx, by, bz + dz), under.defaultBlockState(), 3);
                level.setBlock(new net.minecraft.core.BlockPos(bx + dx, by + 1, bz + dz),
                        (border ? edge : plank).defaultBlockState(), 3);
            }
        }
        src.sendSuccess(() -> Component.literal("[三国杀] 已生成 7x7 牌桌(站到桌上玩)"), false);
        return 1;
    }

    private static int clear(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer sp = src.getPlayerOrException();
        int n = ServerPayloadHandler.clearCards(sp);
        src.sendSuccess(() -> Component.literal("[三国杀] 已清理 " + n + " 张地上的卡牌"), false);
        return 1;
    }

    private static int status(CommandSourceStack src) throws CommandSyntaxException {
        SanguoshaGame game = GameManager.get();
        src.sendSuccess(() -> Component.literal("[三国杀] 状态:" + game.state() + " 玩家:" + game.players().size() + "/4"), false);
        return 1;
    }
}