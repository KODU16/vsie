package com.kodu16.vsie.foundation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
    public static Vec3 toVec3(Vector3d a) {
        return new Vec3(a.x,a.y,a.z);
    }
    public static Vector3d toVector3d(Vec3 a) {
        return new Vector3d(a.x,a.y,a.z);
    }

    public static double Distance(Vector3d a, Vector3d b) {
        return Math.sqrt(Math.pow(a.x-b.x,2) + Math.pow(a.y-b.y,2) + Math.pow(a.z-b.z,2));
    }

    public static float angleBetween(Vec3 a, Vec3 b) {
        // 1. 计算点积
        float dot = (float) (a.x * b.x + a.y * b.y + a.z * b.z);

        // 2. 计算两个向量的模长
        float lenA = (float) Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
        float lenB = (float) Math.sqrt(b.x * b.x + b.y * b.y + b.z * b.z);

        // 3. 防止除以零的情况
        if (lenA == 0 || lenB == 0) {
            return 0;   // 或抛异常，看需求
        }

        // 4. cosθ = dot / (|a| * |b|)
        float cosTheta = dot / (lenA * lenB);

        // 5. 由于浮点误差，cosTheta 可能略微超过 [-1,1]，需要钳制
        cosTheta = Math.max(-1.0f, Math.min(1.0f, cosTheta));

        // 6. 转成弧度角
        return (float) Math.acos(cosTheta);
    }


    //A在B和C的平面上投影与B的夹角
    public static double projectionAngleToB_rad_signed(
            Vector3d A,
            Vector3d B_unit,    // 已单位化
            Vector3d C_unit     // 已单位化，不与 B 平行
    ) {
        // 1. 构造平面内的正交基
        Vector3d u = new Vector3d(B_unit);

        // 计算 C 在 B 方向上的投影长度
        double cProjLen = C_unit.dot(u);

        // v = C - (C·u) u
        Vector3d v = new Vector3d(C_unit).sub(u.x * cProjLen, u.y * cProjLen, u.z * cProjLen);

        // 单位化 v
        double vLen = v.length();
        v.div(vLen);   // 现在 v 是单位向量，且垂直于 u
        // 2. 计算 A 在平面上的投影（其实就是原点到 A 的向量在平面上的分量）
        double x = A.dot(u);   // 在 B 方向上的分量
        double y = A.dot(v);   // 在垂直方向上的分量（v 方向）
        // 3. 用 atan2 得到带符号角度（弧度）
        double angleRad = Math.atan2(y, x);

        return angleRad;
    }
}
