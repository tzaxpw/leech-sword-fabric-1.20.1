package com.yuheng.leech.client;

public final class LeechAirMovement {
    private static final float AIR_DRAG = 0.91F;
    private static final float NORMAL_GROUND_DRAG = 0.6F * AIR_DRAG;

    private LeechAirMovement() {
    }

    public static float accelerationFor(float movementSpeed) {
        // With vanilla air drag, this reaches the same steady horizontal speed
        // as walking/sprinting on ordinary ground (slipperiness 0.6).
        // movementSpeed already includes the sword bonus, sprinting, and effects.
        return movementSpeed * (1.0F - AIR_DRAG) / (1.0F - NORMAL_GROUND_DRAG);
    }
}
