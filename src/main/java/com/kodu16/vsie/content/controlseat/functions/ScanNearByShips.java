package com.kodu16.vsie.content.controlseat.functions;

import com.kodu16.vsie.foundation.ServerShipUtils;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScanNearByShips {
    public static Map<String, Object> scanships(Iterable<?> ignored, BlockPos pos, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return new LinkedHashMap<>();
        }

        SubLevel ownSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, pos);
        Vec3 seatWorldPos = worldSeatPos(pos, ownSubLevel);
        if (seatWorldPos == null) {
            return new LinkedHashMap<>();
        }

        ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        if (container == null) {
            return new LinkedHashMap<>();
        }

        List<ServerSubLevel> subLevels = new ArrayList<>(container.getAllSubLevels());
        subLevels.sort(Comparator.comparingDouble(subLevel -> {
            Vec3 center = ServerShipUtils.getStructureCenterWorld(subLevel);
            return center == null ? Double.MAX_VALUE : center.distanceToSqr(seatWorldPos);
        }));

        Map<String, Object> ships = new LinkedHashMap<>();
        for (ServerSubLevel subLevel : subLevels) {
            if (subLevel == null || subLevel.isRemoved() || subLevel == ownSubLevel) {
                continue;
            }

            Vec3 center = ServerShipUtils.getStructureCenterWorld(subLevel);
            if (center == null) {
                continue;
            }

            // 功能：将 Sable sublevel 的世界中心和名称整理成 HUD/雷达统一使用的结构化扫描数据。
            String slug = normalizedName(subLevel);
            Map<String, Object> attr = new HashMap<>();
            attr.put("id", (long) subLevel.getRuntimeId());
            attr.put("slug", slug);
            attr.put("dimension", serverLevel.dimension().location().toString());
            attr.put("x", center.x);
            attr.put("y", center.y);
            attr.put("z", center.z);
            attr.put("targetIndex", 0);
            ships.put(stableShipKey(subLevel), attr);
        }
        return ships;
    }

    public static ArrayList<Vec3> scanenemyships(Iterable<?> ignored, BlockPos pos, Level level, String enemystr, String allystr) {
        ArrayList<Vec3> enemies = new ArrayList<>();
        Map<String, Object> ships = scanships(ignored, pos, level);
        for (Object value : ships.values()) {
            if (!(value instanceof Map<?, ?> rawMap)) {
                continue;
            }

            String slug = stringValue(rawMap.get("slug"));
            if (getPriority(enemystr, allystr, slug) != 1) {
                continue;
            }

            double x = toDouble(rawMap.get("x"));
            double y = toDouble(rawMap.get("y"));
            double z = toDouble(rawMap.get("z"));
            // 功能：重型炮塔仍消费世界坐标 Vec3，保持旧的敌舰目标接口不变。
            enemies.add(new Vec3(x, y, z));
        }
        return enemies;
    }

    public static SubLevel scanEnemySubLevelByIndex(Iterable<?> ignored, BlockPos pos, Level level, String enemystr, String allystr, int lockedEnemyIndex) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }

        SubLevel ownSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, pos);
        Vec3 seatWorldPos = worldSeatPos(pos, ownSubLevel);
        if (seatWorldPos == null) {
            return null;
        }

        ServerSubLevelContainer container = SubLevelContainer.getContainer(serverLevel);
        if (container == null) {
            return null;
        }

        List<ServerSubLevel> enemies = new ArrayList<>(container.getAllSubLevels());
        enemies.removeIf(subLevel -> {
            if (subLevel == null || subLevel.isRemoved() || subLevel == ownSubLevel) {
                return true;
            }
            // Function: keep target selection consistent with the HUD enemy naming filter.
            return getPriority(enemystr, allystr, normalizedName(subLevel)) != 1
                    || ServerShipUtils.getStructureCenterWorld(subLevel) == null;
        });
        enemies.sort(Comparator.comparingDouble(subLevel -> ServerShipUtils.getStructureCenterWorld(subLevel).distanceToSqr(seatWorldPos)));
        if (enemies.isEmpty()) {
            return null;
        }
        return enemies.get(Math.floorMod(lockedEnemyIndex, enemies.size()));
    }

    public static int getPriority(String a, String b, String c) {
        if (c == null || c.isEmpty()) {
            return 0;
        }

        boolean hasEnemy = a != null && !a.isEmpty();
        boolean hasAlly = b != null && !b.isEmpty();
        int posA = hasEnemy ? c.indexOf(a) : -1;
        int posB = hasAlly ? c.indexOf(b) : -1;

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

    public static Map<String, Object> withEnemyTargetIndexes(Map<String, Object> ships, String enemystr, String allystr) {
        int targetIndex = 1;
        for (Object value : ships.values()) {
            if (!(value instanceof Map<?, ?> rawMap)) {
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> attr = (Map<String, Object>) rawMap;
            String slug = String.valueOf(attr.getOrDefault("slug", ""));
            if (getPriority(enemystr, allystr, slug) == 1) {
                // 功能：写入敌方目标编号，客户端 HUD 用同一个编号显示和确认当前 TGT。
                attr.put("targetIndex", targetIndex++);
            } else {
                attr.put("targetIndex", 0);
            }
        }
        return ships;
    }

    public static String lockedEnemySlug(Map<String, Object> ships, int lockedEnemyIndex) {
        int enemyCount = 0;
        for (Object value : ships.values()) {
            if (!(value instanceof Map<?, ?> rawMap)) {
                continue;
            }

            int targetIndex = toInt(rawMap.get("targetIndex"));
            if (targetIndex <= 0) {
                continue;
            }
            if (enemyCount == lockedEnemyIndex) {
                return stringValue(rawMap.get("slug"));
            }
            enemyCount++;
        }
        return "";
    }

    private static Vec3 worldSeatPos(BlockPos pos, SubLevel ownSubLevel) {
        if (ownSubLevel == null) {
            return Vec3.atCenterOf(pos);
        }
        return ownSubLevel.logicalPose().transformPosition(Vec3.atCenterOf(pos));
    }

    private static String normalizedName(ServerSubLevel subLevel) {
        String name = subLevel.getName();
        if (name == null || name.isBlank()) {
            // 功能：未命名 sublevel 在数据层保持空名称，显示层再兜底为 [Unnamed Sublevel]。
            return "";
        }
        return name;
    }

    private static String stableShipKey(ServerSubLevel subLevel) {
        UUID uuid = subLevel.getUniqueId();
        return uuid != null ? uuid.toString() : String.valueOf(subLevel.getRuntimeId());
    }

    private static double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0D;
    }

    private static int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
