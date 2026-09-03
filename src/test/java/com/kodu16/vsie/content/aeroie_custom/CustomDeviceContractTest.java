package com.kodu16.vsie.content.aeroie_custom;

import org.junit.jupiter.api.Test;
import com.kodu16.vsie.content.aeroie_custom.client.CustomDeviceEditorCamera;
import com.kodu16.vsie.content.turret.TurretMountAxes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Guards the definition snapshot and parent-link invariants used by item/block NBT migration. */
class CustomDeviceContractTest {
    @Test
    void placerDelegatesFacingSelectionToTheDirectionalTurretBlock() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomDevicePlacerItem.java"));

        org.junit.jupiter.api.Assertions.assertTrue(source.contains("getStateForPlacement(placementContext)"));
        org.junit.jupiter.api.Assertions.assertFalse(source.contains("CUSTOM_TURRET_BLOCK.getDefaultState()"));
    }

    @Test
    void definitionRoundTripPreservesBoneLinksAndQuarterTurns() {
        CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("test_turret");
        definition.name = "Test Turret";
        definition.turretType = "heavyturret";
        definition.hudShortName = "TURRET";
        definition.fireCooldownTicks = 37;
        definition.rotationSpeedDegreesPerTick = 7.5F;
        definition.weaponType = "projectile";
        definition.ammoItemId = "minecraft:fire_charge";
        definition.projectileSpeedBlocksPerSecond = 240.0F;
        CustomDeviceDefinition.Bone cannon = CustomDeviceDefinition.Bone.child("cannon_model", "cannon");
        definition.bones.add(cannon);
        cannon.model = "parts/cannon.obj";
        cannon.texture = "textures/cannon.png";
        cannon.position = new float[]{1.0F, 2.0F, 3.0F};
        cannon.pivot = new float[]{0.25F, 0.5F, 0.75F};
        cannon.rotation = new float[]{90.0F, -90.0F, 180.0F};
        definition.projectileFx.fx = "muzzle/projectile_trail";
        definition.projectileFx.scale = 2.0F;
        definition.projectileFx.rotation = new float[]{10.0F, 20.0F, 30.0F};
        definition.findBone("firepoint1").pointFx.fx = "muzzle/fire";
        definition.findBone("firepoint1").pointFx.scale = 0.5F;
        definition.findBone("firepoint1").pointFx.rotation = new float[]{-10.0F, 0.0F, 90.0F};

        CustomDeviceDefinition restored = CustomDeviceDefinition.fromJson(definition.toJson());

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
        assertEquals("muzzle/projectile_trail", restored.projectileFx.fx);
        assertEquals(20.0F, restored.projectileFx.rotation[1]);
        assertEquals("muzzle/fire", restored.findBone("firepoint1").pointFx.fx);
        assertEquals(90.0F, restored.findBone("firepoint1").pointFx.rotation[2]);
    }

    @Test
    void customDeviceDefinitionsRoundTripTheirCategorySpecificPointsAndProperties() {
        CustomDeviceDefinition weapon = CustomDeviceDefinition.createNew("test_weapon", "weapon");
        weapon.hudShortName = "WPN1";
        assertEquals("weapon", weapon.deviceType);
        assertEquals(1, weapon.firepointCount);
        assertEquals("root", weapon.findBone("firepoint1").parent);

        CustomDeviceDefinition thruster = CustomDeviceDefinition.createNew("test_thruster", "thruster");
        thruster.thrusterFuel = "#aeroie:fuel";
        thruster.thrusterThrust = 42_000.0F;
        thruster.flameSegments = 12;
        thruster.trailRadius = 0.75F;

        CustomDeviceDefinition restored = CustomDeviceDefinition.fromJson(thruster.toJson());

        assertEquals("thruster", restored.deviceType);
        assertEquals(0, restored.firepointCount);
        assertEquals(1, restored.flamepointCount);
        assertEquals(1, restored.trailpointCount);
        assertEquals("#aeroie:fuel", restored.thrusterFuel);
        assertEquals(42_000.0F, restored.thrusterThrust);
        assertEquals(12, restored.flameSegments);
        assertEquals(0.75F, restored.trailRadius);

        CustomDeviceDefinition restoredWeapon = CustomDeviceDefinition.fromJson(weapon.toJson());
        assertEquals("WPN1", restoredWeapon.hudShortName);
    }

    @Test
    void decorationsUseOnlyFreeFormBonesAndPreserveEveryBoneTransform() {
        CustomDeviceDefinition decoration = CustomDeviceDefinition.createNew("test_decoration", "decoration");
        CustomDeviceDefinition.Bone first = decoration.findBone("bone_1");
        first.id = "root";
        first.model = "parts/decor.obj";
        first.rotation = new float[]{15.0F, 30.0F, 45.0F};
        first.scale = new float[]{0.5F, 2.0F, 3.0F};
        decoration.bones.add(CustomDeviceDefinition.Bone.child("detail", "root"));

        CustomDeviceDefinition restored = CustomDeviceDefinition.fromJson(decoration.toJson());

        assertEquals("decoration", restored.deviceType);
        assertEquals(0, restored.firepointCount);
        assertEquals("parts/decor.obj", restored.findBone("root").model);
        assertEquals("root", restored.findBone("detail").parent);
        assertEquals(30.0F, restored.findBone("root").rotation[1]);
        assertEquals(2.0F, restored.findBone("root").scale[1]);
    }

    @Test
    void customHeavyTurretsAndWeaponsRequireHudShortNames() {
        CustomDeviceDefinition weapon = CustomDeviceDefinition.createNew("test_weapon", "weapon");
        weapon.hudShortName = "";
        assertThrows(IllegalArgumentException.class, weapon::toJson);

        CustomDeviceDefinition turret = CustomDeviceDefinition.createNew("test_turret", "turret");
        turret.turretType = "heavyturret";
        assertThrows(IllegalArgumentException.class, turret::toJson);
        turret.hudShortName = "TURRET";
        assertEquals("TURRET", CustomDeviceDefinition.fromJson(turret.toJson()).hudShortName);
    }

    @Test
    void placerRoutesEachDefinitionTypeToItsMatchingCustomBlock() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomDevicePlacerItem.java"));

        org.junit.jupiter.api.Assertions.assertTrue(source.contains("CUSTOM_WEAPON_BLOCK"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("CUSTOM_THRUSTER_BLOCK"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("CUSTOM_DECORATION_BLOCK"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("CustomDeviceBlockEntity"));
    }

    @Test
    void selectorUsesExplicitLoadButtonAndDeviceFilters() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceSelectorScreen.java"));

        org.junit.jupiter.api.Assertions.assertTrue(source.contains("drawFilter(graphics"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("showThrusters"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("showWeapons"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("showTurrets"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("showDecorations"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("loadSelection()"));
        int enterBranch = source.indexOf("GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER");
        int nextBranch = source.indexOf("if (!files.isEmpty() &&", enterBranch);
        String enterBody = source.substring(enterBranch, nextBranch);
        org.junit.jupiter.api.Assertions.assertFalse(enterBody.contains("SelectCustomDeviceC2SPacket"));
    }

    @Test
    void customDeviceRendererSamplesWeaponFirepointsAndRendersThrusterFlames() throws IOException {
        String renderer = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceGeoRenderer.java"));
        String mesh = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceMeshRenderer.java"));
        String packet = Files.readString(Path.of("src/main/java/com/kodu16/vsie/network/turret/TurretFirePointC2SPacket.java"));

        org.junit.jupiter.api.Assertions.assertTrue(renderer.contains("CustomWeaponBlockEntity"));
        org.junit.jupiter.api.Assertions.assertTrue(mesh.contains("sampleFirepoint(weapon"));
        org.junit.jupiter.api.Assertions.assertTrue(mesh.contains("sampleFlamepoint(thruster"));
        org.junit.jupiter.api.Assertions.assertTrue(mesh.contains("new Vector4f(0.0F, 0.0F, 1.0F, 0.0F)"));
        org.junit.jupiter.api.Assertions.assertTrue(mesh.contains("isFlamepointBoneGroup"));
        org.junit.jupiter.api.Assertions.assertTrue(mesh.contains("isTrailpointBoneGroup"));
        org.junit.jupiter.api.Assertions.assertTrue(mesh.contains("CustomThrusterTrailRenderer.render"));
        org.junit.jupiter.api.Assertions.assertTrue(packet.contains("CustomWeaponBlockEntity"));
        org.junit.jupiter.api.Assertions.assertTrue(packet.contains("public final Vector3d direction"));
        org.junit.jupiter.api.Assertions.assertTrue(packet.contains("CustomThrusterBlockEntity"));
    }

    @Test
    void multiMaterialObjsKeepEveryMtlTextureInsteadOfBakingTheFirstOne() throws IOException {
        String renderer = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceMeshRenderer.java"));
        String mesh = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomObjMesh.java"));
        String cache = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceAssetCache.java"));
        String resolver = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomObjMaterialResolver.java"));
        String editor = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceEditorScreen.java"));

        org.junit.jupiter.api.Assertions.assertTrue(renderer.contains("renderMeshByMaterial("));
        org.junit.jupiter.api.Assertions.assertTrue(mesh.contains("renderMaterial("));
        org.junit.jupiter.api.Assertions.assertTrue(mesh.contains("trianglesByMaterial"));
        org.junit.jupiter.api.Assertions.assertTrue(cache.contains("materialTextures("));
        org.junit.jupiter.api.Assertions.assertTrue(resolver.contains("resolveTextures("));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("textureOverrideValue()"));
    }

    @Test
    void regionObjExportWarnsAboutAirAndSavesIntoAComponentsFolder() throws IOException {
        String screen = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/export/client/RegionObjExportScreen.java"));
        String exporter = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/export/client/RegionObjExporter.java"));
        String storage = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomDeviceStorage.java"));

        org.junit.jupiter.api.Assertions.assertTrue(screen.contains("air_warning"));
        org.junit.jupiter.api.Assertions.assertTrue(screen.contains("scanComponentsDirectories"));
        org.junit.jupiter.api.Assertions.assertTrue(exporter.contains("componentsRoot"));
        org.junit.jupiter.api.Assertions.assertTrue(storage.contains("componentsRoot()"));
    }

    @Test
    void editorKeepsDevicePropertiesClosedByDefaultAndRaisesTheLastInteractedPanel() throws IOException {
        String editor = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceEditorScreen.java"));

        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("panelZOrder()"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("topmostPanel"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("raisePanel("));
        int create = editor.indexOf("private void createNewDefinition(");
        int load = editor.indexOf("private void loadDefinition(", create);
        String createBody = editor.substring(create, load);
        org.junit.jupiter.api.Assertions.assertFalse(createBody.contains("turretPropertiesOpen = true"));
    }

    @Test
    void editorScrollsHierarchyAndKeepsBindRowsAndOverflowingTextReadable() throws IOException {
        String editor = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceEditorScreen.java"));

        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("hierarchyScroll"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("hierarchyVisibleRows()"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("drawBindRow("));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("insideBindButton("));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("clippedRight("));
    }

    @Test
    void editorExposesRotationAndScaleForSemanticBoneGroups() throws IOException {
        String editor = Files.readString(Path.of(
                "src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceEditorScreen.java"));

        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("isGroupTransformField"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("rotation[axis].setEditable(true)"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("scale[axis].setEditable(true)"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("drawRotationButtons"));
    }

    @Test
    void zeroEnergyCustomTurretsFireStandaloneWithoutAControlSeat() throws IOException {
        String turretBase = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/turret/AbstractTurretBlockEntity.java"));
        String customTurret = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomTurretBlockEntity.java"));

        org.junit.jupiter.api.Assertions.assertTrue(
                turretBase.contains("return getenergypertick() * Math.max(1, getCoolDown());"));
        org.junit.jupiter.api.Assertions.assertFalse(turretBase.contains("Math.max(1, getenergypertick()"));
        org.junit.jupiter.api.Assertions.assertTrue(
                customTurret.contains("return isEnergyTurret() ? definition.energyPerTick : super.getenergypertick();"));
    }

    @Test
    void customPhotonFxUseExternalNamespaceDirectionAlignmentAndMultiPositionPlayback() throws IOException {
        String resources = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomFxResources.java"));
        String transform = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomFxTransform.java"));
        String helper = Files.readString(Path.of("src/main/java/com/kodu16/vsie/utility/vsieFxHelper.java"));
        String turret = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomTurretBlockEntity.java"));
        String weapon = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomWeaponBlockEntity.java"));
        String thruster = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomThrusterBlockEntity.java"));
        String editor = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/client/CustomDeviceEditorScreen.java"));

        org.junit.jupiter.api.Assertions.assertTrue(resources.contains("AeroIE"));
        org.junit.jupiter.api.Assertions.assertTrue(resources.contains("fx"));
        org.junit.jupiter.api.Assertions.assertTrue(resources.contains("aeroie_custom"));
        org.junit.jupiter.api.Assertions.assertTrue(transform.contains("rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), forward)"));
        org.junit.jupiter.api.Assertions.assertTrue(helper.contains("CustomFxResources.open(location)"));
        org.junit.jupiter.api.Assertions.assertTrue(turret.contains("false, true"));
        org.junit.jupiter.api.Assertions.assertTrue(weapon.contains("false, true"));
        org.junit.jupiter.api.Assertions.assertTrue(thruster.contains("false, true"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("CustomFxSelectScreen"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("projectileFx"));
        org.junit.jupiter.api.Assertions.assertTrue(editor.contains("pointFx"));
    }

    @Test
    void customProjectileWeaponsUseFirepointVolleyInsteadOfRayOnlyFire() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/kodu16/vsie/content/aeroie_custom/CustomWeaponBlockEntity.java"));

        org.junit.jupiter.api.Assertions.assertTrue(source.contains("pendingVolleyIndex"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("definition.firepointIntervalTicks"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("new CustomTurretProjectileEntity"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("projectile.setPos(firepoint)"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("consumeAmmoForShot()"));
        org.junit.jupiter.api.Assertions.assertTrue(source.contains("beginProjectileVolley(serverLevel)"));
    }

    @Test
    void rejectsNestedCustomBonesAndEscapingResources() {
        CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("test_turret");
        definition.bones.add(CustomDeviceDefinition.Bone.child("child", "root"));
        definition.bones.add(CustomDeviceDefinition.Bone.child("nested", "child"));
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomDeviceDefinition.createNew("test_turret");
        definition.bones.get(0).model = "../outside.obj";
        assertThrows(IllegalArgumentException.class, definition::toJson);
    }

    @Test
    void rejectsLegacyBaseHierarchy() {
        String legacy = "{\"formatVersion\":1,\"id\":\"legacy\",\"name\":\"Legacy\",\"bones\":["
                + "{\"id\":\"base\",\"parent\":\"\",\"model\":\"\",\"texture\":\"\"},"
                + "{\"id\":\"old_part\",\"parent\":\"base\",\"model\":\"\",\"texture\":\"\"}]}";

        assertThrows(IllegalArgumentException.class, () -> CustomDeviceDefinition.fromJson(legacy));
    }

    @Test
    void rejectsObjOnRequiredBoneGroup() {
        CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("test_turret");
        definition.findBone("turret").model = "illegal.obj";
        assertThrows(IllegalArgumentException.class, definition::toJson);
    }

    @Test
    void firepointGroupsAreContiguousLongCannonChildrenAndTrackTheirCount() {
        CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("firepoints");
        assertEquals(1, definition.firepointCount);
        assertEquals("firepoint2", definition.addFirepoint().id);
        assertEquals("firepoint3", definition.addFirepoint().id);
        assertEquals(3, definition.firepointCount);
        assertEquals("firepoint3", definition.removeLastFirepoint().id);
        assertEquals(2, CustomDeviceDefinition.fromJson(definition.toJson()).firepointCount);
    }

    @Test
    void rejectsUserFirepointPrefixesAndFirepointsOutsideLongCannon() {
        CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("bad_firepoint");
        definition.bones.add(CustomDeviceDefinition.Bone.child("firepoint_helper", "root"));
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomDeviceDefinition.createNew("bad_parent");
        definition.findBone("firepoint1").parent = "cannon";
        assertThrows(IllegalArgumentException.class, definition::toJson);
    }

    @Test
    void rejectsInvalidTurretRuntimeProperties() {
        CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("test_turret");
        definition.turretType = "unknown";
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomDeviceDefinition.createNew("test_turret");
        definition.fireCooldownTicks = 0;
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomDeviceDefinition.createNew("test_turret");
        definition.weaponType = "unknown";
        assertThrows(IllegalArgumentException.class, definition::toJson);

        definition = CustomDeviceDefinition.createNew("test_turret");
        definition.rotationSpeedDegreesPerTick = Float.NaN;
        assertThrows(IllegalArgumentException.class, definition::toJson);
    }

    @Test
    void rootPivotUsesTheBlockBaseVerticallyWithoutTheExtraHalfBlockLift() {
        CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("test_turret");
        CustomDeviceDefinition.Bone root = definition.findBone("root");
        root.position = new float[]{2.0F, -1.0F, 4.0F};
        root.pivot = new float[]{0.25F, 0.75F, -0.5F};

        float[] offset = CustomDeviceAnchor.rootPivotOffset(definition);

        assertEquals(0.5F, offset[0] + root.position[0] + root.pivot[0]);
        assertEquals(0.0F, offset[1] + root.position[1] + root.pivot[1]);
        assertEquals(0.5F, offset[2] + root.position[2] + root.pivot[2]);
    }

    @Test
    void rootPivotWorldPositionUsesBlockCenterOnlyOnTheHorizontalAxes() {
        CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("anchor_contract");
        CustomDeviceDefinition.Bone root = definition.findBone("root");
        root.position = new float[]{2.0F, -3.0F, -0.25F};
        root.pivot = new float[]{0.25F, 0.75F, -0.5F};
        root.rotation = new float[]{90.0F, -45.0F, 180.0F};
        root.scale = new float[]{2.0F, 0.5F, 3.0F};

        float[] rootWorld = CustomDeviceAnchor.rootPivotWorldPosition(10, 20, -30, definition);

        assertEquals(10.5F, rootWorld[0], 0.0001F);
        assertEquals(20.0F, rootWorld[1], 0.0001F);
        assertEquals(-29.5F, rootWorld[2], 0.0001F);
    }

    @Test
    void placerUsesTheClickedAdjacentCellWithoutApplyingTheAuthoredRootPivot() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[]{8, 21, -3},
                CustomDeviceAnchor.placementTarget(8, 20, -3, 0, 1, 0));
    }

    @Test
    void commonTurretAnglesDriveOnlyTheirFixedBoneGroups() {
        assertEquals(-0.25F, CustomTurretRuntimePose.pitchRadians("cannon", 0.25F));
        assertEquals(0.0F, CustomTurretRuntimePose.pitchRadians("turret", 0.25F));
        assertEquals((float) Math.PI - 0.5F, CustomTurretRuntimePose.yawRadians("turret", -0.5F));
        assertEquals(0.0F, CustomTurretRuntimePose.yawRadians("cannon", -0.5F));
    }

    @Test
    void runtimeForwardMatchesTheTargetingBasisForCombinedYawAndPitch() {
        float physicalYaw = (float) Math.toRadians(40.0D);
        float pitch = (float) Math.toRadians(30.0D);
        float[] forward = CustomTurretRuntimePose.aimedBarrelForward(pitch, -physicalYaw);

        float horizontal = (float) Math.cos(pitch);
        assertEquals((float) Math.sin(physicalYaw) * horizontal, forward[0], 0.0001F);
        assertEquals((float) Math.sin(pitch), forward[1], 0.0001F);
        assertEquals(-(float) Math.cos(physicalYaw) * horizontal, forward[2], 0.0001F);
    }

    @Test
    void allMountFacesMapTheNeutralBarrelAndElevationAxesToTheTargetingBasis() {
        assertMountedAxes("down", new float[]{0, 0, -1}, new float[]{0, 1, 0}, new float[]{1, 0, 0});
        assertMountedAxes("up", new float[]{0, 0, -1}, new float[]{0, -1, 0}, new float[]{-1, 0, 0});
        assertMountedAxes("north", new float[]{0, 1, 0}, new float[]{0, 0, 1}, new float[]{1, 0, 0});
        assertMountedAxes("south", new float[]{0, -1, 0}, new float[]{0, 0, -1}, new float[]{1, 0, 0});
        assertMountedAxes("west", new float[]{0, 0, -1}, new float[]{1, 0, 0}, new float[]{0, -1, 0});
        assertMountedAxes("east", new float[]{0, 0, -1}, new float[]{-1, 0, 0}, new float[]{0, 1, 0});
    }

    private static void assertMountedAxes(String facing, float[] forward, float[] up, float[] right) {
        // Function: compare renderer axes with AbstractTurretBlockEntity.updateWorldControlAxes for each face.
        assertDirection(TurretMountAxes.transform(facing, 0, 0, -1), forward);
        assertDirection(TurretMountAxes.transform(facing, 0, 1, 0), up);
        assertDirection(TurretMountAxes.transform(facing, 1, 0, 0), right);
    }

    private static void assertDirection(float[] transformed, float[] expected) {
        assertEquals(expected[0], transformed[0], 0.0001F);
        assertEquals(expected[1], transformed[1], 0.0001F);
        assertEquals(expected[2], transformed[2], 0.0001F);
    }

    @Test
    void editorDeclaresPositiveZAsBarrelForwardAndInitiallyProjectsItLowerRight() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[]{0.0F, 0.0F, 1.0F},
                CustomTurretRuntimePose.defaultBarrelForward());

        float[] projected = CustomDeviceEditorCamera.projectInitialDirection(0.0F, 0.0F, 1.0F);
        org.junit.jupiter.api.Assertions.assertTrue(projected[0] > 0.0F, "+Z must point right initially");
        org.junit.jupiter.api.Assertions.assertTrue(projected[1] > 0.0F, "+Z must point down initially");
    }

    @Test
    void editorPanAccumulatesScreenSpaceMouseDrag() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[]{17.0F, -3.0F},
                CustomDeviceEditorCamera.pan(12.0F, 4.0F, 5.0F, -7.0F));
    }

    @Test
    void pivotDragKeepsRotatedScaledGeometryStationary() {
        CustomDeviceDefinition.Bone bone = CustomDeviceDefinition.Bone.child("barrel_mesh", "long_cannon");
        bone.position = new float[]{2.0F, -1.0F, 4.0F};
        bone.pivot = new float[]{0.25F, 0.75F, -0.5F};
        bone.rotation = new float[]{25.0F, -35.0F, 15.0F};
        bone.scale = new float[]{1.5F, 0.75F, 2.0F};
        float[] vertex = new float[]{3.0F, 1.0F, -2.0F};
        float[] before = CustomTurretPivotEdit.transformPoint(bone, vertex);

        CustomTurretPivotEdit.movePivotKeepingGeometryStable(bone, 2, 1.25F);

        float[] after = CustomTurretPivotEdit.transformPoint(bone, vertex);
        assertEquals(before[0], after[0], 0.0001F);
        assertEquals(before[1], after[1], 0.0001F);
        assertEquals(before[2], after[2], 0.0001F);
        assertEquals(0.75F, bone.pivot[2], 0.0001F);
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
