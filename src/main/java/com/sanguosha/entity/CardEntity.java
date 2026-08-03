package com.sanguosha.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 桌面卡牌实体:固定平放,可翻面,右键翻面/Shift右键捡起 */
public class CardEntity extends Entity {
    private static final EntityDataAccessor<String> DATA_INFO =
            SynchedEntityData.defineId(CardEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_ROT =
            SynchedEntityData.defineId(CardEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_FACE_UP =
            SynchedEntityData.defineId(CardEntity.class, EntityDataSerializers.BOOLEAN);

    public CardEntity(EntityType<CardEntity> type, Level level) {
        super(type, level);
    }

    public CardEntity(Level level, double x, double y, double z, String info, float rot) {
        this(ModEntities.CARD.get(), level);
        setPos(x, y, z);
        setCardInfo(info);
        setCardRotation(rot);
        setFaceUp(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_INFO, "");
        builder.define(DATA_ROT, 0.0F);
        builder.define(DATA_FACE_UP, true);
    }

    public String getCardInfo() { return getEntityData().get(DATA_INFO); }
    public void setCardInfo(String s) { getEntityData().set(DATA_INFO, s); }
    public float getCardRotation() { return getEntityData().get(DATA_ROT); }
    public void setCardRotation(float r) { getEntityData().set(DATA_ROT, r); }
    public boolean isFaceUp() { return getEntityData().get(DATA_FACE_UP); }
    public void setFaceUp(boolean b) { getEntityData().set(DATA_FACE_UP, b); }

    private long nameExpire = 0;

    /** 在牌盒上方临时显示文字(5 秒后自动消失) */
    public void showNameTemporarily(net.minecraft.network.chat.Component name) {
        setCustomName(name);
        setCustomNameVisible(true);
        nameExpire = level().getGameTime() + 100;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            if (getCardInfo().isEmpty()) {
                discard();
            } else if (isCustomNameVisible() && level().getGameTime() > nameExpire) {
                setCustomNameVisible(false);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setCardInfo(tag.getString("Info"));
        setCardRotation(tag.getFloat("Rot"));
        setFaceUp(tag.getBoolean("FaceUp"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Info", getCardInfo());
        tag.putFloat("Rot", getCardRotation());
        tag.putBoolean("FaceUp", isFaceUp());
    }

    /** 转为可拾取物品(卡牌/武将牌) */
    public ItemStack toItemStack() {
        String info = getCardInfo();
        if ("牌盒:".equals(info)) return new ItemStack(com.sanguosha.item.ModItems.DECK_BOX.get());
        if ("将盒:".equals(info)) return new ItemStack(com.sanguosha.item.ModItems.HERO_DECK_BOX.get());
        ItemStack s = new ItemStack(com.sanguosha.item.ModItems.CARD.get());
        if (!info.isEmpty()) {
            s.set(com.sanguosha.item.CardData.CARD_INFO, info);
            String[] parts = info.split("\\|");
            if (parts[0].startsWith("武将:")) {
                String id = parts[0].substring(3);
                s.set(DataComponents.ITEM_NAME, Component.literal("【" + parts[1] + "】"));
                s.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(com.sanguosha.item.CardModelIds.heroIdOf(id)));
            } else {
                s.set(DataComponents.ITEM_NAME, Component.literal("【" + parts[0] + "】"));
                s.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(com.sanguosha.item.CardModelIds.idOf(parts[0])));
            }
        }
        return s;
    }

    /** 左键打掉:掉落为物品实体(像手牌一样捡起) */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide) {
            ItemStack s = toItemStack();
            if (!s.isEmpty()) {
                ItemEntity drop = new ItemEntity(level(), getX(), getY() + 0.2, getZ(), s);
                level().addFreshEntity(drop);
            }
            discard();
        }
        return true;
    }

    /** 碰撞箱:牌盒/将盒为方块大小,普通牌为薄片 */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        String i = getCardInfo();
        if ("牌盒:".equals(i) || "将盒:".equals(i)) {
            return net.minecraft.world.entity.EntityDimensions.fixed(0.8F, 1.0F);
        }
        return net.minecraft.world.entity.EntityDimensions.fixed(0.55F, 0.08F);
    }

    @Override
    public boolean isPickable() { return true; }
    @Override
    public boolean isPushable() { return false; }
}