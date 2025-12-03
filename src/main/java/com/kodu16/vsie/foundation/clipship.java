package com.kodu16.vsie.foundation;


import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.List;
import java.util.Optional;

public class clipship {
    public float getclipdistance(ServerShip ship, BlockPos pos, Level level, Vector3d facing, float ZAxisOffset) {
        //瞄准后调用，必须输入Vector3d形式的朝向向量
        //首先raycast获取距离
        //其次通过距离乘以向量算出
        //controlcraft的射线检测我看不懂，这个我能用
        float effectiveMaxDistance = 128;
        Pair<Vec3, Vec3> raycastPositions = calculateRaycastPositions(ship, level, pos, new Vec3(facing.x,facing.y,facing.z), effectiveMaxDistance, ZAxisOffset);
        Vec3 worldFrom = raycastPositions.getFirst();
        Vec3 worldTo = raycastPositions.getSecond();
        // Perform raycast using world coordinates
        ClipContext.Fluid clipFluid =  ClipContext.Fluid.ANY;
        ClipContext context = new ClipContext(worldFrom, worldTo, ClipContext.Block.COLLIDER, clipFluid, null);
        BlockHitResult hit = level.clip(context);

        // Calculate power based on world distance
        float distance = effectiveMaxDistance;
        BlockPos hitBlockPos = null;

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();
            hitBlockPos = hit.getBlockPos();
            distance = (float)worldFrom.distanceTo(hitPos);
            distance = Math.min(distance, effectiveMaxDistance);
            return distance;
        }
        else
        {
            return 114514;
        }
    }
    protected Vec3 getStartingPoint(Vec3 directionVec, float ZAxisOffset, Vec3 worldPosition) {
        Vec3 offset = directionVec.multiply(ZAxisOffset, ZAxisOffset, ZAxisOffset);
        return worldPosition.add(offset);
    }
    private Pair<Vec3, Vec3> calculateRaycastPositions(ServerShip ship, Level level, BlockPos localBlockPos, Vec3 localDirectionVector, float maxRaycastDistance, float ZAxisOffset) {
        Vec3 worldFrom;
        Vec3 worldDisplacement;
        Vec3 localFromCenter = getStartingPoint(localDirectionVector,ZAxisOffset,localDirectionVector);
        Vec3 localDisplacement = localDirectionVector.scale(maxRaycastDistance);
        boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, localBlockPos);

        if (onShip) {
            Vector3d worldPosition = ship.getShipToWorld().transformPosition(new Vector3d(localBlockPos.getX(), localBlockPos.getY(), localBlockPos.getZ()));
            localFromCenter = getStartingPoint(localDirectionVector, ZAxisOffset,new Vec3(worldPosition.x, worldPosition.y, worldPosition.z));
            localDisplacement = localDirectionVector.scale(maxRaycastDistance);
            if (ship != null && ship.getTransform() != null) {
                worldFrom = VSGameUtilsKt.toWorldCoordinates(ship, localFromCenter);

                Quaterniondc shipRotation = ship.getTransform().getShipToWorldRotation();
                Vector3d rotatedDisplacementJOML = new Vector3d();
                shipRotation.transform(localDisplacement.x, localDisplacement.y, localDisplacement.z, rotatedDisplacementJOML);
                worldDisplacement = new Vec3(rotatedDisplacementJOML.x, rotatedDisplacementJOML.y, rotatedDisplacementJOML.z);
            } else {
                worldFrom = localFromCenter;
                worldDisplacement = localDisplacement;
            }
        } else {
            worldFrom = localFromCenter;
            worldDisplacement = localDisplacement;
        }

        Vec3 worldTo = worldFrom.add(worldDisplacement);
        return new Pair<>(worldFrom, worldTo);
    }
}
