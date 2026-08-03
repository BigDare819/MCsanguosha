package com.sanguosha.client.screen;

import com.sanguosha.network.ActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 观星界面(线下模式):显示牌堆顶 5 张牌,玩家可调顺序、标记放底,确认后重排牌堆。
 * 顶部列表按显示顺序放回牌堆顶,标记"放底"的牌放回牌堆底。
 */
public class GuanXingScreen extends Screen {
    private final int posX, posY, posZ;
    private final List<String> names;      // 原顺序:"牌名|花色|点数"
    private final List<Integer> topOrder = new ArrayList<>(); // 顶部顺序(原始下标)
    private final List<Integer> bottom = new ArrayList<>();   // 放底(原始下标)

    public GuanXingScreen(int posX, int posY, int posZ, List<String> names) {
        super(Component.literal("\u89c2\u661f"));
        this.posX = posX; this.posY = posY; this.posZ = posZ;
        this.names = names;
        for (int i = 0; i < names.size(); i++) topOrder.add(i);
    }

    /** 某行的 "上移/下移/移出/移回" 按钮命中检测 */
    private int rowY(int row) { return 44 + row * 22; }

    private boolean btnHit(double mx, double my, int x0, int y0, int w, int h) {
        return mx >= x0 && mx <= x0 + w && my >= y0 && my <= y0 + h;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, this.width, this.height, 0xC0100A04);
        g.drawCenteredString(this.font, "\u89c2\u661f - \u724c\u5806\u9876 " + names.size() + " \u5f20(\u8bf8\u845b\u4eae)", this.width / 2, 12, 0xFFFFD700);

        int cx = this.width / 2;
        // 顶部列表
        g.drawCenteredString(this.font, "\u2191 \u653e\u56de\u724c\u5806\u9876(\u987a\u5e8f\u5373\u4e0a\u4e0b)", cx - 160, 32, 0xFF88CC88);
        for (int i = 0; i < topOrder.size(); i++) {
            int idx = topOrder.get(i);
            int y = rowY(i);
            String disp = displayName(names.get(idx));
            g.fill(cx - 210, y, cx + 70, y + 18, 0xFF3A3020);
            g.drawString(this.font, disp, cx - 205, y + 4, 0xFFFFFFFF);
            // ▲ ▼ 调序按钮
            if (i > 0) {
                int ax = cx + 76;
                boolean h = btnHit(mouseX, mouseY, ax, y, 14, 8);
                g.fill(ax, y, ax + 14, y + 8, h ? 0xFF6A5A2A : 0xFF3A3020);
                g.drawCenteredString(this.font, "\u25b2", ax + 7, y + 0, 0xFFFFFFFF);
            }
            if (i < topOrder.size() - 1) {
                int ax = cx + 76;
                boolean h = btnHit(mouseX, mouseY, ax, y + 10, 14, 8);
                g.fill(ax, y + 10, ax + 14, y + 18, h ? 0xFF6A5A2A : 0xFF3A3020);
                g.drawCenteredString(this.font, "\u25bc", ax + 7, y + 10, 0xFFFFFFFF);
            }
            // 移到底部按钮
            int bx = cx + 98;
            boolean bh = btnHit(mouseX, mouseY, bx, y, 30, 18);
            g.fill(bx, y, bx + 30, y + 18, bh ? 0xFF8A3A3A : 0xFF5A2020);
            g.drawCenteredString(this.font, "\u2193\u5e95", bx + 15, y + 4, 0xFFFFFFFF);
        }
        // 底部列表
        int by = 44 + topOrder.size() * 22 + 14;
        g.drawCenteredString(this.font, "\u2193 \u653e\u56de\u724c\u5806\u5e95", cx - 160, by - 10, 0xFFCC8888);
        for (int i = 0; i < bottom.size(); i++) {
            int idx = bottom.get(i);
            int y = by + i * 22;
            String disp = displayName(names.get(idx));
            g.fill(cx - 210, y, cx + 70, y + 18, 0xFF2A2A30);
            g.drawString(this.font, disp, cx - 205, y + 4, 0xFFCCCCCC);
            // 移回顶部按钮
            int bx = cx + 98;
            boolean bh = btnHit(mouseX, mouseY, bx, y, 30, 18);
            g.fill(bx, y, bx + 30, y + 18, bh ? 0xFF5A6A3A : 0xFF303A20);
            g.drawCenteredString(this.font, "\u2191\u9876", bx + 15, y + 4, 0xFFFFFFFF);
        }
        // 确认按钮
        int cy = this.height - 44;
        boolean ch = btnHit(mouseX, mouseY, cx - 60, cy, 120, 20);
        g.fill(cx - 60, cy, cx + 60, cy + 20, ch ? 0xFF6A8A3A : 0xFF3A5A20);
        g.drawCenteredString(this.font, "\u786e\u8ba4\u653e\u7f6e", cx, cy + 5, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "ESC \u53d6\u6d88", cx, this.height - 20, 0xFF888888);
    }

    private static String displayName(String info) {
        String[] p = info.split("\\|");
        String name = p.length > 0 ? p[0] : info;
        String suit = p.length > 1 ? p[1] : "";
        String rank = p.length > 2 ? p[2] : "";
        return name + " " + suit + " " + rank;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx = this.width / 2;
        // 顶部列表操作
        for (int i = 0; i < topOrder.size(); i++) {
            int y = rowY(i);
            // ▲ 上移
            if (i > 0 && btnHit(mouseX, mouseY, cx + 76, y, 14, 8)) {
                int tmp = topOrder.get(i); topOrder.set(i, topOrder.get(i - 1)); topOrder.set(i - 1, tmp);
                return true;
            }
            // ▼ 下移
            if (i < topOrder.size() - 1 && btnHit(mouseX, mouseY, cx + 76, y + 10, 14, 8)) {
                int tmp = topOrder.get(i); topOrder.set(i, topOrder.get(i + 1)); topOrder.set(i + 1, tmp);
                return true;
            }
            // 移到底部
            if (btnHit(mouseX, mouseY, cx + 98, y, 30, 18)) {
                int idx = topOrder.remove(i);
                bottom.add(idx);
                return true;
            }
        }
        // 底部列表操作(移回顶部)
        int by = 44 + topOrder.size() * 22 + 14;
        for (int i = 0; i < bottom.size(); i++) {
            int y = by + i * 22;
            if (btnHit(mouseX, mouseY, cx + 98, y, 30, 18)) {
                int idx = bottom.remove(i);
                topOrder.add(idx);
                return true;
            }
        }
        // 确认
        int cy = this.height - 44;
        if (btnHit(mouseX, mouseY, cx - 60, cy, 120, 20)) {
            // 编码 "x,y,z,deck|count|idx,idx,..."(idx 为原顶牌下标,未列出的放底)
            StringBuilder sb = new StringBuilder(posX + "," + posY + "," + posZ + ",deck");
            sb.append("|").append(names.size());
            sb.append("|");
            for (int i = 0; i < topOrder.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(topOrder.get(i));
            }
            PacketDistributor.sendToServer(new ActionPacket(ActionPacket.GUANXING_CONFIRM, 0, -1, false, sb.toString()));
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
