package com.kodu16.vsie.content.custom_turret;

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

/** Serializable custom-turret skeleton shared by the editor, item and block entity. */
public final class CustomTurretDefinition {
    public static final int FORMAT_VERSION = 1;
    public static final int MAX_BONES = 64;
    public static final int MAX_JSON_LENGTH = 262_144;
    public static final List<String> REQUIRED_BONES = List.of("root", "turret", "cannon", "long_cannon");
    public static final List<String> TURRET_TYPES = List.of("turret", "heavyturret", "ciws");
    public static final List<String> WEAPON_TYPES = List.of("energy", "projectile");
    private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int formatVersion = FORMAT_VERSION;
    public String id = "custom_turret";
    public String name = "Custom Turret";
    public String turretType = "turret";
    public int fireCooldownTicks = 20;
    public float rotationSpeedDegreesPerTick = 5.625F;
    public String weaponType = "energy";
    public int firepointCount = 1;
    public int energyPerTick = 1;
    public int laserColor = 0xFF66CCFF;
    public float laserRadius = 0.25F;
    public String ammoItemId = "minecraft:arrow";
    public float projectileScale = 1.0F;
    public float projectileDamage = 15.0F;
    public float projectileExplosionRadius = 0.0F;
    public int projectileLifetimeTicks = 100;
    public float projectileSpeedBlocksPerSecond = 120.0F;
    public int firepointIntervalTicks = 1;
    public List<Bone> bones = new ArrayList<>();

    public static CustomTurretDefinition createNew(String id) {
        CustomTurretDefinition definition = new CustomTurretDefinition();
        definition.id = id;
        definition.name = "Custom Turret";
        definition.bones.add(Bone.root("root"));
        definition.bones.add(Bone.child("turret", "root"));
        definition.bones.add(Bone.child("cannon", "turret"));
        definition.bones.add(Bone.child("long_cannon", "cannon"));
        definition.bones.add(Bone.child("firepoint1", "long_cannon"));
        return definition;
    }

    public static CustomTurretDefinition fromJson(String json) {
        if (json == null || json.isBlank() || json.length() > MAX_JSON_LENGTH) {
            throw new IllegalArgumentException("Custom turret JSON is empty or too large");
        }
        try {
            CustomTurretDefinition definition = GSON.fromJson(json, CustomTurretDefinition.class);
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

    public CustomTurretDefinition copy() {
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
        if (firepointCount >= MAX_BONES - REQUIRED_BONES.size()) {
            throw new IllegalArgumentException("Too many firepoint bone groups");
        }
        Bone firepoint = Bone.child("firepoint" + (firepointCount + 1), "long_cannon");
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

    public void normalizeAndValidate() {
        if (formatVersion != FORMAT_VERSION) {
            throw new IllegalArgumentException("Unsupported custom turret format: " + formatVersion);
        }
        id = normalize(id);
        name = normalize(name);
        turretType = normalize(turretType).toLowerCase(java.util.Locale.ROOT);
        weaponType = normalize(weaponType).toLowerCase(java.util.Locale.ROOT);
        ammoItemId = normalize(ammoItemId).toLowerCase(java.util.Locale.ROOT);
        if (!SAFE_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid custom turret id");
        }
        if (name.isEmpty() || name.length() > 64) {
            throw new IllegalArgumentException("Invalid custom turret name");
        }
        if (!TURRET_TYPES.contains(turretType)) {
            throw new IllegalArgumentException("Invalid custom turret type: " + turretType);
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
        if (projectileLifetimeTicks < 1 || projectileLifetimeTicks > 72_000) {
            throw new IllegalArgumentException("Custom projectile lifetime must be 1-72000 ticks");
        }
        validateFiniteRange(projectileSpeedBlocksPerSecond, 0.01F, 20_000.0F, "projectile speed");
        if (firepointIntervalTicks < 1 || firepointIntervalTicks > 72_000) {
            throw new IllegalArgumentException("Custom firepoint interval must be 1-72000 ticks");
        }
        if (bones == null || bones.isEmpty() || bones.size() > MAX_BONES) {
            throw new IllegalArgumentException("A custom turret needs 1-" + MAX_BONES + " bones");
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
            if (!isBoneGroup(bone.id) && !isRequiredBoneGroup(bone.parent)) {
                throw new IllegalArgumentException("Custom bone parent must be root, turret, cannon or long_cannon: " + bone.id);
            }
            if (isBoneGroup(bone.id) && (!bone.model.isEmpty() || !bone.texture.isEmpty())) {
                throw new IllegalArgumentException("Bone groups cannot own OBJ models: " + bone.id);
            }
        }
        validateRequiredBone(byId, "root", "");
        validateRequiredBone(byId, "turret", "root");
        validateRequiredBone(byId, "cannon", "turret");
        validateRequiredBone(byId, "long_cannon", "cannon");
        validateFirepoints(byId);
    }

    public static boolean isRequiredBoneGroup(String boneId) {
        return REQUIRED_BONES.contains(boneId);
    }

    public static boolean isFirepointBoneGroup(String boneId) {
        return boneId != null && boneId.matches("firepoint[1-9][0-9]*");
    }

    public static boolean isBoneGroup(String boneId) {
        return isRequiredBoneGroup(boneId) || isFirepointBoneGroup(boneId);
    }

    private void validateFirepoints(Map<String, Bone> byId) {
        if (firepointCount < 1 || firepointCount > MAX_BONES - REQUIRED_BONES.size()) {
            throw new IllegalArgumentException("Custom turret needs at least one firepoint bone group");
        }
        int observed = 0;
        for (Bone bone : bones) {
            if (bone.id.startsWith("firepoint")) {
                if (!isFirepointBoneGroup(bone.id) || !"long_cannon".equals(bone.parent)) {
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
            if (bone == null || !"long_cannon".equals(bone.parent)) {
                throw new IllegalArgumentException("Missing firepoint bone group: firepoint" + index);
            }
        }
    }

    private static void validateFiniteRange(float value, float min, float max, String label) {
        if (!Float.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException("Invalid custom turret " + label);
        }
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
            model = CustomTurretStorage.normalizeRelativePath(model);
            texture = CustomTurretStorage.normalizeRelativePath(texture);
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
