package com.sanguosha.client.screen;

import com.sanguosha.client.ClientGameState;
import com.sanguosha.client.widget.CardWidget;
import com.sanguosha.client.widget.PlayerPanelWidget;
import com.sanguosha.network.ActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 三国杀牌桌界面 */
public class SanguoshaScreen extends Screen {
    // 需要选择目标的牌
    private static final Set<String> TARGET_CARDS = Set.of("slash", "fire_slash", "thunder_slash", "duel", "dismantlement", "snatch", "indulgence", "collateral", "iron_chain");

    private static final int CARD_W = 54;
    private static final int CARD_H = 76;

    private int selectedCard = -1;
    private int pendingConvertCard = -1;
    private String pendingConvertEffect = null;
    private int lijianTargetA = -2;   // -2 未激活 / -1 等第一人 / >=0 等第二人
    private int rendeCardIndex = -1;  // 仁德选中的手牌
    private boolean fanjianMode = false; // 反间选目标中
    private int animSeq = -1;            // 出牌动画序号(变化才播放,防重复)
    private int animFrom = -1;
    private int animTo = -1;
    private long animStart = 0;
    private String statusMsg = "";
    private long statusTime = 0;
    private final List<int[]> buttons = new ArrayList<>(); // x,y,w,h

    public SanguoshaScreen() {
        super(Component.literal("三国杀牌桌"));
    }

    private boolean isMyTurn() {
        return ClientGameState.currentSeat == ClientGameState.mySeat
                && "RUNNING".equals(ClientGameState.state)
                && "PLAY".equals(ClientGameState.phase);
    }

    private boolean needsTarget(String effect) {
        return TARGET_CARDS.contains(effect);
    }

    private String myWeapon() {
        for (ClientGameState.CPlayer p : ClientGameState.players) {
            if (p.seat == ClientGameState.mySeat) return p.weapon;
        }
        return "";
    }

    private String myHeroId() {
        for (ClientGameState.CPlayer p : ClientGameState.players) {
            if (p.seat == ClientGameState.mySeat) return p.heroId;
        }
        return "";
    }

    private boolean isRed(String suit) { return "HEART".equals(suit) || "DIAMOND".equals(suit); }
    private boolean isBlack(String suit) { return "SPADE".equals(suit) || "CLUB".equals(suit); }

    private void startConvert(String effect) {
        if ((effect.equals("slash") || effect.equals("fire_slash") || effect.equals("thunder_slash")) && !canUseSlash()) {
            showStatus("本回合杀已用尽,不能出杀!");
            return;
        }
        pendingConvertCard = selectedCard;
        pendingConvertEffect = effect;
        showStatus("已选择转换,点击目标玩家");
    }

    private void convertRespond(String effect) {
        PacketDistributor.sendToServer(new ActionPacket(ActionPacket.CONVERT_PLAY, selectedCard, -1, false, effect));
        selectedCard = -1;
        showStatus("已打出转换牌");
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int w = this.width, h = this.height;
        // 古风暗纹背景(浅灰褐底 + 网格暗纹 + 暗角)
        g.fill(0, 0, w, h, 0xFFB5A68D);
        g.fill(0, 0, w, h, 0x14000000);
        int grid = 24;
        for (int gx = 0; gx < w; gx += grid) g.fill(gx, 0, gx + 1, h, 0x0A504028);
        for (int gy = 0; gy < h; gy += grid) g.fill(0, gy, w, gy + 1, 0x0A504028);
        g.fill(0, 0, w, 12, 0x33000000);
        g.fill(0, h - 12, w, h, 0x33000000);
        // 顶部信息栏底色
        g.fill(0, 0, w, 32, 0x90302820);

        // 顶部:牌堆/弃牌堆/阶段
        g.drawCenteredString(this.font, "牌堆: " + ClientGameState.deckCount, w / 2, 8, 0xFFCCCCCC);
        // 弃牌堆:上一张牌的贴图 + 最近记录
        int dx = w / 2 - 64, dy = 30;
        String lastCard = ClientGameState.discardTop.isEmpty() ? "" : ClientGameState.discardTop.get(ClientGameState.discardTop.size() - 1);
        int cw2 = 52, ch2 = 74;
        g.fill(dx - 2, dy - 2, dx + cw2 + 2, dy + ch2 + 2, 0xFFC8AA6E);
        g.fill(dx, dy, dx + cw2, dy + ch2, 0xFF2A241C);
        if (!lastCard.isEmpty()) {
            String texKey = CardWidget.textureKey(lastCard);
            ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("sanguosha", "textures/card/" + texKey + ".png");
            if (Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()) {
                g.blit(tex, dx + 2, dy + 2, cw2 - 4, ch2 - 4, 0, 0, 1, 1, 1, 1);
            } else {
                g.drawCenteredString(this.font, lastCard, dx + cw2 / 2, dy + ch2 / 2 - 4, 0xFFE8D9B0);
            }
        } else {
            g.drawCenteredString(this.font, "弃", dx + cw2 / 2, dy + ch2 / 2 - 4, 0xFFC8AA6E);
        }
        int lx = dx + cw2 + 10;
        g.drawString(this.font, "弃牌堆 " + ClientGameState.discardCount, lx, dy, 0xFFC8AA6E);
        g.drawString(this.font, "上张: " + (lastCard.isEmpty() ? "无" : lastCard), lx, dy + 14, 0xFFFFD700);
        for (int i = 0; i < ClientGameState.discardTop.size() && i < 3; i++) {
            g.drawString(this.font, "▸" + ClientGameState.discardTop.get(i), lx, dy + 32 + i * 12, 0xFFE8D9B0);
        }
        String phaseInfo = phaseCn() + (isMyTurn() ? "  ← 你的回合" : "");
        g.drawCenteredString(this.font, phaseInfo, w / 2, 20, isMyTurn() ? 0xFFFFFF00 : 0xFFCCCCCC);

        // 中央日志
        if (!ClientGameState.lastLog.isEmpty()) {
            g.drawCenteredString(this.font, ClientGameState.lastLog, w / 2, h / 2 - 60, 0xFFFFFFFF);
        }
        // 响应提示
        if (!ClientGameState.prompt.isEmpty()) {
            String tip = ClientGameState.prompt.equals("jink") ? "请打出【闪】!" : "请打出【杀】!";
            g.drawCenteredString(this.font, tip, w / 2, h / 2 - 80, 0xFFFF4444);
        }
        // 状态消息
        if (System.currentTimeMillis() - statusTime < 2000 && !statusMsg.isEmpty()) {
            g.drawCenteredString(this.font, statusMsg, w / 2, h / 2 - 100, 0xFFFFAA00);
        }

        // 玩家面板:按座位固定四角
        for (ClientGameState.CPlayer p : ClientGameState.players) {
            int px, py;
            switch (p.seat) {
                case 0 -> { px = 10; py = 40; }
                case 1 -> { px = w - 170; py = 40; }
                case 2 -> { px = 10; py = h - 160; }
                default -> { px = w - 170; py = h - 160; }
            }
            boolean mine = p.seat == ClientGameState.mySeat;
            boolean current = p.seat == ClientGameState.currentSeat;
            PlayerPanelWidget.render(g, this.font, px, py, 160, 110, p, current, mine);
        }

        // 底部手牌
        List<ClientGameState.CCard> hand = ClientGameState.hand;
        int totalW = hand.size() * (CARD_W + 4) - 4;
        int startX = (w - totalW) / 2;
        int handY = h - CARD_H - 14;
        for (int i = 0; i < hand.size(); i++) {
            ClientGameState.CCard c = hand.get(i);
            int x = startX + i * (CARD_W + 4);
            boolean highlight = i == selectedCard;
            // 响应阶段只允许对应牌点击
            boolean usable = isCardUsable(c);
            CardWidget.render(g, this.font, x, handY, CARD_W, CARD_H, c.name, c.suit, c.rank, c.cat, highlight && usable);
            if (!usable && !ClientGameState.prompt.isEmpty()) {
                g.fill(x, handY, x + CARD_W, handY + CARD_H, 0x70000000);
            }
        }
        // 手牌提示
        g.drawCenteredString(this.font, handHint(), w / 2, handY - 10, 0xFFAAAAAA);

        renderButtons(g, mouseX, mouseY);
        renderAnim(g);
    }

    private boolean isMyDiscardPhase() {
        return "RUNNING".equals(ClientGameState.state)
                && "DISCARD".equals(ClientGameState.phase)
                && ClientGameState.currentSeat == ClientGameState.mySeat;
    }

    private boolean canUseSlash() {
        for (ClientGameState.CPlayer p : ClientGameState.players) {
            if (p.seat == ClientGameState.mySeat) {
                if (p.noSlashLimit) return true;
                return p.slashUsed < 1;
            }
        }
        return true;
    }

    private boolean isCardUsable(ClientGameState.CCard c) {
        if (!ClientGameState.prompt.isEmpty()) {
            // 响应阶段:只允许对应响应牌
            return ClientGameState.prompt.equals("jink") ? c.effect.equals("jink") : c.effect.equals("slash");
        }
        if (isMyDiscardPhase()) return true;
        if (!isMyTurn()) return false;
        return true;
    }

    private String handHint() {
        if (isMyDiscardPhase()) return "弃牌阶段:点击手牌弃置(还需 " + (ClientGameState.hand.size() > 0 ? "按要求弃完" : "") + ")";
        if (!ClientGameState.prompt.isEmpty()) {
            return ClientGameState.prompt.equals("jink") ? "点击【闪】响应,或点右下角放弃" : "点击【杀】响应,或点右下角放弃";
        }
        if (!isMyTurn()) return "等待其他玩家行动...(按 G 可关闭界面)";
        if (selectedCard >= 0 && selectedCard < ClientGameState.hand.size()) {
            ClientGameState.CCard c = ClientGameState.hand.get(selectedCard);
            if (needsTarget(c.effect)) return "已选【" + c.name + "】,点击目标玩家";
            return "已选【" + c.name + "】,点击右下角【使用】";
        }
        return "点击手牌使用(杀/锦囊需再选目标)";
    }

    private String phaseCn() {
        return switch (ClientGameState.phase) {
            case "PREPARE" -> "准备阶段";
            case "DRAW" -> "摸牌阶段";
            case "PLAY" -> "出牌阶段";
            case "DISCARD" -> "弃牌阶段";
            case "END" -> "结束阶段";
            case "FINISHED" -> "游戏结束";
            default -> ClientGameState.phase;
        };
    }

    // ================= 按钮 =================

    private void renderButtons(GuiGraphics g, int mx, int my) {
        int rowY = this.height - CARD_H - 14 - 42; // 手牌正上方一行
        addButton(80, "结束出牌", () -> {
            if (isMyTurn()) { PacketDistributor.sendToServer(ActionPacket.of(ActionPacket.PASS)); selectedCard = -1; }
            else if (isMyDiscardPhase()) showStatus("请先弃完手牌再结束");
        });
        addButton(92, "使用选中牌", () -> {
            if (isMyTurn() && selectedCard >= 0 && selectedCard < ClientGameState.hand.size()) {
                ClientGameState.CCard c = ClientGameState.hand.get(selectedCard);
                boolean isSlash = c.effect.equals("slash") || c.effect.equals("fire_slash") || c.effect.equals("thunder_slash");
                if (isSlash && !canUseSlash()) {
                    showStatus("本回合杀已用尽,不能出杀!");
                    return;
                }
                if (!needsTarget(c.effect)) {
                    PacketDistributor.sendToServer(ActionPacket.of(ActionPacket.PLAY_CARD, selectedCard, -1));
                    selectedCard = -1;
                } else {
                    showStatus("此牌需要选择目标!");
                }
            }
        });
        // 转换技能按钮(出牌阶段)
        String hero = myHeroId();
        ClientGameState.CCard sel = (selectedCard >= 0 && selectedCard < ClientGameState.hand.size()) ? ClientGameState.hand.get(selectedCard) : null;
        if (isMyTurn() && ClientGameState.prompt.isEmpty() && sel != null) {
            if ("guanyu".equals(hero) && isRed(sel.suit)) {
                addButton(92, "武圣·当杀", () -> startConvert("slash"));
                    }
            if ("zhaoyun".equals(hero) && "jink".equals(sel.effect)) {
                addButton(92, "龙胆·当杀", () -> startConvert("slash"));
                    }
            if ("ganning".equals(hero) && isBlack(sel.suit)) {
                addButton(92, "奇袭·当拆", () -> startConvert("dismantlement"));
                    }
            if ("daqiao".equals(hero) && "DIAMOND".equals(sel.suit)) {
                addButton(92, "国色·当乐", () -> startConvert("indulgence"));
                    }
            if (sel != null && "iron_chain".equals(sel.effect)) {
                addButton(70, "重铸", () -> {
                    PacketDistributor.sendToServer(new ActionPacket(ActionPacket.RECAST, selectedCard, -1, false, ""));
                    selectedCard = -1;
                });
            }
            if (myWeapon().contains("朱雀羽扇") && sel != null && "slash".equals(sel.effect)) {
                addButton(92, "朱雀·当火杀", () -> startConvert("fire_slash"));
            }
        }
        // 响应阶段转换(龙胆杀当闪/倾国黑牌当闪)
        if (!ClientGameState.prompt.isEmpty() && "jink".equals(ClientGameState.prompt) && sel != null) {
            if ("zhaoyun".equals(hero) && "slash".equals(sel.effect)) {
                addButton(92, "龙胆·当闪", () -> convertRespond("jink"));
                    }
            if ("zhenji".equals(hero) && isBlack(sel.suit)) {
                addButton(92, "倾国·当闪", () -> convertRespond("jink"));
                    }
        }
        // 响应按钮
        if (!ClientGameState.prompt.isEmpty()) {
            addButton(92, "打出响应牌", () -> {
                if (selectedCard >= 0 && selectedCard < ClientGameState.hand.size()) {
                    ClientGameState.CCard c = ClientGameState.hand.get(selectedCard);
                    boolean ok = ClientGameState.prompt.equals("jink") ? c.effect.equals("jink") : c.effect.equals("slash");
                    if (ok) {
                        PacketDistributor.sendToServer(ActionPacket.respond(true, selectedCard));
                        selectedCard = -1;
                    }
                }
            });
                addButton(80, "放弃响应", () -> {
                PacketDistributor.sendToServer(ActionPacket.respond(false, -1));
                selectedCard = -1;
            });
        } else {
            // 主动技能按钮
            if ("huanggai".equals(hero)) {
                addButton(70, "苦肉", () ->
                        PacketDistributor.sendToServer(new ActionPacket(ActionPacket.SKILL, -1, -1, false, "kuro")));
                    }
            if ("huatuo".equals(hero)) {
                addButton(70, "青囊", () ->
                        PacketDistributor.sendToServer(new ActionPacket(ActionPacket.SKILL, -1, -1, false, "qingnang")));
                    }
            if ("sunquan".equals(hero)) {
                addButton(70, "制衡", () ->
                        PacketDistributor.sendToServer(new ActionPacket(ActionPacket.SKILL, -1, -1, false, "zhiheng")));
                    }
            if ("zhouyu".equals(hero)) {
                addButton(70, "反间", () -> {
                    fanjianMode = true;
                    showStatus("反间:点击目标角色");
                });
                    }
            if ("diaochan".equals(hero)) {
                addButton(70, "离间", () -> {
                    lijianTargetA = -1;
                    showStatus("离间:点击第一名男性角色");
                });
                    }
            if ("liubei".equals(hero)) {
                addButton(70, "仁德", () -> {
                    if (selectedCard >= 0 && selectedCard < ClientGameState.hand.size()) {
                        rendeCardIndex = selectedCard;
                        showStatus("仁德:点击要交给的角色");
                    } else {
                        showStatus("仁德:先点击选择一张手牌");
                    }
                });
                    }
        }
        // 功能按钮(手牌正上方)
        addButton(70, "洗牌", () -> PacketDistributor.sendToServer(ActionPacket.of(ActionPacket.SORT_HAND)));
        flushButtons(g, mx, my, rowY);
    }

    private void addButton(int w, String label, Runnable action) {
        pendingLabels.add(label);
        pendingActions.add(action);
        pendingWidths.add(w);
    }

    /** 将收集的按钮横排渲染到指定行(手牌正上方) */
    private void flushButtons(GuiGraphics g, int mx, int my, int rowY) {
        buttons.clear();
        registeredActions.clear();
        int gap = 8;
        int totalW = 0;
        for (int i = 0; i < pendingLabels.size(); i++) {
            int w = Math.max(pendingWidths.get(i), this.font.width(pendingLabels.get(i)) + 16);
            pendingWidths.set(i, w);
            totalW += w + gap;
        }
        totalW = Math.max(0, totalW - gap);
        int x = (this.width - totalW) / 2;
        for (int i = 0; i < pendingLabels.size(); i++) {
            int w = pendingWidths.get(i);
            boolean hover = mx >= x && mx <= x + w && my >= rowY && my <= rowY + 24;
            g.fill(x, rowY, x + w, rowY + 24, hover ? 0xFF5A5040 : 0xFF3A3228);
            g.fill(x, rowY, x + w, rowY + 1, 0xFFC8AA6E);
            g.drawCenteredString(this.font, pendingLabels.get(i), x + w / 2, rowY + 8, 0xFFFFFFFF);
            buttons.add(new int[]{x, rowY, w, 24, buttons.size()});
            registeredActions.add(pendingActions.get(i));
            x += w + gap;
        }
        pendingLabels.clear();
        pendingActions.clear();
        pendingWidths.clear();
    }

    // ================= 交互 =================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 右键玩家面板:查看属性
        if (button == 1) {
            for (ClientGameState.CPlayer p : ClientGameState.players) {
                int px, py;
                switch (p.seat) {
                    case 0 -> { px = 10; py = 40; }
                    case 1 -> { px = this.width - 170; py = 40; }
                    case 2 -> { px = 10; py = this.height - 160; }
                    default -> { px = this.width - 170; py = this.height - 160; }
                }
                if (mouseX >= px && mouseX <= px + 160 && mouseY >= py && mouseY <= py + 110) {
                    Minecraft.getInstance().setScreen(new PlayerDetailScreen(p));
                    return true;
                }
            }
        }
        // 按钮
        for (int i = 0; i < buttons.size(); i++) {
            int[] b = buttons.get(i);
            if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                handleButton(i);
                return true;
            }
        }
        // 手牌
        List<ClientGameState.CCard> hand = ClientGameState.hand;
        int totalW = hand.size() * (CARD_W + 4) - 4;
        int startX = (this.width - totalW) / 2;
        int handY = this.height - CARD_H - 14;
        for (int i = 0; i < hand.size(); i++) {
            int x = startX + i * (CARD_W + 4);
            if (mouseX >= x && mouseX <= x + CARD_W && mouseY >= handY && mouseY <= handY + CARD_H) {
                if (isCardUsable(hand.get(i))) {
                    if (isMyDiscardPhase()) {
                        PacketDistributor.sendToServer(ActionPacket.of(ActionPacket.DISCARD, i));
                        selectedCard = -1;
                    } else {
                        selectedCard = (selectedCard == i) ? -1 : i;
                    }
                }
                return true;
            }
        }
        // 转换技能目标选择
        if (pendingConvertEffect != null) {
            for (ClientGameState.CPlayer p : ClientGameState.players) {
                int px, py;
                switch (p.seat) {
                    case 0 -> { px = 10; py = 40; }
                    case 1 -> { px = this.width - 170; py = 40; }
                    case 2 -> { px = 10; py = this.height - 160; }
                    default -> { px = this.width - 170; py = this.height - 160; }
                }
                if (mouseX >= px && mouseX <= px + 160 && mouseY >= py && mouseY <= py + 110) {
                    boolean convSlash = pendingConvertEffect.equals("slash") || pendingConvertEffect.equals("fire_slash") || pendingConvertEffect.equals("thunder_slash");
                    if (convSlash && !canUseSlash()) {
                        showStatus("本回合杀已用尽,不能出杀!");
                        pendingConvertEffect = null;
                        pendingConvertCard = -1;
                        return true;
                    }
                    PacketDistributor.sendToServer(new ActionPacket(ActionPacket.CONVERT_PLAY, pendingConvertCard, p.seat, false, pendingConvertEffect));
                    pendingConvertEffect = null;
                    pendingConvertCard = -1;
                    selectedCard = -1;
                    showStatus("已使用转换牌 → " + p.name);
                    return true;
                }
            }
        }
        // 反间目标选择
        if (fanjianMode && isMyTurn() && ClientGameState.prompt.isEmpty()) {
            for (ClientGameState.CPlayer p : ClientGameState.players) {
                int px, py;
                switch (p.seat) {
                    case 0 -> { px = 10; py = 40; }
                    case 1 -> { px = this.width - 170; py = 40; }
                    case 2 -> { px = 10; py = this.height - 160; }
                    default -> { px = this.width - 170; py = this.height - 160; }
                }
                if (mouseX >= px && mouseX <= px + 160 && mouseY >= py && mouseY <= py + 110
                        && p.seat != ClientGameState.mySeat) {
                    PacketDistributor.sendToServer(new ActionPacket(ActionPacket.FANJIAN, -1, p.seat, false, ""));
                    fanjianMode = false;
                    showStatus("已对 " + p.name + " 发动【反间】");
                    return true;
                }
            }
        }
        // 离间目标选择(两名角色)
        if (lijianTargetA != -2 && isMyTurn() && ClientGameState.prompt.isEmpty()) {
            for (ClientGameState.CPlayer p : ClientGameState.players) {
                int px, py;
                switch (p.seat) {
                    case 0 -> { px = 10; py = 40; }
                    case 1 -> { px = this.width - 170; py = 40; }
                    case 2 -> { px = 10; py = this.height - 160; }
                    default -> { px = this.width - 170; py = this.height - 160; }
                }
                if (mouseX >= px && mouseX <= px + 160 && mouseY >= py && mouseY <= py + 110) {
                    if (lijianTargetA == -1) {
                        lijianTargetA = p.seat;
                        showStatus("离间:再点击第二名角色");
                    } else if (p.seat != lijianTargetA) {
                        PacketDistributor.sendToServer(new ActionPacket(ActionPacket.LIJIAN, -1, lijianTargetA, false, String.valueOf(p.seat)));
                        lijianTargetA = -2;
                        showStatus("已发动【离间】");
                    }
                    return true;
                }
            }
        }
        // 仁德目标选择
        if (rendeCardIndex >= 0 && isMyTurn() && ClientGameState.prompt.isEmpty()) {
            for (ClientGameState.CPlayer p : ClientGameState.players) {
                int px, py;
                switch (p.seat) {
                    case 0 -> { px = 10; py = 40; }
                    case 1 -> { px = this.width - 170; py = 40; }
                    case 2 -> { px = 10; py = this.height - 160; }
                    default -> { px = this.width - 170; py = this.height - 160; }
                }
                if (mouseX >= px && mouseX <= px + 160 && mouseY >= py && mouseY <= py + 110
                        && p.seat != ClientGameState.mySeat) {
                    PacketDistributor.sendToServer(new ActionPacket(ActionPacket.RENDE, rendeCardIndex, p.seat, false, ""));
                    rendeCardIndex = -1;
                    selectedCard = -1;
                    showStatus("已【仁德】→ " + p.name);
                    return true;
                }
            }
        }
        // 玩家面板(选目标)
        if (selectedCard >= 0 && selectedCard < hand.size() && isMyTurn() && ClientGameState.prompt.isEmpty()) {
            ClientGameState.CCard c = hand.get(selectedCard);
            if (needsTarget(c.effect)) {
                for (ClientGameState.CPlayer p : ClientGameState.players) {
                    int px, py;
                    switch (p.seat) {
                        case 0 -> { px = 10; py = 40; }
                        case 1 -> { px = this.width - 170; py = 40; }
                        case 2 -> { px = 10; py = this.height - 160; }
                        default -> { px = this.width - 170; py = this.height - 160; }
                    }
                    if (mouseX >= px && mouseX <= px + 160 && mouseY >= py && mouseY <= py + 110) {
                        boolean isSlash = c.effect.equals("slash") || c.effect.equals("fire_slash") || c.effect.equals("thunder_slash");
                        if (isSlash && !canUseSlash()) {
                            showStatus("本回合杀已用尽,不能出杀!");
                            return true;
                        }
                        PacketDistributor.sendToServer(ActionPacket.of(ActionPacket.PLAY_CARD, selectedCard, p.seat));
                        selectedCard = -1;
                        showStatus("已使用【" + c.name + "】→ " + p.name);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleButton(int index) {
        // 按钮动作在 addButton 里通过闭包注册,这里简化:直接按 index 重新判断
        // (MVP:使用字段注册的 action 列表)
        if (!registeredActions.isEmpty() && index < registeredActions.size()) {
            registeredActions.get(index).run();
        }
    }

    private final List<Runnable> registeredActions = new ArrayList<>();
    private final List<String> pendingLabels = new ArrayList<>();
    private final List<Runnable> pendingActions = new ArrayList<>();
    private final List<Integer> pendingWidths = new ArrayList<>();


    private void showStatus(String msg) {
        statusMsg = msg;
        statusTime = System.currentTimeMillis();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    /** 出牌动画:一束金色光线射出指向目标(序号变化才播一次) */
    private void renderAnim(GuiGraphics g) {
        if (ClientGameState.animSeq != animSeq) {
            animSeq = ClientGameState.animSeq;
            animFrom = ClientGameState.animFrom;
            animTo = ClientGameState.animTo;
            animStart = System.currentTimeMillis();
        }
        if (animFrom < 0 || animTo < 0 || animFrom == animTo) return;
        long elapsed = System.currentTimeMillis() - animStart;
        if (elapsed > 900) return;
        float t = Math.min(1f, elapsed / 900f);
        int[] f = panelCenter(animFrom);
        int[] to = panelCenter(animTo);
        int cx = f[0] + (int) ((to[0] - f[0]) * t);
        int cy = f[1] + (int) ((to[1] - f[1]) * t);
        // 光束:起点到前端逐段光点(越靠前端越亮越粗)
        int dx = cx - f[0], dy = cy - f[1];
        int dist = (int) Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, dist / 5);
        for (int i = 0; i <= steps; i++) {
            float k = i / (float) steps;
            int px = f[0] + (int) (dx * k);
            int py = f[1] + (int) (dy * k);
            int alpha = 120 + (int) (100 * k);
            int size = 2 + (int) (2 * k);
            g.fill(px - size, py - size, px + size + 1, py + size + 1, (alpha << 24) | 0xFFD700);
        }
        // 前端光斑
        g.fill(cx - 5, cy - 5, cx + 6, cy + 6, 0xFFFFF0A0);
    }

    private int[] panelCenter(int seat) {
        int px, py;
        switch (seat) {
            case 0 -> { px = 10; py = 40; }
            case 1 -> { px = this.width - 170; py = 40; }
            case 2 -> { px = 10; py = this.height - 160; }
            default -> { px = this.width - 170; py = this.height - 160; }
        }
        return new int[]{px + 80, py + 55};
    }

    @Override
    public void onClose() {
        selectedCard = -1;
        super.onClose();
    }
}