package com.kodu16.vsie.content.controlseat.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AntiGravityControllerTest {
    private static final double EPSILON = 1.0E-10D;

    @Test
    void removesSmallUpwardDriftWhileCancellingSableGravity() {
        double mass = 20.0D;
        double timeStep = 0.025D;
        double[] gravity = {0.0D, -11.0D, 0.0D};
        double[] velocity = {0.0D, 0.006D, 0.0D};

        AntiGravityController.Impulse impulse = calculateImpulse(gravity, velocity, mass, timeStep, true);
        double nextVerticalVelocity = velocity[1] + impulse.y() / mass + gravity[1] * timeStep;

        assertEquals(0.0D, nextVerticalVelocity, EPSILON);
    }

    @Test
    void followsTheGravityVectorProvidedBySable() {
        double mass = 8.0D;
        double timeStep = 0.025D;
        double[] gravity = {3.0D, -4.0D, 0.0D};
        double[] gravityDirection = {0.6D, -0.8D, 0.0D};
        double[] velocity = {-0.012D, 0.016D, 0.0D};

        AntiGravityController.Impulse impulse = calculateImpulse(gravity, velocity, mass, timeStep, true);
        double nextX = velocity[0] + impulse.x() / mass + gravity[0] * timeStep;
        double nextY = velocity[1] + impulse.y() / mass + gravity[1] * timeStep;
        double nextZ = velocity[2] + impulse.z() / mass + gravity[2] * timeStep;

        assertEquals(0.0D,
                nextX * gravityDirection[0] + nextY * gravityDirection[1] + nextZ * gravityDirection[2],
                EPSILON);
    }

    @Test
    void preservesVerticalMotionDuringManualInput() {
        double mass = 10.0D;
        double timeStep = 0.025D;
        double[] gravity = {0.0D, -11.0D, 0.0D};
        double[] velocity = {0.0D, 0.4D, 0.0D};

        AntiGravityController.Impulse impulse = calculateImpulse(gravity, velocity, mass, timeStep, false);
        double nextVerticalVelocity = velocity[1] + impulse.y() / mass + gravity[1] * timeStep;

        assertEquals(velocity[1], nextVerticalVelocity, EPSILON);
    }

    @Test
    void accountsForFlightAssistImpulseAlreadyQueuedThisSubstep() {
        double mass = 20.0D;
        double timeStep = 0.025D;
        double[] gravity = {0.0D, -11.0D, 0.0D};
        double[] velocity = {0.0D, 0.006D, 0.0D};
        double[] pendingImpulse = {0.0D, -0.02D, 0.0D};

        AntiGravityController.Impulse impulse = calculateImpulse(
                gravity, velocity, pendingImpulse, mass, timeStep, true
        );
        double nextVerticalVelocity = velocity[1]
                + (pendingImpulse[1] + impulse.y()) / mass
                + gravity[1] * timeStep;

        assertEquals(0.0D, nextVerticalVelocity, EPSILON);
    }

    private static AntiGravityController.Impulse calculateImpulse(
            double[] gravity,
            double[] velocity,
            double mass,
            double timeStep,
            boolean holdGravityAxisVelocity
    ) {
        return calculateImpulse(gravity, velocity, new double[3], mass, timeStep, holdGravityAxisVelocity);
    }

    private static AntiGravityController.Impulse calculateImpulse(
            double[] gravity,
            double[] velocity,
            double[] pendingImpulse,
            double mass,
            double timeStep,
            boolean holdGravityAxisVelocity
    ) {
        return AntiGravityController.calculateImpulse(
                gravity[0], gravity[1], gravity[2],
                velocity[0], velocity[1], velocity[2],
                pendingImpulse[0], pendingImpulse[1], pendingImpulse[2],
                mass, timeStep, holdGravityAxisVelocity
        );
    }
}
