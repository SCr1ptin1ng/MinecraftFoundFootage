package com.sp.entity.custom;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.UUID;

public class FacelingEntity extends HostileEntity {
    public enum BehaviourMode {
        IDLE,
        FOLLOW,
        HUNT
    }

    private BehaviourMode behaviourMode = BehaviourMode.IDLE;
    private int aggression;
    private static final UUID NIL_UUID = new UUID(0L, 0L);
    private UUID focusTarget = NIL_UUID;

    public FacelingEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createFacelingAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30F)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0F);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0D, false));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    public void beginFollowing(UUID playerUuid) {
        this.focusTarget = playerUuid;
        this.behaviourMode = BehaviourMode.FOLLOW;
        this.setTarget(null);
    }

    public void beginFollowingClosest() {
        this.focusTarget = NIL_UUID;
        this.behaviourMode = BehaviourMode.FOLLOW;
        this.setTarget(null);
    }

    public void beginHunt() {
        this.behaviourMode = BehaviourMode.HUNT;
    }

    public void resetToIdle() {
        this.behaviourMode = BehaviourMode.IDLE;
        this.focusTarget = NIL_UUID;
        this.aggression = 0;
        this.setTarget(null);
        this.getNavigation().stop();
    }

    public BehaviourMode getBehaviourMode() {
        return behaviourMode;
    }

    public int getAggression() {
        return aggression;
    }

    public void increaseAggression(int amount) {
        this.aggression = Math.min(100, this.aggression + amount);
    }

    @Override
    public void tick() {
        PlayerEntity trackingPlayer = null;

        if (this.behaviourMode == BehaviourMode.FOLLOW) {
            if (!NIL_UUID.equals(this.focusTarget)) {
                trackingPlayer = this.getWorld().getPlayerByUuid(this.focusTarget);
            }
            if (trackingPlayer == null) {
                trackingPlayer = this.getWorld().getClosestPlayer(this, 24.0);
            }
            if (trackingPlayer != null && !trackingPlayer.isSpectator() && !trackingPlayer.isCreative()) {
                this.getNavigation().startMovingTo(trackingPlayer, 0.65D);
            } else {
                this.getNavigation().stop();
            }
            this.setTarget(null);
        } else if (this.behaviourMode == BehaviourMode.HUNT) {
            trackingPlayer = this.getWorld().getClosestPlayer(this, 24.0);
            if (trackingPlayer != null && !trackingPlayer.isSpectator() && !trackingPlayer.isCreative()) {
                this.setTarget(trackingPlayer);
                this.getNavigation().startMovingTo(trackingPlayer, 1.05D);
            } else {
                this.setTarget(null);
                this.getNavigation().stop();
            }
        } else {
            this.setVelocity(0.0, 0.0, 0.0);
            this.setMovementSpeed(0.0F);
            this.getNavigation().stop();
        }

        PlayerEntity nearestPlayer = trackingPlayer != null ? trackingPlayer : this.getWorld().getClosestPlayer(this, 16.0);
        if (nearestPlayer != null) {
            double dx = nearestPlayer.getX() - this.getX();
            double dz = nearestPlayer.getZ() - this.getZ();
            double dy = nearestPlayer.getEyeY() - this.getEyeY();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

            float targetYaw = (float) (MathHelper.atan2(dz, dx) * (180.0F / Math.PI)) - 90.0F;
            float targetPitch = (float) (-(MathHelper.atan2(dy, horizontalDistance) * (180.0F / Math.PI)));

            float newHeadYaw = MathHelper.lerpAngleDegrees(0.35F, this.headYaw, targetYaw);
            float newPitch = MathHelper.lerpAngleDegrees(0.25F, this.getPitch(), targetPitch);

            this.prevHeadYaw = this.headYaw;
            this.headYaw = newHeadYaw;
            this.setHeadYaw(newHeadYaw);

            this.prevPitch = this.getPitch();
            this.setPitch(newPitch);
        } else {
            this.prevHeadYaw = this.headYaw;
            this.headYaw = MathHelper.lerpAngleDegrees(0.1F, this.headYaw, this.bodyYaw);
            this.setHeadYaw(this.headYaw);
        }

        super.tick();
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    protected void mobTick() {
        super.mobTick();

        this.bodyYaw = MathHelper.lerpAngleDegrees(0.2F, this.bodyYaw, this.headYaw);
        this.prevBodyYaw = this.bodyYaw;
        this.setYaw(this.bodyYaw);
    }

    @Override
    protected float turnHead(float bodyRotation, float headRotation) {
        float delta = MathHelper.wrapDegrees(this.getHeadYaw() - this.bodyYaw);
        if (Math.abs(delta) > 75.0F) {
            this.bodyYaw += delta - (float) (MathHelper.sign(delta) * 75);
        }

        return headRotation;
    }

    @Override
    public int getMaxLookYawChange() {
        return 360;
    }

    @Override
    public int getMaxLookPitchChange() {
        return 360;
    }

    @Override
    public boolean canMoveVoluntarily() {
        return this.behaviourMode != BehaviourMode.IDLE;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
