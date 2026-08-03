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

/** 牌盒/将盒剩余卡牌查看/搜索/拿取/洗牌 UI(支持中文与拼音搜索) */
public class RemainBoxScreen extends Screen {
    private final int posX, posY, posZ;
    private final String type;
    private final List<String> names;
    private EditBox searchBox;
    private int scroll = 0;

    public RemainBoxScreen(int posX, int posY, int posZ, String type, List<String> names) {
        super(Component.literal("\u5269\u4f59"));
        this.posX = posX; this.posY = posY; this.posZ = posZ;
        this.type = type;
        this.names = names;

    }

    @Override
    public void init() {
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 24, 200, 16, Component.literal("\u641c\u7d22"));
        this.searchBox.setMaxLength(16);
        this.searchBox.setHint(Component.literal("\u641c\u7d22(\u62fc\u97f3/\u4e2d\u6587)"));
        this.searchBox.setFocused(true);
        this.searchBox.setEditable(true);
    }
    private String boxKey() { return posX + "," + posY + "," + posZ + "," + type; }

    private boolean shuffleBtnHit(double mx, double my) {
        return mx >= this.width / 2 - 40 && mx <= this.width / 2 + 40
                && my >= this.height - 60 && my <= this.height - 44;
    }

    /** 按搜索词过滤,返回原始索引列表(中文包含 + 拼音包含);空搜索=全部 */
    private List<Integer> visibleIndexes() {
        String q = searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            if (q.isEmpty() || n.toLowerCase(Locale.ROOT).contains(q) || PinyinUtil.toPinyin(n).contains(q)) out.add(i);
        }
        return out;
    }


    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0100A04);
        boolean hero = "hero".equals(type);
        List<Integer> vis = visibleIndexes();
        String title = hero
                ? ("\u5c06\u76d2\u5269\u4f59 " + names.size() + " \u4f4d\u6b66\u5c06")
                : ("\u724c\u76d2\u5269\u4f59 " + names.size() + " \u5f20");
        g.drawCenteredString(this.font, title, this.width / 2, 10, 0xFFFFD700);
        // 搜索框
        searchBox.setX(this.width / 2 - 100);
        searchBox.setY(24);
        searchBox.setWidth(200);
        searchBox.render(g, mouseX, mouseY, partialTick);
        if (!searchBox.getValue().isEmpty() && vis.isEmpty()) {
            g.drawCenteredString(this.font, "\u65e0\u5339\u914d\u7ed3\u679c", this.width / 2, this.height / 2 - 10, 0xFF888888);
        }
        g.drawCenteredString(this.font, "\u70b9\u51fb\u62ff\u53d6\u4e00\u5f20,\u6eda\u8f6e\u6eda\u52a8,ESC \u5173\u95ed", this.width / 2, this.height - 42, 0xFFAAAAAA);
        // 洗牌按钮
        boolean hover = shuffleBtnHit(mouseX, mouseY);
        g.fill(this.width / 2 - 40, this.height - 60, this.width / 2 + 40, this.height - 44, hover ? 0xFF6A5A2A : 0xFF3A3020);
        g.drawCenteredString(this.font, "\u6d17\u724c", this.width / 2, this.height - 56, 0xFFFFFFFF);
        if (names.isEmpty()) {
            g.drawCenteredString(this.font, "\u724c\u5806\u5df2\u7a7a,\u70b9\u51fb\u6d17\u724c\u91cd\u65b0\u5f00\u5c40", this.width / 2, this.height / 2 + 10, 0xFF888888);
            return;
        }
        int rowH = 20;
        int maxRows = Math.min(vis.size(), (this.height - 130) / rowH);
        int start = Math.max(0, Math.min(scroll, vis.size() - maxRows));
        for (int i = 0; i < maxRows; i++) {
            int idx = start + i;
            int y = 48 + i * rowH;
            boolean h = mouseX >= this.width / 2 - 130 && mouseX <= this.width / 2 + 130
                    && mouseY >= y && mouseY <= y + rowH - 2;
            g.fill(this.width / 2 - 130, y, this.width / 2 + 130, y + rowH - 2, h ? 0xFF5A4A28 : 0xFF3A3020);
            g.drawString(this.font, names.get(vis.get(idx)), this.width / 2 - 125, y + 5, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searchBox.mouseClicked(mouseX, mouseY, button)) return true;
        if (shuffleBtnHit(mouseX, mouseY)) {
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.REMAIN_SHUFFLE, 0, -1, false, boxKey()));
            return true;
        }
        List<Integer> vis = visibleIndexes();
        int rowH = 20;
        int maxRows = Math.min(vis.size(), (this.height - 130) / rowH);
        int start = Math.max(0, Math.min(scroll, vis.size() - maxRows));
        for (int i = 0; i < maxRows; i++) {
            int idx = start + i;
            int y = 48 + i * rowH;
            if (mouseX >= this.width / 2 - 130 && mouseX <= this.width / 2 + 130
                    && mouseY >= y && mouseY <= y + rowH - 2) {
                PacketDistributor.sendToServer(new ActionPacket(ActionPacket.REMAIN_TAKE, vis.get(idx), -1, false, boxKey()));
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
            int maxRows = Math.min(vis.size(), (this.height - 130) / 20);
            scroll = Math.max(0, Math.min(scroll + (deltaY > 0 ? -1 : 1), Math.max(0, vis.size() - maxRows)));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}