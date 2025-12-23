package com.sp.world.events.generic.lights;

import com.sp.init.BackroomsLevels;
import com.sp.world.events.AbstractEvent;
import com.sp.world.levels.BackroomsLevelWithLights;
import net.minecraft.world.World;

public class LightLevelFlicker extends AbstractEvent {
    @Override
    public void init(World world) {
        if (!(BackroomsLevels.getLevel(world).orElse(BackroomsLevels.OVERWORLD_REPRESENTING_BACKROOMS_LEVEL) instanceof BackroomsLevelWithLights level)) {
            return;
        }

        level.setLightState(BackroomsLevelWithLights.LightState.FLICKER);
    }

    @Override
    public void finish(World world) {
        super.finish(world);

        if (!(BackroomsLevels.getLevel(world).orElse(BackroomsLevels.OVERWORLD_REPRESENTING_BACKROOMS_LEVEL) instanceof BackroomsLevelWithLights level)) {
            return;
        }

        level.setLightState(BackroomsLevelWithLights.LightState.ON);
    }


    @Override
    public int duration() {
        return 200;
    }
}
