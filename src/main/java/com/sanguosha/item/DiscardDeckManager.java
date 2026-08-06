package com.sanguosha.item;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按方块位置管理的弃牌记录:每张弃牌布各有一份有序列表(按弃牌顺序,新弃的在后)。
 * 每条记录 = 完整牌信息("牌名|花色|点数",与 CardData.CARD_INFO 同格式),
 * 支持查看/拿取(拿走后从记录移除)/最近 N 张。
 */
public final class DiscardDeckManager {
    private static final Map<BlockPos, List<String>> RECORDS = new HashMap<>();

    private DiscardDeckManager() {}

    /** 获取某张弃牌布的记录列表(不存在则创建) */
    public static List<String> list(BlockPos pos) {
        return RECORDS.computeIfAbsent(pos, p -> new ArrayList<>());
    }

    /** 追加一条弃牌记录(完整牌信息) */
    public static void add(BlockPos pos, String cardInfo) {
        List<String> l = list(pos);
        l.add(cardInfo);
        // 上限保护:最多保留 64 条,防无限增长
        while (l.size() > 64) l.remove(0);
    }

    /** 拿取某条记录(按索引),返回完整牌信息并从记录移除;越界返回 null */
    public static String take(BlockPos pos, int index) {
        List<String> l = RECORDS.get(pos);
        if (l == null || index < 0 || index >= l.size()) return null;
        String info = l.remove(index);
        if (l.isEmpty()) RECORDS.remove(pos);
        return info;
    }

    /** 最近弃的 n 张(从新到旧排列,即列表尾部往前) */
    public static List<String> recent(BlockPos pos, int n) {
        List<String> l = RECORDS.get(pos);
        if (l == null || l.isEmpty()) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (int i = l.size() - 1; i >= 0 && out.size() < n; i--) {
            out.add(l.get(i));
        }
        return out;
    }

    /** 记录条数 */
    public static int size(BlockPos pos) {
        List<String> l = RECORDS.get(pos);
        return l == null ? 0 : l.size();
    }

    /** 方块被破坏时清理记录 */
    public static void remove(BlockPos pos) {
        RECORDS.remove(pos);
    }
}
