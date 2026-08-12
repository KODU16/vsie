package com.kodu16.vsie.content.custom_turret;

import org.junit.jupiter.api.Test;
import com.kodu16.vsie.content.custom_turret.client.CustomTurretEditorCamera;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Guards the definition snapshot and parent-link invariants used by item/block NBT migration. */
class CustomTurretContractTest {
    @Test
    void definitionRoundTripPreservesBoneLinksAndQuarterTurns() {
        CustomTurretDefinition definition = CustomTurretDefinition.createNew("test_turret");
        definition.name = "Test Turret";
        definition.turretType = "heavyturret";
        definition.fireCooldownTicks = 37;
        definition.rotationSpeedDegreesPerTick = 7.5F;
        definition.weaponType = "projectile";
        definition.ammoItemId = "minecraft:fire_charge";
        definition.projectileSpeedBlocksPerSecond = 240.0F;
        CustomTurretDefinition.Bone cannon = CustomTurretDefinition.Bone.child("cannon_model", "cannon");
        definition.bones.add(cannon);
        cannon.model = "parts/cannon.obj";
        cannon.texture = "textures/cannon.png";
        cannon.position = new float[]{1.0F, 2.0F, 3.0F};
        cannon.pivot = new float[]{0.25F, 0.5F, 0.75F};
        cannon.rotation = new float[]{90.0F, -90.0F, 180.0F};

        CustomTurretDefinition restored = CustomTurretDefinition.fromJson(definition.toJson());

        assertEquals("cannon", restored.findBone("cannon_model").parent);
        assertEquals(90.0F, restored.findBone("cannon_model").rotation[0]);
        assertEquals("parts/cannon.obj", restored.findBone("cannon_model").model);
        assertEquals(0.5F, restored.findBone("cannon_model").pivot[1]);
        assertEquals("heavyturret", restored.turretType);
        assertEquals(37, restored.fireCooldownTicks);
        assertEquals(7.5F, restored.rotationSpeedDegreesPerTick);
        assertEquals("projectile", restored.weaponType);
        assertEquals("minecraft:fire_charge", restored.ammoItemId);
        assertEquals(240.0F, restored.projectileSpeedBlocksPerSecond);
    }

    @Test
    void rejectsNestedCustomBonesAndEscapingResources() {
        CustomTurretDefinition definition = CustomTurretDefinition.createNew("test_turret");
        definition.bones.add(CustomTurretDefinition.Bone.child("child", "root"));
        definition.bones.add(CustomTurretDefinition.Bone.child("nested", "child"));
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomTurretDefinition.createNew("test_turret");
        definition.bones.get(0).model = "../outside.obj";
        assertThrows(IllegalArgumentException.class, definition::toJson);
    }

    @Test
    void rejectsLegacyBaseHierarchy() {
        String legacy = "{\"formatVersion\":1,\"id\":\"legacy\",\"name\":\"Legacy\",\"bones\":["
                + "{\"id\":\"base\",\"parent\":\"\",\"model\":\"\",\"texture\":\"\"},"
                + "{\"id\":\"old_part\",\"parent\":\"base\",\"model\":\"\",\"texture\":\"\"}]}";

        assertThrows(IllegalArgumentException.class, () -> CustomTurretDefinition.fromJson(legacy));
    }

    @Test
    void rejectsObjOnRequiredBoneGroup() {
        CustomTurretDefinition definition = CustomTurretDefinition.createNew("test_turret");
        definition.findBone("turret").model = "illegal.obj";
        assertThrows(IllegalArgumentException.class, definition::toJson);
    }

    @Test
    void firepointGroupsAreContiguousLongCannonChildrenAndTrackTheirCount() {
        CustomTurretDefinition definition = CustomTurretDefinition.createNew("firepoints");
        assertEquals(1, definition.firepointCount);
        assertEquals("firepoint2", definition.addFirepoint().id);
        assertEquals("firepoint3", definition.addFirepoint().id);
        assertEquals(3, definition.firepointCount);
        assertEquals("firepoint3", definition.removeLastFirepoint().id);
        assertEquals(2, CustomTurretDefinition.fromJson(definition.toJson()).firepointCount);
    }

    @Test
    void rejectsUserFirepointPrefixesAndFirepointsOutsideLongCannon() {
        CustomTurretDefinition definition = CustomTurretDefinition.createNew("bad_firepoint");
        definition.bones.add(CustomTurretDefinition.Bone.child("firepoint_helper", "root"));
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomTurretDefinition.createNew("bad_parent");
        definition.findBone("firepoint1").parent = "cannon";
        assertThrows(IllegalArgumentException.class, definition::toJson);
    }

    @Test
    void rejectsInvalidTurretRuntimeProperties() {
        CustomTurretDefinition definition = CustomTurretDefinition.createNew("test_turret");
        definition.turretType = "unknown";
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomTurretDefinition.createNew("test_turret");
        definition.fireCooldownTicks = 0;
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomTurretDefinition.createNew("test_turret");
        definition.weaponType = "unknown";
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomTurretDefinition.createNew("test_turret");
        definition.rotationSpeedDegreesPerTick = Float.NaN;
        assertThrows(IllegalArgumentException.class, definition::toJson);
    }

    @Test
    void rootPivotUsesTheBlockBaseVerticallyWithoutTheExtraHalfBlockLift() {
        CustomTurretDefinition definition = CustomTurretDefinition.createNew("test_turret");
        CustomTurretDefinition.Bone root = definition.findBone("root");
        root.position = new float[]{2.0F, -1.0F, 4.0F};
        root.pivot = new float[]{0.25F, 0.75F, -0.5F};

        float[] offset = CustomTurretAnchor.rootPivotOffset(definition);

        assertEquals(0.5F, offset[0] + root.position[0] + root.pivot[0]);
        assertEquals(0.0F, offset[1] + root.position[1] + root.pivot[1]);
        assertEquals(0.5F, offset[2] + root.position[2] + root.pivot[2]);
    }

    @Test
    void rootPivotWorldPositionUsesBlockCenterOnlyOnTheHorizontalAxes() {
        CustomTurretDefinition definition = CustomTurretDefinition.createNew("anchor_contract");
        CustomTurretDefinition.Bone root = definition.findBone("root");
        root.position = new float[]{2.0F, -3.0F, -0.25F};
        root.pivot = new float[]{0.25F, 0.75F, -0.5F};
        root.rotation = new float[]{90.0F, -45.0F, 180.0F};
        root.scale = new float[]{2.0F, 0.5F, 3.0F};

        float[] rootWorld = CustomTurretAnchor.rootPivotWorldPosition(10, 20, -30, definition);

        assertEquals(10.5F, rootWorld[0], 0.0001F);
        assertEquals(20.0F, rootWorld[1], 0.0001F);
        assertEquals(-29.5F, rootWorld[2], 0.0001F);
    }

    @Test
    void placerUsesTheClickedAdjacentCellWithoutApplyingTheAuthoredRootPivot() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[]{8, 21, -3},
                CustomTurretAnchor.placementTarget(8, 20, -3, 0, 1, 0));
    }

    @Test
    void commonTurretAnglesDriveOnlyTheirFixedBoneGroups() {
        assertEquals(-0.25F, CustomTurretRuntimePose.pitchRadians("cannon", 0.25F));
        assertEquals(0.0F, CustomTurretRuntimePose.pitchRadians("turret", 0.25F));
        assertEquals(-0.5F, CustomTurretRuntimePose.yawRadians("turret", -0.5F));
        assertEquals(0.0F, CustomTurretRuntimePose.yawRadians("cannon", -0.5F));
    }

    @Test
    void editorDeclaresPositiveZAsBarrelForwardAndInitiallyProjectsItLowerRight() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[]{0.0F, 0.0F, 1.0F},
                CustomTurretRuntimePose.defaultBarrelForward());

        float[] projected = CustomTurretEditorCamera.projectInitialDirection(0.0F, 0.0F, 1.0F);
        org.junit.jupiter.api.Assertions.assertTrue(projected[0] > 0.0F, "+Z must point right initially");
        org.junit.jupiter.api.Assertions.assertTrue(projected[1] > 0.0F, "+Z must point down initially");
    }

    @Test
    void editorPanAccumulatesScreenSpaceMouseDrag() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[]{17.0F, -3.0F},
                CustomTurretEditorCamera.pan(12.0F, 4.0F, 5.0F, -7.0F));
    }

    @Test
    void customBoneMotionSmoothsServerStepsAcrossTheShortestArc() {
        assertEquals(0.1F, CustomTurretRuntimePose.smoothAngle(0.0F, 1.0F), 0.0001F);

        float current = (float) Math.toRadians(179.0D);
        float target = (float) Math.toRadians(-179.0D);
        float smoothed = CustomTurretRuntimePose.smoothAngle(current, target);
        assertEquals((float) Math.toRadians(179.2D), smoothed, 0.0001F);
    }
}
