package com.sp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.sp.cca_stuff.InitializeComponents;
import com.sp.cca_stuff.WorldEvents;
import com.sp.init.BackroomsLevels;
import com.sp.world.events.AbstractEvent;
import com.sp.world.events.infinite_grass.InfiniteGrassAmbience;
import com.sp.world.events.generic.lights.LightLevelBlackout;
import com.sp.world.events.generic.lights.LightLevelFlicker;
import com.sp.world.events.level0.Level0IntercomBasic;
import com.sp.world.events.level0.Level0Music;
import com.sp.world.events.level1.Level1Ambience;
import com.sp.world.events.level1.Level1Blackout;
import com.sp.world.events.level2.Level2Warp;
import com.sp.world.events.level324.Level324SmilerSequence;
import com.sp.world.events.level324.Level324StaffDoorAccessEvent;
import com.sp.world.events.poolrooms.PoolroomsAmbience;
import com.sp.world.events.poolrooms.PoolroomsSunset;
import com.sp.world.generation.chunk_generator.Level324ChunkGenerator;
import com.sp.world.levels.custom.Level324Backroomslevel;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class EventCommand {
    private static final SimpleCommandExceptionType FLICKER_BLACKOUT_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Event only occurs in Level 0 and Level 1"));
    private static final SimpleCommandExceptionType ONLY_LEVEL0_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Event only occurs in Level 0"));
    private static final SimpleCommandExceptionType AMBIENCE_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Event only occurs in Level 1, Level 2, The Poolrooms, or the Infinite Field"));
    private static final SimpleCommandExceptionType WARP_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Event only occurs in Level 2"));
    private static final SimpleCommandExceptionType SUNSET_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Event only occurs in The Poolrooms"));
    private static final SimpleCommandExceptionType LEVEL324_EVENT_EXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("Event only occurs in Level 324"));

    public static void register(CommandDispatcher<ServerCommandSource> serverCommandSourceCommandDispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        serverCommandSourceCommandDispatcher.register(
                CommandManager.literal("backroomsevent")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("flicker")
                                .executes(context -> doFlicker(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("blackout")
                                .executes(context -> doBlackout(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("intercom")
                                .executes(context -> doIntercom(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("music")
                                .executes(context -> doMusic(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("ambience")
                                .executes(context -> doAmbience(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("warp")
                                .executes(context -> doWarp(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("sunset")
                                .executes(context -> doSunset(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("gasstation_smiler")
                                .executes(context -> doGasStationSmiler(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("staff_door_access")
                                .executes(context -> doStaffDoorAccess(
                                                context.getSource()
                                        )
                                )
                        )
                        .then(CommandManager.literal("debuger")
                                .then(CommandManager.literal("smiler_gassstation")
                                        .executes(context -> debugSmilerGasStation(
                                                        context.getSource()
                                                )
                                        )
                                )
                                .then(CommandManager.literal("staff_door_access")
                                        .executes(context -> debugStaffDoorAccess(
                                                        context.getSource()
                                                )
                                        )
                                )
                        )
        );
    }

    private static int doFlicker(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        RegistryKey<World> registryKey = world.getRegistryKey();
        WorldEvents events = InitializeComponents.EVENTS.get(world);

        if (registryKey == BackroomsLevels.LEVEL0_WORLD_KEY) {
            LightLevelFlicker flicker = new LightLevelFlicker();
            setEvent(events, world, flicker);
            return 1;
        } else if (registryKey == BackroomsLevels.LEVEL1_WORLD_KEY) {
            LightLevelFlicker flicker = new LightLevelFlicker();
            setEvent(events, world, flicker);


            return 1;
        } else if (registryKey == BackroomsLevels.LEVEL324_WORLD_KEY) {
            LightLevelFlicker flicker = new LightLevelFlicker();
            setEvent(events, world, flicker);


            return 1;
        }

        throw FLICKER_BLACKOUT_EXCEPTION.create();
    }

    private static int doBlackout(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        RegistryKey<World> registryKey = world.getRegistryKey();
        WorldEvents events = InitializeComponents.EVENTS.get(world);

        if (registryKey == BackroomsLevels.LEVEL0_WORLD_KEY) {
            LightLevelBlackout blackout = new LightLevelBlackout();
            setEvent(events, world, blackout);

            return 1;
        } else if (registryKey == BackroomsLevels.LEVEL1_WORLD_KEY) {
            Level1Blackout blackout = new Level1Blackout();
            setEvent(events, world, blackout);

            return 1;
        }

        throw FLICKER_BLACKOUT_EXCEPTION.create();
    }

    private static int doIntercom(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        RegistryKey<World> registryKey = world.getRegistryKey();
        WorldEvents events = InitializeComponents.EVENTS.get(world);

        if (registryKey == BackroomsLevels.LEVEL0_WORLD_KEY) {
            Level0IntercomBasic intercom = new Level0IntercomBasic();
            setEvent(events, world, intercom);
            return 1;
        }

        throw ONLY_LEVEL0_EXCEPTION.create();
    }

    private static int doMusic(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        RegistryKey<World> registryKey = world.getRegistryKey();
        WorldEvents events = InitializeComponents.EVENTS.get(world);

        if (registryKey == BackroomsLevels.LEVEL0_WORLD_KEY) {
            Level0Music music = new Level0Music();
            setEvent(events, world, music);
            return 1;
        }

        throw ONLY_LEVEL0_EXCEPTION.create();
    }

    private static int doAmbience(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        RegistryKey<World> registryKey = world.getRegistryKey();
        WorldEvents events = InitializeComponents.EVENTS.get(world);

        if (registryKey == BackroomsLevels.POOLROOMS_WORLD_KEY) {
            PoolroomsAmbience ambience = new PoolroomsAmbience();
            setEvent(events, world, ambience);
            return 1;
        } else if (registryKey == BackroomsLevels.LEVEL1_WORLD_KEY) {
            Level1Ambience ambience = new Level1Ambience();
            setEvent(events, world, ambience);

            return 1;
        } else if (registryKey == BackroomsLevels.LEVEL2_WORLD_KEY) {
            Level1Ambience ambience = new Level1Ambience();
            setEvent(events, world, ambience);

            return 1;
        } else if (registryKey == BackroomsLevels.INFINITE_FIELD_WORLD_KEY) {
            InfiniteGrassAmbience ambience = new InfiniteGrassAmbience();
            setEvent(events, world, ambience);

            return 1;
        }

        throw AMBIENCE_EXCEPTION.create();
    }

    private static int doWarp(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        RegistryKey<World> registryKey = world.getRegistryKey();
        WorldEvents events = InitializeComponents.EVENTS.get(world);

        if (registryKey == BackroomsLevels.LEVEL2_WORLD_KEY) {
            Level2Warp warp = new Level2Warp();
            setEvent(events, world, warp);
            return 1;
        }

        throw WARP_EXCEPTION.create();
    }

    private static int doSunset(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        RegistryKey<World> registryKey = world.getRegistryKey();
        WorldEvents events = InitializeComponents.EVENTS.get(world);

        if (registryKey == BackroomsLevels.POOLROOMS_WORLD_KEY) {
            PoolroomsSunset sunset = new PoolroomsSunset();
            setEvent(events, world, sunset);
            return 1;
        }

        throw SUNSET_EXCEPTION.create();
    }

    private static int doGasStationSmiler(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        RegistryKey<World> registryKey = world.getRegistryKey();
        WorldEvents events = InitializeComponents.EVENTS.get(world);

        if (registryKey == BackroomsLevels.LEVEL324_WORLD_KEY) {
            Level324SmilerSequence sequence = new Level324SmilerSequence();
            setEvent(events, world, sequence);
            return 1;
        }

        throw LEVEL324_EVENT_EXCEPTION.create();
    }

    private static int doStaffDoorAccess(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        if (world.getRegistryKey() != BackroomsLevels.LEVEL324_WORLD_KEY) {
            throw LEVEL324_EVENT_EXCEPTION.create();
        }

        if (!(source.getEntity() instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            throw LEVEL324_EVENT_EXCEPTION.create();
        }

        WorldEvents events = InitializeComponents.EVENTS.get(world);
        Level324StaffDoorAccessEvent event = new Level324StaffDoorAccessEvent();
        event.setTargetPlayerUuid(serverPlayer.getUuid());
        event.setDoorPos(serverPlayer.getBlockPos());
        setEvent(events, world, event);

        if (BackroomsLevels.getLevel(world).orElse(null) instanceof Level324Backroomslevel level324) {
            level324.beginStaffDoorCooldown(world.getTime());
            level324.setActiveStaffDoorPlayer(serverPlayer.getUuid());
        }

        return 1;
    }

    private static int debugSmilerGasStation(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        if (world.getRegistryKey() != BackroomsLevels.LEVEL324_WORLD_KEY || !(world instanceof ServerWorld serverWorld)) {
            throw LEVEL324_EVENT_EXCEPTION.create();
        }

        double centerX = Level324ChunkGenerator.FACELING_SPAWN_POS.getX() + 0.5;
        double centerY = Level324ChunkGenerator.FACELING_SPAWN_POS.getY() + 0.15;
        double centerZ = Level324ChunkGenerator.FACELING_SPAWN_POS.getZ() + 0.5;
        double radius = Level324Backroomslevel.getSmilerRadius();

        DustParticleEffect ringParticle = new DustParticleEffect(new Vector3f(1.0f, 0.2f, 0.2f), 1.2f);
        DustParticleEffect centerParticle = new DustParticleEffect(new Vector3f(0.9f, 1.0f, 0.3f), 1.5f);

        for (int i = 0; i < 96; i++) {
            double angle = (Math.PI * 2.0 * i) / 96.0;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;

            serverWorld.spawnParticles(ringParticle, x, centerY, z, 6, 0.15, 0.15, 0.15, 0.01);
            serverWorld.spawnParticles(ringParticle, x, centerY + 1.0, z, 4, 0.12, 0.12, 0.12, 0.01);
        }

        for (int y = 0; y < 5; y++) {
            serverWorld.spawnParticles(centerParticle, centerX, centerY + y * 0.45, centerZ, 8, 0.08, 0.08, 0.08, 0.01);
        }

        source.sendFeedback(() -> Text.literal("Smiler gas station debug ring shown at center "
                + Level324ChunkGenerator.FACELING_SPAWN_POS.toShortString()
                + " with radius " + Level324Backroomslevel.getSmilerRadius()), false);
        return 1;
    }

    private static int debugStaffDoorAccess(ServerCommandSource source) throws CommandSyntaxException {
        World world = source.getWorld();
        if (world.getRegistryKey() != BackroomsLevels.LEVEL324_WORLD_KEY || !(world instanceof ServerWorld serverWorld)) {
            throw LEVEL324_EVENT_EXCEPTION.create();
        }

        WorldEvents events = InitializeComponents.EVENTS.get(world);
        String active = "none";
        if (events.getActiveEvent() instanceof Level324StaffDoorAccessEvent event) {
            active = event.getTargetPlayerUuid().toString();
        }

        String facelingState = "missing";
        int facelingAggression = -1;
        var facelings = serverWorld.getEntitiesByClass(com.sp.entity.custom.FacelingEntity.class, new net.minecraft.util.math.Box(Level324ChunkGenerator.FACELING_SPAWN_POS).expand(16.0), entity -> true);
        if (!facelings.isEmpty()) {
            facelingState = facelings.get(0).getBehaviourMode().name();
            facelingAggression = facelings.get(0).getAggression();
        }

        long remaining = 0;
        if (BackroomsLevels.getLevel(world).orElse(null) instanceof Level324Backroomslevel level324) {
            remaining = level324.getStaffDoorCooldownRemaining(world.getTime());
        }

        final long remainingFinal = remaining;
        final String activeFinal = active;
        final String facelingStateFinal = facelingState;
        final int facelingAggressionFinal = facelingAggression;
        source.sendFeedback(() -> Text.literal("staff_door_access debug: cooldown=" + (remainingFinal / 20) + "s, activeTarget=" + activeFinal + ", facelingMode=" + facelingStateFinal + ", aggression=" + facelingAggressionFinal), false);
        return 1;
    }

    private static void setEvent(WorldEvents events, World world, AbstractEvent activeEvent) {
        if (events.getActiveEvent() != null) {
            events.getActiveEvent().finish(world);
        }
        events.setActiveEvent(activeEvent);
        activeEvent.init(world);
        events.ticks = 0;
    }

}
