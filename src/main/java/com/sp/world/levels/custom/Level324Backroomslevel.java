package com.sp.world.levels.custom;

import com.sp.SPBRevamped;
import com.sp.SPBRevampedClient;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModBlocks;
import com.sp.world.generation.chunk_generator.Level324ChunkGenerator;
import com.sp.world.levels.BackroomsLevel;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class Level324Backroomslevel extends BackroomsLevel {
    public Level324Backroomslevel() {
        super("level324", Level324ChunkGenerator.CODEC, new Vec3d(52,65,21), BackroomsLevels.LEVEL324_WORLD_KEY);

        this.registerTransition((world, playerComponent, from) -> {
            List<LevelTransition> playerList = new ArrayList<>();

            int exitRadius = SPBRevamped.getExitSpawnRadius(world);

            if (from instanceof Level324Backroomslevel &&
                    hasGrassBeneath(playerComponent) &&
                    playerComponent.player.getPos().squaredDistanceTo(new Vec3d(0, 65, 0)) >= (double) ((exitRadius / 3) * (exitRadius / 3)) ) {
                playerList.add(getInfiniteFieldsTransition(playerComponent));
            }

            return playerList;
        }, "level324 -> inf_grass");
    }

    private static boolean hasGrassBeneath(PlayerComponent playerComponent) {
        return playerComponent.player.getWorld().getBlockState(playerComponent.player.supportingBlockPos.orElseGet(() ->
                playerComponent.player.getBlockPos().subtract(new Vec3i(0,1,0)))).isOf(ModBlocks.RED_DIRT);
    }

    private LevelTransition getInfiniteFieldsTransition(PlayerComponent playerComponent) {
        return new LevelTransition(
                40,
                (teleport, tick) -> {
                    World world = teleport.playerComponent().player.getWorld();

                    if (world.isClient()) {
                        if (tick == 14) {
                            SPBRevampedClient.getCutsceneManager().blackScreen.showBlackScreen(20, true, false);
                        }
                        return;
                    }

                    if (tick == 20) {
                        teleport.playerComponent().setShouldNoClip(true);
                        teleport.playerComponent().sync();
                    }

                    if (tick == 14) {
                        SPBRevamped.sendBlackScreenPacket((ServerPlayerEntity) teleport.playerComponent().player, 20, true, false);
                    }

                    //After the screen turns black THEN teleport
                    if (tick == 1) {
                        teleport.playerComponent().setShouldNoClip(false);
                        teleport.playerComponent().sync();
                    }
                }, // Tick
                new CrossDimensionTeleport(
                        playerComponent,
                        BackroomsLevels.INFINITE_FIELD_BACKROOMS_LEVEL.getSpawnPos(),
                        this,
                        BackroomsLevels.INFINITE_FIELD_BACKROOMS_LEVEL
                ),
                (teleport, tick) -> {
                    teleport.playerComponent().setShouldNoClip(false);
                    teleport.playerComponent().sync();
                }); // Cancel
    }

    @Override
    public boolean rendersClouds() {
        return false;
    }

    @Override
    public boolean rendersSky() {
        return false;
    }

    @Override
    public int nextEventDelay() {
        return 0;
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {

    }

    @Override
    public void readFromNbt(NbtCompound nbt) {

    }

    @Override
    public void transitionOut(CrossDimensionTeleport crossDimensionTeleport) {

    }

    @Override
    public void transitionIn(CrossDimensionTeleport crossDimensionTeleport) {

    }
}
