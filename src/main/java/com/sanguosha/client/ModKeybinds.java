package com.sanguosha.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** 实体卡牌模式快捷键 */
public final class ModKeybinds {
    public static final KeyMapping HP_UP = new KeyMapping("key.sanguosha.hp_up", GLFW.GLFW_KEY_J, "key.categories.sanguosha");
    public static final KeyMapping HP_DOWN = new KeyMapping("key.sanguosha.hp_down", GLFW.GLFW_KEY_K, "key.categories.sanguosha");
    public static final KeyMapping TOGGLE_UI = new KeyMapping("key.sanguosha.toggle_ui", GLFW.GLFW_KEY_H, "key.categories.sanguosha");
    public static final KeyMapping PLACE_CARD = new KeyMapping("key.sanguosha.place_card", GLFW.GLFW_KEY_R, "key.categories.sanguosha");
    public static final KeyMapping OPEN_TABLE = new KeyMapping("key.sanguosha.open_table", GLFW.GLFW_KEY_G, "key.categories.sanguosha");
    public static final KeyMapping DROP_CARD = new KeyMapping("key.sanguosha.drop_card", GLFW.GLFW_KEY_Q, "key.categories.sanguosha");
    public static final KeyMapping CLEAR_CARDS = new KeyMapping("key.sanguosha.clear_cards", GLFW.GLFW_KEY_L, "key.categories.sanguosha");

    private ModKeybinds() {}
}