package com.sp.mixin.rain;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.sp.init.BackroomsLevels;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerWorld.class)
public class ServerWorldNoHorseTrapMixin {
    @WrapOperation(method = "tickChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;spawnEntity(Lnet/minecraft/entity/Entity;)Z"))
    public boolean spbrevamped$noHorseTrapsInBackroomsLevel(ServerWorld instance, Entity entity, Operation<Boolean> original) {
        if (BackroomsLevels.isInBackroomsLevel((ServerWorld) (Object) this, BackroomsLevels.LEVEL324_BACKROOMS_LEVEL)) {
            return false;
        }

        return original.call(instance, entity);
    }
}
