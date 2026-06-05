package com.kodu16.vsie.content.thruster;

import org.joml.Matrix3d;
import org.joml.Vector3d;

public class ThrusterData {
    // Function: coordAxis stores the thruster's installed local basis for force and flame calculations.
    public volatile Matrix3d coordAxis = new Matrix3d();
    public volatile Vector3d inputtorque;
    public volatile Vector3d inputforce;
    // Function: same-facing thrust sum lets each thruster normalize only against peers that can push the same way.
    public volatile double sameFacingMaxThrustSum;
    public volatile double throttle;

    public Vector3d getDirectionX() {
        return coordAxis.getColumn(0, new Vector3d());
    }

    public Vector3d getDirectionY() {
        return coordAxis.getColumn(1, new Vector3d());
    }

    public Vector3d getDirectionZ() {
        return coordAxis.getColumn(2, new Vector3d());
    }

    public Matrix3d getCoordAxis() {
        return coordAxis;
    }

    public Vector3d getInputtorque() {
        return inputtorque;
    }

    public Vector3d getInputforce() {
        return inputforce;
    }

    public double getSameFacingMaxThrustSum() {
        return sameFacingMaxThrustSum;
    }

    public double getThrottle() {
        return throttle;
    }

    public void setDirectionX(Vector3d direction) {
        this.coordAxis.setColumn(0, direction);
    }

    public void setDirectionY(Vector3d direction) {
        this.coordAxis.setColumn(1, direction);
    }

    public void setDirectionZ(Vector3d direction) {
        this.coordAxis.setColumn(2, direction);
    }

    public void setCoordAxis(Matrix3d mat) {
        this.coordAxis = mat;
    }

    public void setInputtorque(Vector3d inputtorque) {
        this.inputtorque = inputtorque;
    }

    public void setInputforce(Vector3d inputforce) {
        this.inputforce = inputforce;
    }

    public void setSameFacingMaxThrustSum(double sameFacingMaxThrustSum) {
        this.sameFacingMaxThrustSum = sameFacingMaxThrustSum;
    }

    public void setThrottle(double throttle) {
        this.throttle = throttle;
    }
}
