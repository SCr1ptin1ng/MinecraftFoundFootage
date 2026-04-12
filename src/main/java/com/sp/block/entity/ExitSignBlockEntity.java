package com.sp.block.entity;

import com.sp.init.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class ExitSignBlockEntity extends BlockEntity {
    public ExitSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EXIT_SIGN_BLOCK_ENTITY, pos, state);
    }
}
