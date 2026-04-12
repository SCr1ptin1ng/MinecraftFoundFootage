package com.sp.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

@SuppressWarnings("deprecation")
public class GasStationSignBlock extends HorizontalFacingBlock {
    private static final VoxelShape POST = Block.createCuboidShape(6, 0, 6, 10, 16, 10);
    private static final VoxelShape BOARD_NORTH = VoxelShapes.union(
            Block.createCuboidShape(2, 9, 1, 14, 15, 3),
            Block.createCuboidShape(12, 10, 0, 16, 14, 4)
    );
    private static final VoxelShape BOARD_SOUTH = VoxelShapes.union(
            Block.createCuboidShape(2, 9, 13, 14, 15, 15),
            Block.createCuboidShape(0, 10, 12, 4, 14, 16)
    );
    private static final VoxelShape BOARD_WEST = VoxelShapes.union(
            Block.createCuboidShape(1, 9, 2, 3, 15, 14),
            Block.createCuboidShape(0, 10, 12, 4, 14, 16)
    );
    private static final VoxelShape BOARD_EAST = VoxelShapes.union(
            Block.createCuboidShape(13, 9, 2, 15, 15, 14),
            Block.createCuboidShape(12, 10, 0, 16, 14, 4)
    );

    public GasStationSignBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> VoxelShapes.union(POST, BOARD_NORTH);
            case SOUTH -> VoxelShapes.union(POST, BOARD_SOUTH);
            case WEST -> VoxelShapes.union(POST, BOARD_WEST);
            case EAST -> VoxelShapes.union(POST, BOARD_EAST);
            default -> POST;
        };
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
