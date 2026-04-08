package com.sp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.sp.world.generation.chunk_generator.InfGrassChunkGenerator;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class GasStationCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(
                CommandManager.literal("gasstation")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("debug")
                                .executes(context -> debug(context.getSource()))
                        )
        );
    }

    private static int debug(ServerCommandSource source) {
        long seed = source.getWorld().toServerWorld().getSeed();
        BlockPos gasStationPos = InfGrassChunkGenerator.getLevel324GasStationPos(seed);
        BlockPos playerPos = BlockPos.ofFloored(source.getPosition());
        double distance = Math.sqrt(playerPos.getSquaredDistance(gasStationPos));
        String direction = InfGrassChunkGenerator.getDebugDirection(seed, playerPos);

        source.sendFeedback(() -> Text.literal(
                "Gas station at "
                        + gasStationPos.getX() + ", "
                        + gasStationPos.getY() + ", "
                        + gasStationPos.getZ()
                        + " [" + direction + "] "
                        + (int) distance + "m"
        ), false);

        return 1;
    }
}
