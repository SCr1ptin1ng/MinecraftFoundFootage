package com.sp.sounds;

import com.sp.init.BackroomsLevels;
import com.sp.init.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;

@Environment(EnvType.CLIENT)
public class Level959LoopingAmbienceSoundInstance extends MovingSoundInstance {
    private final PlayerEntity player;

    public Level959LoopingAmbienceSoundInstance(PlayerEntity player) {
        super(ModSounds.LEVEL959_AMBIENCE, SoundCategory.AMBIENT, SoundInstance.createRandom());
        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
        this.relative = true;
        this.volume = 0.85F;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved() || this.player.getWorld().getRegistryKey() != BackroomsLevels.A_PLACE_YOU_DONT_WANT_TO_KNOW_WORLD_KEY) {
            this.setDone();
        }
    }
}
