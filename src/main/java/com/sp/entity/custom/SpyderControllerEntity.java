package com.sp.entity.custom;

import com.sp.entity.spyder.runtime.SpyderGaitProfile;
import com.sp.entity.spyder.runtime.SpyderLegIndexing;
import com.sp.entity.spyder.runtime.SpyderLegState;
import com.sp.init.ModEntities;
import com.sp.init.ModItems;
import com.sp.item.custom.SpyderDebugItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpyderControllerEntity extends Entity {
    private static final int LEG_COUNT = 8;
    private static final double LEG_LANE_X = 0.42;
    private static final double LEG_LANE_Z = 0.36;
    private static final double LEG_LANE_Y_UP = 0.18;
    private static final double LEG_LANE_Y_DOWN = 0.95;
    private static final double LEG_MAX_ROOT_DISTANCE = 1.42;
    private static final double LEG_SIDE_CLEARANCE = 0.46;
    private static final double LEG_PAIR_SEPARATION_Z = 0.18;
    private static final double LEG_COMFORT_HORIZONTAL = 0.20;
    private static final double LEG_COMFORT_VERTICAL = 0.16;
    private static final double LEG_COMFORT_LONGITUDINAL = 0.18;
    private static final double LEG_COMFORT_FOLLOW_VERTICAL = 0.42;
    private static final double LEG_LOOKAHEAD_DISTANCE = 0.38;
    private static final double LEG_TRIGGER_HORIZONTAL = 0.38;
    private static final double LEG_TRIGGER_VERTICAL = 0.26;
    private static final double LEG_IDLE_SETTLE = 0.08;
    private static final double LEG_CARRY_FRACTION = 0.06;
    private static final double LEG_STEP_REACH = 0.18;
    private static final double LEG_DROP_DISTANCE = 0.24;
    private static final double LEG_SURFACE_OFFSET = 0.055;
    private static final double LEG_TARGET_FOLLOW = 0.14;
    private static final double LEG_TARGET_FOLLOW_UNCOMFORTABLE = 0.42;
    private static final double LEG_STEP_TARGET_BLEND = 0.10;
    private static final double LEG_PLANTED_TARGET_BLEND = 0.18;
    private static final double LEG_MOVING_TARGET_BLEND = 0.12;
    private static final double LEG_TARGET_STEP_TRIGGER = 0.20;
    private static final double LEG_SCAN_UP = 1.20;
    private static final double LEG_SCAN_DOWN = 2.60;
    private static final double LEG_SCAN_GRID_SPACING = 0.20;
    private static final float LEG_STEP_ARC_HEIGHT = 0.22F;
    private static final float LEG_STEP_MIN_SPEED = 0.08F;
    private static final float LEG_STEP_MAX_SPEED = 0.17F;
    private static final float BODY_LIFT_MIN = 0.10F;
    private static final float BODY_LIFT_MAX = 0.90F;
    private static final double BODY_SUPPORT_BLEND = 0.72;
    private static final double BODY_GOAL_BLEND = 0.28;
    private static final double BODY_MAX_FOLLOW_STEP = 0.11;
    private static final double BODY_SPRING_HORIZONTAL = 0.26;
    private static final double BODY_SPRING_VERTICAL = 0.34;
    private static final double BODY_SPRING_DAMPING = 0.74;
    private static final double BODY_GOAL_ACCEL = 0.18;
    private static final double BODY_MAX_SPEED = 0.085;
    private static final double BODY_PLANAR_DAMPING = 0.82;
    private static final double BODY_PLANAR_ACCEL = 0.032;
    private static final double BODY_PLANAR_SUPPORT_PULL = 0.18;
    private static final double BODY_VERTICAL_DAMPING = 0.68;
    private static final double BODY_VERTICAL_MAX_SPEED = 0.080;
    private static final double BODY_SUPPORT_PLANAR_LEEWAY = 0.16;
    private static final double SURFACE_SUPPORT_BLEND = 0.14;
    private static final double DEBUG_WALK_ACCEL = 0.028;
    private static final double DEBUG_WALK_STOP_RADIUS = 0.10;
    private static final int[] SUPPORT_POLYGON_ORDER = new int[]{0, 2, 4, 6, 7, 5, 3, 1};
    private static final int SAME_PAIR_COOLDOWN = 3;
    private static final int CROSS_PAIR_COOLDOWN = 5;
    private static final double[] LEG_SEGMENT_LENGTHS = new double[]{0.32, 0.34, 0.29, 0.20};
    private static final float[][] LEG_REST_ANGLES = new float[][]{
            {58.0F, -84.0F, 76.0F, 18.0F},
            {58.0F, -84.0F, 76.0F, 18.0F},
            {50.0F, -76.0F, 72.0F, 16.0F},
            {50.0F, -76.0F, 72.0F, 16.0F},
            {42.0F, -72.0F, 76.0F, 12.0F},
            {42.0F, -72.0F, 76.0F, 12.0F},
            {52.0F, -80.0F, 82.0F, 16.0F},
            {52.0F, -80.0F, 82.0F, 16.0F}
    };
    private static final Vec3d[] LEG_ROOT_OFFSETS = new Vec3d[]{
            new Vec3d(0.24, 0.14, -0.16),
            new Vec3d(-0.24, 0.14, -0.16),
            new Vec3d(0.28, 0.15, -0.02),
            new Vec3d(-0.28, 0.15, -0.02),
            new Vec3d(0.28, 0.15, 0.16),
            new Vec3d(-0.28, 0.15, 0.16),
            new Vec3d(0.24, 0.14, 0.30),
            new Vec3d(-0.24, 0.14, 0.30)
    };
    private static final Vec3d[] LEG_FOOT_OFFSETS = new Vec3d[]{
            new Vec3d(0.90, -0.70, -0.20),
            new Vec3d(-0.90, -0.70, -0.20),
            new Vec3d(1.04, -0.68, -0.02),
            new Vec3d(-1.04, -0.68, -0.02),
            new Vec3d(1.04, -0.68, 0.16),
            new Vec3d(-1.04, -0.68, 0.16),
            new Vec3d(0.90, -0.70, 0.34),
            new Vec3d(-0.90, -0.70, 0.34)
    };
    private static final UUID NIL_UUID = new UUID(0L, 0L);
    private static final int ENTERING_TICKS = 28;
    private static final int TRAVERSING_TICKS = 150;
    private static final int EXITING_TICKS = 40;
    private static final int TOTAL_TICKS = ENTERING_TICKS + TRAVERSING_TICKS + EXITING_TICKS;

    public enum SpyderState {
        HIDDEN,
        ENTERING,
        TRAVERSING,
        EXITING,
        DESPAWNED
    }

    private final LegRuntime[] legs = new LegRuntime[LEG_COUNT];
    private final SpyderGaitProfile gaitProfile = SpyderGaitProfile.defaultWalk();

    private Vec3d startPos = Vec3d.ZERO;
    private Vec3d endPos = Vec3d.ZERO;
    private UUID watchedPlayer = NIL_UUID;
    private SpyderState state = SpyderState.HIDDEN;
    private float bodyPitchDegrees;
    private float bodyRollDegrees;
    private float movementAmount;
    private float strideProgress;
    private float bodyHeightOffset = 0.34F;
    private Vec3d surfaceNormal = new Vec3d(0.0, 1.0, 0.0);
    private Vec3d surfaceForward = new Vec3d(0.0, 0.0, 1.0);
    private Vec3d supportFront = Vec3d.ZERO;
    private Vec3d supportBack = Vec3d.ZERO;
    private Vec3d supportLeft = Vec3d.ZERO;
    private Vec3d supportRight = Vec3d.ZERO;
    private Vec3d debugGoalPos = Vec3d.ZERO;
    private Vec3d debugGoalNormal = new Vec3d(0.0, 1.0, 0.0);
    private boolean debugGoalActive;
    private Vec3d debugWalkVelocity = Vec3d.ZERO;
    private Vec3d bodyVelocity = Vec3d.ZERO;
    private int preferredStepGroup;
    private int lastStepLeg = -1;
    private int gaitCycleCursor;

    public SpyderControllerEntity(EntityType<? extends SpyderControllerEntity> entityType, World world) {
        super(ModEntities.SPYDER_CONTROLLER_ENTITY, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.bodyHeightOffset = (float) this.gaitProfile.bodyHeight();
        for (int i = 0; i < LEG_COUNT; i++) {
            this.legs[i] = new LegRuntime(i);
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
        return this.state;
    }

    public UUID getWatchedPlayer() {
        return this.watchedPlayer;
    }

    public boolean isConfigured() {
        return !this.startPos.equals(Vec3d.ZERO) || !this.endPos.equals(Vec3d.ZERO);
    }

    public float getBodyPitchDegrees() {
        return this.bodyPitchDegrees;
    }

    public float getBodyRollDegrees() {
        return this.bodyRollDegrees;
    }

    public float getMovementAmount() {
        return this.movementAmount;
    }

    public float getStrideProgress() {
        return this.strideProgress;
    }

    public Vec3d getSurfaceNormal() {
        return this.surfaceNormal;
    }

    public Vec3d getSurfaceForward() {
        return this.surfaceForward;
    }

    public Vec3d getSupportFront() {
        return this.supportFront;
    }

    public Vec3d getSupportBack() {
        return this.supportBack;
    }

    public Vec3d getSupportLeft() {
        return this.supportLeft;
    }

    public Vec3d getSupportRight() {
        return this.supportRight;
    }

    public int getLegCount() {
        return LEG_COUNT;
    }

    public Vec3d getLegRootOffset(int legIndex) {
        return LEG_ROOT_OFFSETS[legIndex];
    }

    public Vec3d getLegTarget(int legIndex) {
        return this.legs[legIndex].target;
    }

    public Vec3d getLegEndEffector(int legIndex) {
        return this.legs[legIndex].endEffector;
    }

    public boolean isLegGrounded(int legIndex) {
        return this.legs[legIndex].isGrounded();
    }

    public boolean isLegStepping(int legIndex) {
        return this.legs[legIndex].moving;
    }

    public float getBodyHeightOffset() {
        return this.bodyHeightOffset;
    }

    public Vec3d getLegGroundPosition(int legIndex) {
        return this.legs[legIndex].groundPosition;
    }

    public Vec3d[] getLegJoints(int legIndex) {
        return this.legs[legIndex].joints.clone();
    }

    public Vec3d getDebugGoalPos() {
        return this.debugGoalPos;
    }

    public Vec3d getDebugGoalNormal() {
        return this.debugGoalNormal;
    }

    public boolean hasDebugGoal() {
        return this.debugGoalActive;
    }

    public void configureFromCurrentPosition() {
        Vec3d origin = this.getPos();
        configure(origin.add(-4.5, 0.0, 0.0), origin.add(4.5, 0.0, 0.0), NIL_UUID);
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    public void tick() {
        this.baseTick();
        for (LegRuntime leg : this.legs) {
            leg.tickTimers();
        }

        PlayerEntity debugPlayer = this.getWorld().getClosestPlayer(this.getX(), this.getY(), this.getZ(), 64.0,
                entity -> entity instanceof PlayerEntity player && this.isHoldingDebugStick(player));
        if (debugPlayer != null) {
            tickDebugFollow(debugPlayer);
            return;
        }

        this.debugGoalActive = false;
        this.debugWalkVelocity = Vec3d.ZERO;
        tickCinematicTraverse();
    }

    private void tickCinematicTraverse() {
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
        Vec3d pathPos = this.startPos.lerp(this.endPos, progress);
        Vec3d moveDir = this.endPos.subtract(this.startPos);
        Vec3d desiredForward = moveDir.lengthSquared() < 0.0001 ? this.surfaceForward : moveDir.normalize();
        Vec3d desiredVelocity = desiredForward.multiply(0.055);

        Vec3d seedPos = this.getPos().equals(Vec3d.ZERO) ? pathPos : this.getPos();
        Vec3d nextPos = updateSpyderRuntime(seedPos, new Vec3d(0.0, 1.0, 0.0), desiredForward, desiredVelocity);
        updateMovementAnimation(this.getPos(), nextPos);
        this.setPosition(nextPos);

        if (moveDir.lengthSquared() > 0.0001) {
            float yaw = (float) (MathHelper.atan2(moveDir.z, moveDir.x) * (180.0F / Math.PI)) - 90.0F;
            this.setYaw(yaw);
            this.prevYaw = yaw;
        }
    }

    private void tickDebugFollow(PlayerEntity player) {
        ItemStack debugStick = getDebugStick(player);
        Vec3d targetPos;
        Vec3d desiredNormal;
        Vec3d lookDir = player.getRotationVec(1.0F).normalize();

        if (!debugStick.isEmpty()
                && SpyderDebugItem.getMode(debugStick) == SpyderDebugItem.Mode.WALK_TO_POINT
                && SpyderDebugItem.hasTarget(debugStick)) {
            targetPos = SpyderDebugItem.getTarget(debugStick);
            desiredNormal = SpyderDebugItem.getTargetNormal(debugStick);
        } else {
            Vec3d eyePos = player.getEyePos();
            Vec3d end = eyePos.add(lookDir.multiply(32.0));
            HitResult hit = this.getWorld().raycast(new RaycastContext(
                    eyePos,
                    end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
            ));

            targetPos = hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
            desiredNormal = getSurfaceNormal(hit, lookDir);
            if (hit instanceof BlockHitResult blockHit && blockHit.getSide() != null) {
                targetPos = targetPos.add(new Vec3d(
                        blockHit.getSide().getOffsetX(),
                        blockHit.getSide().getOffsetY(),
                        blockHit.getSide().getOffsetZ()
                ).multiply(0.28));
            } else {
                targetPos = targetPos.add(desiredNormal.multiply(0.28));
            }
        }

        this.debugGoalActive = true;
        this.debugGoalPos = targetPos;
        this.debugGoalNormal = desiredNormal;

        Vec3d currentPos = this.getPos();
        double toGoal = currentPos.distanceTo(targetPos);
        Vec3d desiredVelocity = computeDesiredDebugVelocity(currentPos, targetPos, desiredNormal);
        this.debugWalkVelocity = approachVelocity(this.debugWalkVelocity, desiredVelocity, DEBUG_WALK_ACCEL);
        if (toGoal <= DEBUG_WALK_STOP_RADIUS && this.debugWalkVelocity.lengthSquared() < 0.0004) {
            this.debugWalkVelocity = Vec3d.ZERO;
        }

        Vec3d intentDir = this.debugWalkVelocity.lengthSquared() > 0.0001 ? this.debugWalkVelocity : lookDir;
        Vec3d desiredForward = getProjectedForward(desiredNormal, intentDir);
        Vec3d nextPos = updateSpyderRuntime(currentPos, desiredNormal, desiredForward, this.debugWalkVelocity);

        updateMovementAnimation(currentPos, nextPos);
        this.state = SpyderState.TRAVERSING;
        this.setPosition(nextPos);
        this.startPos = nextPos;
        this.endPos = targetPos;
        this.age = Math.min(this.age, 40);
    }

    private Vec3d updateSpyderRuntime(Vec3d currentPos, Vec3d desiredNormal, Vec3d desiredForward, Vec3d desiredVelocity) {
        this.surfaceNormal = normalizeOr(this.surfaceNormal.lerp(normalizeOr(desiredNormal, new Vec3d(0.0, 1.0, 0.0)), 0.18), new Vec3d(0.0, 1.0, 0.0));
        this.surfaceForward = getProjectedForward(this.surfaceNormal, this.surfaceForward.lerp(getProjectedForward(this.surfaceNormal, desiredForward), 0.16));

        BodyFrame frame = createBodyFrame(currentPos, this.surfaceNormal, this.surfaceForward);
        Vec3d locomotionDir = desiredVelocity.lengthSquared() > 0.0001 ? getProjectedForward(frame.up, desiredVelocity) : frame.forward;

        for (LegRuntime leg : this.legs) {
            leg.updateMemo(frame, locomotionDir);
            leg.refreshTarget(frame, desiredVelocity);
        }
        enforceLegTargetSeparation(frame);

        SupportState preStepSupport = computeSupportState(frame, desiredVelocity);
        applySupportDebug(preStepSupport, frame.bodyPos);

        LegRuntime stepCandidate = selectStepCandidate(desiredVelocity.length(), preStepSupport);
        if (stepCandidate != null) {
            stepCandidate.beginStep();
            this.lastStepLeg = stepCandidate.index;
            this.preferredStepGroup = SpyderLegIndexing.isDiagonal1(stepCandidate.index) ? 1 : 0;
        }

        for (LegRuntime leg : this.legs) {
            leg.updateMovement(frame, desiredVelocity.length());
        }

        SupportState postMoveSupport = computeSupportState(frame, desiredVelocity);
        this.bodyHeightOffset = MathHelper.lerp(0.12F, this.bodyHeightOffset, (float) postMoveSupport.preferredHeight());
        this.surfaceNormal = normalizeOr(this.surfaceNormal.lerp(postMoveSupport.normal(), SURFACE_SUPPORT_BLEND), this.surfaceNormal);
        this.surfaceForward = getProjectedForward(this.surfaceNormal, this.surfaceForward);

        BodyFrame supportFrame = createBodyFrame(currentPos, this.surfaceNormal, this.surfaceForward);
        Vec3d nextPos = computeSupportedBodyPosition(supportFrame, desiredVelocity, postMoveSupport);
        BodyFrame solvedFrame = createBodyFrame(nextPos, this.surfaceNormal, this.surfaceForward);
        for (LegRuntime leg : this.legs) {
            leg.updateMemo(solvedFrame, locomotionDir);
            leg.solveJoints(solvedFrame);
        }

        SupportState solvedSupport = computeSupportState(solvedFrame, desiredVelocity);
        applySupportDebug(solvedSupport, solvedFrame.bodyPos);
        updateBodyTiltFromSupport(solvedSupport, solvedFrame);
        updateBodyHeightDebug(solvedSupport);
        spawnDebugSupportParticles();
        return nextPos;
    }

    private void updateMovementAnimation(Vec3d currentPos, Vec3d nextPos) {
        double moveDistance = nextPos.distanceTo(currentPos);
        if (moveDistance > 0.006) {
            this.strideProgress += (float) MathHelper.clamp(moveDistance * 6.4, 0.0, 0.24);
            float targetMovement = MathHelper.clamp((float) ((moveDistance - 0.012) * 22.0), 0.0F, 1.0F);
            this.movementAmount = MathHelper.lerp(0.18F, this.movementAmount, targetMovement);
        } else {
            this.movementAmount = MathHelper.lerp(0.20F, this.movementAmount, 0.0F);
        }
    }

    private LegRuntime selectStepCandidate(double moveDistance, SupportState support) {
        int[] updateOrder = this.gaitProfile.type().getLegsInUpdateOrder(LEG_COUNT);
        boolean isWalking = moveDistance > 0.006 || this.debugWalkVelocity.lengthSquared() > 0.0004;
        boolean hasGroundSupport = support.groundedCount() > 0;
        LegRuntime best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int offset = 0; offset < updateOrder.length; offset++) {
            int orderedIndex = (this.gaitCycleCursor + offset) % updateOrder.length;
            LegRuntime leg = this.legs[updateOrder[orderedIndex]];
            if (!this.gaitProfile.type().canMoveLeg(leg, this.legs, this.gaitProfile, isWalking, hasGroundSupport)) {
                continue;
            }

            double targetDistance = Math.sqrt(leg.distanceToTargetSquared());
            double score = leg.triggerPhase * 1.45
                    + leg.projectedRestDistance * 0.55
                    + leg.triggerDistance * 1.15
                    + leg.comfortDistance * 0.70
                    + targetDistance * 1.05
                    + (leg.targetGrounded ? 0.0 : 0.80)
                    + (!leg.touchingGround ? 0.55 : 0.0);
            int diagonalGroup = SpyderLegIndexing.isDiagonal1(leg.index) ? 0 : 1;
            if (diagonalGroup != this.preferredStepGroup) {
                score -= 0.10;
            }
            if (leg.index == this.lastStepLeg) {
                score -= 0.24;
            }
            if (isBackRow(leg.index)) {
                score -= 0.05;
            } else if (isMiddleRow(leg.index)) {
                score += 0.04;
            }

            if (leg.outsideTrigger || !leg.targetGrounded || leg.triggerPhase > 0.92 || targetDistance > LEG_TARGET_STEP_TRIGGER) {
                this.gaitCycleCursor = (orderedIndex + 1) % updateOrder.length;
                return leg;
            }

            if (score > bestScore) {
                bestScore = score;
                best = leg;
            }
        }
        if (best != null) {
            for (int orderIndex = 0; orderIndex < updateOrder.length; orderIndex++) {
                if (updateOrder[orderIndex] == best.index) {
                    this.gaitCycleCursor = (orderIndex + 1) % updateOrder.length;
                    break;
                }
            }
        }
        return best;
    }

    private Vec3d computeDesiredDebugVelocity(Vec3d currentPos, Vec3d targetPos, Vec3d desiredNormal) {
        Vec3d toGoalVec = targetPos.subtract(currentPos);
        Vec3d planarGoal = toGoalVec.subtract(desiredNormal.multiply(toGoalVec.dotProduct(desiredNormal)));
        double planarDistance = planarGoal.length();
        if (planarDistance <= DEBUG_WALK_STOP_RADIUS || planarDistance < 0.0001) {
            return Vec3d.ZERO;
        }

        double slowRadius = 2.2;
        double speedFactor = MathHelper.clamp((float) (planarDistance / slowRadius), 0.42F, 1.0F);
        double targetSpeed = Math.max(this.gaitProfile.debugFollowSpeed() * speedFactor, Math.min(this.gaitProfile.debugFollowSpeed(), planarDistance * 0.42 + 0.025));
        return planarGoal.normalize().multiply(targetSpeed);
    }

    private Vec3d computeSupportedBodyPosition(BodyFrame frame, Vec3d desiredVelocity, SupportState support) {
        Vec3d supportDriven = support.groundedCount() == 0 ? frame.bodyPos : support.bodyAnchor();
        Vec3d supportDelta = supportDriven.subtract(frame.bodyPos);
        Vec3d supportVertical = frame.up.multiply(supportDelta.dotProduct(frame.up));
        Vec3d supportPlanar = supportDelta.subtract(supportVertical);
        Vec3d constrainedVelocity = desiredVelocity.multiply(Math.max(0.35, support.travelScale()));

        Vec3d planarVelocity = this.bodyVelocity.subtract(frame.up.multiply(this.bodyVelocity.dotProduct(frame.up)));
        Vec3d targetPlanarVelocity = constrainedVelocity;
        double supportDistance = supportPlanar.length();
        if (supportDistance > BODY_SUPPORT_PLANAR_LEEWAY) {
            Vec3d supportCorrection = supportPlanar.normalize().multiply((supportDistance - BODY_SUPPORT_PLANAR_LEEWAY) * BODY_PLANAR_SUPPORT_PULL);
            targetPlanarVelocity = targetPlanarVelocity.add(supportCorrection);
        }

        planarVelocity = planarVelocity.multiply(BODY_PLANAR_DAMPING);
        planarVelocity = approachVelocity(planarVelocity, targetPlanarVelocity, BODY_PLANAR_ACCEL + constrainedVelocity.length() * 0.20);

        double verticalSpeed = this.bodyVelocity.dotProduct(frame.up);
        verticalSpeed = verticalSpeed * BODY_VERTICAL_DAMPING + supportVertical.dotProduct(frame.up) * BODY_SPRING_VERTICAL;
        verticalSpeed = MathHelper.clamp((float) verticalSpeed, (float) -BODY_VERTICAL_MAX_SPEED, (float) BODY_VERTICAL_MAX_SPEED);

        this.bodyVelocity = planarVelocity.add(frame.up.multiply(verticalSpeed));
        double maxSpeed = Math.max(0.022, (BODY_MAX_SPEED + desiredVelocity.length() * 0.65) * Math.max(0.45, support.travelScale()));
        this.bodyVelocity = clampMagnitude(this.bodyVelocity, maxSpeed);

        Vec3d candidate = frame.bodyPos.add(this.bodyVelocity);
        double maxFollow = Math.max(0.020, BODY_MAX_FOLLOW_STEP * Math.max(0.45, support.travelScale()));
        double maxStep = Math.min(maxFollow, 0.024 + constrainedVelocity.length() * 1.0 + this.bodyVelocity.length() * 0.55);
        return moveTowards(frame.bodyPos, candidate, Math.max(0.018, maxStep));
    }

    private void updateBodyHeightDebug(SupportState support) {
        this.bodyHeightOffset = MathHelper.lerp(0.18F, this.bodyHeightOffset, (float) support.preferredHeight());
    }

    private void applySupportDebug(SupportState support, Vec3d fallbackBodyPos) {
        this.supportFront = support.front().equals(Vec3d.ZERO) ? fallbackBodyPos : support.front();
        this.supportBack = support.back().equals(Vec3d.ZERO) ? fallbackBodyPos : support.back();
        this.supportLeft = support.left().equals(Vec3d.ZERO) ? fallbackBodyPos : support.left();
        this.supportRight = support.right().equals(Vec3d.ZERO) ? fallbackBodyPos : support.right();
    }

    private void updateBodyTiltFromSupport(SupportState support, BodyFrame frame) {
        Vec3d frontLocal = toLocal(frame, support.front());
        Vec3d backLocal = toLocal(frame, support.back());
        Vec3d leftLocal = toLocal(frame, support.left());
        Vec3d rightLocal = toLocal(frame, support.right());

        float targetPitch = MathHelper.clamp((float) ((frontLocal.y - backLocal.y) * 36.0), -22.0F, 22.0F);
        float targetRoll = MathHelper.clamp((float) ((leftLocal.y - rightLocal.y) * 36.0), -22.0F, 22.0F);
        this.bodyPitchDegrees = MathHelper.lerp(0.08F, this.bodyPitchDegrees, targetPitch);
        this.bodyRollDegrees = MathHelper.lerp(0.08F, this.bodyRollDegrees, targetRoll);
    }

    private SupportState computeSupportState(BodyFrame frame, Vec3d desiredVelocity) {
        List<Vec3d> polygon = new ArrayList<>();
        Vec3d centroidSum = Vec3d.ZERO;
        Vec3d bodyAnchorSum = Vec3d.ZERO;
        Vec3d frontSum = Vec3d.ZERO;
        Vec3d backSum = Vec3d.ZERO;
        Vec3d leftSum = Vec3d.ZERO;
        Vec3d rightSum = Vec3d.ZERO;
        int groundedCount = 0;
        int frontCount = 0;
        int backCount = 0;
        int leftCount = 0;
        int rightCount = 0;
        int movingCount = 0;
        double maxTension = 0.0;

        for (LegRuntime leg : this.legs) {
            if (leg.moving) {
                movingCount++;
            }
        }

        for (int legIndex : SUPPORT_POLYGON_ORDER) {
            LegRuntime leg = this.legs[legIndex];
            if (!leg.isGrounded()) {
                continue;
            }

            polygon.add(leg.endEffector);
            centroidSum = centroidSum.add(leg.endEffector);
            bodyAnchorSum = bodyAnchorSum.add(
                    leg.endEffector
                            .subtract(frame.right.multiply(leg.restOffset.x))
                            .subtract(frame.forward.multiply(leg.restOffset.z))
                            .subtract(frame.up.multiply(leg.restOffset.y))
            );
            groundedCount++;
            maxTension = Math.max(maxTension, leg.triggerPhase);

            if (isFrontRow(legIndex) || isMiddleRow(legIndex)) {
                frontSum = frontSum.add(leg.endEffector);
                frontCount++;
            }
            if (!isFrontRow(legIndex)) {
                backSum = backSum.add(leg.endEffector);
                backCount++;
            }
            if (isLeftLeg(legIndex)) {
                leftSum = leftSum.add(leg.endEffector);
                leftCount++;
            } else {
                rightSum = rightSum.add(leg.endEffector);
                rightCount++;
            }
        }

        if (groundedCount == 0) {
            return new SupportState(
                    frame.bodyPos,
                    frame.bodyPos,
                    frame.up,
                    frame.bodyPos,
                    frame.bodyPos,
                    frame.bodyPos,
                    frame.bodyPos,
                    MathHelper.clamp(this.bodyHeightOffset, BODY_LIFT_MIN, BODY_LIFT_MAX),
                    1.0,
                    0.10,
                    0,
                    movingCount
            );
        }

        Vec3d centroid = centroidSum.multiply(1.0 / groundedCount);
        Vec3d bodyAnchor = bodyAnchorSum.multiply(1.0 / groundedCount);
        Vec3d normal = computePolygonNormal(polygon, frame.up);
        double preferredHeight = MathHelper.clamp((float) bodyAnchor.subtract(centroid).dotProduct(normal), BODY_LIFT_MIN, BODY_LIFT_MAX);
        double travelScale = computeSupportTravelScale(groundedCount, movingCount, maxTension, desiredVelocity.length());

        return new SupportState(
                centroid,
                bodyAnchor,
                normal,
                frontCount == 0 ? bodyAnchor : frontSum.multiply(1.0 / frontCount),
                backCount == 0 ? bodyAnchor : backSum.multiply(1.0 / backCount),
                leftCount == 0 ? bodyAnchor : leftSum.multiply(1.0 / leftCount),
                rightCount == 0 ? bodyAnchor : rightSum.multiply(1.0 / rightCount),
                preferredHeight,
                maxTension,
                travelScale,
                groundedCount,
                movingCount
        );
    }

    private Vec3d computePolygonNormal(List<Vec3d> polygon, Vec3d fallback) {
        if (polygon.size() < 3) {
            return fallback;
        }

        double nx = 0.0;
        double ny = 0.0;
        double nz = 0.0;
        for (int i = 0; i < polygon.size(); i++) {
            Vec3d current = polygon.get(i);
            Vec3d next = polygon.get((i + 1) % polygon.size());
            nx += (current.y - next.y) * (current.z + next.z);
            ny += (current.z - next.z) * (current.x + next.x);
            nz += (current.x - next.x) * (current.y + next.y);
        }

        Vec3d normal = new Vec3d(nx, ny, nz);
        if (normal.lengthSquared() < 0.0001) {
            return fallback;
        }
        normal = normal.normalize();
        if (normal.dotProduct(fallback) < 0.0) {
            normal = normal.negate();
        }
        return normal;
    }

    private double computeSupportTravelScale(int groundedCount, int movingCount, double maxTension, double desiredSpeed) {
        double plantedScale = MathHelper.clamp((float) (1.0 - Math.max(0.0, maxTension - 0.45) * 0.75), 0.35F, 1.0F);
        double supportScale = MathHelper.clamp((float) (groundedCount / 5.5), 0.42F, 1.0F);
        double movementScale = movingCount > 0
                ? Math.max(plantedScale, MathHelper.clamp((float) (0.56 + movingCount * 0.10), 0.56F, 1.0F))
                : plantedScale;
        double desiredBias = desiredSpeed > 0.006 ? 1.0 : 0.82;
        return MathHelper.clamp((float) (movementScale * supportScale * desiredBias), 0.32F, 1.0F);
    }

    private Vec3d averageSupport(int[] legIndexes) {
        Vec3d sum = Vec3d.ZERO;
        int count = 0;
        for (int legIndex : legIndexes) {
            LegRuntime leg = this.legs[legIndex];
            if (!leg.isGrounded() || leg.endEffector.equals(Vec3d.ZERO)) {
                continue;
            }
            sum = sum.add(leg.endEffector);
            count++;
        }
        return count == 0 ? Vec3d.ZERO : sum.multiply(1.0 / count);
    }

    private Vec3d computeSupportNormal(BodyFrame frame) {
        Vec3d frontBack = this.supportFront.subtract(this.supportBack);
        Vec3d rightLeft = this.supportRight.subtract(this.supportLeft);
        if (frontBack.lengthSquared() < 0.0001 || rightLeft.lengthSquared() < 0.0001) {
            return frame.up;
        }

        Vec3d normal = rightLeft.crossProduct(frontBack);
        if (normal.lengthSquared() < 0.0001) {
            return frame.up;
        }
        normal = normal.normalize();
        if (normal.dotProduct(frame.up) < 0.0) {
            normal = normal.negate();
        }
        return normal;
    }

    private void enforceLegTargetSeparation(BodyFrame frame) {
        Vec3d[] locals = new Vec3d[LEG_COUNT];
        int[] updateOrder = this.gaitProfile.type().getLegsInUpdateOrder(LEG_COUNT);
        for (int orderIndex = 0; orderIndex < updateOrder.length; orderIndex++) {
            int legIndex = updateOrder[orderIndex];
            Vec3d local = toLocal(frame, this.legs[legIndex].target);
            double nominalZ = LEG_FOOT_OFFSETS[legIndex].z;

            for (int other = 0; other < locals.length; other++) {
                if (other == legIndex || locals[other] == null || isLeftLeg(other) != isLeftLeg(legIndex)) {
                    continue;
                }

                double otherNominalZ = LEG_FOOT_OFFSETS[other].z;
                if (nominalZ > otherNominalZ) {
                    local = new Vec3d(local.x, local.y, Math.max(local.z, locals[other].z + LEG_PAIR_SEPARATION_Z));
                } else if (nominalZ < otherNominalZ) {
                    local = new Vec3d(local.x, local.y, Math.min(local.z, locals[other].z - LEG_PAIR_SEPARATION_Z));
                }
            }

            Vec3d nominal = LEG_FOOT_OFFSETS[legIndex];
            double clampedX = isLeftLeg(legIndex) ? Math.max(local.x, LEG_SIDE_CLEARANCE) : Math.min(local.x, -LEG_SIDE_CLEARANCE);
            double clampedZ = MathHelper.clamp(local.z, nominal.z - LEG_LANE_Z, nominal.z + LEG_LANE_Z);
            Vec3d adjusted = new Vec3d(clampedX, local.y, clampedZ);
            locals[legIndex] = adjusted;
            this.legs[legIndex].target = fromLocal(frame, adjusted);
        }
    }

    private Vec3d getSurfaceNormal(HitResult hit, Vec3d fallbackLook) {
        if (hit instanceof BlockHitResult blockHit && blockHit.getSide() != null) {
            return new Vec3d(blockHit.getSide().getOffsetX(), blockHit.getSide().getOffsetY(), blockHit.getSide().getOffsetZ()).normalize();
        }
        return normalizeOr(fallbackLook.multiply(-1.0), new Vec3d(0.0, 1.0, 0.0));
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

    private Vec3d normalizeOr(Vec3d vec, Vec3d fallback) {
        return vec.lengthSquared() < 0.0001 ? fallback : vec.normalize();
    }

    private BodyFrame createBodyFrame(Vec3d bodyPos, Vec3d up, Vec3d forward) {
        Vec3d safeUp = normalizeOr(up, new Vec3d(0.0, 1.0, 0.0));
        Vec3d safeForward = getProjectedForward(safeUp, forward);
        Vec3d right = normalizeOr(safeUp.crossProduct(safeForward), new Vec3d(1.0, 0.0, 0.0));
        return new BodyFrame(bodyPos, safeUp, safeForward, right);
    }

    private Vec3d toLocal(BodyFrame frame, Vec3d worldTarget) {
        Vec3d offset = worldTarget.subtract(frame.bodyPos);
        return new Vec3d(
                offset.dotProduct(frame.right),
                offset.dotProduct(frame.up),
                offset.dotProduct(frame.forward)
        );
    }

    private Vec3d fromLocal(BodyFrame frame, Vec3d local) {
        return frame.bodyPos
                .add(frame.right.multiply(local.x))
                .add(frame.up.multiply(local.y))
                .add(frame.forward.multiply(local.z));
    }

    private Vec3d clampLegTarget(BodyFrame frame, int legIndex, Vec3d worldTarget) {
        Vec3d local = clampLegTargetLocal(frame, legIndex, worldTarget);
        return fromLocal(frame, local);
    }

    private Vec3d clampLegTargetLocal(BodyFrame frame, int legIndex, Vec3d worldTarget) {
        Vec3d local = toLocal(frame, worldTarget);
        Vec3d nominal = LEG_FOOT_OFFSETS[legIndex];
        Vec3d root = LEG_ROOT_OFFSETS[legIndex];

        double clampedX = MathHelper.clamp(local.x, nominal.x - LEG_LANE_X, nominal.x + LEG_LANE_X);
        double clampedY = MathHelper.clamp(local.y, nominal.y - LEG_LANE_Y_DOWN, nominal.y + LEG_LANE_Y_UP);
        double clampedZ = MathHelper.clamp(local.z, nominal.z - LEG_LANE_Z, nominal.z + LEG_LANE_Z);
        if (isLeftLeg(legIndex)) {
            clampedX = Math.max(clampedX, LEG_SIDE_CLEARANCE);
        } else {
            clampedX = Math.min(clampedX, -LEG_SIDE_CLEARANCE);
        }

        Vec3d clampedLocal = new Vec3d(clampedX, clampedY, clampedZ);
        Vec3d fromRoot = clampedLocal.subtract(root);
        double fromRootLength = fromRoot.length();
        if (fromRootLength > LEG_MAX_ROOT_DISTANCE) {
            clampedLocal = root.add(fromRoot.normalize().multiply(LEG_MAX_ROOT_DISTANCE));
        }
        return clampedLocal;
    }

    private GroundSample sampleGroundGrid(Vec3d scanCenter, Vec3d up, Vec3d right, Vec3d forward) {
        Vec3d[] offsets = new Vec3d[]{
                Vec3d.ZERO,
                right.multiply(LEG_SCAN_GRID_SPACING),
                right.multiply(-LEG_SCAN_GRID_SPACING),
                forward.multiply(LEG_SCAN_GRID_SPACING),
                forward.multiply(-LEG_SCAN_GRID_SPACING),
                right.multiply(LEG_SCAN_GRID_SPACING).add(forward.multiply(LEG_SCAN_GRID_SPACING)),
                right.multiply(LEG_SCAN_GRID_SPACING).add(forward.multiply(-LEG_SCAN_GRID_SPACING)),
                right.multiply(-LEG_SCAN_GRID_SPACING).add(forward.multiply(LEG_SCAN_GRID_SPACING)),
                right.multiply(-LEG_SCAN_GRID_SPACING).add(forward.multiply(-LEG_SCAN_GRID_SPACING))
        };

        GroundSample best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Vec3d offset : offsets) {
            GroundSample candidate = sampleGround(scanCenter.add(offset), up);
            if (!candidate.grounded) {
                continue;
            }
            double distance = candidate.position.squaredDistanceTo(scanCenter);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best != null ? best : new GroundSample(scanCenter, up, false);
    }

    private GroundSample sampleGround(Vec3d pointOnPlane, Vec3d up) {
        Vec3d start = pointOnPlane.add(up.multiply(LEG_SCAN_UP));
        Vec3d end = pointOnPlane.subtract(up.multiply(LEG_SCAN_DOWN));
        HitResult hit = this.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                this
        ));

        if (hit instanceof BlockHitResult blockHit) {
            Vec3d normal = new Vec3d(blockHit.getSide().getOffsetX(), blockHit.getSide().getOffsetY(), blockHit.getSide().getOffsetZ()).normalize();
            return new GroundSample(hit.getPos().add(normal.multiply(LEG_SURFACE_OFFSET)), normal, true);
        }

        return new GroundSample(pointOnPlane, up, false);
    }

    private Vec3d moveTowards(Vec3d current, Vec3d target, double maxDistance) {
        Vec3d delta = target.subtract(current);
        double distance = delta.length();
        if (distance <= maxDistance || distance < 0.0001) {
            return target;
        }
        return current.add(delta.multiply(maxDistance / distance));
    }

    private Vec3d approachVelocity(Vec3d current, Vec3d target, double acceleration) {
        Vec3d delta = target.subtract(current);
        double distance = delta.length();
        if (distance <= acceleration || distance < 0.0001) {
            return target;
        }
        return current.add(delta.multiply(acceleration / distance));
    }

    private Vec3d clampMagnitude(Vec3d vec, double maxLength) {
        double length = vec.length();
        if (length <= maxLength || length < 0.0001) {
            return vec;
        }
        return vec.multiply(maxLength / length);
    }

    private double horizontalDistance(Vec3d a, Vec3d b, Vec3d up) {
        Vec3d delta = b.subtract(a);
        Vec3d planar = delta.subtract(up.multiply(delta.dotProduct(up)));
        return planar.length();
    }

    private boolean canStartStep(int legIndex) {
        LegRuntime candidate = this.legs[legIndex];
        if (candidate.moving) {
            return false;
        }

        for (LegRuntime leg : this.legs) {
            if (leg.index != legIndex && leg.moving) {
                return false;
            }
        }

        if (!isAnyLegSupporting()) {
            return true;
        }

        for (int adjacent : getAdjacentLegs(legIndex)) {
            if (!isValidLeg(adjacent)) {
                continue;
            }
            LegRuntime adjacentLeg = this.legs[adjacent];
            if (adjacentLeg.moving || !adjacentLeg.isGrounded()) {
                return false;
            }
            if (adjacentLeg.timeSinceStopMove < CROSS_PAIR_COOLDOWN) {
                return false;
            }
        }

        for (int diagonal : getDiagonalLegs(legIndex)) {
            if (!isValidLeg(diagonal)) {
                continue;
            }
            if (this.legs[diagonal].timeSinceBeginMove < SAME_PAIR_COOLDOWN) {
                return false;
            }
        }

        return true;
    }

    private boolean isAnyLegSupporting() {
        for (LegRuntime leg : this.legs) {
            if (leg.isGrounded()) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidLeg(int legIndex) {
        return legIndex >= 0 && legIndex < LEG_COUNT;
    }

    private boolean isLeftLeg(int legIndex) {
        return legIndex % 2 == 0;
    }

    private boolean isFrontRow(int legIndex) {
        return legIndex == 0 || legIndex == 1;
    }

    private boolean isBackRow(int legIndex) {
        return legIndex == 6 || legIndex == 7;
    }

    private boolean isMiddleRow(int legIndex) {
        return legIndex >= 2 && legIndex <= 5;
    }

    private int frontLeg(int legIndex) {
        return legIndex - 2;
    }

    private int backLeg(int legIndex) {
        return legIndex + 2;
    }

    private int horizontalLeg(int legIndex) {
        return isLeftLeg(legIndex) ? legIndex + 1 : legIndex - 1;
    }

    private int diagonalFrontLeg(int legIndex) {
        return isLeftLeg(legIndex) ? legIndex - 1 : legIndex - 3;
    }

    private int diagonalBackLeg(int legIndex) {
        return isLeftLeg(legIndex) ? legIndex + 3 : legIndex + 1;
    }

    private int[] getDiagonalLegs(int legIndex) {
        return new int[]{diagonalFrontLeg(legIndex), diagonalBackLeg(legIndex)};
    }

    private int[] getAdjacentLegs(int legIndex) {
        return new int[]{frontLeg(legIndex), backLeg(legIndex), horizontalLeg(legIndex)};
    }

    private boolean isHoldingDebugStick(PlayerEntity player) {
        return player.getMainHandStack().isOf(ModItems.SPYDER_DEBUG) || player.getOffHandStack().isOf(ModItems.SPYDER_DEBUG);
    }

    private ItemStack getDebugStick(PlayerEntity player) {
        if (player.getMainHandStack().isOf(ModItems.SPYDER_DEBUG)) {
            return player.getMainHandStack();
        }
        if (player.getOffHandStack().isOf(ModItems.SPYDER_DEBUG)) {
            return player.getOffHandStack();
        }
        return ItemStack.EMPTY;
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
        this.watchedPlayer = nbt.containsUuid("spyderWatchedPlayer") ? nbt.getUuid("spyderWatchedPlayer") : NIL_UUID;
        if (nbt.contains("spyderState")) {
            this.state = SpyderState.valueOf(nbt.getString("spyderState"));
        }
        this.bodyPitchDegrees = nbt.getFloat("spyderBodyPitch");
        this.bodyRollDegrees = nbt.getFloat("spyderBodyRoll");
        this.bodyHeightOffset = nbt.getFloat("spyderBodyHeightOffset");
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
        nbt.putFloat("spyderBodyHeightOffset", this.bodyHeightOffset);
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

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private record BodyFrame(Vec3d bodyPos, Vec3d up, Vec3d forward, Vec3d right) {
    }

    private record GroundSample(Vec3d position, Vec3d normal, boolean grounded) {
    }

    private record SupportState(Vec3d centroid,
                                Vec3d bodyAnchor,
                                Vec3d normal,
                                Vec3d front,
                                Vec3d back,
                                Vec3d left,
                                Vec3d right,
                                double preferredHeight,
                                double plantedTension,
                                double travelScale,
                                int groundedCount,
                                int movingCount) {
    }

    private record LegZone(Vec3d center, double horizontalRadius, double verticalRadius) {
        private boolean contains(Vec3d point, Vec3d up) {
            Vec3d delta = point.subtract(this.center);
            double vertical = Math.abs(delta.dotProduct(up));
            Vec3d planar = delta.subtract(up.multiply(delta.dotProduct(up)));
            return planar.length() <= this.horizontalRadius && vertical <= this.verticalRadius;
        }

        private double distanceOutside(Vec3d point, Vec3d up) {
            Vec3d delta = point.subtract(this.center);
            double vertical = Math.max(0.0, Math.abs(delta.dotProduct(up)) - this.verticalRadius);
            Vec3d planar = delta.subtract(up.multiply(delta.dotProduct(up)));
            double horizontal = Math.max(0.0, planar.length() - this.horizontalRadius);
            return horizontal + vertical;
        }
    }

    private record PlanarChain(double[] x, double[] y) {
    }

    private final class LegRuntime implements SpyderLegState {
        private final int index;
        private final Vec3d rootOffset;
        private final Vec3d restOffset;
        private final Vec3d[] joints = new Vec3d[LEG_SEGMENT_LENGTHS.length + 1];
        private LegZone triggerZone;
        private LegZone comfortZone;
        private Vec3d attachmentPosition = Vec3d.ZERO;
        private Vec3d restPosition = Vec3d.ZERO;
        private Vec3d lookAheadPosition = Vec3d.ZERO;
        private Vec3d target = Vec3d.ZERO;
        private Vec3d endEffector = Vec3d.ZERO;
        private Vec3d previousEndEffector = Vec3d.ZERO;
        private Vec3d groundPosition = Vec3d.ZERO;
        private Vec3d groundNormal = new Vec3d(0.0, 1.0, 0.0);
        private Vec3d stepStart = Vec3d.ZERO;
        private Vec3d stepEnd = Vec3d.ZERO;
        private boolean moving;
        private boolean touchingGround = true;
        private boolean targetGrounded = true;
        private int timeSinceBeginMove = 999;
        private int timeSinceStopMove = 999;
        private float stepProgress = 1.0F;
        private double urgency;
        private boolean outsideTrigger;
        private double triggerDistance;
        private double comfortDistance;
        private double triggerPhase;
        private double projectedRestDistance;

        private LegRuntime(int index) {
            this.index = index;
            this.rootOffset = LEG_ROOT_OFFSETS[index];
            this.restOffset = LEG_FOOT_OFFSETS[index];
            for (int i = 0; i < this.joints.length; i++) {
                this.joints[i] = Vec3d.ZERO;
            }
        }

        private void tickTimers() {
            this.timeSinceBeginMove++;
            this.timeSinceStopMove++;
        }

        @Override
        public boolean isGrounded() {
            return this.touchingGround && !this.moving;
        }

        @Override
        public int index() {
            return this.index;
        }

        @Override
        public boolean isMoving() {
            return this.moving;
        }

        @Override
        public boolean isDisabled() {
            return false;
        }

        @Override
        public boolean isOutsideTriggerZone() {
            return this.outsideTrigger;
        }

        @Override
        public boolean isTargetGrounded() {
            return this.targetGrounded;
        }

        @Override
        public int timeSinceBeginMove() {
            return this.timeSinceBeginMove;
        }

        @Override
        public int timeSinceStopMove() {
            return this.timeSinceStopMove;
        }

        @Override
        public double distanceToTargetSquared() {
            return this.endEffector.squaredDistanceTo(this.target);
        }

        private void updateMemo(BodyFrame frame, Vec3d locomotionDir) {
            this.attachmentPosition = fromLocal(frame, this.rootOffset);
            this.restPosition = fromLocal(frame, this.restOffset);
            double lookAheadFactor = isFrontRow(this.index) ? 0.95 : (isBackRow(this.index) ? 0.32 : 0.62);
            this.lookAheadPosition = this.restPosition.add(locomotionDir.multiply(LEG_LOOKAHEAD_DISTANCE * lookAheadFactor));

            double attachmentLift = Math.max(0.0, this.attachmentPosition.subtract(this.restPosition).dotProduct(frame.up));
            Vec3d comfortCenter = this.restPosition.add(frame.up.multiply(attachmentLift * 0.45));
            this.triggerZone = new LegZone(
                    this.restPosition,
                    LEG_TRIGGER_HORIZONTAL,
                    LEG_TRIGGER_VERTICAL
            );
            this.comfortZone = new LegZone(
                    comfortCenter,
                    LEG_TRIGGER_HORIZONTAL + LEG_COMFORT_HORIZONTAL + LEG_COMFORT_LONGITUDINAL,
                    LEG_TRIGGER_VERTICAL + LEG_COMFORT_VERTICAL + Math.min(attachmentLift, LEG_COMFORT_FOLLOW_VERTICAL)
            );

            if (this.target.equals(Vec3d.ZERO)) {
                GroundSample seeded = sampleGroundGrid(this.lookAheadPosition, frame.up, frame.right, frame.forward);
                this.targetGrounded = seeded.grounded;
                this.target = clampLegTarget(frame, this.index, seeded.grounded ? seeded.position : this.restPosition);
                this.endEffector = this.target;
                this.groundPosition = this.target;
                this.groundNormal = seeded.normal;
                solveJoints(frame);
            }
        }

        private void refreshTarget(BodyFrame frame, Vec3d desiredVelocity) {
            GroundSample ground = sampleGroundGrid(this.lookAheadPosition, frame.up, frame.right, frame.forward);
            Vec3d desiredWorld = ground.grounded ? ground.position : this.restPosition;
            Vec3d desiredTarget = clampLegTarget(frame, this.index, desiredWorld);
            this.groundNormal = ground.normal;
            this.targetGrounded = ground.grounded;
            if (this.target.equals(Vec3d.ZERO)) {
                this.target = desiredTarget;
            } else {
                double targetBlend = this.moving ? LEG_MOVING_TARGET_BLEND : LEG_PLANTED_TARGET_BLEND;
                if (!ground.grounded || this.comfortDistance > 0.02) {
                    targetBlend = Math.max(targetBlend, LEG_TARGET_FOLLOW_UNCOMFORTABLE);
                }
                this.target = this.target.lerp(desiredTarget, targetBlend);
            }

            boolean outsideTriggerZone = this.triggerZone != null && !this.triggerZone.contains(this.endEffector, frame.up);
            double horizontalRestDrift = horizontalDistance(this.endEffector, this.restPosition, frame.up);
            double verticalRestDrift = Math.abs(this.endEffector.subtract(this.restPosition).dotProduct(frame.up));
            double horizontalStretch = horizontalDistance(this.endEffector, desiredTarget, frame.up);
            double verticalStretch = Math.abs(this.endEffector.subtract(desiredTarget).dotProduct(frame.up));
            double rootDistance = this.attachmentPosition.distanceTo(this.endEffector);
            this.comfortDistance = this.comfortZone == null ? 0.0 : this.comfortZone.distanceOutside(this.endEffector, frame.up);
            this.triggerDistance = this.triggerZone == null ? 0.0 : this.triggerZone.distanceOutside(this.endEffector, frame.up);
            this.outsideTrigger = outsideTriggerZone
                    || horizontalStretch > LEG_TARGET_STEP_TRIGGER
                    || verticalStretch > LEG_TRIGGER_VERTICAL * 0.90
                    || rootDistance > 1.28;
            double horizontalPhase = this.triggerZone == null ? 0.0 : horizontalRestDrift / Math.max(0.0001, this.triggerZone.horizontalRadius());
            double verticalPhase = this.triggerZone == null ? 0.0 : verticalRestDrift / Math.max(0.0001, this.triggerZone.verticalRadius());
            double targetHorizontalPhase = horizontalStretch / Math.max(0.0001, LEG_TARGET_STEP_TRIGGER);
            double targetVerticalPhase = verticalStretch / Math.max(0.0001, LEG_TRIGGER_VERTICAL);
            this.triggerPhase = Math.max(Math.max(horizontalPhase, verticalPhase), Math.max(targetHorizontalPhase * 0.90, targetVerticalPhase));
            this.projectedRestDistance = horizontalRestDrift + verticalRestDrift * 0.65;

            this.urgency = this.triggerPhase * 1.10
                    + horizontalStretch * 0.62
                    + verticalStretch * 0.92
                    + this.triggerDistance * 1.35
                    + this.comfortDistance * 0.95
                    + Math.max(0.0, rootDistance - 1.02) * 0.80
                    + desiredVelocity.length() * 0.45;
        }

        private boolean wantsStep(double moveDistance) {
            if (this.moving) {
                return false;
            }
            if (moveDistance < 0.005) {
                return this.outsideTrigger || this.urgency > 0.30;
            }
            return this.outsideTrigger || this.urgency > 0.24;
        }

        private void beginStep() {
            this.moving = true;
            this.touchingGround = false;
            this.stepProgress = 0.0F;
            this.stepStart = this.endEffector;
            this.stepEnd = this.target;
            this.timeSinceBeginMove = 0;
        }

        private void updateMovement(BodyFrame frame, double moveDistance) {
            this.previousEndEffector = this.endEffector;
            if (this.moving) {
                float gaitSpeed = (float) SpyderControllerEntity.this.gaitProfile.legMoveSpeed();
                float stepSpeed = MathHelper.clamp(gaitSpeed * (moveDistance > 0.01 ? 0.95F : 0.72F), LEG_STEP_MIN_SPEED, LEG_STEP_MAX_SPEED + 0.04F);
                this.stepProgress = Math.min(1.0F, this.stepProgress + stepSpeed);
                this.stepEnd = this.stepEnd.lerp(this.target, LEG_STEP_TARGET_BLEND);

                GroundSample movingGround = sampleGround(this.stepEnd, frame.up);
                Vec3d groundedStepEnd = clampLegTarget(frame, this.index, movingGround.grounded ? movingGround.position : this.stepEnd);

                float eased = MathHelper.clamp(this.stepProgress, 0.0F, 1.0F);
                float smooth = eased * eased * (3.0F - 2.0F * eased);
                Vec3d basePath = this.stepStart.lerp(groundedStepEnd, smooth);
                Vec3d carriedPath = basePath.add(frame.forward.multiply(moveDistance * LEG_CARRY_FRACTION * (1.0F - smooth)));
                double arcHeight = Math.sin(smooth * Math.PI) * (LEG_STEP_ARC_HEIGHT + Math.min(0.12, moveDistance * 0.8));
                this.endEffector = clampLegTarget(frame, this.index, carriedPath.add(frame.up.multiply(arcHeight)));

                if (this.stepProgress >= 1.0F || this.endEffector.distanceTo(groundedStepEnd) < 0.045) {
                    GroundSample landed = sampleGround(groundedStepEnd, frame.up);
                    this.endEffector = clampLegTarget(frame, this.index, landed.grounded ? landed.position : groundedStepEnd);
                    this.groundPosition = this.endEffector;
                    this.groundNormal = landed.normal;
                    this.moving = false;
                    this.touchingGround = true;
                    this.timeSinceStopMove = 0;
                }
                return;
            }

            if (moveDistance < 0.01) {
                GroundSample settledGround = sampleGround(this.endEffector.lerp(this.target, LEG_IDLE_SETTLE), frame.up);
                Vec3d planted = clampLegTarget(frame, this.index, settledGround.grounded ? settledGround.position : this.endEffector);
                this.endEffector = this.endEffector.lerp(planted, 0.55);
                this.groundPosition = this.endEffector;
                this.groundNormal = settledGround.normal;
            } else {
                GroundSample plantedGround = sampleGround(this.endEffector, frame.up);
                if (plantedGround.grounded) {
                    Vec3d planted = clampLegTarget(frame, this.index, plantedGround.position);
                    this.endEffector = this.endEffector.lerp(planted, 0.22);
                    this.groundPosition = this.endEffector;
                    this.groundNormal = plantedGround.normal;
                }
            }
            this.touchingGround = true;
        }

        private void solveJoints(BodyFrame frame) {
            Vec3d root = this.attachmentPosition;
            Vec3d tip = this.endEffector;
            Vec3d rootToTip = tip.subtract(root);
            if (rootToTip.lengthSquared() < 0.0001) {
                for (int i = 0; i < this.joints.length; i++) {
                    this.joints[i] = root;
                }
                return;
            }

            Vec3d outward = isLeftLeg(this.index) ? frame.right : frame.right.negate();
            double foreAftBias = isFrontRow(this.index) ? 0.55 : (isBackRow(this.index) ? -0.42 : (this.index < 4 ? 0.18 : -0.18));
            Vec3d bendGuide = normalizeOr(outward.add(frame.forward.multiply(foreAftBias)), outward);
            Vec3d planeNormal = rootToTip.crossProduct(bendGuide);
            if (planeNormal.lengthSquared() < 0.0001) {
                planeNormal = outward.crossProduct(frame.up);
            }
            planeNormal = normalizeOr(planeNormal, frame.forward);

            Vec3d planeForward = rootToTip.subtract(planeNormal.multiply(rootToTip.dotProduct(planeNormal)));
            planeForward = normalizeOr(planeForward, outward);
            Vec3d planeUp = normalizeOr(planeNormal.crossProduct(planeForward), frame.up);
            if (planeUp.dotProduct(frame.up) < 0.0) {
                planeUp = planeUp.negate();
            }

            double targetForward = rootToTip.dotProduct(planeForward);
            double targetUp = rootToTip.dotProduct(planeUp);
            PlanarChain chain = solvePlanarFabrik(targetForward, targetUp, this.index);

            this.joints[0] = root;
            for (int i = 1; i < this.joints.length; i++) {
                this.joints[i] = root
                        .add(planeForward.multiply(chain.x[i]))
                        .add(planeUp.multiply(chain.y[i]));
            }
            this.joints[this.joints.length - 1] = tip;
        }
    }

    private PlanarChain solvePlanarFabrik(double targetForward, double targetUp, int legIndex) {
        double[] x = new double[LEG_SEGMENT_LENGTHS.length + 1];
        double[] y = new double[LEG_SEGMENT_LENGTHS.length + 1];
        float[] rest = LEG_REST_ANGLES[MathHelper.clamp(legIndex, 0, LEG_REST_ANGLES.length - 1)];

        double worldAngle = 0.0;
        for (int i = 0; i < LEG_SEGMENT_LENGTHS.length; i++) {
            worldAngle += rest[i];
            double radians = Math.toRadians(worldAngle);
            x[i + 1] = x[i] + Math.cos(radians) * LEG_SEGMENT_LENGTHS[i];
            y[i + 1] = y[i] + Math.sin(radians) * LEG_SEGMENT_LENGTHS[i];
        }

        double totalLength = 0.0;
        for (double segmentLength : LEG_SEGMENT_LENGTHS) {
            totalLength += segmentLength;
        }

        double targetDistance = Math.sqrt(targetForward * targetForward + targetUp * targetUp);
        if (targetDistance >= totalLength - 0.0001) {
            double dirX = targetForward / Math.max(0.0001, targetDistance);
            double dirY = targetUp / Math.max(0.0001, targetDistance);
            x[0] = 0.0;
            y[0] = 0.0;
            for (int i = 0; i < LEG_SEGMENT_LENGTHS.length; i++) {
                x[i + 1] = x[i] + dirX * LEG_SEGMENT_LENGTHS[i];
                y[i + 1] = y[i] + dirY * LEG_SEGMENT_LENGTHS[i];
            }
            return new PlanarChain(x, y);
        }

        for (int iteration = 0; iteration < 16; iteration++) {
            x[LEG_SEGMENT_LENGTHS.length] = targetForward;
            y[LEG_SEGMENT_LENGTHS.length] = targetUp;

            for (int i = LEG_SEGMENT_LENGTHS.length - 1; i >= 0; i--) {
                double dx = x[i] - x[i + 1];
                double dy = y[i] - y[i + 1];
                double distance = Math.max(0.0001, Math.sqrt(dx * dx + dy * dy));
                double scale = LEG_SEGMENT_LENGTHS[i] / distance;
                x[i] = x[i + 1] + dx * scale;
                y[i] = y[i + 1] + dy * scale;
            }

            x[0] = 0.0;
            y[0] = 0.0;
            for (int i = 0; i < LEG_SEGMENT_LENGTHS.length; i++) {
                double dx = x[i + 1] - x[i];
                double dy = y[i + 1] - y[i];
                double distance = Math.max(0.0001, Math.sqrt(dx * dx + dy * dy));
                double scale = LEG_SEGMENT_LENGTHS[i] / distance;
                x[i + 1] = x[i] + dx * scale;
                y[i + 1] = y[i] + dy * scale;
            }
        }

        return new PlanarChain(x, y);
    }
}
