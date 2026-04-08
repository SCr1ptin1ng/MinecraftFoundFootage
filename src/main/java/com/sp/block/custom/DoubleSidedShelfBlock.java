package com.sp.block.custom;

import com.sp.SPBRevamped;
import com.sp.cca_stuff.InitializeComponents;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModSounds;
import com.sp.world.levels.BackroomsLevel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class DoubleSidedShelfBlock extends Block {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final IntProperty STUFF = IntProperty.of("stuff", 0, 4);

    private static final double[] SHAPE = new double[] {0.125, 0, 0.125, 1, 1, 1};

    public static Random random = Random.create();

    public DoubleSidedShelfBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> VoxelShapes.cuboid(SHAPE[0], SHAPE[1], SHAPE[2], SHAPE[3], SHAPE[4], SHAPE[5]);
            case SOUTH -> VoxelShapes.cuboid(SHAPE[0], SHAPE[1], 1 - SHAPE[5], SHAPE[3], SHAPE[4], 1 - SHAPE[2]);
            case WEST -> VoxelShapes.cuboid(SHAPE[2], SHAPE[1], SHAPE[0], SHAPE[5], SHAPE[4], SHAPE[3]);
            case EAST -> VoxelShapes.cuboid(1 - SHAPE[5], SHAPE[1], SHAPE[0], 1 - SHAPE[2], SHAPE[4], SHAPE[3]);
            default -> VoxelShapes.fullCube();
        };
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing()).with(STUFF, random.nextBetween(0, 4));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, STUFF);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (world.getRegistryKey() == BackroomsLevels.INFINITE_FIELD_WORLD_KEY && player instanceof ServerPlayerEntity serverPlayer) {
            PlayerComponent playerComponent = InitializeComponents.PLAYER.get(serverPlayer);
            if (playerComponent.currentTransition == null) {
                playerComponent.currentTransition = getLevel324Transition(playerComponent);
                return ActionResult.CONSUME;
            }
        }

        return ActionResult.PASS;
    }

    private BackroomsLevel.LevelTransition getLevel324Transition(PlayerComponent playerComponent) {
        return new BackroomsLevel.LevelTransition(
                20,
                (teleport, tick) -> {
                    World world = teleport.playerComponent().player.getWorld();

                    if (tick == 18 && !world.isClient()) {
                        SPBRevamped.sendPersonalPlaySoundPacket((ServerPlayerEntity) teleport.playerComponent().player, ModSounds.MIDNIGHT_TRANSITION, 1.0f, 1.0f);
                    }

                    if (tick == 14) {
                        if (world.isClient()) {
                            return;
                        }
                        SPBRevamped.sendBlackScreenPacket((ServerPlayerEntity) teleport.playerComponent().player, 20, true, false);
                    }

                    if (tick == 9 && !world.isClient()) {
                        teleport.playerComponent().setShouldNoClip(true);
                        teleport.playerComponent().sync();
                    }

                    if (tick == 1 && !world.isClient()) {
                        teleport.playerComponent().setShouldNoClip(false);
                        teleport.playerComponent().sync();
                    }
                },
                new BackroomsLevel.CrossDimensionTeleport(
                        playerComponent,
                        BackroomsLevels.LEVEL324_BACKROOMS_LEVEL.getSpawnPos(),
                        BackroomsLevels.INFINITE_FIELD_BACKROOMS_LEVEL,
                        BackroomsLevels.LEVEL324_BACKROOMS_LEVEL
                ),
                (teleport, tick) -> {
                    teleport.playerComponent().setShouldNoClip(false);
                    teleport.playerComponent().sync();
                }
        );
    }
}
