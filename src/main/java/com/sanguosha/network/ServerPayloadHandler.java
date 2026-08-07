package com.sanguosha.network;

import com.google.gson.Gson;
import com.sanguosha.SanguoshaMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** 服务器端处理客户端动作 */
public final class ServerPayloadHandler {
    private static final Gson GSON = new Gson();
    private ServerPayloadHandler() {}

    public static void handleAction(ActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            switch (packet.action()) {
            case ActionPacket.HP_UP -> {
                int hpU = com.sanguosha.game.PlayerHp.adjust(player.getUUID(), 1);
                com.sanguosha.SanguoshaMod.LOGGER.info("[HP] server adjust up -> {}", hpU);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("♥ 血量: " + hpU), true);
                syncHpList(player.server);
            }
            case ActionPacket.MAX_HP_UP -> {
                int maxU = com.sanguosha.game.PlayerHp.adjustMax(player.getUUID(), 1);
                com.sanguosha.SanguoshaMod.LOGGER.info("[HP] server adjust max up -> {}", maxU);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("♠ 血量上限: " + maxU), true);
                syncHpList(player.server);
            }
            case ActionPacket.PLACE_CARD -> placeSelectedCard(player, packet.cardIndex());
            case ActionPacket.DROP_CARD -> dropSelectedCard(player, packet.cardIndex());
            case ActionPacket.DEMOLISH -> demolishCard(player, packet.heroId(), packet.cardIndex(), packet.responded());
            case ActionPacket.CLEAR_CARDS -> clearCards(player);
            case ActionPacket.REMAIN_TAKE -> remainTake(player, packet.heroId(), packet.cardIndex());
            case ActionPacket.REMAIN_SHUFFLE -> remainShuffle(player, packet.heroId());
            case ActionPacket.GUANXING_VIEW -> guanXingView(player, packet.heroId());
            case ActionPacket.GUANXING_CONFIRM -> guanXingConfirm(player, packet.heroId());
            case ActionPacket.DISCARD_VIEW -> discardView(player, packet.heroId());
            case ActionPacket.DISCARD_TAKE -> discardTake(player, packet.heroId(), packet.cardIndex());
            case ActionPacket.DISCARD_CLEAR -> discardClear(player, packet.heroId());
            case ActionPacket.HP_DOWN -> {
                int hpD = com.sanguosha.game.PlayerHp.adjust(player.getUUID(), -1);
                com.sanguosha.SanguoshaMod.LOGGER.info("[HP] server adjust down -> {}", hpD);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("♥ 血量: " + hpD), true);
                syncHpList(player.server);
            }
            case ActionPacket.MAX_HP_DOWN -> {
                int maxD = com.sanguosha.game.PlayerHp.adjustMax(player.getUUID(), -1);
                com.sanguosha.SanguoshaMod.LOGGER.info("[HP] server adjust max down -> {}", maxD);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("♠ 血量上限: " + maxD), true);
                syncHpList(player.server);
            }
                default -> {}
            }
            syncHpList(player.server);
        });
    }

    /** 手牌 UI 选中的牌放置到地上(准星指向的方块表面,面向玩家) */
    private static void placeSelectedCard(ServerPlayer player, int index) {
        int found = 0;
        int slot = -1;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            net.minecraft.world.item.ItemStack s = player.getInventory().items.get(i);
            if (!s.isEmpty() && s.is(com.sanguosha.item.ModItems.CARD.get())) {
                if (found == index) { slot = i; break; }
                found++;
            }
        }
        if (slot < 0) return;
        net.minecraft.world.item.ItemStack card = player.getInventory().items.get(slot);
        String info = card.get(com.sanguosha.item.CardData.CARD_INFO);
        if (info == null || info.isEmpty()) return;
        net.minecraft.world.phys.HitResult hit = player.pick(5.0, 0.0F, false);
        net.minecraft.world.phys.Vec3 pos;
        if (hit != null && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.world.phys.Vec3 loc = hit.getLocation();
            pos = new net.minecraft.world.phys.Vec3(Math.floor(loc.x) + 0.5, Math.floor(loc.y) + 0.05, Math.floor(loc.z) + 0.5);
        } else {
            net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
            net.minecraft.world.phys.Vec3 look = player.getLookAngle();
            pos = new net.minecraft.world.phys.Vec3(eye.x + look.x * 2.5, eye.y + look.y * 2.5, eye.z + look.z * 2.5);
        }
        float rot = 180.0F - player.getYRot(); // 牌顶朝玩家面朝方向(渲染逆时针 vs yRot 顺时针,南北需转 180)
        com.sanguosha.entity.CardEntity e = new com.sanguosha.entity.CardEntity(player.serverLevel(), pos.x, pos.y, pos.z, info, rot);
        player.serverLevel().addFreshEntity(e);
        player.getInventory().removeItem(slot, 1);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("已放置: " + info.split("\\|")[0]), true);
    }

    /** 丢出选中的牌(原版凋落物:player.drop,像原版按 Q 丢物品) */
    private static void dropSelectedCard(ServerPlayer player, int index) {
        int found = 0;
        int slot = -1;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            net.minecraft.world.item.ItemStack s = player.getInventory().items.get(i);
            if (!s.isEmpty() && s.is(com.sanguosha.item.ModItems.CARD.get())) {
                if (found == index) { slot = i; break; }
                found++;
            }
        }
        if (slot < 0) return;
        // 先 copy:removeItem 会修改原 ItemStack 对象(数量归零),直接传引用会让凋落物内物品为空,被原版 tick 立即销毁
        net.minecraft.world.item.ItemStack card = player.getInventory().items.get(slot).copy();
        player.getInventory().removeItem(slot, 1);
        player.drop(card, false); // 原版丢物品:生成凋落物在玩家面前
        com.sanguosha.SanguoshaMod.LOGGER.info("[DROP] card dropped via player.drop");
        syncHpList(player.server);
    }

    /** 过河拆桥:丢弃目标手牌;顺手牵羊:把目标手牌顺到自己手里 */
    private static void demolishCard(ServerPlayer actor, String targetName, int index, boolean isShun) {
        if (targetName == null || targetName.isEmpty()) return;
        ServerPlayer target = actor.server.getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            actor.displayClientMessage(net.minecraft.network.chat.Component.literal("目标玩家不在线"), true);
            return;
        }
        int found = 0;
        for (int i = 0; i < target.getInventory().items.size(); i++) {
            net.minecraft.world.item.ItemStack s = target.getInventory().items.get(i);
            if (!s.isEmpty() && s.is(com.sanguosha.item.ModItems.CARD.get())) {
                if (found == index) {
                    net.minecraft.world.item.ItemStack stolen = target.getInventory().removeItem(i, 1);
                    String stolenInfo = stolen.get(com.sanguosha.item.CardData.CARD_INFO);
                    String stolenName = stolenInfo != null ? stolenInfo.split("\\|")[0] : "一张牌";
                    if (isShun) {
                        if (actor.getInventory().add(stolen.copy())) {
                            actor.displayClientMessage(net.minecraft.network.chat.Component.literal("顺走了 " + targetName + " 的【" + stolenName + "】"), true);
                        } else {
                            target.getInventory().add(stolen); // 背包满,还回去
                            actor.displayClientMessage(net.minecraft.network.chat.Component.literal("你的背包已满,无法顺牌!"), true);
                        }
                    } else {
                        // 拆:被拆的牌掉出来显示(牌面朝上)
                        if (stolenInfo != null) {
                            net.minecraft.world.phys.Vec3 eye = actor.getEyePosition();
                            net.minecraft.world.phys.Vec3 look = actor.getLookAngle();
                            net.minecraft.world.phys.Vec3 pos = eye.add(look.scale(2.0));
                            com.sanguosha.entity.CardEntity ce = new com.sanguosha.entity.CardEntity(
                                    actor.serverLevel(), pos.x, pos.y, pos.z, stolenInfo, -actor.getYRot());
                            actor.serverLevel().addFreshEntity(ce);
                        }
                        actor.displayClientMessage(net.minecraft.network.chat.Component.literal("拆掉了 " + targetName + " 的【" + stolenName + "】"), true);
                    }
                    target.displayClientMessage(net.minecraft.network.chat.Component.literal(actor.getName().getString() + (isShun ? " 顺走了你的一张手牌" : " 拆了你的一张手牌")), true);

                    syncHpList(actor.server);
                    return;
                }
                found++;
            }
        }
        actor.displayClientMessage(net.minecraft.network.chat.Component.literal("目标没有可" + (isShun ? "顺" : "拆") + "的手牌"), true);
    }

    /** 一键清理地上所有卡牌(保留牌盒/将盒实体)。返回清理数量 */
    public static int clearCards(ServerPlayer player) {
        java.util.List<com.sanguosha.entity.CardEntity> cards = player.serverLevel().getEntitiesOfClass(
                com.sanguosha.entity.CardEntity.class, player.getBoundingBox().inflate(64));
        int n = 0;
        for (com.sanguosha.entity.CardEntity e : cards) {
            String info = e.getCardInfo();
            if (info != null && (info.startsWith("牌盒:") || info.startsWith("将盒:"))) continue;
            e.discard();
            n++;
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("已清理 " + n + " 张地上的卡牌"), true);
        com.sanguosha.SanguoshaMod.LOGGER.info("[CLEAR] removed {} cards", n);
        return n;
    }

    /** 剩余 UI 拿取:从牌盒/将盒剩余堆抽一张给玩家 */
    private static void remainTake(ServerPlayer player, String enc, int index) {
        String type = parseType(enc);
        net.minecraft.core.BlockPos pos = parsePos(enc);
        if (pos == null) return;
        if ("hero".equals(type)) {
            com.sanguosha.hero.HeroDefinition h = com.sanguosha.item.BoxDeckManager.heroDeck(pos).take(index);
            if (h == null) return;
            net.minecraft.world.item.ItemStack heroCard = new net.minecraft.world.item.ItemStack(com.sanguosha.item.ModItems.CARD.get());
            String heroInfo = "\u6b66\u5c06:" + h.id + "|" + h.name;
            heroCard.set(com.sanguosha.item.CardData.CARD_INFO, heroInfo);
            heroCard.set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("\u3010" + h.name + "\u3011"));
            heroCard.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(com.sanguosha.item.CardModelIds.heroIdOf(h.id)));
            if (player.getInventory().add(heroCard)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u62ff\u53d6\u6b66\u5c06: " + h.name), true);
                syncHpList(player.server);
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u80cc\u5305\u5df2\u6ee1"), true);
            }
        } else {
            com.sanguosha.card.CardDefinition c = com.sanguosha.item.BoxDeckManager.cardDeck(pos).take(index);
            if (c == null) return;
            net.minecraft.world.item.ItemStack card = new net.minecraft.world.item.ItemStack(com.sanguosha.item.ModItems.CARD.get());
            String cardInfo = c.name + "|" + c.suit.cn + "|" + c.rankText();
            card.set(com.sanguosha.item.CardData.CARD_INFO, cardInfo);
            card.set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("\u3010" + c.name + "\u3011"));
            card.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(com.sanguosha.item.CardModelIds.idOf(c.name)));
            if (player.getInventory().add(card)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u62ff\u53d6: " + c.name), true);
                syncHpList(player.server);
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u80cc\u5305\u5df2\u6ee1"), true);
            }
        }
        sendRemain(player, pos, type);
    }

    /** 洗牌:重新洗该牌盒的独立牌堆 */
    private static void remainShuffle(ServerPlayer player, String enc) {
        String type = parseType(enc);
        net.minecraft.core.BlockPos pos = parsePos(enc);
        if (pos == null) return;
        if ("hero".equals(type)) com.sanguosha.item.BoxDeckManager.heroDeck(pos).shuffle();
        else com.sanguosha.item.BoxDeckManager.cardDeck(pos).shuffle();
        sendRemain(player, pos, type);
    }

    /** "x,y,z,type" -> BlockPos;非法返回 null */
    private static net.minecraft.core.BlockPos parsePos(String enc) {
        String[] parts = enc.split(",");
        if (parts.length < 4) return null;
        try {
            return new net.minecraft.core.BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) { return null; }
    }

    private static String parseType(String enc) {
        String[] parts = enc.split(",");
        return parts.length >= 4 ? parts[3] : "deck";
    }

    /** 发送牌盒/将盒剩余列表给玩家(打开/刷新 UI);按方块位置取独立牌堆 */
    public static void sendRemain(ServerPlayer player, net.minecraft.core.BlockPos pos, String type) {
        java.util.List<String> names = new java.util.ArrayList<>();
        if ("hero".equals(type)) {
            for (com.sanguosha.hero.HeroDefinition h : com.sanguosha.item.BoxDeckManager.heroDeck(pos).remainingList()) names.add(h.name);
        } else {
            for (com.sanguosha.card.CardDefinition c : com.sanguosha.item.BoxDeckManager.cardDeck(pos).remainingList()) {
                names.add(c.suit.symbol + c.rankText() + " " + c.name);
            }
        }
        PacketDistributor.sendToPlayer(player, new RemainSyncPacket(pos.getX(), pos.getY(), pos.getZ(), type, names));
    }

    /** 发送弃牌布记录列表给玩家(打开/刷新弃牌 UI);编码 "x,y,z,discard";显示名 = 牌名(武将牌取汉字名) */
    public static void discardView(ServerPlayer player, String enc) {
        net.minecraft.core.BlockPos pos = parsePos(enc);
        if (pos == null) return;
        java.util.List<String> names = new java.util.ArrayList<>();
        for (String info : com.sanguosha.item.DiscardDeckManager.list(pos)) {
            String[] parts = info.split("\\|");
            // 武将牌("武将:id|名字")显示汉字名;普通牌显示牌名
            if (parts.length > 1 && parts[0].startsWith("武将:")) {
                names.add(parts[1]);
            } else {
                names.add(parts.length > 0 ? parts[0] : info);
            }
        }
        PacketDistributor.sendToPlayer(player, new RemainSyncPacket(pos.getX(), pos.getY(), pos.getZ(), "discard", names));
    }

    /** 从弃牌布拿取一条记录:移除记录并生成卡牌物品加入玩家背包;编码 "x,y,z,discard",cardIndex=记录索引 */
    private static void discardTake(ServerPlayer player, String enc, int index) {
        net.minecraft.core.BlockPos pos = parsePos(enc);
        if (pos == null) return;
        String info = com.sanguosha.item.DiscardDeckManager.take(pos, index);
        if (info == null) return;
        String[] parts = info.split("\\|");
        // 武将牌("武将:id|名字")用汉字名;普通牌用牌名
        String name = (parts.length > 1 && parts[0].startsWith("武将:"))
                ? parts[1]
                : (parts.length > 0 ? parts[0] : info);
        // 生成卡牌物品进背包(与 remainTake 同格式:普通牌 / 武将牌)
        net.minecraft.world.item.ItemStack card = new net.minecraft.world.item.ItemStack(com.sanguosha.item.ModItems.CARD.get());
        card.set(com.sanguosha.item.CardData.CARD_INFO, info);
        if (info.startsWith("武将:")) {
            // 武将牌:武将:id|名字
            String heroId = parts[0].substring("武将:".length());
            card.set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("【" + name + "】"));
            card.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(com.sanguosha.item.CardModelIds.heroIdOf(heroId)));
        } else {
            // 普通牌:牌名|花色|点数
            card.set(net.minecraft.core.component.DataComponents.ITEM_NAME, net.minecraft.network.chat.Component.literal("【" + name + "】"));
            card.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(com.sanguosha.item.CardModelIds.idOf(name)));
        }
        if (player.getInventory().add(card)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("拿回弃牌: " + name), true);
        } else {
            // 背包满:放回记录
            com.sanguosha.item.DiscardDeckManager.add(pos, info);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("背包已满,拿回失败"), true);
        }
        // 刷新记录 UI + 浮空文本
        discardView(player, enc);
        com.sanguosha.item.DiscardMatScanner.refreshDisplay(player.serverLevel(), pos);
    }

    /** 一键清空弃牌区:删除 4x4 弃牌布上方残留的卡牌实体/凋落物并清空记录;编码 "x,y,z,discard" */
    private static void discardClear(ServerPlayer player, String enc) {
        net.minecraft.core.BlockPos pos = parsePos(enc);
        if (pos == null) return;
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        net.minecraft.world.level.block.state.BlockState s0 = level.getBlockState(pos);
        if (s0.getBlock() != com.sanguosha.block.ModBlocks.DISCARD_MAT.get()) return;
        net.minecraft.core.Direction dir = s0.getValue(com.sanguosha.block.DiscardMatBlock.FACING);
        net.minecraft.core.Direction right = dir.getClockWise();
        // 4x4 区域上方 0.02 ~ 1.2 格(与 DiscardMatScanner.updateMat 同区域)
        double bx = pos.getX() + right.getStepX() * 3.0 + dir.getStepX() * 3.0;
        double bz = pos.getZ() + right.getStepZ() * 3.0 + dir.getStepZ() * 3.0;
        double minX = Math.min(pos.getX(), bx);
        double maxX = Math.max(pos.getX(), bx) + 1.0;
        double minZ = Math.min(pos.getZ(), bz);
        double maxZ = Math.max(pos.getZ(), bz) + 1.0;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(minX, pos.getY() + 0.02, minZ, maxX, pos.getY() + 1.2, maxZ);
        int n = 0;
        for (com.sanguosha.entity.CardEntity e : level.getEntitiesOfClass(com.sanguosha.entity.CardEntity.class, box)) {
            String info = e.getCardInfo();
            if (info == null || info.isEmpty()) continue;
            e.discard();
            n++;
        }
        for (net.minecraft.world.entity.item.ItemEntity it : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            net.minecraft.world.item.ItemStack st = it.getItem();
            if (st.isEmpty() || !st.is(com.sanguosha.item.ModItems.CARD.get())) continue;
            if (st.get(com.sanguosha.item.CardData.CARD_INFO) == null) continue;
            it.discard();
            n++;
        }
        com.sanguosha.item.DiscardDeckManager.clear(pos);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u5df2\u6e05\u7a7a\u5f03\u724c\u533a " + n + " \u5f20"), true);
        com.sanguosha.SanguoshaMod.LOGGER.info("[DISCARD] cleared {} cards at {}", n, pos);
        // 刷新记录 UI(空列表) + 移除浮空文本
        discardView(player, enc);
        com.sanguosha.item.DiscardMatScanner.refreshDisplay(level, pos);
    }

    /** 观星:发送牌堆顶 n 张(诸葛亮观星,线下模式);编码 "x,y,z,deck|n",默认 5 */
    private static void guanXingView(ServerPlayer player, String enc) {
        String[] parts = enc.split("\\|");
        if (parts.length < 1) return;
        net.minecraft.core.BlockPos pos = parsePos(parts[0]);
        if (pos == null) return;
        int count = 5;
        if (parts.length > 1) {
            try { count = Math.max(1, Math.min(5, Integer.parseInt(parts[1].trim()))); } catch (NumberFormatException ignored) {}
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        for (com.sanguosha.card.CardDefinition c : com.sanguosha.item.BoxDeckManager.cardDeck(pos).peekTop(count)) {
            names.add(c.name + "|" + c.suit.cn + "|" + c.rankText());
        }
        PacketDistributor.sendToPlayer(player, new GuanXingSyncPacket(pos.getX(), pos.getY(), pos.getZ(), names));
    }

    /** 观星确认:按玩家顺序重排牌堆顶 n 张;编码 "x,y,z,deck|count|idx,idx,..."(idx 为原顶牌下标,未列出的放底) */
    private static void guanXingConfirm(ServerPlayer player, String enc) {
        String[] parts = enc.split("\\|");
        if (parts.length < 1) return;
        net.minecraft.core.BlockPos pos = parsePos(parts[0]);
        if (pos == null) return;
        int count = 5;
        if (parts.length > 1) {
            try { count = Math.max(1, Math.min(5, Integer.parseInt(parts[1].trim()))); } catch (NumberFormatException ignored) {}
        }
        java.util.List<Integer> order = new java.util.ArrayList<>();
        if (parts.length > 2 && !parts[2].isEmpty()) {
            for (String s : parts[2].split(",")) {
                try { order.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
            }
        }
        com.sanguosha.item.LocalCardDeck deck = com.sanguosha.item.BoxDeckManager.cardDeck(pos);
        deck.reorderTop(count, order);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u89c2\u661f\u5b8c\u6210"), true);
        com.sanguosha.SanguoshaMod.LOGGER.info("[GUANXING] reorder at {} count={} order={}", pos, count, order);
    }

    /** 轻量同步:只广播 hpList(手牌数/血量),摸牌丢牌后调用 */
    public static void syncHpList(MinecraftServer server) {
        if (server == null) return;
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonArray hpList = new com.google.gson.JsonArray();
        for (ServerPlayer p2 : server.getPlayerList().getPlayers()) {
            com.google.gson.JsonObject m2 = new com.google.gson.JsonObject();
            m2.addProperty("name", p2.getName().getString());
            m2.addProperty("hp", com.sanguosha.game.PlayerHp.get(p2.getUUID()));
            m2.addProperty("maxHp", com.sanguosha.game.PlayerHp.getMax(p2.getUUID()));
            int hc = 0;
            for (net.minecraft.world.item.ItemStack is : p2.getInventory().items) {
                if (!is.isEmpty() && is.is(com.sanguosha.item.ModItems.CARD.get())) hc++;
            }
            m2.addProperty("handCount", hc);
            hpList.add(m2);
        }
        root.add("hpList", hpList);
        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(sp, new GameSyncPacket(root.toString()));
        }
    }
}