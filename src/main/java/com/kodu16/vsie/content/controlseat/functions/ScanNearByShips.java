package com.kodu16.vsie.content.controlseat.functions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ScanNearByShips {
    public static Map<String, Object> scanships(Iterable<?> ignored, BlockPos pos, Level level) {
        return new HashMap<>();
    }

    public static ArrayList<Vector3d> scanenemyships(Iterable<?> ignored, BlockPos pos, Level level, String enemystr, String allystr) {
        return new ArrayList<>();
    }

    public static int getPriority(String a, String b, String c) {
        if (a == null || b == null || c == null || a.isEmpty() || b.isEmpty() || c.isEmpty()) {
            return 0;
        }

        int posA = c.indexOf(a);
        int posB = c.indexOf(b);

        if (posA == -1 && posB == -1) {
            return 0;
        }
        if (posA != -1 && posB == -1) {
            return 1;
        }
        if (posA == -1) {
            return 2;
        }
        return posA < posB ? 1 : 2;
    }
}
