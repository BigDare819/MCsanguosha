package com.sanguosha.client.screen;

import com.sanguosha.network.ActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 弃牌布记录 UI:按弃牌顺序(旧 -> 新)箭头连接显示,点击某条记录从弃牌布拿回(生成实体牌)。
 * 数据由服务端 RemainSyncPacket(boxType="discard") 下发。
 */
public class DiscardRecordScreen extends Screen {
    private final int posX, posY, posZ;
    private final List<String> names;
    private int scroll = 0;

    public DiscardRecordScreen(int posX, int posY, int posZ, List<String> names) {
        super(Component.literal("\u5f03\u724c\u8bb0\u5f55"));
        this.posX = posX; this.posY = posY; this.posZ = posZ;
        this.names = names;
    }

    private String boxKey() { return posX + "," + posY + "," + posZ + ",discard"; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0100A04);
        g.drawCenteredString(this.font, "\u5f03\u724c\u8bb0\u5f55 " + names.size() + " \u5f20", this.width / 2, 10, 0xFFFFD700);
        // 顺序说明:最上面的是最后弃的牌
        g.drawCenteredString(this.font, "\u25b2 \u6700\u4e0a\u9762\u662f\u6700\u540e\u5f03\u7684\u724c", this.width / 2, 24, 0xFF88CCFF);
        g.drawCenteredString(this.font, "\u70b9\u51fb\u62ff\u56de,\u6eda\u8f6e\u6eda\u52a8,ESC \u5173\u95ed", this.width / 2, this.height - 42, 0xFFAAAAAA);
        if (names.isEmpty()) {
            g.drawCenteredString(this.font, "\u8fd8\u6ca1\u6709\u5f03\u724c\u8bb0\u5f55", this.width / 2, this.height / 2 - 10, 0xFF888888);
            return;
        }
        int rowH = 20;
        int maxRows = Math.min(names.size(), (this.height - 150) / rowH);
        int start = Math.max(0, Math.min(scroll, names.size() - maxRows));
        for (int i = 0; i < maxRows; i++) {
            int idx = start + i;
            int y = 44 + i * rowH;
            boolean h = mouseX >= this.width / 2 - 140 && mouseX <= this.width / 2 + 140
                    && mouseY >= y && mouseY <= y + rowH - 2;
            g.fill(this.width / 2 - 140, y, this.width / 2 + 140, y + rowH - 2, h ? 0xFF5A4A28 : 0xFF3A3020);
            // 倒序显示:最上面(行 0)= 最后弃的牌(names 末尾)
            int nameIdx = names.size() - 1 - idx;
            g.drawString(this.font, (idx + 1) + ". " + names.get(nameIdx), this.width / 2 - 135, y + 5, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int rowH = 20;
        int maxRows = Math.min(names.size(), (this.height - 150) / rowH);
        int start = Math.max(0, Math.min(scroll, names.size() - maxRows));
        for (int i = 0; i < maxRows; i++) {
            int idx = start + i;
            int y = 44 + i * rowH;
            if (mouseX >= this.width / 2 - 140 && mouseX <= this.width / 2 + 140
                    && mouseY >= y && mouseY <= y + rowH - 2) {
                // 显示是倒序的(行 0 = 最后弃的),拿取要映射回实际记录索引
                int takeIdx = names.size() - 1 - idx;
                // 拿取该条记录(服务端移除记录并生成卡牌进背包,随后刷新 UI)
                PacketDistributor.sendToServer(new ActionPacket(ActionPacket.DISCARD_TAKE, takeIdx, -1, false, boxKey()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (names.size() > 0) {
            int maxRows = Math.min(names.size(), (this.height - 150) / 20);
            scroll = Math.max(0, Math.min(scroll + (deltaY > 0 ? -1 : 1), Math.max(0, names.size() - maxRows)));
        }
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
