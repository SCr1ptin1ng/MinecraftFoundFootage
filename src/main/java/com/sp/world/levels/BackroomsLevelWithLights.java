package com.sp.world.levels;

import com.sp.world.levels.custom.Level0BackroomsLevel;

public interface BackroomsLevelWithLights {
    Level0BackroomsLevel.LightState getLightState();

    void setLightState(Level0BackroomsLevel.LightState lightState);

    enum LightState {
        ON,
        OFF,
        FLICKER,
        BLACKOUT
    }
}
