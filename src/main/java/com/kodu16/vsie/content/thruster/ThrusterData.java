package com.kodu16.vsie.content.thruster;

import org.joml.Vector3d;

public class ThrusterData {
    //仅服务端需要data，客户端只需要renderdata就行了
    //注意这里必须是所有推进器都需要用的共同data
    public volatile Vector3d direction;
    public volatile Vector3d directionX;
    public volatile Vector3d directionZ;
    public volatile Vector3d inputtorque;
    public volatile Vector3d inputforce;
    public volatile double throttle;
    public Vector3d getDirection() { return direction; }
    public Vector3d getDirectionX() { return directionX;}
    public Vector3d getDirectionZ() { return directionZ;}
    public Vector3d getInputtorque() { return inputtorque; }
    public Vector3d getInputforce() { return inputforce; }
    public double getThrottle() { return throttle; }

    public void setDirection(Vector3d direction) { this.direction = direction; }
    public void setDirectionX(Vector3d direction) { this.directionX = direction; }
    public void setDirectionZ(Vector3d direction) { this.directionZ = direction; }
    public void setInputtorque(Vector3d inputtorque) { this.inputtorque = inputtorque; }
    public void setInputforce(Vector3d inputforce) { this.inputforce = inputforce; }
    public void setThrottle(double throttle) { this.throttle = throttle; }
}
