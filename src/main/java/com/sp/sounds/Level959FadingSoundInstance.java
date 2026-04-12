package com.sp.sounds;

import com.sp.init.BackroomsLevels;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

@Environment(EnvType.CLIENT)
public class Level959FadingSoundInstance extends MovingSoundInstance {
    private final PlayerEntity player;
    private final float maxVolume;
    private final int durationTicks;
    private final int fadeInTicks;
    private final int fadeOutTicks;
    private int age;

    public Level959FadingSoundInstance(PlayerEntity player, SoundEvent sound, double x, double y, double z, float maxVolume, int durationTicks, int fadeInTicks, int fadeOutTicks) {
        super(sound, SoundCategory.AMBIENT, SoundInstance.createRandom());
        this.player = player;
        this.x = x;
        this.y = y;
        this.z = z;
        this.maxVolume = maxVolume;
        this.durationTicks = durationTicks;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;
        this.repeat = false;
        this.repeatDelay = 0;
        this.relative = false;
        this.volume = 0.0F;
        this.age = 0;
    }

    @Override
    public void tick() {
        if (this.player.isRemoved() || this.player.getWorld().getRegistryKey() != BackroomsLevels.A_PLACE_YOU_DONT_WANT_TO_KNOW_WORLD_KEY) {
            this.setDone();
            return;
        }

        this.age++;
        this.x = this.player.getX();
        this.y = this.player.getY();
        this.z = this.player.getZ();

        float fadeMultiplier = 1.0F;
        if (this.fadeInTicks > 0 && this.age <= this.fadeInTicks) {
            fadeMultiplier = (float) this.age / this.fadeInTicks;
        } else if (this.fadeOutTicks > 0 && this.age >= this.durationTicks - this.fadeOutTicks) {
            fadeMultiplier = Math.max(0.0F, (float) (this.durationTicks - this.age) / this.fadeOutTicks);
        }

        this.volume = this.maxVolume * fadeMultiplier;

        if (this.age >= this.durationTicks) {
            this.setDone();
        }
    }
}
