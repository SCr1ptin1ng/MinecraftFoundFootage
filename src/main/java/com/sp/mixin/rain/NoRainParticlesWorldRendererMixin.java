package com.sp.mixin.rain;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sp.SPBRevampedClient;
import com.sp.init.BackroomsLevels;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldRenderer.class)
public class NoRainParticlesWorldRendererMixin {
    @Unique private static final float RAIN_TILT = 2.0f;
//    @Unique private static double rainDelta = 0;
//    @Unique private static int rainTiltTicks = 0;
//    @Unique private static boolean tickingBackwards = false;

    @WrapOperation(method = "tickRainSplashing", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"))
    private void spbrevamped$noRainSoundsInLevel324(ClientWorld instance, ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, Operation<Void> original) {
        if (SPBRevampedClient.isInLevel(BackroomsLevels.LEVEL324_BACKROOMS_LEVEL)) {
//            if (rainTiltTicks >= 20) {
//                tickingBackwards = true;
//            } else if (rainTiltTicks <= -0) {
//                tickingBackwards = false;
//            }
//
//            int value = tickingBackwards ? -1 : 1;
//
//            rainTiltTicks += value;
//
//            rainDelta = rainTiltTicks / 20.0 - .5;
//
//            System.out.println("Delta:" + rainDelta);
//            System.out.println("Tick:" + rainTiltTicks);


            return;
        }
        original.call(instance, parameters, (Object) x, (Object) y, (Object) z, (Object) velocityX, (Object) velocityY, (Object) velocityZ);
    }

    @WrapOperation(method = "renderWeather", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;vertex(DDD)Lnet/minecraft/client/render/VertexConsumer;", ordinal = 0))
    private VertexConsumer spbrevamped$noRainRenderInLevel3241(BufferBuilder instance, double x, double y, double z, Operation<VertexConsumer> original) {
        if (SPBRevampedClient.isInLevel(BackroomsLevels.LEVEL324_BACKROOMS_LEVEL)) {
            return original.call(instance, (Object) (x + ((RAIN_TILT) / 2) /* * rainDelta */), (Object) (y), (Object) (z + RAIN_TILT /** -rainDelta*/));
        }

        return original.call(instance, (Object) x, (Object) y, (Object) z);
    }

    @WrapOperation(method = "renderWeather", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/BufferBuilder;vertex(DDD)Lnet/minecraft/client/render/VertexConsumer;", ordinal = 1))
    private VertexConsumer spbrevamped$noRainRenderInLevel3242(BufferBuilder instance, double x, double y, double z, Operation<VertexConsumer> original) {
        if (SPBRevampedClient.isInLevel(BackroomsLevels.LEVEL324_BACKROOMS_LEVEL)) {
            return original.call(instance, (Object) (x + ((RAIN_TILT) / 2) /* * rainDelta */), (Object) (y), (Object) (z + RAIN_TILT /** -rainDelta*/));
        }

        return original.call(instance, (Object) x, (Object) y, (Object) z);
    }
}
