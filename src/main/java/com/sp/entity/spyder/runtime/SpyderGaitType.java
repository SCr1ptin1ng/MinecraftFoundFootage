package com.sp.entity.spyder.runtime;

public enum SpyderGaitType {
    WALK;

    public int[] getLegsInUpdateOrder(int legCount) {
        return SpyderLegIndexing.walkUpdateOrder(legCount);
    }

    public boolean canMoveLeg(SpyderLegState leg,
                              SpyderLegState[] legs,
                              SpyderGaitProfile gait,
                              boolean isWalking,
                              boolean hasGroundSupport) {
        if (leg.isMoving()) {
            return false;
        }

        if (!leg.isTargetGrounded()) {
            return true;
        }

        for (int adjacentIndex : SpyderLegIndexing.adjacent(leg.index())) {
            if (!isValid(legs, adjacentIndex)) {
                continue;
            }
            SpyderLegState adjacent = legs[adjacentIndex];
            if (adjacent.isDisabled() || !adjacent.isTargetGrounded()) {
                continue;
            }
            if (!adjacent.isGrounded()) {
                return false;
            }
            if (adjacent.timeSinceStopMove() < gait.crossPairCooldown()) {
                return false;
            }
        }

        for (int diagonalIndex : SpyderLegIndexing.diagonal(leg.index())) {
            if (!isValid(legs, diagonalIndex)) {
                continue;
            }
            SpyderLegState diagonal = legs[diagonalIndex];
            if (diagonal.isDisabled() || !diagonal.isTargetGrounded()) {
                continue;
            }
            if (diagonal.timeSinceBeginMove() < gait.samePairCooldown()) {
                return false;
            }
        }

        boolean wantsToMove = leg.isOutsideTriggerZone() || !leg.isGrounded() || leg.distanceToTargetSquared() > 0.0324;
        boolean alreadyAtTarget = leg.distanceToTargetSquared() < 0.01;
        boolean supported = hasGroundSupport;

        if (!isWalking && !leg.isOutsideTriggerZone()) {
            return false;
        }

        return wantsToMove && !alreadyAtTarget && supported;
    }

    private static boolean isValid(SpyderLegState[] legs, int index) {
        return index >= 0 && index < legs.length;
    }
}
