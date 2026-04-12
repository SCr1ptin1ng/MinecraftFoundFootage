package com.sp.mixin;

import com.mojang.datafixers.util.Either;
import com.sp.world.generation.Level959PlacementDebug;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SinglePoolElement.class)
public abstract class SinglePoolElementMixin {
    @Shadow
    protected Either<Identifier, StructureTemplate> location;

    @Inject(
            method = "generate(Lnet/minecraft/structure/StructureTemplateManager;Lnet/minecraft/world/StructureWorldAccess;Lnet/minecraft/world/gen/StructureAccessor;Lnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/BlockRotation;Lnet/minecraft/util/math/BlockBox;Lnet/minecraft/util/math/random/Random;Z)Z",
            at = @At("HEAD")
    )
    private void recordLevel959Placement(
            StructureTemplateManager structureTemplateManager,
            StructureWorldAccess world,
            StructureAccessor structureAccessor,
            ChunkGenerator chunkGenerator,
            BlockPos pos,
            BlockPos pivot,
            BlockRotation rotation,
            BlockBox box,
            Random random,
            boolean keepJigsaws,
            CallbackInfoReturnable<Boolean> cir
    ) {
        this.location.left().ifPresent(identifier -> Level959PlacementDebug.record(world, identifier, pos));
    }
}
