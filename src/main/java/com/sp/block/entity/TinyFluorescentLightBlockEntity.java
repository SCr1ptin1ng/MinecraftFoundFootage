package com.sp.block.entity;

import com.sp.block.custom.FluorescentLightBlock;
import com.sp.block.custom.TinyFluorescentLightBlock;
import com.sp.init.BackroomsLevels;
import com.sp.init.ModBlockEntities;
import com.sp.init.ModBlocks;
import com.sp.world.levels.BackroomsLevelWithLights;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.deferred.light.PointLight;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import static com.sp.clientWrapper.ClientWrapper.doClientSideTinyFluorescentsTick;


public class TinyFluorescentLightBlockEntity extends BlockEntity {
    BlockState currentState;
    public boolean playingSound;
    public PointLight pointLight;
    public boolean prevOn;
    public final int randInt;
    public int ticks = 0;
    public final Random random = Random.create();

    public TinyFluorescentLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TINY_FLUORESCENT_LIGHT_BLOCK_ENTITY, pos, state);
        java.util.Random random = new java.util.Random();

        this.currentState = state;
        this.playingSound = false;
        this.randInt = random.nextInt(1,8);
    }

    @Override
    public void markRemoved() {
        if (this.getWorld() != null && this.getWorld().isClient){
            if(pointLight != null) {
                VeilRenderSystem.renderer().getDeferredRenderer().getLightRenderer().removeLight(pointLight);
                pointLight = null;
            }
        }

        super.markRemoved();
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        if (world.getBlockState(pos).getBlock() != ModBlocks.TINY_FLUORESCENT_LIGHT) {
            return;
        }

        Vec3d position = pos.toCenterPos();
        java.util.Random random1 = new java.util.Random();


        if (world.isClient) {
            doClientSideTinyFluorescentsTick(world, pos, state, random1, position, this);
        }

        if (ticks > 100) {
            ticks = 1;
        }

        prevOn = world.getBlockState(pos).get(FluorescentLightBlock.ON);
    }

    public BlockState getCurrentState() {
        return this.currentState;
    }

    public boolean isPlayingSound() {
        return playingSound;
    }

    public void setPlayingSound(boolean playingSound) {
        this.playingSound = playingSound;
    }
}
