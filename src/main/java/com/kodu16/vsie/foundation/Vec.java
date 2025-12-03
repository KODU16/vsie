package com.kodu16.vsie.foundation;

import org.joml.Matrix3d;
import org.joml.AxisAngle4d;
import org.joml.Vector3d;

public class Vec {

    // 计算从坐标系 b 到坐标系 c 的变换
    public static Vector3d transformToStandardBasis(Vector3d a, Vector3d b, Vector3d c, Vector3d d) {
        // 计算 d 在基底 a, b, c 下的坐标
        double xPrime = d.dot(a); // d 和 a 的点积
        double yPrime = d.dot(b); // d 和 b 的点积
        double zPrime = d.dot(c); // d 和 c 的点积

        // 返回新的向量 (x', y', z')
        return new Vector3d(xPrime, yPrime, zPrime);
    }

    public static Vector3d cross(Vector3d a, Vector3d b) {
        double newX = a.y * b.z - a.z * b.y;
        double newY = a.z * b.x - a.x * b.z;
        double newZ = a.x * b.y - a.y * b.x;
        return new Vector3d(newX, newY, newZ);
    }

    public static Vector3d VectorNormalization(Vector3d a) {
        if(a.length()==0){
            return new Vector3d(0,1,0);
        }
        return new Vector3d(a.x/a.length(), a.y/a.length(), a.z/a.length());
    }

    public static double Distance(Vector3d a, Vector3d b) {
        return Math.sqrt(Math.pow(a.x-b.x,2) + Math.pow(a.y-b.y,2) + Math.pow(a.z-b.z,2));
    }
}
