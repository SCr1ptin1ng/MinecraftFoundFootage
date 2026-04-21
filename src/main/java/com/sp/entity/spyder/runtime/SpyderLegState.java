package com.sp.entity.spyder.runtime;

public interface SpyderLegState {
    int index();

    boolean isGrounded();

    boolean isMoving();

    boolean isDisabled();

    boolean isOutsideTriggerZone();

    boolean isTargetGrounded();

    int timeSinceBeginMove();

    int timeSinceStopMove();

    double distanceToTargetSquared();
}
