package com.sp.entity.spyder.runtime;

public final class SpyderGaitProfile {
    private final SpyderGaitType type;
    private final double bodyHeight;
    private final double maxSpeed;
    private final double debugFollowSpeed;
    private final double legMoveSpeed;
    private final int samePairCooldown;
    private final int crossPairCooldown;

    public SpyderGaitProfile(SpyderGaitType type,
                             double bodyHeight,
                             double maxSpeed,
                             double debugFollowSpeed,
                             double legMoveSpeed,
                             int samePairCooldown,
                             int crossPairCooldown) {
        this.type = type;
        this.bodyHeight = bodyHeight;
        this.maxSpeed = maxSpeed;
        this.debugFollowSpeed = debugFollowSpeed;
        this.legMoveSpeed = legMoveSpeed;
        this.samePairCooldown = samePairCooldown;
        this.crossPairCooldown = crossPairCooldown;
    }

    public static SpyderGaitProfile defaultWalk() {
        return new SpyderGaitProfile(
                SpyderGaitType.WALK,
                0.72,
                0.18,
                0.16,
                0.23,
                1,
                2
        );
    }

    public SpyderGaitType type() {
        return this.type;
    }

    public double bodyHeight() {
        return this.bodyHeight;
    }

    public double maxSpeed() {
        return this.maxSpeed;
    }

    public double debugFollowSpeed() {
        return this.debugFollowSpeed;
    }

    public double legMoveSpeed() {
        return this.legMoveSpeed;
    }

    public int samePairCooldown() {
        return this.samePairCooldown;
    }

    public int crossPairCooldown() {
        return this.crossPairCooldown;
    }
}
