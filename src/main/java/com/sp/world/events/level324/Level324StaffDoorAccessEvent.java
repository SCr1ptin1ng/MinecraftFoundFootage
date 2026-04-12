package com.sp.world.events.level324;

import com.sp.entity.custom.FacelingEntity;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModSounds;
import com.sp.world.events.AbstractEvent;
import com.sp.world.generation.chunk_generator.Level324ChunkGenerator;
import com.sp.world.levels.custom.Level324Backroomslevel;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;
import java.util.UUID;

public class Level324StaffDoorAccessEvent extends AbstractEvent {
    public static final int DURATION = 20 * 60 * 5;
    private static final UUID NIL_UUID = new UUID(0L, 0L);

    private UUID targetPlayerUuid = NIL_UUID;
    private BlockPos doorPos = BlockPos.ORIGIN;

    public void setTargetPlayerUuid(UUID targetPlayerUuid) {
        this.targetPlayerUuid = targetPlayerUuid;
    }

    public UUID getTargetPlayerUuid() {
        return targetPlayerUuid;
    }

    public void setDoorPos(BlockPos doorPos) {
        this.doorPos = doorPos;
    }

    public BlockPos getDoorPos() {
        return doorPos;
    }

    @Override
    public void init(World world) {
        playSound(world, ModSounds.EMERGENCY_LIGHT_ALARM);
    }

    @Override
    public void finish(World world) {
        if (world instanceof ServerWorld serverWorld) {
            resetFacelings(serverWorld);

            if (BackroomsLevels.getLevel(world).orElse(null) instanceof Level324Backroomslevel level324) {
                level324.clearActiveStaffDoorPlayer();
            }
        }

        super.finish(world);
    }

    @Override
    public int duration() {
        return DURATION;
    }

    @Override
    public void ticks(int ticks, World world) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (ticks % 100 == 0) {
            playSound(world, ModSounds.EMERGENCY_LIGHT_ALARM);
        }

        List<FacelingEntity> facelings = serverWorld.getEntitiesByClass(FacelingEntity.class, new Box(Level324ChunkGenerator.FACELING_SPAWN_POS).expand(32.0), entity -> true);
        for (FacelingEntity faceling : facelings) {
            if (faceling.getBehaviourMode() == FacelingEntity.BehaviourMode.IDLE) {
                faceling.beginFollowingClosest();
            }

            if (ticks % 20 == 0) {
                faceling.increaseAggression(1);
            }

            if (faceling.getAggression() >= 100) {
                faceling.beginHunt();
            }
        }
    }

    private void resetFacelings(ServerWorld world) {
        for (FacelingEntity faceling : world.getEntitiesByClass(FacelingEntity.class, new Box(Level324ChunkGenerator.FACELING_SPAWN_POS).expand(48.0), entity -> true)) {
            faceling.resetToIdle();
        }
    }
}
