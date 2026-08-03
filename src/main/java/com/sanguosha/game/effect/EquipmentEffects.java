package com.sanguosha.game.effect;

import com.sanguosha.card.CardDefinition;
import com.sanguosha.game.GamePlayer;
import com.sanguosha.game.SanguoshaGame;

/** 装备牌效果 */
public final class EquipmentEffects {
    private EquipmentEffects() {}

    public static void init() {
        // 诸葛连弩:出杀无次数限制
        EffectRegistry.register("crossbow", new EquipHandler() {
            @Override public void onEquip(SanguoshaGame game, GamePlayer p) { p.noSlashLimit = true; }
            @Override public void onUnequip(SanguoshaGame game, GamePlayer p) { p.noSlashLimit = false; }
        });
        // 其余武器/防具/坐骑:先仅挂载,特殊效果(青釭破防/雌雄/青龙/贯石/丈八/方天/麒麟/八卦/仁王)逐步实现
        String[] others = {"qinggang","double_sword","green_dragon","spear","axe","halberd","kylin",
                           "bagua","renwang","vine","horse_plus","horse_minus"};
        for (String key : others) {
            EffectRegistry.register(key, new EquipHandler());
        }
        // 古锭刀:杀对无手牌目标伤害+1(在 damage 结算)
        EffectRegistry.register("gudingdao", new EquipHandler() {});
        // 朱雀羽扇:杀当火杀(转换,在 canConvert 结算)
        EffectRegistry.register("zhuqueyushan", new EquipHandler() {});
        // 白银狮子:受到伤害-1、失去回血(在 damage/onLoseEquip 结算)
        EffectRegistry.register("baiyinshizi", new EquipHandler() {});
    }

    /** 装备挂载处理器(由 SanguoshaGame 调用 onEquip/onUnequip) */
    public static class EquipHandler implements CardEffect {
        @Override public void use(SanguoshaGame game, GamePlayer user, GamePlayer target, CardDefinition card) {}
        public void onEquip(SanguoshaGame game, GamePlayer p) {}
        public void onUnequip(SanguoshaGame game, GamePlayer p) {}
    }
}