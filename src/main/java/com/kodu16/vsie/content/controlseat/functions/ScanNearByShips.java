package com.kodu16.vsie.content.controlseat.functions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.impl.shadow.Bl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashMap;
import java.util.Map;

public class ScanNearByShips {
    public static Map<String, Object> scanships(QueryableShipData<Ship> qsd, BlockPos pos, Level level) {
        Map<String, Object> mapper = new HashMap<>();
        try {
            qsd.iterator().forEachRemaining(e -> {
                AABBdc p = e.getWorldAABB();
                double[] c = getAABBdcCenter(p);
                LogUtils.getLogger().warn("detected ship:"+e.getSlug()+"filter:");
                Map<String, Object> attr = new HashMap<>();
                attr.put("id", e.getId());
                attr.put("slug", e.getSlug());
                attr.put("dimension", e.getChunkClaimDimension());
                attr.put("x", c[0]);
                attr.put("y", c[1]);
                attr.put("z", c[2]);
                AABBdc box = e.getWorldAABB();
                attr.put("max_x", box.maxX());
                attr.put("max_y", box.maxY());
                attr.put("max_z", box.maxZ());
                attr.put("min_x", box.minX());
                attr.put("min_y", box.minY());
                attr.put("min_z", box.minZ());
                //LogUtils.getLogger().warn("detected ship:"+e.getSlug()+"position:"+attr.get("x")+","+attr.get("y")+","+attr.get("z"));
                mapper.put(String.valueOf(e.getId()), attr);
            });
        } catch (RuntimeException ex) {
        }
        return mapper;
    }
    private static double[] getAABBdcCenter(AABBdc aabb) {
        double width = aabb.maxX() - aabb.minX();
        double len = aabb.maxZ() - aabb.minZ();
        double hight = aabb.maxY() - aabb.minY();
        double centerX = aabb.minX() + width / 2;
        double centerY = aabb.minY() + hight / 2;
        double centerZ = aabb.minZ() + len / 2;
        return new double[]{centerX, centerY, centerZ};
    }
}
