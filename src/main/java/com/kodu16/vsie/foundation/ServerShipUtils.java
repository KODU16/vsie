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

    public static void applyWorldForceAndTorqueAtCenterOfMass(
            ServerSubLevel subLevel,
            Vector3dc worldImpulse,
            Vector3dc worldTorqueImpulse
    ) {
        MassData massData = subLevel.getMassTracker();

        if (massData == null || massData.isInvalid() || massData.getCenterOfMass() == null) {
            return;
        }

        // world frame -> sublevel local frame
        Vector3d localForce = new Vector3d(worldImpulse);
        Vector3d localTorque = new Vector3d(worldTorqueImpulse);

        subLevel.logicalPose().orientation().transformInverse(localForce);
        subLevel.logicalPose().orientation().transformInverse(localTorque);
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
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

    public static double getStructureMaxDimension(SubLevel subLevel) {
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        if (bounds == null || bounds.volume() <= 0) {
            return 0.0D;
        }

        // Function: plot bounds are block-inclusive, so add one block when measuring each side length.
        double sizeX = bounds.maxX() - bounds.minX() + 1.0D;
        double sizeY = bounds.maxY() - bounds.minY() + 1.0D;
        double sizeZ = bounds.maxZ() - bounds.minZ() + 1.0D;
        return Math.max(sizeX, Math.max(sizeY, sizeZ));
    }

    public static Vec3 getBlockCenterWorld(@Nullable SubLevel subLevel, BlockPos pos) {
        // Function: Sable logical poses expect sublevel-space positions, so use the block center as the local point.
        Vec3 localCenter = Vec3.atCenterOf(pos);
        return subLevel == null ? localCenter : subLevel.logicalPose().transformPosition(localCenter);
    }

    public static boolean teleportKeepOrientation(
            ServerSubLevel subLevel,
            Vector3dc targetWorldPos
    ) {
        RigidBodyHandle handle = RigidBodyHandle.of(subLevel);

        if (handle == null || !handle.isValid()) {
            return false;
        }

        Quaterniond currentOrientation = new Quaterniond(
                subLevel.logicalPose().orientation()
        );

        // Function: Sable teleport takes the sublevel's world pose position; keep orientation unchanged.
        handle.teleport(
                new Vector3d(targetWorldPos),
                currentOrientation
        );
        return true;
    }

    public static @Nullable SubLevel getSubLevelAtBlockPos(Level level, BlockPos pos) {
        return Sable.HELPER.getContaining(level, pos);
    }
}
