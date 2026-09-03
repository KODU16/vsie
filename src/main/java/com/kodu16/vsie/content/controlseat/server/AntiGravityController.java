package com.kodu16.vsie.content.controlseat.server;

final class AntiGravityController {
    private static final double GRAVITY_EPSILON_SQUARED = 1.0E-12D;

    private AntiGravityController() {
    }

    static Impulse calculateImpulse(
            double gravityX,
            double gravityY,
            double gravityZ,
            double velocityX,
            double velocityY,
            double velocityZ,
            double pendingImpulseX,
            double pendingImpulseY,
            double pendingImpulseZ,
            double mass,
            double timeStep,
            boolean holdGravityAxisVelocity
    ) {
        double gravityLengthSquared = gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ;
        if (!Double.isFinite(gravityLengthSquared)
                || gravityLengthSquared <= GRAVITY_EPSILON_SQUARED
                || !Double.isFinite(mass)
                || mass <= 0.0D
                || !Double.isFinite(timeStep)
                || timeStep <= 0.0D) {
            return Impulse.ZERO;
        }

        double impulseX = -mass * gravityX * timeStep;
        double impulseY = -mass * gravityY * timeStep;
        double impulseZ = -mass * gravityZ * timeStep;
        if (holdGravityAxisVelocity
                && Double.isFinite(velocityX)
                && Double.isFinite(velocityY)
                && Double.isFinite(velocityZ)
                && Double.isFinite(pendingImpulseX)
                && Double.isFinite(pendingImpulseY)
                && Double.isFinite(pendingImpulseZ)) {
            double inverseGravityLength = 1.0D / Math.sqrt(gravityLengthSquared);
            double directionX = gravityX * inverseGravityLength;
            double directionY = gravityY * inverseGravityLength;
            double directionZ = gravityZ * inverseGravityLength;
            double gravityAxisVelocity = velocityX * directionX + velocityY * directionY + velocityZ * directionZ
                    + (pendingImpulseX * directionX + pendingImpulseY * directionY + pendingImpulseZ * directionZ) / mass;
            // Function: remove even tiny residual drift instead of allowing a permanent velocity dead zone.
            impulseX -= mass * gravityAxisVelocity * directionX;
            impulseY -= mass * gravityAxisVelocity * directionY;
            impulseZ -= mass * gravityAxisVelocity * directionZ;
        }
        return new Impulse(impulseX, impulseY, impulseZ);
    }

    record Impulse(double x, double y, double z) {
        private static final Impulse ZERO = new Impulse(0.0D, 0.0D, 0.0D);
    }
}
