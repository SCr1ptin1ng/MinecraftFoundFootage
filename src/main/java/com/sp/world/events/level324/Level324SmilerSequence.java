package com.sp.world.events.level324;

import com.sp.entity.custom.SmilerEntity;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModEntities;
import com.sp.init.ModSounds;
import com.sp.world.events.AbstractEvent;
import com.sp.world.generation.chunk_generator.Level324ChunkGenerator;
import com.sp.world.levels.custom.Level324Backroomslevel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

public class Level324SmilerSequence extends AbstractEvent {
    private int smilerSpawnDelay = 40;

    @Override
    public void init(World world) {
        playDistantSound(world, ModSounds.SMILER_AMBIENCE);
        this.smilerSpawnDelay = spawnSmilerNearEligiblePlayer(world) ? 80 : 20;
    }

    @Override
    public void ticks(int ticks, World world) {
        if (world.getRegistryKey() != BackroomsLevels.LEVEL324_WORLD_KEY) {
            return;
        }

        List<? extends PlayerEntity> playerList = world.getPlayers();
        if (playerList.isEmpty()) {
            return;
        }

        this.smilerSpawnDelay--;
        if (this.smilerSpawnDelay > 0) {
            return;
        }

        this.smilerSpawnDelay = spawnSmilerNearEligiblePlayer(world) ? 80 : 20;
    }

    @Override
    public int duration() {
        return 300;
    }

    private boolean spawnSmilerNearEligiblePlayer(World world) {
        List<? extends PlayerEntity> playerList = world.getPlayers();
        Random random = Random.create();

        for (PlayerEntity player : playerList) {
            if (!Level324Backroomslevel.isInSmilerZone(player)) {
                continue;
            }

            if (!world.getEntitiesByClass(SmilerEntity.class, player.getBoundingBox().expand(100, 32, 100), entity -> true).isEmpty()) {
                continue;
            }

            SmilerEntity smiler = ModEntities.SMILER_ENTITY.create(world);
            if (smiler == null) {
                continue;
            }

            BlockPos.Mutable mutable = new BlockPos.Mutable();
            float randomAngle = random.nextFloat() * 360.0f;
            Vec3d spawnPos = new Vec3d(0, 0, 15).rotateY(randomAngle).add(player.getPos());
            if (!world.getBlockState(mutable.set(spawnPos.x, spawnPos.y, spawnPos.z)).blocksMovement()) {
                smiler.refreshPositionAndAngles(Math.floor(spawnPos.x) + 0.5f, spawnPos.y, Math.floor(spawnPos.z) + 0.5f, 0.0f, 0.0f);
                world.spawnEntity(smiler);
                return true;
            }
        }

        return false;
    }
}
