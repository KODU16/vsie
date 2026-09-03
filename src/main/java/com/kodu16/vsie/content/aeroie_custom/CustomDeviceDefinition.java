package com.kodu16.vsie.content.aeroie_custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Serializable custom-device skeleton shared by the editor, item and block entities. */
public final class CustomDeviceDefinition {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_BONES = 64;
    public static final int MAX_JSON_LENGTH = 262_144;
    public static final List<String> REQUIRED_BONES = List.of("root", "turret", "cannon", "long_cannon");
    public static final List<String> DEVICE_TYPES = List.of("turret", "weapon", "thruster", "decoration");
    public static final List<String> TURRET_TYPES = List.of("turret", "heavyturret", "ciws");
    public static final List<String> WEAPON_TYPES = List.of("energy", "projectile");
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int formatVersion = FORMAT_VERSION;
    public String deviceType = "turret";
    public String id = "custom_turret";
    public String name = "Custom Turret";
    public String hudShortName = "";
    public String geckoModel = "";
    public String turretType = "turret";
    public String fireAnimation = "";
    public String fireAnimationName = "";
    public int fireCooldownTicks = 20;
    public float rotationSpeedDegreesPerTick = 5.625F;
    public String weaponType = "energy";
    public int firepointCount = 1;
    public int flamepointCount = 0;
    public int trailpointCount = 0;
    public int energyPerTick = 1;
    public int laserColor = 0xFF66CCFF;
    public float laserRadius = 0.25F;
    public String ammoItemId = "minecraft:arrow";
    public float projectileScale = 1.0F;
    public float projectileDamage = 15.0F;
    public float projectileExplosionRadius = 0.0F;
    public int projectileLifetimeTicks = 20 * 15;
    public float projectileSpeedBlocksPerSecond = 120.0F;
    public CustomFxConfig projectileFx = new CustomFxConfig();
    public int firepointIntervalTicks = 1;
    public float thrusterThrust = 20_000.0F;
    public int thrusterFuelMbPerTickPerPercent = 1;
    public String thrusterFuel = "#aeroie:fuel";
    public int flameSegments = 8;
    public float flameRadius = 0.5F;
    public int flameColor = 0xFF33AAFF;
    public float trailRadius = 0.25F;
    public int trailColor = 0xAA66CCFF;
    public List<Bone> bones = new ArrayList<>();

    public static CustomDeviceDefinition createNew(String id) {
        return createNew(id, "turret");
    }

    public static CustomDeviceDefinition createNew(String id, String deviceType) {
        CustomDeviceDefinition definition = new CustomDeviceDefinition();
        definition.deviceType = normalize(deviceType).toLowerCase(java.util.Locale.ROOT);
        definition.id = id;
        definition.name = switch (definition.deviceType) {
            case "weapon" -> "Custom Weapon";
            case "thruster" -> "Custom Thruster";
            case "decoration" -> "Custom Decoration";
            default -> "Custom Turret";
        };
        definition.hudShortName = "weapon".equals(definition.deviceType) ? "WPN" : "";
        definition.bones.add(Bone.root("root"));
        if ("turret".equals(definition.deviceType)) {
            definition.bones.add(Bone.child("turret", "root"));
            definition.bones.add(Bone.child("cannon", "turret"));
            definition.bones.add(Bone.child("long_cannon", "cannon"));
            definition.bones.add(Bone.child("firepoint1", "long_cannon"));
        } else if ("weapon".equals(definition.deviceType)) {
            definition.bones.add(Bone.child("firepoint1", "root"));
        } else if ("thruster".equals(definition.deviceType)) {
            definition.firepointCount = 0;
            definition.flamepointCount = 1;
            definition.trailpointCount = 1;
            definition.bones.add(Bone.child("flamepoint1", "root"));
            definition.bones.add(Bone.child("trailpoint1", "root"));
        } else if ("decoration".equals(definition.deviceType)) {
            // Function: decorations start with an ordinary root-level bone and never impose semantic bone groups.
            definition.firepointCount = 0;
            definition.bones.clear();
            definition.bones.add(Bone.root("bone_1"));
        }
        return definition;
    }

    public static CustomDeviceDefinition fromJson(String json) {
        if (json == null || json.isBlank() || json.length() > MAX_JSON_LENGTH) {
            throw new IllegalArgumentException("Custom turret JSON is empty or too large");
        }
        try {
            CustomDeviceDefinition definition = GSON.fromJson(json, CustomDeviceDefinition.class);
            if (definition == null) {
                throw new IllegalArgumentException("Custom turret JSON has no root object");
            }
            definition.normalizeAndValidate();
            return definition;
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("Invalid custom turret JSON", exception);
        }
    }

    public String toJson() {
        normalizeAndValidate();
        String json = GSON.toJson(this);
        if (json.length() > MAX_JSON_LENGTH) {
            throw new IllegalArgumentException("Custom turret JSON is too large");
        }
        return json;
    }

    public CustomDeviceDefinition copy() {
        return fromJson(toJson());
    }

    public Bone findBone(String boneId) {
        for (Bone bone : bones) {
            if (bone.id.equals(boneId)) {
                return bone;
            }
        }
        return null;
    }

    public Bone addFirepoint() {
        if (firepointCount >= MAX_BONES) {
            throw new IllegalArgumentException("Too many firepoint bone groups");
        }
        String parent = "turret".equals(deviceType) ? "long_cannon" : "root";
        Bone firepoint = Bone.child("firepoint" + (firepointCount + 1), parent);
        bones.add(firepoint);
        firepointCount++;
        return firepoint;
    }

    public Bone removeLastFirepoint() {
        if (firepointCount <= 1) {
            return null;
        }
        Bone firepoint = findBone("firepoint" + firepointCount);
        if (firepoint != null) {
            bones.remove(firepoint);
        }
        firepointCount--;
        return firepoint;
    }

    public Bone addFlamepoint() {
        Bone flamepoint = Bone.child("flamepoint" + (flamepointCount + 1), "root");
        bones.add(flamepoint);
        flamepointCount++;
        return flamepoint;
    }

    public Bone removeLastFlamepoint() {
        if (flamepointCount <= 1) {
            return null;
        }
        Bone flamepoint = findBone("flamepoint" + flamepointCount);
        if (flamepoint != null) {
            bones.remove(flamepoint);
        }
        flamepointCount--;
        return flamepoint;
    }

    public Bone addTrailpoint() {
        Bone trailpoint = Bone.child("trailpoint" + (trailpointCount + 1), "root");
        bones.add(trailpoint);
        trailpointCount++;
        return trailpoint;
    }

    public Bone removeLastTrailpoint() {
        if (trailpointCount <= 0) {
            return null;
        }
        Bone trailpoint = findBone("trailpoint" + trailpointCount);
        if (trailpoint != null) {
            bones.remove(trailpoint);
        }
        trailpointCount--;
        return trailpoint;
    }

    public void normalizeAndValidate() {
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported custom turret format: " + formatVersion);
        }
        deviceType = normalize(deviceType).toLowerCase(java.util.Locale.ROOT);
        id = normalize(id);
        name = normalize(name);
        hudShortName = sanitizeHudShortName(hudShortName);
        geckoModel = CustomDeviceStorage.normalizeRelativePath(geckoModel);
        turretType = normalize(turretType).toLowerCase(java.util.Locale.ROOT);
        fireAnimation = "turret".equals(deviceType)
                ? CustomTurretAnimationResources.normalizeRelativeAnimationPath(fireAnimation) : "";
        fireAnimationName = "turret".equals(deviceType) ? normalize(fireAnimationName) : "";
        weaponType = normalize(weaponType).toLowerCase(java.util.Locale.ROOT);
        ammoItemId = normalize(ammoItemId).toLowerCase(java.util.Locale.ROOT);
        thrusterFuel = normalize(thrusterFuel).toLowerCase(java.util.Locale.ROOT);
        if (!DEVICE_TYPES.contains(deviceType)) {
            throw new IllegalArgumentException("Invalid custom device type: " + deviceType);
        }
        if (!SAFE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid custom turret id");
        }
        if (name.isEmpty() || name.length() > 64) {
            throw new IllegalArgumentException("Invalid custom turret name");
        }
        if (!geckoModel.isBlank() && !geckoModel.toLowerCase(java.util.Locale.ROOT).endsWith(".geo.json")) {
            throw new IllegalArgumentException("Custom GeckoLib model must use .geo.json");
        }
        if (("weapon".equals(deviceType) || ("turret".equals(deviceType) && "heavyturret".equals(turretType)))
                && hudShortName.isBlank()) {
            throw new IllegalArgumentException("Custom heavy turrets and weapons require a HUD short name");
        }
        if ("turret".equals(deviceType) && !TURRET_TYPES.contains(turretType)) {
            throw new IllegalArgumentException("Invalid custom turret type: " + turretType);
        }
        if (!fireAnimation.isBlank() && fireAnimationName.isBlank()) {
            fireAnimationName = CustomTurretAnimationResources.defaultAnimationName(fireAnimation);
        }
        if (!fireAnimationName.isBlank() && !fireAnimationName.matches("[A-Za-z0-9_./:-]{1,128}")) {
            throw new IllegalArgumentException("Invalid custom turret fire animation name");
        }
        if (fireCooldownTicks < 1 || fireCooldownTicks > 72_000) {
            throw new IllegalArgumentException("Custom turret cooldown must be 1-72000 ticks");
        }
        if (!WEAPON_TYPES.contains(weaponType)) {
            throw new IllegalArgumentException("Invalid custom weapon type: " + weaponType);
        }
        validateFiniteRange(rotationSpeedDegreesPerTick, 0.01F, 180.0F, "rotation speed");
        if (energyPerTick < 0 || energyPerTick > 1_000_000) {
            throw new IllegalArgumentException("Custom turret energy rate must be 0-1000000 FE/t");
        }
        validateFiniteRange(laserRadius, 0.001F, 64.0F, "laser radius");
        if (!ammoItemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid custom turret ammo item id");
        }
        validateFiniteRange(projectileScale, 0.01F, 64.0F, "projectile scale");
        validateFiniteRange(projectileDamage, 0.0F, 1_000_000.0F, "projectile damage");
        validateFiniteRange(projectileExplosionRadius, 0.0F, 128.0F, "projectile explosion radius");
        if (projectileFx == null) {
            projectileFx = new CustomFxConfig();
        }
        projectileFx.normalizeAndValidate();
        if (projectileLifetimeTicks < 1 || projectileLifetimeTicks > 72_000) {
            throw new IllegalArgumentException("Custom projectile lifetime must be 1-72000 ticks");
        }
        validateFiniteRange(projectileSpeedBlocksPerSecond, 0.01F, 20_000.0F, "projectile speed");
        if (firepointIntervalTicks < 1 || firepointIntervalTicks > 72_000) {
            throw new IllegalArgumentException("Custom firepoint interval must be 1-72000 ticks");
        }
        validateThrusterProperties();
        if (bones == null || bones.isEmpty() || bones.size() > MAX_BONES) {
            throw new IllegalArgumentException("A custom device needs 1-" + MAX_BONES + " bones");
        }

        Map<String, Bone> byId = new HashMap<>();
        for (Bone bone : bones) {
            if (bone == null) {
                throw new IllegalArgumentException("Null bone");
            }
            bone.normalizeAndValidate();
            if (byId.put(bone.id, bone) != null) {
                throw new IllegalArgumentException("Duplicate bone id: " + bone.id);
            }
        }
        for (Bone bone : bones) {
            if (!bone.parent.isEmpty() && !byId.containsKey(bone.parent)) {
                throw new IllegalArgumentException("Missing parent bone: " + bone.parent);
            }
            validateParentChain(bone, byId);
            if ("turret".equals(deviceType) && !isBoneGroup(bone.id) && !isRequiredBoneGroup(bone.parent)) {
                throw new IllegalArgumentException("Custom bone parent must be root, turret, cannon or long_cannon: " + bone.id);
            }
            if (!"decoration".equals(deviceType) && isBoneGroup(bone.id)
                    && (!bone.model.isEmpty() || !bone.texture.isEmpty())) {
                throw new IllegalArgumentException("Bone groups cannot own OBJ models: " + bone.id);
            }
        }
        if (!"decoration".equals(deviceType)) {
            validateRequiredBone(byId, "root", "");
        }
        if ("turret".equals(deviceType)) {
            validateRequiredBone(byId, "turret", "root");
            validateRequiredBone(byId, "cannon", "turret");
            validateRequiredBone(byId, "long_cannon", "cannon");
        }
        validateFirepoints(byId);
        validateThrusterPoints(byId);
    }

    public static boolean isRequiredBoneGroup(String boneId) {
        return REQUIRED_BONES.contains(boneId);
    }

    public static boolean isFirepointBoneGroup(String boneId) {
        return boneId != null && boneId.matches("firepoint[1-9][0-9]*");
    }

    public static boolean isFlamepointBoneGroup(String boneId) {
        return boneId != null && boneId.matches("flamepoint[1-9][0-9]*");
    }

    public static boolean isTrailpointBoneGroup(String boneId) {
        return boneId != null && boneId.matches("trailpoint[1-9][0-9]*");
    }

    public static boolean isBoneGroup(String boneId) {
        return isRequiredBoneGroup(boneId) || isFirepointBoneGroup(boneId)
                || isFlamepointBoneGroup(boneId) || isTrailpointBoneGroup(boneId);
    }

    private void validateFirepoints(Map<String, Bone> byId) {
        if ("thruster".equals(deviceType) || "decoration".equals(deviceType)) {
            if (firepointCount != 0) {
                throw new IllegalArgumentException("This custom device type does not use firepoint groups");
            }
            return;
        }
        if (firepointCount < 1 || firepointCount > MAX_BONES) {
            throw new IllegalArgumentException("Custom weapon/turret needs at least one firepoint bone group");
        }
        int observed = 0;
        for (Bone bone : bones) {
            if (bone.id.startsWith("firepoint")) {
                if (!isFirepointBoneGroup(bone.id)
                        || ("turret".equals(deviceType) && !"long_cannon".equals(bone.parent))) {
                    throw new IllegalArgumentException("Firepoint groups must be contiguous children of long_cannon: " + bone.id);
                }
                observed++;
            }
        }
        if (observed != firepointCount) {
            throw new IllegalArgumentException("Recorded firepoint count does not match the skeleton");
        }
        for (int index = 1; index <= firepointCount; index++) {
            Bone bone = byId.get("firepoint" + index);
            if (bone == null || ("turret".equals(deviceType) && !"long_cannon".equals(bone.parent))) {
                throw new IllegalArgumentException("Missing firepoint bone group: firepoint" + index);
            }
        }
    }

    private void validateThrusterPoints(Map<String, Bone> byId) {
        if (!"thruster".equals(deviceType)) {
            return;
        }
        validateContiguousPointSet(byId, "flamepoint", flamepointCount, true);
        validateContiguousPointSet(byId, "trailpoint", trailpointCount, false);
    }

    private void validateContiguousPointSet(Map<String, Bone> byId, String prefix, int count, boolean required) {
        if (required && count < 1) {
            throw new IllegalArgumentException("Custom thrusters need at least one " + prefix + " group");
        }
        if (count < 0 || count > MAX_BONES) {
            throw new IllegalArgumentException("Invalid custom " + prefix + " count");
        }
        int observed = 0;
        for (Bone bone : bones) {
            if (bone.id.startsWith(prefix)) {
                if (!bone.id.matches(prefix + "[1-9][0-9]*")) {
                    throw new IllegalArgumentException("Invalid custom " + prefix + " group: " + bone.id);
                }
                observed++;
            }
        }
        if (observed != count) {
            throw new IllegalArgumentException("Recorded " + prefix + " count does not match the skeleton");
        }
        for (int index = 1; index <= count; index++) {
            if (byId.get(prefix + index) == null) {
                throw new IllegalArgumentException("Missing " + prefix + " bone group: " + prefix + index);
            }
        }
    }

    private void validateThrusterProperties() {
        validateFiniteRange(thrusterThrust, 0.01F, 1_000_000_000.0F, "thruster thrust");
        if (thrusterFuelMbPerTickPerPercent < 0 || thrusterFuelMbPerTickPerPercent > 1_000_000) {
            throw new IllegalArgumentException("Custom thruster fuel rate must be 0-1000000 mb/tick");
        }
        if (!thrusterFuel.matches("#?[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid custom thruster fuel id or tag");
        }
        if (flameSegments < 3 || flameSegments > 64) {
            throw new IllegalArgumentException("Custom thruster flame segments must be 3-64");
        }
        validateFiniteRange(flameRadius, 0.001F, 64.0F, "flame radius");
        validateFiniteRange(trailRadius, 0.0F, 64.0F, "trail radius");
    }

    private static void validateFiniteRange(float value, float min, float max, String label) {
        if (!Float.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Invalid custom turret " + label);
        }
    }

    public static String sanitizeHudShortName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > 12) {
            return trimmed.substring(0, 12);
        }
        return trimmed;
    }

    private static void validateRequiredBone(Map<String, Bone> byId, String id, String parent) {
        Bone bone = byId.get(id);
        if (bone == null || !parent.equals(bone.parent)) {
            throw new IllegalArgumentException("Required turret bone hierarchy is invalid at: " + id);
        }
    }

    private static void validateParentChain(Bone start, Map<String, Bone> byId) {
        Set<String> visited = new HashSet<>();
        Bone current = start;
        while (current != null && !current.parent.isEmpty()) {
            if (!visited.add(current.id)) {
                throw new IllegalArgumentException("Bone parent cycle at: " + current.id);
            }
            current = byId.get(current.parent);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Bone {
        public String id = "bone";
        public String parent = "";
        public String model = "";
        public String texture = "";
        public float[] position = vector(0.0F, 0.0F, 0.0F);
        public float[] pivot = vector(0.0F, 0.0F, 0.0F);
        public float[] rotation = vector(0.0F, 0.0F, 0.0F);
        public float[] scale = vector(1.0F, 1.0F, 1.0F);
        public CustomFxConfig pointFx = new CustomFxConfig();

        public static Bone root(String id) {
            Bone bone = new Bone();
            bone.id = id;
            return bone;
        }

        public static Bone child(String id, String parent) {
            Bone bone = root(id);
            bone.parent = parent;
            return bone;
        }

        private void normalizeAndValidate() {
            id = normalize(id);
            parent = normalize(parent);
            model = CustomDeviceStorage.normalizeRelativePath(model);
            texture = CustomDeviceStorage.normalizeRelativePath(texture);
            if (!SAFE_ID.matcher(id).matches()) {
                throw new IllegalArgumentException("Invalid bone id: " + id);
            }
            if (id.equals(parent)) {
                throw new IllegalArgumentException("A bone cannot parent itself: " + id);
            }
            validateResource(model, ".obj", "model");
            validateResource(texture, ".png", "texture");
            position = validateVector(position, 0.0F, -4096.0F, 4096.0F, "position");
            pivot = validateVector(pivot, 0.0F, -4096.0F, 4096.0F, "pivot");
            rotation = validateVector(rotation, 0.0F, -36000.0F, 36000.0F, "rotation");
            scale = validateVector(scale, 1.0F, 0.001F, 1000.0F, "scale");
            if (pointFx == null) {
                pointFx = new CustomFxConfig();
            }
            pointFx.normalizeAndValidate();
        }

        private static void validateResource(String path, String extension, String label) {
            if (!path.isEmpty() && !path.toLowerCase(java.util.Locale.ROOT).endsWith(extension)) {
                throw new IllegalArgumentException("Bone " + label + " must use " + extension);
            }
        }

        private static float[] validateVector(float[] vector, float fallback, float min, float max, String label) {
            if (vector == null) {
                return vector(fallback, fallback, fallback);
            }
            if (vector.length != 3) {
                throw new IllegalArgumentException("Bone " + label + " must have three values");
            }
            for (float value : vector) {
                if (!Float.isFinite(value) || value < min || value > max) {
                    throw new IllegalArgumentException("Invalid bone " + label);
                }
            }
            return vector;
        }

        private static float[] vector(float x, float y, float z) {
            return new float[]{x, y, z};
        }
    }
}
