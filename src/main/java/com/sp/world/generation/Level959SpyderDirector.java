package com.sp.world.generation;

import com.sp.SPBRevamped;
import com.sp.entity.custom.SpyderControllerEntity;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModEntities;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.List;

public final class Level959SpyderDirector {
    private static final int MIN_RESPAWN_TICKS = 20 * 18;
    private static final int MAX_RESPAWN_TICKS = 20 * 40;
    private static long nextSpawnTick = 0L;

    private Level959SpyderDirector() {
    }

    public static void tick(ServerWorld world) {
        if (world.getRegistryKey() != BackroomsLevels.A_PLACE_YOU_DONT_WANT_TO_KNOW_WORLD_KEY) {
            return;
        }

        List<ServerPlayerEntity> players = world.getPlayers(player ->
                !player.isSpectator() && !player.isCreative() && player.isAlive());
        if (players.isEmpty()) {
            return;
        }

        if (!world.getEntitiesByClass(SpyderControllerEntity.class, new Box(-512, -64, -512, 512, 256, 512), entity -> true).isEmpty()) {
            return;
        }

        long worldTime = world.getTime();
        if (worldTime < nextSpawnTick) {
            return;
        }

        Random random = world.getRandom();
        ServerPlayerEntity player = players.get(random.nextInt(players.size()));

        Vec3d travelCenter = getTravelCenter(player, random);
        Vec3d travelDirection = getHorizontalFacing(player, random).rotateY((float) (random.nextBoolean() ? Math.PI / 2.0 : -Math.PI / 2.0));
        double travelHalfSpan = 3.5 + random.nextDouble() * 3.5;

        Vec3d startPos = travelCenter.subtract(travelDirection.multiply(travelHalfSpan));
        Vec3d endPos = travelCenter.add(travelDirection.multiply(travelHalfSpan));

        SpyderControllerEntity spyder = ModEntities.SPYDER_CONTROLLER_ENTITY.create(world);
        if (spyder == null) {
            nextSpawnTick = worldTime + MIN_RESPAWN_TICKS;
            return;
        }

        spyder.configure(startPos, endPos, player.getUuid());
        world.spawnEntity(spyder);

        nextSpawnTick = worldTime + random.nextBetween(MIN_RESPAWN_TICKS, MAX_RESPAWN_TICKS);
        SPBRevamped.LOGGER.info("Level959 spyder controller spawned at {} heading to {}", startPos, endPos);
    }

    private static Vec3d getTravelCenter(ServerPlayerEntity player, Random random) {
        Vec3d forward = getHorizontalFacing(player, random);
        Vec3d side = new Vec3d(-forward.z, 0.0, forward.x);

        double forwardDistance = 10.0 + random.nextDouble() * 10.0;
        double sideOffset = (random.nextDouble() * 6.0) - 3.0;
        double heightOffset = 4.2 + random.nextDouble() * 2.6;

        return player.getPos()
                .add(forward.multiply(forwardDistance))
                .add(side.multiply(sideOffset))
                .add(0.0, heightOffset, 0.0);
    }

    private static Vec3d getHorizontalFacing(ServerPlayerEntity player, Random random) {
        Vec3d forward = player.getRotationVec(1.0F);
        Vec3d horizontal = new Vec3d(forward.x, 0.0, forward.z);
        if (horizontal.lengthSquared() < 0.0001) {
            return random.nextBoolean() ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 0.0, 1.0);
        }

        return horizontal.normalize();
    }
}
