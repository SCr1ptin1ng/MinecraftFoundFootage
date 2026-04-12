package com.sp.world.levels.custom;

import com.sp.SPBRevamped;
import com.sp.SPBRevampedClient;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModBlocks;
import com.sp.world.events.generic.lights.LightLevelFlicker;
import com.sp.world.events.level324.Level324SmilerSequence;
import com.sp.world.events.EmptyEvent;
import com.sp.world.events.AbstractEvent;
import com.sp.world.generation.chunk_generator.Level324ChunkGenerator;
import com.sp.world.levels.BackroomsLevel;
import com.sp.world.levels.BackroomsLevelWithLights;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class Level324Backroomslevel extends BackroomsLevel implements BackroomsLevelWithLights {
    private static final float LEVEL324_EXIT_RADIUS = 100.0f;
    private static final float LEVEL324_SMILER_RADIUS = 40.0f;
    public static final long STAFF_DOOR_COOLDOWN_TICKS = 20L * 60L * 5L;
    private static final java.util.UUID NIL_UUID = new java.util.UUID(0L, 0L);
    private Level0BackroomsLevel.LightState lightState = BackroomsLevelWithLights.LightState.ON;
    private boolean batterySpawned;
    private boolean batteryTaken;
    private long staffDoorCooldownEndTime;
    private java.util.UUID activeStaffDoorPlayer = NIL_UUID;

    public Level324Backroomslevel() {
        super("level324", Level324ChunkGenerator.CODEC, new Vec3d(52,65,21), BackroomsLevels.LEVEL324_WORLD_KEY);

        this.registerEvent("flicker", LightLevelFlicker::new);
        this.registerEvent("smiler_sequence", Level324SmilerSequence::new);

        this.registerTransition((world, playerComponent, from) -> {
            List<LevelTransition> playerList = new ArrayList<>();
            Vec2f playerPos = new Vec2f((float) playerComponent.player.getX(), (float) playerComponent.player.getZ());
            Vec2f facelingPos = new Vec2f(Level324ChunkGenerator.FACELING_SPAWN_POS.getX(), Level324ChunkGenerator.FACELING_SPAWN_POS.getZ());

            if (from instanceof Level324Backroomslevel &&
                    hasGrassBeneath(playerComponent) &&
                    playerPos.distanceSquared(facelingPos) >= LEVEL324_EXIT_RADIUS * LEVEL324_EXIT_RADIUS) {
                playerList.add(getInfiniteFieldsTransition(playerComponent));
            }

            return playerList;
        }, this.getLevelId() + " -> " + BackroomsLevels.INFINITE_FIELD_BACKROOMS_LEVEL.getLevelId());

        this.registerTransition((world, playerComponent, from) -> {
            List<LevelTransition> playerList = new ArrayList<>();

            Vec2f[] puddleLocations = new Vec2f[]{
                    new Vec2f(300.0f, 0.0f),
                    new Vec2f(-300.0f, 0.0f),
                    new Vec2f(0.0f, 300.0f),
                    new Vec2f(0.0f, -300.0f),
                    new Vec2f(150.0f, 150.0f),
                    new Vec2f(150.0f, -150.0f),
                    new Vec2f(-150.0f, 150.0f),
                    new Vec2f(-150.0f, -150.0f),
                    new Vec2f(100.0f, 200.0f),
                    new Vec2f(100.0f, -200.0f),
                    new Vec2f(-100.0f, 200.0f),
                    new Vec2f(-100.0f, -200.0f),
                    new Vec2f(200.0f, 100.0f),
                    new Vec2f(-200.0f, 100.0f),
                    new Vec2f(200.0f, -100.0f),
                    new Vec2f(-200.0f, -100.0f)
            };

            if (from instanceof Level324Backroomslevel && playerComponent.player.getY() < 20) {
                for (Vec2f vec2f : puddleLocations) {
                    if (4 > vec2f.distanceSquared(new Vec2f((float) playerComponent.player.getX(), (float) playerComponent.player.getZ()))) {
                        playerList.add(getPoolRoomsTransition(playerComponent));
                    }
                }
            }

            return playerList;
        }, this.getLevelId() + " -> " + BackroomsLevels.POOLROOMS_BACKROOMS_LEVEL.getLevelId());
    }

    private static boolean hasGrassBeneath(PlayerComponent playerComponent) {
        return playerComponent.player.getWorld().getBlockState(playerComponent.player.supportingBlockPos.orElseGet(() ->
                playerComponent.player.getBlockPos().subtract(new Vec3i(0,1,0)))).isOf(ModBlocks.RED_DIRT);
    }

    public static boolean isInSmilerZone(PlayerEntity player) {
        Vec2f playerPos = new Vec2f((float) player.getX(), (float) player.getZ());
        Vec2f facelingPos = new Vec2f(Level324ChunkGenerator.FACELING_SPAWN_POS.getX(), Level324ChunkGenerator.FACELING_SPAWN_POS.getZ());

        return playerPos.distanceSquared(facelingPos) >= LEVEL324_SMILER_RADIUS * LEVEL324_SMILER_RADIUS
                && player.getY() >= 60
                && player.getY() <= 65;
    }

    public static float getSmilerRadius() {
        return LEVEL324_SMILER_RADIUS;
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

    private LevelTransition getPoolRoomsTransition(PlayerComponent playerComponent) {
        return new LevelTransition(
                10,
                (teleport, tick) -> {
                    World world = teleport.playerComponent().player.getWorld();
                    if (tick == 9) {
                        teleport.playerComponent().setShouldNoClip(true);
                        teleport.playerComponent().sync();
                    }

                    if (world.isClient()) {
                        if (tick == 4) {
                            SPBRevampedClient.getCutsceneManager().blackScreen.showBlackScreen(20, true, false);
                        }
                        return;
                    }

                    if (tick == 4) {
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
                        BackroomsLevels.POOLROOMS_BACKROOMS_LEVEL.getSpawnPos(),
                        this,
                        BackroomsLevels.POOLROOMS_BACKROOMS_LEVEL
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
    public BoolTextPair allowsTorch() {
        return new BoolTextPair(false, Text.translatable("spb-revamped.flashlight.wet1").append(Text.translatable("spb-revamped.flashlight.wet2").formatted(Formatting.RED)));
    }

    @Override
    public int nextEventDelay() {
        return random.nextInt(600, 900);
    }

    @Override
    public AbstractEvent getRandomEvent(World world) {
        boolean anyPlayerEligible = world.getPlayers().stream().anyMatch(Level324Backroomslevel::isInSmilerZone);

        if (!anyPlayerEligible) {
            return new EmptyEvent();
        }

        return super.getRandomEvent(world);
    }

    @Override
    public void writeToNbt(NbtCompound nbt) {
        nbt.putString("lightState", lightState.name());
        nbt.putBoolean("batterySpawned", batterySpawned);
        nbt.putBoolean("batteryTaken", batteryTaken);
        nbt.putLong("staffDoorCooldownEndTime", staffDoorCooldownEndTime);
        nbt.putUuid("activeStaffDoorPlayer", activeStaffDoorPlayer);
    }

    @Override
    public void readFromNbt(NbtCompound nbt) {
        this.lightState = BackroomsLevelWithLights.LightState.valueOf(nbt.getString("lightState"));
        this.batterySpawned = nbt.getBoolean("batterySpawned");
        this.batteryTaken = nbt.getBoolean("batteryTaken");
        this.staffDoorCooldownEndTime = nbt.getLong("staffDoorCooldownEndTime");
        if (nbt.containsUuid("activeStaffDoorPlayer")) {
            this.activeStaffDoorPlayer = nbt.getUuid("activeStaffDoorPlayer");
        } else {
            this.activeStaffDoorPlayer = NIL_UUID;
        }
    }

    @Override
    public void transitionOut(CrossDimensionTeleport crossDimensionTeleport) {

    }

    @Override
    public void transitionIn(CrossDimensionTeleport crossDimensionTeleport) {
        if (!crossDimensionTeleport.playerComponent().player.getWorld().isClient()) {
            ServerWorld serverWorld = (ServerWorld) crossDimensionTeleport.playerComponent().player.getWorld();
            Level324ChunkGenerator.ensureFacelingPresent(serverWorld);
        }
    }

    public boolean isBatterySpawned() {
        return batterySpawned;
    }

    public void setBatterySpawned(boolean batterySpawned) {
        this.batterySpawned = batterySpawned;
        this.justChanged();
    }

    public boolean isBatteryTaken() {
        return batteryTaken;
    }

    public void setBatteryTaken(boolean batteryTaken) {
        this.batteryTaken = batteryTaken;
        this.justChanged();
    }

    public void setLightState(Level0BackroomsLevel.LightState lightState) {
        this.justChanged();
        this.lightState = lightState;
    }

    public Level0BackroomsLevel.LightState getLightState() {
        return this.lightState;
    }

    public long getStaffDoorCooldownRemaining(long worldTime) {
        return Math.max(0, this.staffDoorCooldownEndTime - worldTime);
    }

    public void beginStaffDoorCooldown(long worldTime) {
        this.staffDoorCooldownEndTime = worldTime + STAFF_DOOR_COOLDOWN_TICKS;
        this.justChanged();
    }

    public java.util.UUID getActiveStaffDoorPlayer() {
        return activeStaffDoorPlayer;
    }

    public void setActiveStaffDoorPlayer(java.util.UUID activeStaffDoorPlayer) {
        this.activeStaffDoorPlayer = activeStaffDoorPlayer;
        this.justChanged();
    }

    public void clearActiveStaffDoorPlayer() {
        this.activeStaffDoorPlayer = NIL_UUID;
        this.justChanged();
    }
}
