package com.sp.entity.custom;

import com.sp.init.ModEntities;
import com.sp.init.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.UUID;

public class SpyderControllerEntity extends Entity {
    private static final int LEG_COUNT = 8;
    private static final Vec3d[] LEG_ROOT_OFFSETS = new Vec3d[]{
            new Vec3d(0.24, 0.0, -0.16),
            new Vec3d(-0.24, 0.0, -0.16),
            new Vec3d(0.28, 0.0, -0.02),
            new Vec3d(-0.28, 0.0, -0.02),
            new Vec3d(0.28, 0.0, 0.16),
            new Vec3d(-0.28, 0.0, 0.16),
            new Vec3d(0.24, 0.0, 0.30),
            new Vec3d(-0.24, 0.0, 0.30)
    };
    private static final Vec3d[] LEG_FOOT_OFFSETS = new Vec3d[]{
            new Vec3d(0.88, -0.58, -0.18),
            new Vec3d(-0.88, -0.58, -0.18),
            new Vec3d(1.00, -0.56, -0.02),
            new Vec3d(-1.00, -0.56, -0.02),
            new Vec3d(1.00, -0.56, 0.16),
            new Vec3d(-1.00, -0.56, 0.16),
            new Vec3d(0.88, -0.58, 0.32),
            new Vec3d(-0.88, -0.58, 0.32)
    };
    private static final int[] LEG_GROUPS = new int[]{0, 1, 1, 0, 0, 1, 1, 0};

    public enum SpyderState {
        HIDDEN,
        ENTERING,
        TRAVERSING,
        EXITING,
        DESPAWNED
    }

    private static final UUID NIL_UUID = new UUID(0L, 0L);
    private static final int ENTERING_TICKS = 28;
    private static final int TRAVERSING_TICKS = 150;
    private static final int EXITING_TICKS = 40;
    private static final int TOTAL_TICKS = ENTERING_TICKS + TRAVERSING_TICKS + EXITING_TICKS;

    private Vec3d startPos = Vec3d.ZERO;
    private Vec3d endPos = Vec3d.ZERO;
    private UUID watchedPlayer = NIL_UUID;
    private SpyderState state = SpyderState.HIDDEN;
    private float bodyPitchDegrees;
    private float bodyRollDegrees;
    private float movementAmount;
    private float strideProgress;
    private Vec3d surfaceNormal = new Vec3d(0.0, 1.0, 0.0);
    private Vec3d surfaceForward = new Vec3d(0.0, 0.0, 1.0);
    private Vec3d supportFront = Vec3d.ZERO;
    private Vec3d supportBack = Vec3d.ZERO;
    private Vec3d supportLeft = Vec3d.ZERO;
    private Vec3d supportRight = Vec3d.ZERO;
    private final Vec3d[] legTargets = new Vec3d[LEG_COUNT];
    private final Vec3d[] legStepStarts = new Vec3d[LEG_COUNT];
    private final Vec3d[] legStepEnds = new Vec3d[LEG_COUNT];
    private final float[] legStepProgress = new float[LEG_COUNT];
    private final boolean[] legStepping = new boolean[LEG_COUNT];

    public SpyderControllerEntity(EntityType<? extends SpyderControllerEntity> entityType, World world) {
        super(ModEntities.SPYDER_CONTROLLER_ENTITY, world);
        this.noClip = true;
        this.setNoGravity(true);
        for (int i = 0; i < LEG_COUNT; i++) {
            this.legTargets[i] = Vec3d.ZERO;
            this.legStepStarts[i] = Vec3d.ZERO;
            this.legStepEnds[i] = Vec3d.ZERO;
        }
    }

    public void configure(Vec3d startPos, Vec3d endPos, UUID watchedPlayer) {
        this.startPos = startPos;
        this.endPos = endPos;
        this.watchedPlayer = watchedPlayer;
        this.state = SpyderState.ENTERING;
        this.setPosition(startPos);
    }

    public SpyderState getSpyderState() {
        return state;
    }

    public UUID getWatchedPlayer() {
        return watchedPlayer;
    }

    public boolean isConfigured() {
        return !this.startPos.equals(Vec3d.ZERO) || !this.endPos.equals(Vec3d.ZERO);
    }

    public float getBodyPitchDegrees() {
        return bodyPitchDegrees;
    }

    public float getBodyRollDegrees() {
        return bodyRollDegrees;
    }

    public float getMovementAmount() {
        return movementAmount;
    }

    public float getStrideProgress() {
        return strideProgress;
    }

    public Vec3d getSurfaceNormal() {
        return surfaceNormal;
    }

    public Vec3d getSurfaceForward() {
        return surfaceForward;
    }

    public Vec3d getSupportFront() {
        return supportFront;
    }

    public Vec3d getSupportBack() {
        return supportBack;
    }

    public Vec3d getSupportLeft() {
        return supportLeft;
    }

    public Vec3d getSupportRight() {
        return supportRight;
    }

    public int getLegCount() {
        return LEG_COUNT;
    }

    public Vec3d getLegRootOffset(int legIndex) {
        return LEG_ROOT_OFFSETS[legIndex];
    }

    public Vec3d getLegTarget(int legIndex) {
        return this.legTargets[legIndex];
    }

    public void configureFromCurrentPosition() {
        Vec3d origin = this.getPos();
        Vec3d fallbackStart = origin.add(-4.5, 0.0, 0.0);
        Vec3d fallbackEnd = origin.add(4.5, 0.0, 0.0);
        configure(fallbackStart, fallbackEnd, NIL_UUID);
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    public void tick() {
        this.baseTick();

        PlayerEntity debugPlayer = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), 64.0,
                entity -> entity instanceof PlayerEntity player && this.isHoldingDebugStick(player));
        if (debugPlayer != null) {
            tickDebugFollow(debugPlayer);
            return;
        }

        if (!this.isConfigured()) {
            this.configureFromCurrentPosition();
        }

        int lifeTicks = this.age;
        if (lifeTicks >= TOTAL_TICKS) {
            this.state = SpyderState.DESPAWNED;
            this.discard();
            return;
        }

        if (lifeTicks < ENTERING_TICKS) {
            this.state = SpyderState.ENTERING;
        } else if (lifeTicks < ENTERING_TICKS + TRAVERSING_TICKS) {
            this.state = SpyderState.TRAVERSING;
        } else {
            this.state = SpyderState.EXITING;
        }

        double progress = MathHelper.clamp((lifeTicks - (ENTERING_TICKS * 0.5)) / (double) TRAVERSING_TICKS, 0.0, 1.0);
        Vec3d currentPos = this.startPos.lerp(this.endPos, progress).add(0.0, Math.sin(lifeTicks * 0.22) * 0.04, 0.0);

        Vec3d moveDir = this.endPos.subtract(this.startPos);
        if (moveDir.lengthSquared() > 0.0001) {
            float yaw = (float) (MathHelper.atan2(moveDir.z, moveDir.x) * (180.0F / Math.PI)) - 90.0F;
            this.setYaw(yaw);
            this.prevYaw = yaw;
        }

        this.setPosition(currentPos);
    }

    private void tickDebugFollow(PlayerEntity player) {
        Vec3d eyePos = player.getEyePos();
        Vec3d lookDir = player.getRotationVec(1.0F).normalize();
        Vec3d end = eyePos.add(lookDir.multiply(32.0));

        HitResult hit = this.getWorld().raycast(new RaycastContext(
                eyePos,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        Vec3d targetPos = hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
        Vec3d surfaceNormal = getSurfaceNormal(hit, lookDir);
        if (hit instanceof BlockHitResult blockHitResult && blockHitResult.getSide() != null) {
            Direction side = blockHitResult.getSide();
            targetPos = targetPos.add(side.getOffsetX() * 0.28, side.getOffsetY() * 0.28, side.getOffsetZ() * 0.28);
        } else {
            targetPos = targetPos.add(surfaceNormal.multiply(0.28));
        }

        Vec3d currentPos = this.getPos();
        Vec3d nextPos = currentPos.lerp(targetPos, 0.18);
        Vec3d frameDelta = nextPos.subtract(currentPos);
        double moveDistance = frameDelta.length();
        Vec3d moveDir = targetPos.subtract(this.getPos());
        Vec3d projectedForward = getProjectedForward(surfaceNormal, moveDir.lengthSquared() > 0.0001 ? moveDir.normalize() : lookDir);

        if (moveDistance > 0.012) {
            this.strideProgress += (float) MathHelper.clamp(moveDistance * 5.4, 0.0, 0.22);
            float targetMovement = MathHelper.clamp((float) ((moveDistance - 0.012) * 22.0), 0.0F, 1.0F);
            this.movementAmount = MathHelper.lerp(0.18F, this.movementAmount, targetMovement);
        } else {
            this.movementAmount = MathHelper.lerp(0.20F, this.movementAmount, 0.0F);
        }

        this.surfaceNormal = this.surfaceNormal.lerp(surfaceNormal, 0.30).normalize();
        this.surfaceForward = this.surfaceForward.lerp(projectedForward, 0.25).normalize();
        updateLegTargets(nextPos, this.surfaceNormal, this.surfaceForward, moveDistance);
        updateBodyTilt(nextPos, this.surfaceNormal, this.surfaceForward);
        spawnDebugSupportParticles();

        this.state = SpyderState.TRAVERSING;
        this.setPosition(nextPos);
        this.startPos = nextPos;
        this.endPos = targetPos;
        this.age = Math.min(this.age, 40);
    }

    private Vec3d getSurfaceNormal(HitResult hit, Vec3d fallbackLook) {
        if (hit instanceof BlockHitResult blockHitResult && blockHitResult.getSide() != null) {
            Direction side = blockHitResult.getSide();
            return new Vec3d(side.getOffsetX(), side.getOffsetY(), side.getOffsetZ()).normalize();
        }

        return fallbackLook.multiply(-1.0).normalize();
    }

    private Vec3d getProjectedForward(Vec3d surfaceNormal, Vec3d desiredForward) {
        Vec3d projectedForward = desiredForward.subtract(surfaceNormal.multiply(desiredForward.dotProduct(surfaceNormal)));
        if (projectedForward.lengthSquared() < 0.0001) {
            projectedForward = new Vec3d(0.0, 0.0, 1.0).subtract(surfaceNormal.multiply(surfaceNormal.z));
        }

        if (projectedForward.lengthSquared() < 0.0001) {
            projectedForward = new Vec3d(1.0, 0.0, 0.0);
        }

        return projectedForward.normalize();
    }

    private void updateBodyTilt(Vec3d anchorPos, Vec3d surfaceNormal, Vec3d desiredForward) {
        Vec3d projectedForward = getProjectedForward(surfaceNormal, desiredForward);
        Vec3d right = projectedForward.crossProduct(surfaceNormal).normalize();
        if (right.lengthSquared() < 0.0001) {
            right = new Vec3d(1.0, 0.0, 0.0);
        }

        double sampleDistance = 0.65;
        this.supportFront = sampleContact(anchorPos.add(projectedForward.multiply(sampleDistance)), surfaceNormal);
        this.supportBack = sampleContact(anchorPos.subtract(projectedForward.multiply(sampleDistance)), surfaceNormal);
        this.supportLeft = sampleContact(anchorPos.subtract(right.multiply(sampleDistance)), surfaceNormal);
        this.supportRight = sampleContact(anchorPos.add(right.multiply(sampleDistance)), surfaceNormal);

        Vec3d worldUp = new Vec3d(0.0, 1.0, 0.0);
        double frontY = this.supportFront.dotProduct(worldUp);
        double backY = this.supportBack.dotProduct(worldUp);
        double leftY = this.supportLeft.dotProduct(worldUp);
        double rightY = this.supportRight.dotProduct(worldUp);

        float targetPitch = MathHelper.clamp((float) ((frontY - backY) * 42.0), -22.0F, 22.0F);
        float targetRoll = MathHelper.clamp((float) ((leftY - rightY) * 42.0), -22.0F, 22.0F);

        this.bodyPitchDegrees = MathHelper.lerp(0.12F, this.bodyPitchDegrees, targetPitch);
        this.bodyRollDegrees = MathHelper.lerp(0.12F, this.bodyRollDegrees, targetRoll);
    }

    private Vec3d sampleContact(Vec3d pointOnPlane, Vec3d surfaceNormal) {
        Vec3d start = pointOnPlane.add(surfaceNormal.multiply(1.8));
        Vec3d end = pointOnPlane.subtract(surfaceNormal.multiply(1.8));
        HitResult supportHit = this.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this
        ));

        return supportHit.getType() == HitResult.Type.MISS ? pointOnPlane : supportHit.getPos();
    }

    private void updateLegTargets(Vec3d anchorPos, Vec3d surfaceNormal, Vec3d desiredForward, double moveDistance) {
        Vec3d forward = getProjectedForward(surfaceNormal, desiredForward);
        Vec3d right = surfaceNormal.crossProduct(forward).normalize();
        if (right.lengthSquared() < 0.0001) {
            right = new Vec3d(1.0, 0.0, 0.0);
        }

        int activeGroup = getActiveStepGroup();
        float stepSpeed = moveDistance > 0.02 ? 0.28F : 0.18F;

        for (int i = 0; i < LEG_COUNT; i++) {
            Vec3d rootWorld = anchorPos
                    .add(right.multiply(LEG_ROOT_OFFSETS[i].x))
                    .add(surfaceNormal.multiply(LEG_ROOT_OFFSETS[i].y))
                    .add(forward.multiply(LEG_ROOT_OFFSETS[i].z));

            Vec3d desiredWorld = anchorPos
                    .add(right.multiply(LEG_FOOT_OFFSETS[i].x))
                    .add(surfaceNormal.multiply(LEG_FOOT_OFFSETS[i].y))
                    .add(forward.multiply(LEG_FOOT_OFFSETS[i].z));
            desiredWorld = sampleContact(desiredWorld, surfaceNormal);

            if (this.legTargets[i].equals(Vec3d.ZERO)) {
                this.legTargets[i] = desiredWorld;
            }

            if (this.legStepping[i]) {
                this.legStepProgress[i] = Math.min(1.0F, this.legStepProgress[i] + stepSpeed);
                float eased = MathHelper.clamp(this.legStepProgress[i], 0.0F, 1.0F);
                Vec3d blended = this.legStepStarts[i].lerp(this.legStepEnds[i], eased);
                double arcHeight = Math.sin(eased * Math.PI) * 0.22;
                this.legTargets[i] = blended.add(surfaceNormal.multiply(arcHeight));
                if (this.legStepProgress[i] >= 1.0F) {
                    this.legStepping[i] = false;
                    this.legTargets[i] = this.legStepEnds[i];
                }
                continue;
            }

            double stretch = this.legTargets[i].distanceTo(desiredWorld);
            double rootDistance = this.legTargets[i].distanceTo(rootWorld);
            boolean shouldStep = stretch > 0.34 || rootDistance > 1.28;
            if (moveDistance < 0.01) {
                shouldStep = stretch > 0.42 || rootDistance > 1.36;
            }

            int legGroup = LEG_GROUPS[i];
            boolean groupAllowed = activeGroup == -1 || activeGroup == legGroup;
            if (shouldStep && groupAllowed) {
                this.legStepping[i] = true;
                this.legStepProgress[i] = 0.0F;
                this.legStepStarts[i] = this.legTargets[i];
                this.legStepEnds[i] = desiredWorld;
                activeGroup = legGroup;
            } else {
                this.legTargets[i] = this.legTargets[i].lerp(desiredWorld, 0.05);
            }
        }
    }

    private int getActiveStepGroup() {
        for (int i = 0; i < LEG_COUNT; i++) {
            if (this.legStepping[i]) {
                return LEG_GROUPS[i];
            }
        }
        return -1;
    }

    private boolean isHoldingDebugStick(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.SPYDER_DEBUG) || player.getOffHandStack().isOf(ModItems.SPYDER_DEBUG);
    }

    private void spawnDebugSupportParticles() {
        if (!(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        spawnMarker(serverWorld, this.supportFront, new Vector3f(1.0F, 0.25F, 0.25F));
        spawnMarker(serverWorld, this.supportBack, new Vector3f(0.25F, 1.0F, 0.25F));
        spawnMarker(serverWorld, this.supportLeft, new Vector3f(0.25F, 0.5F, 1.0F));
        spawnMarker(serverWorld, this.supportRight, new Vector3f(1.0F, 0.85F, 0.25F));
    }

    private void spawnMarker(ServerWorld serverWorld, Vec3d pos, Vector3f color) {
        if (pos.equals(Vec3d.ZERO)) {
            return;
        }

        serverWorld.spawnParticles(new DustParticleEffect(color, 0.55F), pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        this.startPos = new Vec3d(nbt.getDouble("spyderStartX"), nbt.getDouble("spyderStartY"), nbt.getDouble("spyderStartZ"));
        this.endPos = new Vec3d(nbt.getDouble("spyderEndX"), nbt.getDouble("spyderEndY"), nbt.getDouble("spyderEndZ"));
        if (nbt.containsUuid("spyderWatchedPlayer")) {
            this.watchedPlayer = nbt.getUuid("spyderWatchedPlayer");
        } else {
            this.watchedPlayer = NIL_UUID;
        }

        if (nbt.contains("spyderState")) {
            this.state = SpyderState.valueOf(nbt.getString("spyderState"));
        }

        this.bodyPitchDegrees = nbt.getFloat("spyderBodyPitch");
        this.bodyRollDegrees = nbt.getFloat("spyderBodyRoll");
        this.movementAmount = nbt.getFloat("spyderMovementAmount");
        this.strideProgress = nbt.getFloat("spyderStrideProgress");
        this.surfaceNormal = new Vec3d(nbt.getDouble("spyderNormalX"), nbt.getDouble("spyderNormalY"), nbt.getDouble("spyderNormalZ"));
        this.surfaceForward = new Vec3d(nbt.getDouble("spyderForwardX"), nbt.getDouble("spyderForwardY"), nbt.getDouble("spyderForwardZ"));
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putDouble("spyderStartX", this.startPos.x);
        nbt.putDouble("spyderStartY", this.startPos.y);
        nbt.putDouble("spyderStartZ", this.startPos.z);
        nbt.putDouble("spyderEndX", this.endPos.x);
        nbt.putDouble("spyderEndY", this.endPos.y);
        nbt.putDouble("spyderEndZ", this.endPos.z);
        nbt.putUuid("spyderWatchedPlayer", this.watchedPlayer);
        nbt.putString("spyderState", this.state.name());
        nbt.putFloat("spyderBodyPitch", this.bodyPitchDegrees);
        nbt.putFloat("spyderBodyRoll", this.bodyRollDegrees);
        nbt.putFloat("spyderMovementAmount", this.movementAmount);
        nbt.putFloat("spyderStrideProgress", this.strideProgress);
        nbt.putDouble("spyderNormalX", this.surfaceNormal.x);
        nbt.putDouble("spyderNormalY", this.surfaceNormal.y);
        nbt.putDouble("spyderNormalZ", this.surfaceNormal.z);
        nbt.putDouble("spyderForwardX", this.surfaceForward.x);
        nbt.putDouble("spyderForwardY", this.surfaceForward.y);
        nbt.putDouble("spyderForwardZ", this.surfaceForward.z);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public boolean collides() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
