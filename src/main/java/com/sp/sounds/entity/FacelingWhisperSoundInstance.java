package com.sp.sounds.entity;

import com.sp.entity.custom.FacelingEntity;
import com.sp.init.ModSounds;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;

import java.util.List;

public class FacelingWhisperSoundInstance extends MovingSoundInstance {
    private final PlayerEntity player;

    public FacelingWhisperSoundInstance(PlayerEntity player) {
        super(ModSounds.FACELING_WHISPER, SoundCategory.AMBIENT, SoundInstance.createRandom());
        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.15F;
        this.pitch = 0.92F;
        this.relative = false;
        this.attenuationType = AttenuationType.LINEAR;
        this.x = player.getX();
        this.y = player.getEyeY();
        this.z = player.getZ();
    }

    @Override
    public void tick() {
        if (this.player.isRemoved() || this.player.getWorld() == null) {
            this.setDone();
            return;
        }

        List<FacelingEntity> facelings = this.player.getWorld().getEntitiesByClass(FacelingEntity.class, this.player.getBoundingBox().expand(6.0, 3.0, 6.0), faceling -> true);
        FacelingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (FacelingEntity faceling : facelings) {
            double distance = this.player.getPos().distanceTo(faceling.getPos());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = faceling;
            }
        }

        if (nearest == null || nearestDistance >= 4.0) {
            this.volume = 0.0F;
            this.pitch = 0.92F;
            return;
        }

        this.x = nearest.getX();
        this.y = nearest.getEyeY();
        this.z = nearest.getZ();
        this.volume = (1.0F - ((float) nearestDistance / 4.0F)) * 0.9F;
        this.pitch = 0.88F + ((1.0F - ((float) nearestDistance / 4.0F)) * 0.08F);
    }
}
