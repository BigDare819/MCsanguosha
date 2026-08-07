package com.sanguosha.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** 实体卡牌模式快捷键(全部可在设置-按键绑定中更改) */
public final class ModKeybinds {
    public static final KeyMapping OPEN_HP_UI = new KeyMapping("key.sanguosha.open_hp_ui", GLFW.GLFW_KEY_X, "key.categories.sanguosha");
    public static final KeyMapping TOGGLE_UI = new KeyMapping("key.sanguosha.toggle_ui", GLFW.GLFW_KEY_H, "key.categories.sanguosha");
    public static final KeyMapping PLACE_CARD = new KeyMapping("key.sanguosha.place_card", GLFW.GLFW_KEY_R, "key.categories.sanguosha");
    public static final KeyMapping DROP_CARD = new KeyMapping("key.sanguosha.drop_card", GLFW.GLFW_KEY_Q, "key.categories.sanguosha");
    public static final KeyMapping CLEAR_CARDS = new KeyMapping("key.sanguosha.clear_cards", GLFW.GLFW_KEY_C, "key.categories.sanguosha");

    private ModKeybinds() {}
}
