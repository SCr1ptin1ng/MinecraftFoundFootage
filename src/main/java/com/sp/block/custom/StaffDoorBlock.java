package com.sp.block.custom;

import com.sp.SPBRevamped;
import com.sp.cca_stuff.InitializeComponents;
import com.sp.cca_stuff.PlayerComponent;
import com.sp.cca_stuff.WorldEvents;
import com.sp.entity.custom.FacelingEntity;
import com.sp.init.BackroomsLevels;
import com.sp.world.events.level324.Level324StaffDoorAccessEvent;
import com.sp.world.levels.BackroomsLevel;
import com.sp.world.levels.custom.Level324Backroomslevel;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;

public class StaffDoorBlock extends DoorBlock {
    public StaffDoorBlock(Settings settings, BlockSetType blockSetType) {
        super(settings, blockSetType);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, net.minecraft.util.math.BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getRegistryKey() != BackroomsLevels.LEVEL324_WORLD_KEY || !(player instanceof ServerPlayerEntity serverPlayer) || world.isClient()) {
            return super.onUse(state, world, pos, player, hand, hit);
        }

        if (!(BackroomsLevels.getLevel(world).orElse(null) instanceof Level324Backroomslevel level324)) {
            return super.onUse(state, world, pos, player, hand, hit);
        }

        long remainingTicks = level324.getStaffDoorCooldownRemaining(world.getTime());
        if (remainingTicks > 0) {
            serverPlayer.sendMessage(Text.literal("The staff door stays sealed. Cooldown: " + (remainingTicks / 20) + "s"), true);
            return ActionResult.SUCCESS;
        }

        WorldEvents events = InitializeComponents.EVENTS.get(world);
        if (events.getActiveEvent() instanceof Level324StaffDoorAccessEvent) {
            serverPlayer.sendMessage(Text.literal("Someone is already being processed by the staff door."), true);
            return ActionResult.SUCCESS;
        }

        Level324StaffDoorAccessEvent event = new Level324StaffDoorAccessEvent();
        event.setTargetPlayerUuid(serverPlayer.getUuid());
        event.setDoorPos(pos);
        events.setActiveEvent(event);
        event.init(world);
        events.ticks = 0;

        level324.beginStaffDoorCooldown(world.getTime());
        level324.setActiveStaffDoorPlayer(serverPlayer.getUuid());

        for (FacelingEntity faceling : ((ServerWorld) world).getEntitiesByClass(FacelingEntity.class, serverPlayer.getBoundingBox().expand(128.0), entity -> true)) {
            faceling.beginFollowingClosest();
        }

        for (ServerPlayerEntity otherPlayer : ((ServerWorld) world).getPlayers()) {
            PlayerComponent playerComponent = InitializeComponents.PLAYER.get(otherPlayer);
            if (playerComponent.currentTransition == null) {
                playerComponent.currentTransition = createAPlaceTransition(playerComponent);
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    private BackroomsLevel.LevelTransition createAPlaceTransition(PlayerComponent playerComponent) {
        return new BackroomsLevel.LevelTransition(
                30,
                (teleport, tick) -> {
                    if (!teleport.playerComponent().player.getWorld().isClient() && tick == 30) {
                        if (!teleport.playerComponent().isTeleporting()) {
                            SPBRevamped.sendLevelTransitionLightsOutPacket((ServerPlayerEntity) teleport.playerComponent().player, 80);
                        }
                    }
                },
                new BackroomsLevel.CrossDimensionTeleport(
                        playerComponent,
                        BackroomsLevels.A_PLACE_YOU_DONT_WANT_TO_KNOW_BACKROOMS_LEVEL.getSpawnPos(),
                        BackroomsLevels.LEVEL324_BACKROOMS_LEVEL,
                        BackroomsLevels.A_PLACE_YOU_DONT_WANT_TO_KNOW_BACKROOMS_LEVEL
                ),
                (teleport, tick) -> {
                }
        );
    }
}
