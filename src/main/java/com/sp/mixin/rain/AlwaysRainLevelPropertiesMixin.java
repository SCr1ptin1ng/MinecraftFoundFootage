package com.sp.mixin.rain;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.sp.SPBRevampedClient;
import com.sp.init.BackroomsLevels;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.LevelProperties;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelProperties.class)
public class AlwaysRainLevelPropertiesMixin {
    @WrapMethod(method = "getRainTime")
    private int spbrevamped$alwaysRainGetRainTime(Operation<Integer> original) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT &&
                SPBRevampedClient.isInLevel(BackroomsLevels.LEVEL324_BACKROOMS_LEVEL)) {
            return 100;
        }

        return original.call();
    }

    @WrapMethod(method = "isRaining")
    private boolean spbrevamped$IsRaining(Operation<Boolean> original) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT &&
                SPBRevampedClient.isInLevel(BackroomsLevels.LEVEL324_BACKROOMS_LEVEL)) {
            return true;
        }

        return original.call();
    }

    @WrapMethod(method = "getThunderTime")
    private int spbrevamped$alwaysRainGetThunderTime(Operation<Integer> original) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT &&
                SPBRevampedClient.isInLevel(BackroomsLevels.LEVEL324_BACKROOMS_LEVEL)) {
            return 100;
        }

        return original.call();
    }

    @WrapMethod(method = "isThundering")
    private boolean spbrevamped$IsThundering(Operation<Boolean> original) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT &&
                SPBRevampedClient.isInLevel(BackroomsLevels.LEVEL324_BACKROOMS_LEVEL)) {
            return true;
        }

        return original.call();
    }
}
