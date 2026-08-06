package com.sanguosha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 弃牌布:4x4 分片地毯(mat_x 0-3, mat_y 0-3,按相对位置取分片;铺 4x4 拼成一张大布)。
 * 牌放到上面会被 DiscardMatScanner 清除并累计记录,浮空文本显示弃牌历史。
 * 与牌布(CardMatBlock 4x3)的区别:弃牌布是 4x4,且功能是"清除+记录"而非"统计当前牌面"。
 */
public class DiscardMatBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final IntegerProperty MAT_X = IntegerProperty.create("mat_x", 0, 3);
    public static final IntegerProperty MAT_Y = IntegerProperty.create("mat_y", 0, 3);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public DiscardMatBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(MAT_X, 0).setValue(MAT_Y, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MAT_X, MAT_Y);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** 蹲下右键:打开弃牌记录 UI;普通右键:无操作(弃牌布不摸牌) */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            // 反推布起点(右键的是 4x4 布中的某一片,记录 key = 起点)
            Direction dir = state.getValue(FACING);
            Direction right = dir.getClockWise();
            int mx = state.getValue(MAT_X);
            int my = state.getValue(MAT_Y);
            int dz0 = 3 - my;
            BlockPos start = pos.offset(-right.getStepX() * mx - dir.getStepX() * dz0,
                    0,
                    -right.getStepZ() * mx - dir.getStepZ() * dz0);
            String enc = start.getX() + "," + start.getY() + "," + start.getZ() + ",discard";
            com.sanguosha.network.ServerPayloadHandler.discardView((net.minecraft.server.level.ServerPlayer) player, enc);
            return InteractionResult.sidedSuccess(true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** 破坏一片时,整张 4x4 弃牌布一起消失(其他片不掉落,本片正常掉落 1 个物品);
     *  只删同一张布(用 facing + mat 反推布起点,不误伤相邻的其他弃牌布) */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!level.isClientSide) {
            Direction dir = state.getValue(FACING);
            Direction right = dir.getClockWise();
            int fX = dir.getStepX(), fZ = dir.getStepZ();
            int rX = right.getStepX(), rZ = right.getStepZ();
            int mx = state.getValue(MAT_X);
            int my = state.getValue(MAT_Y);
            int dz0 = 3 - my; // 被挖片在布中的行(mat_y = 3 - dz)
            BlockPos start = pos.offset(-rX * mx - fX * dz0, 0, -rZ * mx - fZ * dz0);
            // 整张弃牌布被移除,立即清理上方记录文字 + 弃牌记录
            com.sanguosha.item.DiscardMatScanner.removeMat((net.minecraft.server.level.ServerLevel) level, start);
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    BlockPos p = start.offset(rX * dx + fX * dz, 0, rZ * dx + fZ * dz);
                    if (p.equals(pos)) continue;
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() == this && s.getValue(FACING) == dir) {
                        level.removeBlock(p, false);
                    }
                }
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    /** 方块被任何方式移除(玩家挖/爆炸/命令/替换)时,立即清理上方记录文字——文字与弃牌布"绑定" */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this)) {
            Direction dir = state.getValue(FACING);
            Direction right = dir.getClockWise();
            int mx = state.getValue(MAT_X);
            int my = state.getValue(MAT_Y);
            int dz0 = 3 - my;
            BlockPos start = pos.offset(-right.getStepX() * mx - dir.getStepX() * dz0,
                    0,
                    -right.getStepZ() * mx - dir.getStepZ() * dz0);
            com.sanguosha.item.DiscardMatScanner.removeMat((net.minecraft.server.level.ServerLevel) level, start);
        }
    }
}
