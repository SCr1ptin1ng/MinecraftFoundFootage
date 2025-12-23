package com.sp.entity.ai.node_maker;

import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

public class WalkerPathNodeMaker extends LandPathNodeMaker {
    private static final int HOVER_HEIGHT = 3;
    private static final int SCAN_RADIUS = 4;

    @Override
    protected PathNode getStart(BlockPos pos) {
        BlockPos hoverPos = findNearestSupportBlock(pos);
        if (hoverPos != null) {
            PathNode pathNode = this.getNode(hoverPos.getX(), hoverPos.getY() - HOVER_HEIGHT, hoverPos.getZ());
            pathNode.type = PathNodeType.WALKABLE;
            pathNode.penalty = 0.0F;
            return pathNode;
        }
        return super.getStart(pos);
    }

    @Override
    public int getSuccessors(PathNode[] successors, PathNode node) {
        int i = 0;

        // Check all 6 directions (including up and down)
        for (Direction direction : Direction.values()) {
            PathNode successor = getHoveringPathNode(
                    node.x + direction.getOffsetX(),
                    node.y + direction.getOffsetY(),
                    node.z + direction.getOffsetZ()
            );

            if (successor != null && !successor.visited && successor.penalty >= 0.0F) {
                successors[i++] = successor;
            }
        }

        return i;
    }

    @Nullable
    private PathNode getHoveringPathNode(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);

        // Check if current position has air
        if (!this.cachedWorld.getBlockState(pos).isAir()) {
            return null;
        }

        // Check if there's a support block within radius
        if (!hasSupportBlockNearby(pos)) {
            return null;
        }

        PathNode pathNode = this.getNode(x, y, z);
        pathNode.type = PathNodeType.WALKABLE;
        pathNode.penalty = 0.0F;

        return pathNode;
    }

    private boolean hasSupportBlockNearby(BlockPos center) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    // Check if position one block below support has air
                    BlockPos airCheck = mutable.up();
                    if (!this.cachedWorld.getBlockState(airCheck).isAir()) {
                        continue;
                    }

                    // Check if support block is solid
                    if (isSolidSupport(this.cachedWorld, mutable)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Nullable
    private BlockPos findNearestSupportBlock(BlockPos start) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int radius = 0; radius <= SCAN_RADIUS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        mutable.set(start.getX() + x, start.getY() + y, start.getZ() + z);

                        if (isSolidSupport(this.cachedWorld, mutable) &&
                                this.cachedWorld.getBlockState(mutable.up()).isAir()) {
                            return mutable.toImmutable();
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isSolidSupport(BlockView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.isAir() && state.isSolidBlock(world, pos);
    }

    @Override
    protected double getFeetY(BlockPos pos) {
        return pos.getY() - HOVER_HEIGHT;
    }
}
