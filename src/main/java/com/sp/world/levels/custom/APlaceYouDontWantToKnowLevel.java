package com.sp.world.levels.custom;

import com.sp.SPBRevamped;
import com.sp.cca_stuff.InitializeComponents;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.init.BackroomsLevels;
import com.sp.world.events.AbstractEvent;
import com.sp.world.events.generic.lights.LightLevelBlackout;
import com.sp.world.events.generic.lights.LightLevelFlicker;
import com.sp.world.events.level0.Level0IntercomBasic;
import com.sp.world.events.level0.Level0Music;
import com.sp.world.generation.chunk_generator.Level959ChunkGenerator;
import com.sp.world.levels.BackroomsLevel;
import com.sp.world.levels.BackroomsLevelWithLights;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class APlaceYouDontWantToKnowLevel extends BackroomsLevel implements BackroomsLevelWithLights {
    private int blackoutCount = 0;
    private int intercomCount = 0;
    private Level0BackroomsLevel.LightState lightState = BackroomsLevelWithLights.LightState.ON;
    private boolean layoutGenerated = false;

    public APlaceYouDontWantToKnowLevel() {
        super("a_place_you_dont_want_to_know", Level959ChunkGenerator.CODEC, new RoomCount(8), new Vec3d(0, 32, 0), BackroomsLevels.A_PLACE_YOU_DONT_WANT_TO_KNOW_WORLD_KEY);
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
    public void register() {
        super.register();
        this.registerEvent("flicker", LightLevelFlicker::new);
        this.registerEvent("intercom", Level0IntercomBasic::new);
        this.registerEvent("music", Level0Music::new);

        this.registerTransition((world, playerComponent, from) -> {
            List<LevelTransition> playerList = new ArrayList<>();

            if (from instanceof APlaceYouDontWantToKnowLevel && playerComponent.player.getPos().getY() <= 11 && playerComponent.player.isOnGround()) {
                for (PlayerEntity player : playerComponent.player.getWorld().getPlayers()) {
                    PlayerComponent otherPlayerComponent = InitializeComponents.PLAYER.get(player);
                    playerList.add(getLevel1Transition(otherPlayerComponent));
                }
            }

            return playerList;
        }, this.getLevelId() + "->" + BackroomsLevels.LEVEL1_BACKROOMS_LEVEL.getLevelId());
    }

    private LevelTransition getLevel1Transition(PlayerComponent playerComponent) {
        return new LevelTransition(
                30,
                (teleport, tick) -> {
                    if (!teleport.playerComponent().player.getWorld().isClient() && tick == 30) {
                        if (!teleport.playerComponent().isTeleporting()) {
                            SPBRevamped.sendLevelTransitionLightsOutPacket((ServerPlayerEntity) teleport.playerComponent().player, 80);
                        }
                    }
                },
                new CrossDimensionTeleport(playerComponent,
                        calculateLevel1TeleportCoords(
                                playerComponent.player,
                                playerComponent.player.getChunkPos()),
                        this,
                        BackroomsLevels.LEVEL1_BACKROOMS_LEVEL),
                (teleport, tick) -> {
                });
    }

    private Vec3d calculateLevel1TeleportCoords(PlayerEntity player, ChunkPos chunkPos) {
        if (chunkPos.x == player.getChunkPos().x && chunkPos.z == player.getChunkPos().z) {
            int chunkX = chunkPos.getStartX();
            int chunkZ = chunkPos.getStartZ();

            double playerX = player.getPos().x;
            double playerZ = player.getPos().z;

            return new Vec3d(playerX - chunkX, player.getPos().y + 15, playerZ - chunkZ);
        } else {
            return this.getSpawnPos();
        }
    }

    @Override
    public int nextEventDelay() {
        return random.nextInt(1000, 1500);
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        nbt.putInt("blackoutCount", blackoutCount);
        nbt.putInt("intercomCount", intercomCount);
        nbt.putString("lightState", lightState.name());
        nbt.putBoolean("layoutGenerated", layoutGenerated);
    }

    @Override
    public void readFromNbt(NbtCompound nbt) {
        this.blackoutCount = nbt.getInt("blackoutCount");
        this.intercomCount = nbt.getInt("intercomCount");
        this.lightState = Level0BackroomsLevel.LightState.valueOf(nbt.getString("lightState"));
        this.layoutGenerated = nbt.getBoolean("layoutGenerated");
    }

    @Override
    public void transitionOut(CrossDimensionTeleport crossDimensionTeleport) {
    }

    @Override
    public void transitionIn(CrossDimensionTeleport crossDimensionTeleport) {
    }

    public void setLightState(Level0BackroomsLevel.LightState lightState) {
        this.justChanged();
        this.lightState = lightState;
    }

    public Level0BackroomsLevel.LightState getLightState() {
        return this.lightState;
    }

    public boolean isLayoutGenerated() {
        return this.layoutGenerated;
    }

    public void setLayoutGenerated(boolean layoutGenerated) {
        this.justChanged();
        this.layoutGenerated = layoutGenerated;
    }
}
