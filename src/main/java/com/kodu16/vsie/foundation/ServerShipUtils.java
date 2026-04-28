package com.kodu16.vsie.foundation;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import javax.annotation.Nullable;

public final class ServerShipUtils {

    /**
     * 在 sublevel 重心施加力和力矩。
     *
     * @param subLevel    目标 ServerSubLevel
     * @param worldForce  世界坐标系下的力 [N]
     * @param worldTorque 世界坐标系下的力矩 [Nm]
     */
    public static void applyWorldForceAndTorqueAtCenterOfMass(
            ServerSubLevel subLevel,
            Vector3dc worldForce,
            Vector3dc worldTorque
    ) {
        MassData massData = subLevel.getMassTracker();

        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return;
        }

        // world frame -> sublevel local frame
        // 这里只做 orientation inverse，不建议用 transformNormalInverse，
        // 因为 transformNormalInverse 会受 scale 影响，可能改变力的大小。
        Vector3d localForce = new Vector3d(worldForce);
        Vector3d localTorque = new Vector3d(worldTorque);

        subLevel.logicalPose().orientation().transformInverse(localForce);
        subLevel.logicalPose().orientation().transformInverse(localTorque);
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
        // 作用点为重心时，不需要 r x F；直接给 linear force + torque。
        handle.applyLinearAndAngularImpulse(localForce, localTorque, true);
    }
    public static @Nullable Vec3 getCenterOfMassWorld(ServerSubLevel subLevel) {
        MassData massData = subLevel.getMassTracker();

        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return null;
        }

        Vector3dc centerOfMassLocal = massData.getCenterOfMass();

        Vector3d centerOfMassWorld = subLevel.logicalPose()
                .transformPosition(centerOfMassLocal, new Vector3d());

        return JOMLConversion.toMojang(centerOfMassWorld);
    }

    /**
     * 获取 sublevel 结构包围盒的几何中心，世界坐标系。
     *
     * 这里使用 bounds 的实际空间范围：
     * [minX, maxX + 1], [minY, maxY + 1], [minZ, maxZ + 1]
     */
    public static @Nullable Vec3 getStructureCenterWorld(SubLevel subLevel) {
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();

        if (bounds == null || bounds.volume() <= 0) {
            return null;
        }

        Vector3d structureCenterLocal = new Vector3d(
                (bounds.minX() + bounds.maxX() + 1.0) * 0.5,
                (bounds.minY() + bounds.maxY() + 1.0) * 0.5,
                (bounds.minZ() + bounds.maxZ() + 1.0) * 0.5
        );

        Vector3d structureCenterWorld = subLevel.logicalPose()
                .transformPosition(structureCenterLocal, new Vector3d());

        return JOMLConversion.toMojang(structureCenterWorld);
    }

    public static void teleportKeepOrientation(
            ServerSubLevel subLevel,
            Vector3dc targetWorldPos
    ) {
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);

        if (handle == null || !handle.isValid()) {
            return;
        }

        // 复制当前方向，避免后续 mutation 影响
        Quaterniond currentOrientation = new Quaterniond(
                subLevel.logicalPose().orientation()
        );

        handle.teleport(
                new Vector3d(targetWorldPos),
                currentOrientation
        );
    }

    public static @Nullable SubLevel getSubLevelAtBlockPos(Level level, BlockPos pos) {
        return Sable.HELPER.getContaining(level, pos);
    }
}