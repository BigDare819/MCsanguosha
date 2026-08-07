package com.sanguosha.client.screen;

import com.sanguosha.network.ActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 弃牌布记录 UI:按弃牌顺序(旧 -> 新)箭头连接显示,点击某条记录从弃牌布拿回(生成实体牌)。
 * 支持中文/拼音搜索。
 * 数据由服务端 RemainSyncPacket(boxType="discard") 下发。
 */
public class DiscardRecordScreen extends Screen {
    private final int posX, posY, posZ;
    private final List<String> names;
    private EditBox searchBox;
    private String lastQuery = "";
    private int scroll = 0;

    public DiscardRecordScreen(int posX, int posY, int posZ, List<String> names) {
        super(Component.literal("\u5f03\u724c\u8bb0\u5f55"));
        this.posX = posX; this.posY = posY; this.posZ = posZ;
        this.names = names;
    }

    private String boxKey() { return posX + "," + posY + "," + posZ + ",discard"; }

    @Override
    public void init() {
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 24, 200, 16, Component.literal("\u641c\u7d22"));
        this.searchBox.setMaxLength(16);
        this.searchBox.setHint(Component.literal("\u641c\u7d22(\u62fc\u97f3/\u4e2d\u6587)"));
        this.searchBox.setFocused(true);
        this.searchBox.setEditable(true);
    }

    /** 按搜索词过滤,返回原始记录索引列表(中文包含 + 拼音包含);空搜索=全部 */
    private List<Integer> visibleIndexes() {
        String q = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            if (q.isEmpty() || n.toLowerCase(Locale.ROOT).contains(q) || PinyinUtil.toPinyin(n).contains(q)) out.add(i);
        }
        return out;
    }

    /** 搜索词变化时重置滚动位置 */
    private void resetScrollIfQueryChanged() {
        String q = searchBox.getValue();
        if (!q.equals(lastQuery)) {
            lastQuery = q;
            scroll = 0;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        resetScrollIfQueryChanged();
        g.fill(0, 0, this.width, this.height, 0xC0100A04);
        List<Integer> vis = visibleIndexes();
        g.drawCenteredString(this.font, "\u5f03\u724c\u8bb0\u5f55 " + names.size() + " \u5f20", this.width / 2, 10, 0xFFFFD700);
        // 搜索框
        searchBox.setX(this.width / 2 - 100);
        searchBox.setY(24);
        searchBox.setWidth(200);
        searchBox.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, "\u70b9\u51fb\u62ff\u56de,\u6eda\u8f6e\u6eda\u52a8,ESC \u5173\u95ed", this.width / 2, this.height - 42, 0xFFAAAAAA);
        // 一键清空按钮(底部提示上方)
        int btnY = this.height - 70;
        boolean overBtn = mouseX >= this.width / 2 - 60 && mouseX <= this.width / 2 + 60
                && mouseY >= btnY && mouseY <= btnY + 18;
        g.fill(this.width / 2 - 60, btnY, this.width / 2 + 60, btnY + 18, overBtn ? 0xFF8A3030 : 0xFF6A2020);
        g.drawCenteredString(this.font, "\u4e00\u952e\u6e05\u7a7a", this.width / 2, btnY + 5, 0xFFFF9090);
        if (vis.isEmpty()) {
            if (searchBox.getValue().isEmpty()) {
                g.drawCenteredString(this.font, "\u8fd8\u6ca1\u6709\u5f03\u724c\u8bb0\u5f55", this.width / 2, this.height / 2 - 10, 0xFF888888);
            } else {
                g.drawCenteredString(this.font, "\u65e0\u5339\u914d\u7ed3\u679c", this.width / 2, this.height / 2 - 10, 0xFF888888);
            }
            return;
        }
        int rowH = 20;
        int maxRows = Math.min(vis.size(), (this.height - 150) / rowH);
        int start = Math.max(0, Math.min(scroll, vis.size() - maxRows));
        for (int i = 0; i < maxRows; i++) {
            int vi = start + i;
            int y = 48 + i * rowH;
            boolean h = mouseX >= this.width / 2 - 140 && mouseX <= this.width / 2 + 140
                    && mouseY >= y && mouseY <= y + rowH - 2;
            g.fill(this.width / 2 - 140, y, this.width / 2 + 140, y + rowH - 2, h ? 0xFF5A4A28 : 0xFF3A3020);
            // 倒序显示:最上面(行 0)= 最后弃的牌(可见列表末尾)
            int viIdx = vis.size() - 1 - vi;
            g.drawString(this.font, (vi + 1) + ". " + names.get(vis.get(viIdx)), this.width / 2 - 135, y + 5, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) return true;
        // 一键清空按钮
        int btnY = this.height - 70;
        if (mouseX >= this.width / 2 - 60 && mouseX <= this.width / 2 + 60
                && mouseY >= btnY && mouseY <= btnY + 18) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.DISCARD_CLEAR, -1, -1, false, boxKey()));
            return true;
        }
        List<Integer> vis = visibleIndexes();
        int rowH = 20;
        int maxRows = Math.min(vis.size(), (this.height - 150) / rowH);
        int start = Math.max(0, Math.min(scroll, vis.size() - maxRows));
        for (int i = 0; i < maxRows; i++) {
            int vi = start + i;
            int y = 48 + i * rowH;
            if (mouseX >= this.width / 2 - 140 && mouseX <= this.width / 2 + 140
                    && mouseY >= y && mouseY <= y + rowH - 2) {
                // 显示是倒序的(行 0 = 最后弃的),拿取要映射回实际记录索引
                int takeIdx = vis.get(vis.size() - 1 - vi);
                // 拿取该条记录(服务端移除记录并生成卡牌进背包,随后刷新 UI)
                PacketDistributor.sendToServer(new ActionPacket(ActionPacket.DISCARD_TAKE, takeIdx, -1, false, boxKey()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 256 && searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox.isFocused()) {
            if (searchBox.charTyped(codePoint, modifiers)) return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        List<Integer> vis = visibleIndexes();
        if (vis.size() > 0) {
            int maxRows = Math.min(vis.size(), (this.height - 150) / 20);
            scroll = Math.max(0, Math.min(scroll + (deltaY > 0 ? -1 : 1), Math.max(0, vis.size() - maxRows)));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
