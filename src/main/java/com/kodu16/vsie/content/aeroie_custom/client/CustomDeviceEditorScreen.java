package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceDefinition;
import com.kodu16.vsie.content.aeroie_custom.CustomTurretPivotEdit;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceAssetPackage;
import com.kodu16.vsie.content.aeroie_custom.CustomTurretAnimationResources;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceStorage;
import com.kodu16.vsie.content.aeroie_custom.CustomTurretRuntimePose;
import com.kodu16.vsie.foundation.client.CrispScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Independent hierarchy/viewport/property editor for external custom-turret assets. */
public final class CustomDeviceEditorScreen extends CrispScreen {
    private static final int FRAME = 0xFF2E3238;
    private static final int PANEL = 0xFF3A3D42;
    private static final int SIDEBAR = 0xFF494D52;
    private static final int ACCENT = 0xFF87CEFA;
    private static final int TEXT = 0xFFE4FAF7;
    private static final int MUTED = 0xFF789B9D;
    private static final int FIELD_BACKGROUND = 0xCC272A2F;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_HEIGHT = 22;
    private static final int HIERARCHY_WIDTH = 150;
    private static final int HIERARCHY_HEIGHT = 228;
    private static final int PROPERTIES_DEFAULT_WIDTH = 276;
    private static final int PROPERTIES_DEFAULT_HEIGHT = 246;
    private static final int PROPERTIES_MIN_WIDTH = 250;
    private static final int PROPERTIES_MIN_HEIGHT = 96;
    private static final int FLOATING_PANEL_EDGE = 5;
    private static final int PROPERTIES_LABEL_WIDTH = 70;
    private static final int TURRET_PROPERTIES_DEFAULT_WIDTH = 276;
    private static final int TURRET_PROPERTIES_DEFAULT_HEIGHT = 246;
    private static final int TURRET_PROPERTIES_MIN_WIDTH = 250;
    private static final int TURRET_PROPERTIES_MIN_HEIGHT = 116;
    private static final int TURRET_PROPERTIES_LABEL_WIDTH = 112;
    private static final int FLOATING_TITLE_HEIGHT = 20;
    private static final int ROW_HEIGHT = 18;
    private static final int AXIS_X = 0xFFFF6464;
    private static final int AXIS_Y = 0xFF79D27C;
    private static final int AXIS_Z = 0xFF75A7FF;
    private static final int BARREL_FORWARD = 0xFFFFC857;

    private CustomDeviceDefinition definition;
    private int selectedBone;
    private int hierarchyScroll;
    private List<CustomDeviceDefinition> savedDefinitions = List.of();
    private final List<EditBox> allFields = new ArrayList<>();
    private EditBox definitionName;
    private EditBox boneId;
    private EditBox parentId;
    private EditBox modelPath;
    private EditBox texturePath;
    private EditBox pointFxPath;
    private EditBox pointFxScale;
    private final EditBox[] pointFxRotation = new EditBox[3];
    private final EditBox[] position = new EditBox[3];
    private final EditBox[] pivot = new EditBox[3];
    private final EditBox[] rotation = new EditBox[3];
    private final EditBox[] scale = new EditBox[3];
    private EditBox fireCooldown;
    private EditBox hudShortName;
    private EditBox rotationSpeed;
    private EditBox fireAnimationPath;
    private EditBox fireAnimationName;
    private EditBox energyPerTick;
    private EditBox laserRadius;
    private EditBox ammoItemId;
    private EditBox projectileScale;
    private EditBox projectileDamage;
    private EditBox projectileExplosionRadius;
    private EditBox projectileLifetime;
    private EditBox projectileSpeed;
    private EditBox projectileFxPath;
    private EditBox projectileFxScale;
    private final EditBox[] projectileFxRotation = new EditBox[3];
    private EditBox firepointInterval;
    private EditBox thrusterThrust;
    private EditBox thrusterFuelRate;
    private EditBox thrusterFuel;
    private EditBox flameSegments;
    private EditBox flameRadius;
    private EditBox trailRadius;
    private Component status = Component.empty();
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int viewportLeft;
    private int viewportRight;
    private int viewportTop;
    private int viewportBottom;
    private int hierarchyX;
    private int hierarchyY;
    private int propertiesX;
    private int propertiesY;
    private int propertiesWidth = PROPERTIES_DEFAULT_WIDTH;
    private int propertiesHeight = PROPERTIES_DEFAULT_HEIGHT;
    private int propertiesScroll;
    private int turretPropertiesX;
    private int turretPropertiesY;
    private int turretPropertiesWidth = TURRET_PROPERTIES_DEFAULT_WIDTH;
    private int turretPropertiesHeight = TURRET_PROPERTIES_DEFAULT_HEIGHT;
    private int turretPropertiesScroll;
    private boolean hierarchyCollapsed;
    private boolean propertiesCollapsed;
    private boolean turretPropertiesOpen;
    private boolean newDefinitionMenuOpen;
    private FloatingPanel draggedPanel;
    private FloatingPanel topmostPanel = FloatingPanel.PROPERTIES;
    private ResizedPanel resizedPanel = ResizedPanel.NONE;
    private boolean resizeLeft;
    private boolean resizeRight;
    private boolean resizeTop;
    private boolean resizeBottom;
    private boolean resizingTurretProperties;
    private boolean resizeTurretLeft;
    private boolean resizeTurretRight;
    private boolean resizeTurretTop;
    private boolean resizeTurretBottom;
    private int panelDragOffsetX;
    private int panelDragOffsetY;
    private TransformEditMode transformEditMode = TransformEditMode.POSITION;
    private float viewYaw = CustomDeviceEditorCamera.INITIAL_YAW_DEGREES;
    private float viewPitch = CustomDeviceEditorCamera.INITIAL_PITCH_DEGREES;
    private float viewScale = 36.0F;
    private float viewPanX;
    private float viewPanY;
    private int activeAxis = -1;
    private boolean rotatingView;
    private boolean panningView;

    public CustomDeviceEditorScreen() {
        super(Component.translatable("screen.vsie.custom_turret_editor"));
        savedDefinitions = CustomDeviceStorage.loadAllDefinitions();
        definition = null;
    }

    @Override
    protected void init() {
        panelWidth = Math.max(620, width - 20);
        panelHeight = Math.max(300, height - 18);
        panelLeft = (width - panelWidth) / 2;
        panelTop = (height - panelHeight) / 2;
        viewportLeft = panelLeft;
        viewportRight = panelLeft + panelWidth;
        viewportTop = panelTop + HEADER_HEIGHT;
        viewportBottom = panelTop + panelHeight - FOOTER_HEIGHT;
        if (hierarchyX == 0 && hierarchyY == 0) {
            hierarchyX = viewportLeft + 8;
            hierarchyY = viewportTop + 8;
        }
        if (propertiesX == 0 && propertiesY == 0) {
            propertiesX = viewportRight - propertiesWidth - 8;
            propertiesY = viewportTop + 8;
        }
        if (turretPropertiesX == 0 && turretPropertiesY == 0) {
            turretPropertiesX = viewportRight - turretPropertiesWidth - 8;
            turretPropertiesY = viewportBottom - turretPropertiesPanelHeight() - 8;
        }
        clampFloatingPanels();
        createFields();
        loadFields();
    }

    private void createFields() {
        allFields.clear();
        createPropertyFields();
        int x = turretPropertiesFieldX();
        int width = turretPropertiesFieldWidth();
        fireCooldown = addField(x, turretPropertyY(44), width, 6);
        hudShortName = addField(x, turretPropertyY(62), width, 12);
        rotationSpeed = addField(x, turretPropertyY(80), width, 12);
        fireAnimationPath = addField(x, turretPropertyY(98), width, 200);
        fireAnimationName = addField(x, turretPropertyY(116), width, 128);
        energyPerTick = addField(x, turretPropertyY(152), width, 9);
        laserRadius = addField(x, turretPropertyY(188), width, 12);
        ammoItemId = addField(x, turretPropertyY(152), width, 128);
        projectileScale = addField(x, turretPropertyY(170), width, 12);
        projectileDamage = addField(x, turretPropertyY(188), width, 12);
        projectileExplosionRadius = addField(x, turretPropertyY(206), width, 12);
        projectileLifetime = addField(x, turretPropertyY(224), width, 6);
        projectileSpeed = addField(x, turretPropertyY(242), width, 12);
        projectileFxPath = addField(x, turretPropertyY(260), width, 200);
        projectileFxScale = addField(x, turretPropertyY(278), width / 3 - 2, 12);
        for (int axis = 0; axis < 3; axis++) {
            projectileFxRotation[axis] = addField(x + axis * (width / 3), turretPropertyY(296),
                    width / 3 - 2, 20);
        }
        firepointInterval = addField(x, turretPropertyY(314), width, 6);
        thrusterThrust = addField(x, turretPropertyY(44), width, 12);
        thrusterFuelRate = addField(x, turretPropertyY(62), width, 8);
        thrusterFuel = addField(x, turretPropertyY(80), width, 128);
        flameSegments = addField(x, turretPropertyY(98), width, 3);
        flameRadius = addField(x, turretPropertyY(116), width, 12);
        trailRadius = addField(x, turretPropertyY(134), width, 12);
        projectileFxPath.setEditable(false);
        fireAnimationPath.setEditable(false);
    }

    private void createPropertyFields() {
        int fieldX = propertiesFieldX();
        int fieldWidth = propertiesFieldWidth();
        int y = propertyY(25);
        definitionName = addField(fieldX, y, fieldWidth, 64);
        boneId = addField(fieldX, y += ROW_HEIGHT, fieldWidth, 64);
        parentId = addField(fieldX, y += ROW_HEIGHT, fieldWidth, 64);
        modelPath = addField(fieldX, y += ROW_HEIGHT, fieldWidth, 200);
        texturePath = addField(fieldX, y += ROW_HEIGHT, fieldWidth, 200);
        for (int axis = 0; axis < 3; axis++) {
            position[axis] = addField(fieldX + axis * (fieldWidth / 3), y += axis == 0 ? ROW_HEIGHT : 0,
                    fieldWidth / 3 - 2, 20);
        }
        for (int axis = 0; axis < 3; axis++) {
            pivot[axis] = addField(fieldX + axis * (fieldWidth / 3), y += axis == 0 ? ROW_HEIGHT : 0,
                    fieldWidth / 3 - 2, 20);
        }
        for (int axis = 0; axis < 3; axis++) {
            rotation[axis] = addField(fieldX + axis * (fieldWidth / 3), y += axis == 0 ? ROW_HEIGHT : 0,
                    fieldWidth / 3 - 2, 20);
        }
        for (int axis = 0; axis < 3; axis++) {
            scale[axis] = addField(fieldX + axis * (fieldWidth / 3), y += axis == 0 ? ROW_HEIGHT : 0,
                    fieldWidth / 3 - 2, 20);
        }
        pointFxPath = addField(fieldX, y += ROW_HEIGHT, fieldWidth, 200);
        pointFxScale = addField(fieldX, y += ROW_HEIGHT, fieldWidth / 3 - 2, 12);
        for (int axis = 0; axis < 3; axis++) {
            pointFxRotation[axis] = addField(fieldX + axis * (fieldWidth / 3), y += axis == 0 ? ROW_HEIGHT : 0,
                    fieldWidth / 3 - 2, 20);
        }
    }

    private void positionPropertyFields() {
        clampPropertiesScroll();
        int fieldX = propertiesFieldX();
        int fieldWidth = propertiesFieldWidth();
        if (isSelectedBoneGroup()) {
            // Function: semantic groups lock identity/resources but still expose authored pivot, rotation and scale.
            for (int axis = 0; axis < 3; axis++) {
                moveField(pivot[axis], fieldX + axis * (fieldWidth / 3),
                        propertyY(25), fieldWidth / 3 - 2);
                moveField(rotation[axis], fieldX + axis * (fieldWidth / 3),
                        propertyY(43), fieldWidth / 3 - 2);
                moveField(scale[axis], fieldX + axis * (fieldWidth / 3),
                        propertyY(61), fieldWidth / 3 - 2);
            }
            int y = propertyY(81);
            moveField(pointFxPath, fieldX, y, fieldWidth);
            moveField(pointFxScale, fieldX, y += ROW_HEIGHT, fieldWidth / 3 - 2);
            for (int axis = 0; axis < 3; axis++) {
                moveField(pointFxRotation[axis], fieldX + axis * (fieldWidth / 3),
                        y + ROW_HEIGHT, fieldWidth / 3 - 2);
            }
            return;
        }
        int y = propertyY(25);
        moveField(definitionName, fieldX, y, fieldWidth);
        moveField(boneId, fieldX, y += ROW_HEIGHT, fieldWidth);
        moveField(parentId, fieldX, y += ROW_HEIGHT, fieldWidth);
        moveField(modelPath, fieldX, y += ROW_HEIGHT, fieldWidth);
        moveField(texturePath, fieldX, y += ROW_HEIGHT, fieldWidth);
        for (EditBox field : position) {
            moveField(field, fieldX + java.util.Arrays.asList(position).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT, fieldWidth / 3 - 2);
        }
        y += ROW_HEIGHT;
        for (EditBox field : pivot) {
            moveField(field, fieldX + java.util.Arrays.asList(pivot).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT, fieldWidth / 3 - 2);
        }
        y += ROW_HEIGHT;
        for (EditBox field : rotation) {
            moveField(field, fieldX + java.util.Arrays.asList(rotation).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT, fieldWidth / 3 - 2);
        }
        y += ROW_HEIGHT;
        for (EditBox field : scale) {
            moveField(field, fieldX + java.util.Arrays.asList(scale).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT, fieldWidth / 3 - 2);
        }
        y += ROW_HEIGHT;
        moveField(pointFxPath, fieldX, y + ROW_HEIGHT, fieldWidth);
        y += ROW_HEIGHT;
        moveField(pointFxScale, fieldX, y + ROW_HEIGHT, fieldWidth / 3 - 2);
        y += ROW_HEIGHT;
        for (EditBox field : pointFxRotation) {
            moveField(field, fieldX + java.util.Arrays.asList(pointFxRotation).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT, fieldWidth / 3 - 2);
        }
    }

    private static void moveField(EditBox field, int x, int y) {
        field.setX(x);
        field.setY(y);
    }

    private static void moveField(EditBox field, int x, int y, int width) {
        field.setX(x);
        field.setY(y);
        field.setWidth(width);
    }

    private int propertiesFieldX() {
        return propertiesX + PROPERTIES_LABEL_WIDTH + 8;
    }

    private int propertiesFieldWidth() {
        return Math.max(64, propertiesWidth - PROPERTIES_LABEL_WIDTH - 26);
    }

    private int propertyY(int contentY) {
        return propertiesY + contentY - propertiesScroll;
    }

    private int propertiesContentHeight() {
        if (isSelectedBoneGroup()) {
            return isSelectedPointFxBone() ? 106 : 44;
        }
        return isSelectedPointFxBone() ? 250 : 210;
    }

    private int propertiesVisibleContentHeight() {
        return Math.max(1, propertiesPanelHeight() - FLOATING_TITLE_HEIGHT - 6);
    }

    private int propertiesMaxScroll() {
        return Math.max(0, propertiesContentHeight() - propertiesVisibleContentHeight());
    }

    private void clampPropertiesScroll() {
        propertiesScroll = Math.max(0, Math.min(propertiesScroll, propertiesMaxScroll()));
    }

    private EditBox addField(int x, int y, int width, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, 16, Component.empty());
        field.setMaxLength(maxLength);
        field.setTextColor(TEXT);
        field.setTextColorUneditable(MUTED);
        field.setBordered(false);
        allFields.add(field);
        return addRenderableWidget(field);
    }

    private void loadFields() {
        if (definition == null) {
            for (EditBox field : allFields) {
                field.visible = false;
                field.setFocused(false);
            }
            return;
        }
        if (definition.bones.isEmpty()) {
            definition = CustomDeviceDefinition.createNew(definition.id);
        }
        selectedBone = Math.min(selectedBone, definition.bones.size() - 1);
        CustomDeviceDefinition.Bone bone = selectedBone();
        boolean boneGroup = CustomDeviceDefinition.isBoneGroup(bone.id);
        if (boneGroup) {
            // Function: fixed AeroIE bone groups expose only their pivot while remaining selectable in the hierarchy.
            transformEditMode = TransformEditMode.PIVOT;
        }
        definitionName.setValue(definition.name);
        definitionName.setEditable(!boneGroup);
        fireCooldown.setValue(Integer.toString(definition.fireCooldownTicks));
        hudShortName.setValue(definition.hudShortName);
        rotationSpeed.setValue(format(definition.rotationSpeedDegreesPerTick));
        fireAnimationPath.setValue(definition.fireAnimation);
        fireAnimationName.setValue(definition.fireAnimationName);
        energyPerTick.setValue(Integer.toString(definition.energyPerTick));
        laserRadius.setValue(format(definition.laserRadius));
        ammoItemId.setValue(definition.ammoItemId);
        projectileScale.setValue(format(definition.projectileScale));
        projectileDamage.setValue(format(definition.projectileDamage));
        projectileExplosionRadius.setValue(format(definition.projectileExplosionRadius));
        projectileLifetime.setValue(Integer.toString(definition.projectileLifetimeTicks));
        projectileSpeed.setValue(format(definition.projectileSpeedBlocksPerSecond));
        projectileFxPath.setValue(definition.projectileFx.fx);
        projectileFxScale.setValue(format(definition.projectileFx.scale));
        for (int axis = 0; axis < 3; axis++) {
            projectileFxRotation[axis].setValue(format(definition.projectileFx.rotation[axis]));
        }
        firepointInterval.setValue(Integer.toString(definition.firepointIntervalTicks));
        thrusterThrust.setValue(format(definition.thrusterThrust));
        thrusterFuelRate.setValue(Integer.toString(definition.thrusterFuelMbPerTickPerPercent));
        thrusterFuel.setValue(definition.thrusterFuel);
        flameSegments.setValue(Integer.toString(definition.flameSegments));
        flameRadius.setValue(format(definition.flameRadius));
        trailRadius.setValue(format(definition.trailRadius));
        boneId.setValue(bone.id);
        boneId.setEditable(!boneGroup);
        parentId.setValue(bone.parent);
        parentId.setEditable(!boneGroup);
        modelPath.setValue(bone.model);
        modelPath.setEditable(!boneGroup);
        // Function: imported/exported OBJ files populate an empty texture field from their MTL map_Kd link.
        texturePath.setValue(CustomDeviceAssetCache.texturePath(definition.deviceType, bone.model, bone.texture));
        texturePath.setEditable(!boneGroup);
        pointFxPath.setValue(bone.pointFx.fx);
        pointFxScale.setValue(format(bone.pointFx.scale));
        for (int axis = 0; axis < 3; axis++) {
            position[axis].setValue(format(bone.position[axis]));
            pivot[axis].setValue(format(bone.pivot[axis]));
            rotation[axis].setValue(format(bone.rotation[axis]));
            scale[axis].setValue(format(bone.scale[axis]));
            pointFxRotation[axis].setValue(format(bone.pointFx.rotation[axis]));
            position[axis].setEditable(!boneGroup);
            pivot[axis].setEditable(true);
            rotation[axis].setEditable(true);
            scale[axis].setEditable(true);
            pointFxRotation[axis].setEditable(isSelectedPointFxBone());
        }
        pointFxPath.setEditable(false);
        pointFxScale.setEditable(isSelectedPointFxBone());
        updatePropertyFieldVisibility();
        updateTurretFieldVisibility();
        clampFloatingPanels();
        positionPropertyFields();
    }

    private boolean applyFields(boolean validateDefinition) {
        try {
            if (definition == null) {
                status = Component.empty();
                return false;
            }
            synchronizePackageTexture();
            CustomDeviceDefinition.Bone bone = selectedBone();
            String previousId = bone.id;
            definition.name = definitionName.getValue().trim();
            definition.hudShortName = CustomDeviceDefinition.sanitizeHudShortName(hudShortName.getValue());
            definition.fireCooldownTicks = parsePositiveInt(fireCooldown);
            definition.rotationSpeedDegreesPerTick = parse(rotationSpeed);
            definition.fireAnimation = fireAnimationPath.getValue().trim();
            definition.fireAnimationName = fireAnimationName.getValue().trim();
            definition.energyPerTick = parseNonNegativeInt(energyPerTick);
            definition.laserRadius = parse(laserRadius);
            definition.ammoItemId = ammoItemId.getValue().trim();
            definition.projectileScale = parse(projectileScale);
            definition.projectileDamage = parse(projectileDamage);
            definition.projectileExplosionRadius = parse(projectileExplosionRadius);
            definition.projectileLifetimeTicks = parsePositiveInt(projectileLifetime);
            definition.projectileSpeedBlocksPerSecond = parse(projectileSpeed);
            definition.projectileFx.fx = projectileFxPath.getValue().trim();
            definition.projectileFx.scale = parse(projectileFxScale);
            for (int axis = 0; axis < 3; axis++) {
                definition.projectileFx.rotation[axis] = parse(projectileFxRotation[axis]);
            }
            definition.firepointIntervalTicks = parsePositiveInt(firepointInterval);
            definition.thrusterThrust = parse(thrusterThrust);
            definition.thrusterFuelMbPerTickPerPercent = parseNonNegativeInt(thrusterFuelRate);
            definition.thrusterFuel = thrusterFuel.getValue().trim();
            definition.flameSegments = parsePositiveInt(flameSegments);
            definition.flameRadius = parse(flameRadius);
            definition.trailRadius = parse(trailRadius);
            bone.id = boneId.getValue().trim().toLowerCase(Locale.ROOT);
            if (!"decoration".equals(definition.deviceType)
                    && !CustomDeviceDefinition.isFirepointBoneGroup(previousId) && bone.id.startsWith("firepoint")) {
                throw new IllegalArgumentException(Component.translatable(
                        "screen.vsie.custom_turret_editor.firepoint_prefix_reserved").getString());
            }
            if (!"decoration".equals(definition.deviceType)
                    && !CustomDeviceDefinition.isFlamepointBoneGroup(previousId) && bone.id.startsWith("flamepoint")) {
                throw new IllegalArgumentException(Component.translatable(
                        "screen.vsie.custom_turret_editor.flamepoint_prefix_reserved").getString());
            }
            if (!"decoration".equals(definition.deviceType)
                    && !CustomDeviceDefinition.isTrailpointBoneGroup(previousId) && bone.id.startsWith("trailpoint")) {
                throw new IllegalArgumentException(Component.translatable(
                        "screen.vsie.custom_turret_editor.trailpoint_prefix_reserved").getString());
            }
            bone.parent = parentId.getValue().trim().toLowerCase(Locale.ROOT);
            bone.model = modelPath.getValue().trim();
            bone.texture = textureOverrideValue();
            for (int axis = 0; axis < 3; axis++) {
                bone.position[axis] = parse(position[axis]);
                bone.pivot[axis] = parse(pivot[axis]);
                bone.rotation[axis] = parse(rotation[axis]);
                bone.scale[axis] = parse(scale[axis]);
                bone.pointFx.rotation[axis] = parse(pointFxRotation[axis]);
            }
            bone.pointFx.fx = pointFxPath.getValue().trim();
            bone.pointFx.scale = parse(pointFxScale);
            if (!previousId.equals(bone.id)) {
                for (CustomDeviceDefinition.Bone child : definition.bones) {
                    if (previousId.equals(child.parent)) {
                        child.parent = bone.id;
                    }
                }
            }
            if (validateDefinition) {
                definition.normalizeAndValidate();
            }
            return true;
        } catch (IllegalArgumentException exception) {
            status = exception.getMessage() == null
                    ? Component.translatable("screen.vsie.custom_turret_editor.invalid_value")
                    : Component.literal(exception.getMessage());
            return false;
        }
    }

    /** Keeps the texture override implicit while the field still matches the OBJ/MTL's resolved texture. */
    private String textureOverrideValue() {
        String fieldValue = texturePath.getValue().trim();
        String resolved = CustomDeviceAssetCache.texturePath(definition.deviceType, modelPath.getValue(), "");
        return fieldValue.equals(resolved) ? "" : fieldValue;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Function: blur the world before drawing the editor so the blur shader never processes GUI pixels.
        renderBackground(graphics, mouseX, mouseY, partialTick);
        RenderSystem.disableDepthTest();
        drawFrame(graphics);
        drawViewport(graphics);
        if (definition != null) {
            // Function: draw panels bottom-to-top so the last-interacted panel and every control in it stay on top.
            for (FloatingPanel panel : panelZOrder()) {
                switch (panel) {
                    case HIERARCHY -> drawHierarchy(graphics, mouseX, mouseY);
                    case PROPERTIES -> {
                        drawProperties(graphics, mouseX, mouseY);
                        drawFieldLayer(graphics, mouseX, mouseY, partialTick, false);
                    }
                    case TURRET_PROPERTIES -> {
                        if (turretPropertiesOpen) {
                            drawTurretProperties(graphics, mouseX, mouseY);
                            drawFieldLayer(graphics, mouseX, mouseY, partialTick, true);
                        }
                    }
                }
            }
        }
        if (newDefinitionMenuOpen) {
            drawNewDefinitionMenu(graphics, mouseX, mouseY);
        }
        graphics.flush();
        RenderSystem.enableDepthTest();
    }

    private void drawFieldLayer(GuiGraphics graphics, int mouseX, int mouseY, float partialTick,
                                boolean turretLayer) {
        boolean scissor = false;
        if (turretLayer) {
            if (turretPropertiesPanelHeight() > FLOATING_TITLE_HEIGHT + 5) {
                graphics.enableScissor(turretPropertiesX + 3, turretPropertiesY + FLOATING_TITLE_HEIGHT + 2,
                        turretPropertiesX + turretPropertiesWidth - 3, turretPropertiesY + turretPropertiesPanelHeight() - 3);
                scissor = true;
            }
        } else if (propertiesPanelHeight() > FLOATING_TITLE_HEIGHT + 5) {
            graphics.enableScissor(propertiesX + 3, propertiesY + FLOATING_TITLE_HEIGHT + 2,
                    propertiesX + propertiesWidth - 3, propertiesY + propertiesPanelHeight() - 3);
            scissor = true;
        }
        for (EditBox field : allFields) {
            if (!field.visible || isTurretPropertyField(field) != turretLayer) {
                continue;
            }
            drawFieldBackground(graphics, field);
            if (!field.isFocused()) {
                // Function: keep overlong values showing their tail so the right end never hides behind the box edge.
                field.setValue(field.getValue());
            }
            field.render(graphics, mouseX, mouseY, partialTick);
        }
        if (scissor) {
            graphics.disableScissor();
        }
    }

    private void drawFrame(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xA61C1E22);
        int right = panelLeft + panelWidth;
        int bottom = panelTop + panelHeight;
        graphics.fill(panelLeft - 2, panelTop - 2, right + 2, bottom + 2, 0xAA87CEFA);
        graphics.fill(panelLeft, panelTop, right, bottom, PANEL);
        graphics.fill(panelLeft, panelTop, right, panelTop + HEADER_HEIGHT, FRAME);
        graphics.fill(panelLeft, viewportBottom, right, bottom, FRAME);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.header"),
                panelLeft + 14, panelTop + 15, ACCENT, false);
        drawButton(graphics, panelLeft + panelWidth - 202, panelTop + 10, 54, 22,
                Component.translatable("screen.vsie.custom_turret_editor.load"), false);
        drawButton(graphics, panelLeft + panelWidth - 142, panelTop + 10, 54, 22,
                Component.translatable("screen.vsie.custom_turret_editor.new"), false);
        drawButton(graphics, panelLeft + panelWidth - 82, panelTop + 10, 68, 22,
                Component.translatable("screen.vsie.custom_turret_editor.save"), true);
        // Keep the device-properties toggle hidden until a definition exists; opening it is meaningless otherwise.
        if (definition != null && !"decoration".equals(definition.deviceType)) {
            drawTopToolIcons(graphics);
        }
        Component viewHint = Component.translatable("screen.vsie.custom_turret_editor.view_hint");
        int statusX = panelLeft + 12;
        int statusMaxWidth = viewportRight - font.width(viewHint) - 24 - statusX;
        // Function: trim the status tail-first so long messages never run off the right edge of the screen.
        graphics.drawString(font, Component.literal(clippedRight(status.getString(), statusMaxWidth)),
                statusX, viewportBottom + 7, MUTED, false);
        // Function: right-align the expanded pan/rotate/zoom hint without relying on language-specific width.
        graphics.drawString(font, viewHint, viewportRight - font.width(viewHint) - 12,
                viewportBottom + 7, MUTED, false);
    }

    private void drawNewDefinitionMenu(GuiGraphics graphics, int mouseX, int mouseY) {
        int menuWidth = Math.min(360, width - 34);
        int menuHeight = 108;
        int x = (width - menuWidth) / 2;
        int y = (height - menuHeight) / 2;
        graphics.fill(0, 0, width, height, 0xCC1C1E22);
        drawFloatingPanel(graphics, x, y, menuWidth, menuHeight);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.new_device_type"),
                x + 10, y + 7, ACCENT, false);
        int gap = 10;
        int buttonWidth = (menuWidth - 30) / 2;
        drawButton(graphics, x + 10, y + 34, buttonWidth, 24,
                Component.translatable("screen.vsie.custom_turret_editor.device_type.turret"), true);
        drawButton(graphics, x + 10 + buttonWidth + gap, y + 34, buttonWidth, 24,
                Component.translatable("screen.vsie.custom_turret_editor.device_type.weapon"), false);
        drawButton(graphics, x + 10, y + 66, buttonWidth, 24,
                Component.translatable("screen.vsie.custom_turret_editor.device_type.thruster"), false);
        drawButton(graphics, x + 10 + buttonWidth + gap, y + 66, buttonWidth, 24,
                Component.translatable("screen.vsie.custom_turret_editor.device_type.decoration"), false);
    }

    private void drawTopToolIcons(GuiGraphics graphics) {
        int x = turretPropertiesToolX();
        int y = panelTop + 9;
        graphics.fill(x - 5, y - 5, x + 29, y + 29, 0x66272A2F);
        graphics.fill(x - 1, y - 1, x + 25, y + 25, turretPropertiesOpen ? ACCENT : 0xFF565B61);
        graphics.fill(x, y, x + 24, y + 24, FRAME);
        // Function: the turret silhouette identifies the definition-level properties popup without text crowding the toolbar.
        graphics.fill(x + 5, y + 8, x + 15, y + 15, MUTED);
        graphics.fill(x + 9, y + 15, x + 13, y + 20, MUTED);
        graphics.fill(x + 14, y + 10, x + 21, y + 12, ACCENT);
        graphics.fill(x + 3, y + 20, x + 19, y + 22, ACCENT);
    }

    private void drawHierarchy(GuiGraphics graphics, int mouseX, int mouseY) {
        drawFloatingPanel(graphics, hierarchyX, hierarchyY, HIERARCHY_WIDTH, hierarchyPanelHeight());
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.bone_hierarchy"),
                hierarchyX + 7, hierarchyY + 6, ACCENT, false);
        drawCollapseIcon(graphics, hierarchyX + HIERARCHY_WIDTH - 17, hierarchyY + 3, hierarchyCollapsed);
        if (hierarchyCollapsed) {
            return;
        }
        List<Integer> orderedBones = orderedBoneIndices();
        hierarchyScroll = Math.max(0, Math.min(hierarchyScroll, hierarchyMaxScroll(orderedBones.size())));
        int listTop = hierarchyY + FLOATING_TITLE_HEIGHT + 4;
        for (int row = 0; row < hierarchyVisibleRows(); row++) {
            int boneRow = hierarchyScroll + row;
            if (boneRow >= orderedBones.size()) {
                break;
            }
            int index = orderedBones.get(boneRow);
            CustomDeviceDefinition.Bone bone = definition.bones.get(index);
            int y = listTop + row * 17;
            boolean selected = index == selectedBone;
            boolean hovered = mouseX >= hierarchyX + 4 && mouseX < hierarchyX + HIERARCHY_WIDTH - 4
                    && mouseY >= y && mouseY < y + 16;
            graphics.fill(hierarchyX + 4, y, hierarchyX + HIERARCHY_WIDTH - 4, y + 16,
                    selected ? 0xCC62686E : hovered ? 0x99565B61 : 0x00494D52);
            if (selected) {
                graphics.fill(hierarchyX + 4, y, hierarchyX + 7, y + 16, ACCENT);
            }
            int depth = boneDepth(bone);
            String marker = isSemanticBoneGroup(bone) ? "▣ " : "◇ ";
            String label = marker + bone.id;
            int labelX = hierarchyX + 10 + depth * 8;
            int maxLabelWidth = hierarchyX + HIERARCHY_WIDTH - 6 - labelX;
            String display = clippedRight(label, maxLabelWidth);
            // Function: overlong bone names right-align inside the panel so the meaningful tail stays visible.
            int displayX = font.width(label) <= maxLabelWidth
                    ? labelX : hierarchyX + HIERARCHY_WIDTH - 6 - font.width(display);
            graphics.drawString(font, display, displayX, y + 4, selected ? TEXT : 0xFFAAC3C1, false);
        }
        drawHierarchyScrollBar(graphics, orderedBones.size());
        int buttonsY = hierarchyY + HIERARCHY_HEIGHT - 24;
        if (!"decoration".equals(definition.deviceType)) {
            boolean managesFirepoints = canManageFirepoints();
            drawButton(graphics, hierarchyX + 6, buttonsY - 21, 66, 18,
                    Component.translatable("screen.vsie.custom_turret_editor.add_firepoint"), managesFirepoints);
            drawButton(graphics, hierarchyX + 78, buttonsY - 21, 66, 18,
                    Component.translatable("screen.vsie.custom_turret_editor.remove_firepoint"), managesFirepoints);
        }
        if ("thruster".equals(definition.deviceType)) {
            drawButton(graphics, hierarchyX + 6, buttonsY - 42, 66, 18,
                    Component.translatable("screen.vsie.custom_turret_editor.add_flamepoint"), false);
            drawButton(graphics, hierarchyX + 78, buttonsY - 42, 66, 18,
                    Component.translatable("screen.vsie.custom_turret_editor.add_trailpoint"), false);
        }
        drawButton(graphics, hierarchyX + 6, buttonsY, 66, 18,
                Component.translatable("screen.vsie.custom_turret_editor.add_bone"), false);
        drawButton(graphics, hierarchyX + 78, buttonsY, 66, 18,
                Component.translatable("screen.vsie.custom_turret_editor.remove_bone"), false);
    }

    private void drawProperties(GuiGraphics graphics, int mouseX, int mouseY) {
        drawFloatingPanel(graphics, propertiesX, propertiesY, propertiesWidth, propertiesPanelHeight());
        int x = propertiesX + 7;
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.properties"),
                x, propertiesY + 6, ACCENT, false);
        drawCollapseIcon(graphics, propertiesX + propertiesWidth - 17, propertiesY + 3, propertiesCollapsed);
        if (propertiesCollapsed) {
            return;
        }
        graphics.enableScissor(propertiesX + 3, propertiesY + FLOATING_TITLE_HEIGHT + 2,
                propertiesX + propertiesWidth - 3, propertiesY + propertiesPanelHeight() - 3);
        if (isSelectedBoneGroup()) {
            Component[] labels = {
                    Component.translatable("screen.vsie.custom_turret_editor.pivot"),
                    Component.translatable("screen.vsie.custom_turret_editor.rotation"),
                    Component.translatable("screen.vsie.custom_turret_editor.scale")
            };
            EditBox[] fields = {pivot[0], rotation[0], scale[0]};
            for (int index = 0; index < labels.length; index++) {
                graphics.drawString(font, clipped(labels[index], PROPERTIES_LABEL_WIDTH - 12),
                        x, fields[index].getY() + 4, MUTED, false);
            }
            drawPointFxControls(graphics, x);
            int rotationActionY = propertyActionY();
            graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.rotate_quarter"),
                    x, rotationActionY + 4, MUTED, false);
            drawRotationButtons(graphics, x, rotationActionY);
            graphics.disableScissor();
            drawPropertiesScrollBar(graphics);
            drawResizeHandle(graphics, propertiesX, propertiesY, propertiesWidth, propertiesPanelHeight());
            return;
        }
        Component[] labels = {
                Component.translatable("screen.vsie.custom_turret_editor.name"),
                Component.translatable("screen.vsie.custom_turret_editor.bone_id"),
                Component.translatable("screen.vsie.custom_turret_editor.parent"),
                Component.translatable("screen.vsie.custom_turret_editor.obj_model"),
                Component.translatable("screen.vsie.custom_turret_editor.texture"),
                Component.translatable("screen.vsie.custom_turret_editor.position"),
                Component.translatable("screen.vsie.custom_turret_editor.pivot"),
                Component.translatable("screen.vsie.custom_turret_editor.rotation"),
                Component.translatable("screen.vsie.custom_turret_editor.scale")
        };
        EditBox[] labelFields = {definitionName, boneId, parentId, modelPath, texturePath,
                position[0], pivot[0], rotation[0], scale[0]};
        for (int index = 0; index < labels.length; index++) {
            graphics.drawString(font, clipped(labels[index], PROPERTIES_LABEL_WIDTH - 12),
                    x, labelFields[index].getY() + 4, MUTED, false);
        }
        drawPointFxControls(graphics, x);
        int actionY = propertyActionY();
        int actionWidth = Math.min(150, propertiesWidth - 14);
        drawButton(graphics, x, actionY, actionWidth, 18,
                Component.translatable("screen.vsie.custom_turret_editor.select_asset_package"), false);
        drawButton(graphics, x + actionWidth + 6, actionY,
                Math.max(40, propertiesWidth - actionWidth - 27), 18,
                Component.translatable("screen.vsie.custom_turret_editor.parent_button"), false);
        drawButton(graphics, x, actionY + 24, 100, 18,
                Component.translatable(transformEditMode.translationKey), true);
        drawButton(graphics, x + 106, actionY + 24, 80, 18,
                Component.translatable("screen.vsie.custom_turret_editor.center_pivot"), false);
        graphics.drawString(font, clipped(Component.translatable("screen.vsie.custom_turret_editor.rotate_quarter"),
                        Math.max(0, propertiesWidth - 206)),
                x + 192, actionY + 29, MUTED, false);
        drawRotationButtons(graphics, x, actionY + 29);
        graphics.disableScissor();
        drawPropertiesScrollBar(graphics);
        drawResizeHandle(graphics, propertiesX, propertiesY, propertiesWidth, propertiesPanelHeight());
    }

    private void drawRotationButtons(GuiGraphics graphics, int x, int labelY) {
        String[] axisNames = {"X", "Y", "Z"};
        for (int axis = 0; axis < 3; axis++) {
            int buttonX = x + axis * 78;
            drawButton(graphics, buttonX, labelY + 18, 34, 18, Component.literal(axisNames[axis] + "-"), false);
            drawButton(graphics, buttonX + 38, labelY + 18, 34, 18,
                    Component.literal(axisNames[axis] + "+"), false);
        }
    }

    private void drawPointFxControls(GuiGraphics graphics, int x) {
        if (!isSelectedPointFxBone()) {
            return;
        }
        drawButton(graphics, x, pointFxPath.getY() - 1, 62, 18,
                Component.translatable("screen.vsie.custom_turret_editor.fx_bind"), false);
        graphics.drawString(font, clipped(Component.translatable("screen.vsie.custom_turret_editor.fx_scale"),
                        PROPERTIES_LABEL_WIDTH - 12),
                x, pointFxScale.getY() + 4, MUTED, false);
        graphics.drawString(font, clipped(Component.translatable("screen.vsie.custom_turret_editor.fx_rotation"),
                        PROPERTIES_LABEL_WIDTH - 12),
                x, pointFxRotation[0].getY() + 4, MUTED, false);
    }

    private void drawTurretProperties(GuiGraphics graphics, int mouseX, int mouseY) {
        drawFloatingPanel(graphics, turretPropertiesX, turretPropertiesY,
                turretPropertiesWidth, turretPropertiesPanelHeight());
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.device_properties"),
                turretPropertiesX + 7, turretPropertiesY + 6, ACCENT, false);
        drawCloseIcon(graphics, turretPropertiesX + turretPropertiesWidth - 17, turretPropertiesY + 4);
        graphics.enableScissor(turretPropertiesX + 3, turretPropertiesY + FLOATING_TITLE_HEIGHT + 2,
                turretPropertiesX + turretPropertiesWidth - 3, turretPropertiesY + turretPropertiesPanelHeight() - 3);
        if ("thruster".equals(definition.deviceType)) {
            drawTurretFieldLabel(graphics, thrusterThrust, "thruster_thrust");
            drawTurretFieldLabel(graphics, thrusterFuelRate, "thruster_fuel_rate");
            drawTurretFieldLabel(graphics, thrusterFuel, "thruster_fuel");
            drawTurretFieldLabel(graphics, flameSegments, "flame_segments");
            drawTurretFieldLabel(graphics, flameRadius, "flame_radius");
            drawTurretFieldLabel(graphics, trailRadius, "trail_radius");
            drawTurretLabel(graphics, Component.translatable("screen.vsie.custom_turret_editor.flame_color"),
                    turretPropertyY(155));
            drawColorButton(graphics, turretPropertiesFieldX(), turretPropertyY(149), definition.flameColor);
            drawTurretLabel(graphics, Component.translatable("screen.vsie.custom_turret_editor.trail_color"),
                    turretPropertyY(177));
            drawColorButton(graphics, turretPropertiesFieldX(), turretPropertyY(171), definition.trailColor);
            graphics.disableScissor();
            drawTurretPropertiesScrollBar(graphics);
            drawResizeHandle(graphics, turretPropertiesX, turretPropertiesY,
                    turretPropertiesWidth, turretPropertiesPanelHeight());
            return;
        }
        if ("turret".equals(definition.deviceType)) {
            drawTurretLabel(graphics, Component.translatable("screen.vsie.custom_turret_editor.turret_type"),
                    turretPropertyY(29));
            drawButton(graphics, turretPropertiesFieldX(), turretPropertyY(23),
                    turretPropertiesFieldWidth(), 18,
                    Component.translatable("screen.vsie.custom_turret_editor.turret_type." + definition.turretType), true);
        }
        drawTurretLabel(graphics, Component.translatable("screen.vsie.custom_turret_editor.fire_cooldown"),
                fireCooldown.getY() + 4);
        if (definitionRequiresHudShortName()) {
            drawTurretFieldLabel(graphics, hudShortName, "hud_short_name");
        }
        if ("turret".equals(definition.deviceType)) {
            drawTurretFieldLabel(graphics, rotationSpeed, "rotation_speed");
            drawTurretFieldLabel(graphics, fireAnimationPath, "fire_animation");
            drawBindRow(graphics, "fire_animation_pick", "fire_animation_bind", turretPropertyY(116));
            drawTurretFieldLabel(graphics, fireAnimationName, "fire_animation_name");
        }
        drawTurretLabel(graphics, Component.translatable("screen.vsie.custom_turret_editor.weapon_type"),
                turretPropertyY(137 + fireAnimationBindShift()));
        drawButton(graphics, turretPropertiesFieldX(), turretPropertyY(131 + fireAnimationBindShift()),
                turretPropertiesFieldWidth(), 18,
                Component.translatable("screen.vsie.custom_turret_editor.weapon_type." + definition.weaponType), true);
        if ("energy".equals(definition.weaponType)) {
            drawTurretFieldLabel(graphics, energyPerTick, "energy_per_tick");
            drawTurretLabel(graphics, Component.translatable("screen.vsie.custom_turret_editor.laser_color"),
                    turretPropertyY(173 + fireAnimationBindShift()));
            drawColorButton(graphics, turretPropertiesFieldX(),
                    turretPropertyY(167 + fireAnimationBindShift()), definition.laserColor);
            drawTurretFieldLabel(graphics, laserRadius, "laser_radius");
        } else {
            drawTurretFieldLabel(graphics, ammoItemId, "ammo_item");
            drawTurretFieldLabel(graphics, projectileScale, "projectile_scale");
            drawTurretFieldLabel(graphics, projectileDamage, "projectile_damage");
            drawTurretFieldLabel(graphics, projectileExplosionRadius, "projectile_explosion_radius");
            drawTurretFieldLabel(graphics, projectileLifetime, "projectile_lifetime");
            drawTurretFieldLabel(graphics, projectileSpeed, "projectile_speed");
            drawTurretFieldLabel(graphics, projectileFxPath, "projectile_fx");
            drawBindRow(graphics, "projectile_fx_pick", "fx_bind",
                    turretPropertyY(278 + fireAnimationBindShift()));
            drawTurretFieldLabel(graphics, projectileFxScale, "fx_scale");
            drawTurretFieldLabel(graphics, projectileFxRotation[0], "fx_rotation");
            if (definition.firepointCount > 1) {
                drawTurretFieldLabel(graphics, firepointInterval, "firepoint_interval");
            }
        }
        graphics.disableScissor();
        drawTurretPropertiesScrollBar(graphics);
        drawResizeHandle(graphics, turretPropertiesX, turretPropertiesY,
                turretPropertiesWidth, turretPropertiesPanelHeight());
    }

    private void drawCloseIcon(GuiGraphics graphics, int x, int y) {
        int color = 0xFFE4FAF7;
        for (int step = 0; step < 9; step++) {
            graphics.fill(x + step, y + step, x + step + 2, y + step + 2, color);
            graphics.fill(x + 8 - step, y + step, x + 10 - step, y + step + 2, color);
        }
    }

    private void drawTurretFieldLabel(GuiGraphics graphics, EditBox field, String key) {
        drawTurretLabel(graphics, Component.translatable("screen.vsie.custom_turret_editor." + key),
                field.getY() + 4);
    }

    private void drawBindRow(GuiGraphics graphics, String labelKey, String buttonKey, int y) {
        // Function: keep the picker label on the left and the bind button on the right of one dedicated row.
        drawTurretLabel(graphics, Component.translatable("screen.vsie.custom_turret_editor." + labelKey), y + 1);
        drawButton(graphics, turretPropertiesX + turretPropertiesWidth - 116, y, 108, 18,
                Component.translatable("screen.vsie.custom_turret_editor." + buttonKey), false);
    }

    private boolean insideBindButton(double mouseX, double mouseY, int y) {
        return inside(mouseX, mouseY, turretPropertiesX + turretPropertiesWidth - 116, y, 108, 18);
    }

    private void drawColorButton(GuiGraphics graphics, int x, int y, int color) {
        int width = Math.max(24, turretPropertiesFieldWidth());
        graphics.fill(x, y, x + width, y + 18, 0xFF565B61);
        graphics.fill(x + 2, y + 2, x + width - 2, y + 16, color);
    }

    private void drawTurretLabel(GuiGraphics graphics, Component label, int y) {
        graphics.drawString(font, clipped(label, TURRET_PROPERTIES_LABEL_WIDTH),
                turretPropertiesX + 7, y, MUTED, false);
    }

    private void drawFloatingPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x6687CEFA);
        graphics.fill(x, y, x + width, y + height, SIDEBAR);
        graphics.fill(x, y, x + width, y + FLOATING_TITLE_HEIGHT, FRAME);
        graphics.fill(x, y + FLOATING_TITLE_HEIGHT - 1, x + width, y + FLOATING_TITLE_HEIGHT, 0xAA87CEFA);
    }

    private void drawTurretPropertiesScrollBar(GuiGraphics graphics) {
        int maxScroll = turretPropertiesMaxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int trackTop = turretPropertiesY + FLOATING_TITLE_HEIGHT + 4;
        int trackBottom = turretPropertiesY + turretPropertiesPanelHeight() - 12;
        int trackHeight = Math.max(8, trackBottom - trackTop);
        int thumbHeight = Math.max(14,
                trackHeight * turretPropertiesVisibleContentHeight() / turretPropertiesContentHeight());
        int thumbY = trackTop + (trackHeight - thumbHeight) * turretPropertiesScroll / maxScroll;
        int x = turretPropertiesX + turretPropertiesWidth - 7;
        graphics.fill(x, trackTop, x + 3, trackBottom, 0x66494D52);
        graphics.fill(x - 1, thumbY, x + 4, thumbY + thumbHeight, ACCENT);
    }

    private void drawPropertiesScrollBar(GuiGraphics graphics) {
        int maxScroll = propertiesMaxScroll();
        if (maxScroll <= 0) {
            return;
        }
        int trackTop = propertiesY + FLOATING_TITLE_HEIGHT + 4;
        int trackBottom = propertiesY + propertiesPanelHeight() - 12;
        int trackHeight = Math.max(8, trackBottom - trackTop);
        int thumbHeight = Math.max(14, trackHeight * propertiesVisibleContentHeight() / propertiesContentHeight());
        int thumbY = trackTop + (trackHeight - thumbHeight) * propertiesScroll / maxScroll;
        int x = propertiesX + propertiesWidth - 7;
        graphics.fill(x, trackTop, x + 3, trackBottom, 0x66494D52);
        graphics.fill(x - 1, thumbY, x + 4, thumbY + thumbHeight, ACCENT);
    }

    private void drawResizeHandle(GuiGraphics graphics, int x, int y, int width, int height) {
        int right = x + width;
        int bottom = y + height;
        for (int step = 0; step < 10; step += 3) {
            graphics.fill(right - 3 - step, bottom - 1, right - 1 - step, bottom + 1, ACCENT);
            graphics.fill(right - 1, bottom - 3 - step, right + 1, bottom - 1 - step, ACCENT);
        }
    }

    /** Draws the compact in-box/out-of-box diagonal collapse state without relying on font glyphs. */
    private void drawCollapseIcon(GuiGraphics graphics, int x, int y, boolean collapsed) {
        int color = 0xFFAAC3C1;
        if (collapsed) {
            graphics.renderOutline(x, y + 5, 7, 7, color);
            for (int step = 0; step < 7; step++) {
                graphics.fill(x + 5 + step, y + 6 - step, x + 7 + step, y + 8 - step, color);
            }
            graphics.fill(x + 9, y, x + 13, y + 2, color);
            graphics.fill(x + 11, y, x + 13, y + 4, color);
        } else {
            graphics.renderOutline(x + 5, y + 5, 7, 7, color);
            for (int step = 0; step < 7; step++) {
                graphics.fill(x + step, y + step, x + step + 2, y + step + 2, color);
            }
            graphics.fill(x + 5, y + 7, x + 9, y + 9, color);
            graphics.fill(x + 7, y + 5, x + 9, y + 9, color);
        }
    }

    private void drawFieldBackground(GuiGraphics graphics, EditBox field) {
        graphics.fill(field.getX() - 2, field.getY() - 2,
                field.getX() + field.getWidth() + 2, field.getY() + field.getHeight() + 2,
                field.isFocused() ? ACCENT : 0xFF565B61);
        graphics.fill(field.getX(), field.getY(), field.getX() + field.getWidth(),
                field.getY() + field.getHeight(), FIELD_BACKGROUND);
    }

    private void drawViewport(GuiGraphics graphics) {
        if (definition == null) {
            graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
            for (int y = viewportTop; y < viewportBottom; y += 4) {
                graphics.fill(viewportLeft, y, viewportRight, y + 1, 0x0887CEFA);
            }
            graphics.disableScissor();
            return;
        }
        applyPreviewFields();
        graphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
        for (int y = viewportTop; y < viewportBottom; y += 4) {
            graphics.fill(viewportLeft, y, viewportRight, y + 1, 0x0887CEFA);
        }
        int centerX = (viewportLeft + viewportRight) / 2;
        int centerY = (viewportTop + viewportBottom) / 2;
        graphics.flush();
        RenderSystem.enableDepthTest();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(centerX + viewPanX, centerY + viewPanY, 200.0F);
        poseStack.scale(viewScale, -viewScale, viewScale);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(viewPitch));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(viewYaw));
        CustomDeviceMeshRenderer.render(definition, poseStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT);
        poseStack.popPose();
        graphics.flush();
        // Function: isolate the model depth layer before drawing an always-visible screen-space gizmo.
        clearDepthLayer();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        drawGizmo(graphics, centerX, centerY);
        drawDefaultBarrelDirection(graphics, centerX);
        graphics.flush();
        RenderSystem.enableCull();
        // Function: later floating windows start on a clean depth layer and therefore cover the viewport.
        clearDepthLayer();
        RenderSystem.disableDepthTest();
        graphics.disableScissor();
    }

    private static void clearDepthLayer() {
        RenderSystem.depthMask(true);
        RenderSystem.clear(256, net.minecraft.client.Minecraft.ON_OSX);
    }

    private void drawGizmo(GuiGraphics graphics, int centerX, int centerY) {
        AxisProjection projection = projectGizmo(centerX, centerY);
        int[] colors = {AXIS_X, AXIS_Y, AXIS_Z};
        String[] labels = {"X", "Y", "Z"};
        for (int axis = 0; axis < 3; axis++) {
            int color = activeAxis == axis ? ACCENT : colors[axis];
            drawLine(projection.origin, projection.ends[axis], color,
                    transformEditMode == TransformEditMode.PIVOT ? 2.0F : 1.0F);
            Vector2f end = projection.ends[axis];
            if (transformEditMode == TransformEditMode.POSITION) {
                drawArrowHead(graphics, projection.origin, end, color);
            } else {
                graphics.fill((int) end.x - 3, (int) end.y - 3, (int) end.x + 4, (int) end.y + 4, color);
            }
            graphics.drawString(font, labels[axis], (int) end.x + 4, (int) end.y - 4, color, false);
        }
        if (transformEditMode == TransformEditMode.PIVOT) {
            graphics.fill((int) projection.origin.x - 4, (int) projection.origin.y - 4,
                    (int) projection.origin.x + 5, (int) projection.origin.y + 5, ACCENT);
        }
    }

    private void drawDefaultBarrelDirection(GuiGraphics graphics, int centerX) {
        float[] forwardAxis = CustomTurretRuntimePose.defaultBarrelForward();
        Vector4f forward = new Vector4f(forwardAxis[0], forwardAxis[1], forwardAxis[2], 0.0F);
        cameraTransform().transform(forward);
        Vector2f screenDirection = new Vector2f(forward.x, -forward.y);
        if (screenDirection.lengthSquared() > 0.0001F) {
            screenDirection.normalize();
        }
        Vector2f origin = new Vector2f(centerX - 52.0F, viewportTop + 18.0F);
        Vector2f end = new Vector2f(origin).add(screenDirection.mul(58.0F));
        // Function: keep the orientation guide on the viewport edge so it never emerges through the model.
        drawLine(origin, end, BARREL_FORWARD, 1.5F);
        drawArrowHead(graphics, origin, end, BARREL_FORWARD);
        Component label = Component.translatable("screen.vsie.custom_turret_editor.default_barrel_direction");
        int labelWidth = font.width(label);
        int labelX = Math.max(viewportLeft + 4, Math.min(viewportRight - labelWidth - 6, (int) end.x + 8));
        int labelY = Math.max(viewportTop + 4, Math.min(viewportBottom - 13, (int) end.y - 4));
        graphics.fill(labelX - 2, labelY - 2, labelX + labelWidth + 2, labelY + 11, 0xCC272A2F);
        graphics.drawString(font, label, labelX, labelY, BARREL_FORWARD, false);
    }

    private void drawArrowHead(GuiGraphics graphics, Vector2f origin, Vector2f end, int color) {
        Vector2f direction = new Vector2f(end).sub(origin);
        if (direction.lengthSquared() > 0.001F) {
            direction.normalize();
        }
        Vector2f side = new Vector2f(-direction.y, direction.x);
        drawLine(end, new Vector2f(end).sub(new Vector2f(direction).mul(7)).add(new Vector2f(side).mul(3)), color, 1.0F);
        drawLine(end, new Vector2f(end).sub(new Vector2f(direction).mul(7)).sub(new Vector2f(side).mul(3)), color, 1.0F);
    }

    private void drawLine(Vector2f start, Vector2f end, int argb, float width) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        float a = (argb >>> 24 & 255) / 255.0F;
        float r = (argb >>> 16 & 255) / 255.0F;
        float g = (argb >>> 8 & 255) / 255.0F;
        float b = (argb & 255) / 255.0F;
        Vector2f direction = new Vector2f(end).sub(start);
        if (direction.lengthSquared() < 0.001F) {
            return;
        }
        direction.normalize().mul(width * 0.5F);
        Vector2f side = new Vector2f(-direction.y, direction.x);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(start.x + side.x, start.y + side.y, 300.0F).setColor(r, g, b, a);
        buffer.addVertex(start.x - side.x, start.y - side.y, 300.0F).setColor(r, g, b, a);
        buffer.addVertex(end.x - side.x, end.y - side.y, 300.0F).setColor(r, g, b, a);
        buffer.addVertex(end.x + side.x, end.y + side.y, 300.0F).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (newDefinitionMenuOpen) {
                return handleNewDefinitionMenuClick(mouseX, mouseY);
            }
            // The empty editor has no model or gizmo to manipulate, so only the header actions remain clickable.
            if (definition == null) {
                return handleHeaderClick(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
            }
            if (inside(mouseX, mouseY, turretPropertiesToolX(), panelTop + 9, 24, 24)) {
                turretPropertiesOpen = true;
                raisePanel(FloatingPanel.TURRET_PROPERTIES);
                updateTurretFieldVisibility();
                clampFloatingPanels();
                positionTurretPropertyFields();
                return true;
            }
            if (handleTopmostFloatingPanelClick(mouseX, mouseY, button)
                    || handleHeaderClick(mouseX, mouseY)) {
                return true;
            }
            if (insideViewport(mouseX, mouseY)) {
                AxisProjection projection = projectGizmo((viewportLeft + viewportRight) / 2,
                        (viewportTop + viewportBottom) / 2);
                activeAxis = nearestAxis(projection, (float) mouseX, (float) mouseY);
                if (activeAxis >= 0) {
                    return true;
                }
                // Function: empty viewport space begins camera panning without stealing axis manipulation.
                panningView = true;
                return true;
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && insideFloatingPanel(mouseX, mouseY)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && insideViewport(mouseX, mouseY)) {
            rotatingView = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleTopmostFloatingPanelClick(double mouseX, double mouseY, int button) {
        // Function: dispatch in visual Z order so the last-interacted top panel and its controls own every click.
        List<FloatingPanel> order = panelZOrder();
        for (int index = order.size() - 1; index >= 0; index--) {
            if (handleFloatingPanelClick(order.get(index), mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    private boolean handleFloatingPanelClick(FloatingPanel panel, double mouseX, double mouseY, int button) {
        return switch (panel) {
            case TURRET_PROPERTIES -> handleTurretPropertiesClick(mouseX, mouseY, button);
            case PROPERTIES -> handlePropertiesClick(mouseX, mouseY, button);
            case HIERARCHY -> handleHierarchyPanelClick(mouseX, mouseY, button);
        };
    }

    private boolean handleTurretPropertiesClick(double mouseX, double mouseY, int button) {
        if (!turretPropertiesOpen || !inside(mouseX, mouseY, turretPropertiesX, turretPropertiesY,
                turretPropertiesWidth, turretPropertiesPanelHeight())) {
            return false;
        }
        raisePanel(FloatingPanel.TURRET_PROPERTIES);
        if (beginTurretPropertyResize(mouseX, mouseY)) {
            return true;
        }
        if (inside(mouseX, mouseY, turretPropertiesX + turretPropertiesWidth - 21,
                turretPropertiesY, 21, FLOATING_TITLE_HEIGHT)) {
            closeTurretProperties();
        } else if (focusVisibleField(mouseX, mouseY, button, true)) {
            return true;
        } else if (inside(mouseX, mouseY, turretPropertiesX, turretPropertiesY,
                turretPropertiesWidth, FLOATING_TITLE_HEIGHT)) {
            beginPanelDrag(FloatingPanel.TURRET_PROPERTIES, mouseX, mouseY,
                    turretPropertiesX, turretPropertiesY);
        } else {
            handleTurretPropertyClick(mouseX, mouseY);
        }
        return true;
    }

    private boolean handlePropertiesClick(double mouseX, double mouseY, int button) {
        if (!inside(mouseX, mouseY, propertiesX, propertiesY, propertiesWidth, propertiesPanelHeight())) {
            return false;
        }
        raisePanel(FloatingPanel.PROPERTIES);
        if (beginPropertiesResize(mouseX, mouseY)) {
            return true;
        }
        if (inside(mouseX, mouseY, propertiesX + propertiesWidth - 20,
                propertiesY, 20, FLOATING_TITLE_HEIGHT)) {
            togglePropertiesPanel();
        } else if (focusVisibleField(mouseX, mouseY, button, false)) {
            return true;
        } else if (inside(mouseX, mouseY, propertiesX, propertiesY,
                propertiesWidth, FLOATING_TITLE_HEIGHT)) {
            beginPanelDrag(FloatingPanel.PROPERTIES, mouseX, mouseY, propertiesX, propertiesY);
        } else if (!propertiesCollapsed) {
            handlePropertyButtons(mouseX, mouseY);
        }
        return true;
    }

    private boolean handleHierarchyPanelClick(double mouseX, double mouseY, int button) {
        if (!inside(mouseX, mouseY, hierarchyX, hierarchyY, HIERARCHY_WIDTH, hierarchyPanelHeight())) {
            return false;
        }
        raisePanel(FloatingPanel.HIERARCHY);
        if (inside(mouseX, mouseY, hierarchyX + HIERARCHY_WIDTH - 20,
                hierarchyY, 20, FLOATING_TITLE_HEIGHT)) {
            toggleHierarchyPanel();
        } else if (inside(mouseX, mouseY, hierarchyX, hierarchyY,
                HIERARCHY_WIDTH, FLOATING_TITLE_HEIGHT)) {
            beginPanelDrag(FloatingPanel.HIERARCHY, mouseX, mouseY, hierarchyX, hierarchyY);
        } else if (!hierarchyCollapsed) {
            handleHierarchyClick(mouseX, mouseY);
        }
        return true;
    }

    private boolean focusVisibleField(double mouseX, double mouseY, int button, boolean turretLayer) {
        for (int index = allFields.size() - 1; index >= 0; index--) {
            EditBox field = allFields.get(index);
            if (field.visible && isTurretPropertyField(field) == turretLayer
                    && (!turretLayer || insideTurretPropertiesContent(mouseX, mouseY))
                    && (turretLayer || insidePropertiesContent(mouseX, mouseY))
                    && field.mouseClicked(mouseX, mouseY, button)) {
                // Function: explicitly assign Screen focus before any lower panel can consume this click.
                setFocused(field);
                setDragging(button == GLFW.GLFW_MOUSE_BUTTON_LEFT);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && resizingTurretProperties) {
            resizeTurretProperties(mouseX, mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && resizedPanel == ResizedPanel.PROPERTIES) {
            resizeProperties(mouseX, mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggedPanel != null) {
            if (draggedPanel == FloatingPanel.HIERARCHY) {
                hierarchyX = (int) mouseX - panelDragOffsetX;
                hierarchyY = (int) mouseY - panelDragOffsetY;
            } else if (draggedPanel == FloatingPanel.PROPERTIES) {
                propertiesX = (int) mouseX - panelDragOffsetX;
                propertiesY = (int) mouseY - panelDragOffsetY;
            } else {
                turretPropertiesX = (int) mouseX - panelDragOffsetX;
                turretPropertiesY = (int) mouseY - panelDragOffsetY;
            }
            clampFloatingPanels();
            positionPropertyFields();
            positionTurretPropertyFields();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && rotatingView) {
            viewYaw += (float) dragX * 0.6F;
            viewPitch = Math.max(-89.0F, Math.min(89.0F, viewPitch + (float) dragY * 0.6F));
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && activeAxis >= 0) {
            AxisProjection projection = projectGizmo((viewportLeft + viewportRight) / 2,
                    (viewportTop + viewportBottom) / 2);
            Vector2f direction = new Vector2f(projection.ends[activeAxis]).sub(projection.origin);
            float pixelsPerLocalUnit = direction.length() / 1.1F;
            if (pixelsPerLocalUnit > 0.1F) {
                direction.normalize();
                float amount = ((float) dragX * direction.x + (float) dragY * direction.y) / pixelsPerLocalUnit;
                CustomDeviceDefinition.Bone bone = selectedBone();
                if (transformEditMode == TransformEditMode.POSITION) {
                    bone.position[activeAxis] += amount;
                    position[activeAxis].setValue(format(bone.position[activeAxis]));
                } else {
                    CustomTurretPivotEdit.movePivotKeepingGeometryStable(bone, activeAxis, amount);
                    // Function: keep hidden position fields synchronized with the pivot-origin compensation.
                    for (int component = 0; component < 3; component++) {
                        position[component].setValue(format(bone.position[component]));
                    }
                    pivot[activeAxis].setValue(format(bone.pivot[activeAxis]));
                }
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && panningView) {
            float[] nextPan = CustomDeviceEditorCamera.pan(viewPanX, viewPanY, (float) dragX, (float) dragY);
            viewPanX = nextPan[0];
            viewPanY = nextPan[1];
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void applyPreviewFields() {
        synchronizePackageTexture();
        CustomDeviceDefinition.Bone bone = selectedBone();
        bone.model = modelPath.getValue().trim();
        bone.texture = textureOverrideValue();
        try {
            float[] nextPosition = new float[3];
            float[] nextPivot = new float[3];
            float[] nextRotation = new float[3];
            float[] nextScale = new float[3];
            for (int axis = 0; axis < 3; axis++) {
                nextPosition[axis] = parse(position[axis]);
                nextPivot[axis] = parse(pivot[axis]);
                nextRotation[axis] = parse(rotation[axis]);
                nextScale[axis] = parse(scale[axis]);
            }
            bone.position = nextPosition;
            boolean pivotCompensated = false;
            for (int axis = 0; axis < 3; axis++) {
                float pivotDelta = nextPivot[axis] - bone.pivot[axis];
                if (Math.abs(pivotDelta) > 0.000001F) {
                    CustomTurretPivotEdit.movePivotKeepingGeometryStable(bone, axis, pivotDelta);
                    pivotCompensated = true;
                }
            }
            // Function: numeric pivot edits use the same geometry-stable compensation as gizmo dragging.
            if (pivotCompensated) {
                for (int axis = 0; axis < 3; axis++) {
                    position[axis].setValue(format(bone.position[axis]));
                }
            }
            bone.rotation = nextRotation;
            bone.scale = nextScale;
        } catch (IllegalArgumentException ignored) {
            // Partially typed numeric fields keep the last valid preview transform.
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            activeAxis = -1;
            draggedPanel = null;
            resizedPanel = ResizedPanel.NONE;
            resizingTurretProperties = false;
            panningView = false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rotatingView = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (turretPropertiesOpen && inside(mouseX, mouseY, turretPropertiesX, turretPropertiesY,
                turretPropertiesWidth, turretPropertiesPanelHeight())) {
            turretPropertiesScroll = Math.max(0, Math.min(turretPropertiesMaxScroll(),
                    turretPropertiesScroll + (scrollY < 0 ? ROW_HEIGHT : -ROW_HEIGHT)));
            positionTurretPropertyFields();
            return true;
        }
        if (inside(mouseX, mouseY, propertiesX, propertiesY, propertiesWidth, propertiesPanelHeight())) {
            propertiesScroll = Math.max(0, Math.min(propertiesMaxScroll(),
                    propertiesScroll + (scrollY < 0 ? ROW_HEIGHT : -ROW_HEIGHT)));
            positionPropertyFields();
            return true;
        }
        if (definition != null
                && inside(mouseX, mouseY, hierarchyX, hierarchyY, HIERARCHY_WIDTH, hierarchyPanelHeight())) {
            List<Integer> orderedBones = orderedBoneIndices();
            hierarchyScroll = Math.max(0, Math.min(hierarchyMaxScroll(orderedBones.size()),
                    hierarchyScroll + (scrollY < 0 ? 3 : -3)));
            return true;
        }
        if (insideFloatingPanel(mouseX, mouseY)) {
            return true;
        }
        if (insideViewport(mouseX, mouseY)) {
            viewScale = Math.max(2.0F, Math.min(300.0F, viewScale * (scrollY > 0 ? 1.12F : 0.89F)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean handleHeaderClick(double mouseX, double mouseY) {
        int buttonX = panelLeft + panelWidth - 202;
        if (!inside(mouseX, mouseY, buttonX, panelTop + 10, 54, 22)
                && !inside(mouseX, mouseY, buttonX + 60, panelTop + 10, 54, 22)
                && !inside(mouseX, mouseY, buttonX + 120, panelTop + 10, 68, 22)) {
            return false;
        }
        if (inside(mouseX, mouseY, buttonX, panelTop + 10, 54, 22)) {
            openLoadDialog();
        } else if (inside(mouseX, mouseY, buttonX + 60, panelTop + 10, 54, 22)) {
            newDefinitionMenuOpen = true;
        } else {
            openSaveDialog();
        }
        return true;
    }

    private boolean handleNewDefinitionMenuClick(double mouseX, double mouseY) {
        int menuWidth = Math.min(360, width - 34);
        int menuHeight = 108;
        int x = (width - menuWidth) / 2;
        int y = (height - menuHeight) / 2;
        int gap = 10;
        int buttonWidth = (menuWidth - 30) / 2;
        if (inside(mouseX, mouseY, x + 10, y + 34, buttonWidth, 24)) {
            createNewDefinition("turret");
            return true;
        }
        if (inside(mouseX, mouseY, x + 10 + buttonWidth + gap, y + 34, buttonWidth, 24)) {
            createNewDefinition("weapon");
            return true;
        }
        if (inside(mouseX, mouseY, x + 10, y + 66, buttonWidth, 24)) {
            createNewDefinition("thruster");
            return true;
        }
        if (inside(mouseX, mouseY, x + 10 + buttonWidth + gap, y + 66, buttonWidth, 24)) {
            createNewDefinition("decoration");
            return true;
        }
        if (!inside(mouseX, mouseY, x, y, menuWidth, menuHeight)) {
            newDefinitionMenuOpen = false;
            return true;
        }
        return true;
    }

    private void createNewDefinition(String deviceType) {
        definition = CustomDeviceDefinition.createNew(nextDefinitionId(deviceType), deviceType);
        if ("decoration".equals(deviceType)) {
            turretPropertiesOpen = false;
        }
        selectedBone = 0;
        hierarchyScroll = 0;
        newDefinitionMenuOpen = false;
        status = Component.translatable("screen.vsie.custom_turret_editor.new_definition");
        loadFields();
    }

    private boolean handleHierarchyClick(double mouseX, double mouseY) {
        int row = (int) ((mouseY - (hierarchyY + FLOATING_TITLE_HEIGHT + 4)) / 17);
        List<Integer> orderedBones = orderedBoneIndices();
        if (mouseX >= hierarchyX + 4 && mouseX < hierarchyX + HIERARCHY_WIDTH - 4
                && row >= 0 && hierarchyScroll + row < orderedBones.size()) {
            applyFields(false);
            selectedBone = orderedBones.get(hierarchyScroll + row);
            loadFields();
            return true;
        }
        int y = hierarchyY + HIERARCHY_HEIGHT - 24;
        if ("thruster".equals(definition.deviceType)
                && inside(mouseX, mouseY, hierarchyX + 6, y - 42, 66, 18)) {
            applyFields(false);
            CustomDeviceDefinition.Bone flamepoint = definition.addFlamepoint();
            selectedBone = definition.bones.indexOf(flamepoint);
            loadFields();
            return true;
        }
        if ("thruster".equals(definition.deviceType)
                && inside(mouseX, mouseY, hierarchyX + 78, y - 42, 66, 18)) {
            applyFields(false);
            CustomDeviceDefinition.Bone trailpoint = definition.addTrailpoint();
            selectedBone = definition.bones.indexOf(trailpoint);
            loadFields();
            return true;
        }
        if (!"decoration".equals(definition.deviceType)
                && inside(mouseX, mouseY, hierarchyX + 6, y - 21, 66, 18)) {
            if (!canManageFirepoints()) {
                status = Component.translatable("screen.vsie.custom_turret_editor.select_firepoint_parent");
                return true;
            }
            applyFields(false);
            CustomDeviceDefinition.Bone firepoint = definition.addFirepoint();
            selectedBone = definition.bones.indexOf(firepoint);
            loadFields();
            return true;
        }
        if (!"decoration".equals(definition.deviceType)
                && inside(mouseX, mouseY, hierarchyX + 78, y - 21, 66, 18)) {
            if (!canManageFirepoints()) {
                status = Component.translatable("screen.vsie.custom_turret_editor.select_firepoint_parent");
                return true;
            }
            applyFields(false);
            CustomDeviceDefinition.Bone removed = definition.removeLastFirepoint();
            if (removed != null && selectedBone >= definition.bones.size()) {
                selectedBone = definition.bones.indexOf(definition.findBone("firepoint" + definition.firepointCount));
            }
            loadFields();
            return true;
        }
        if (inside(mouseX, mouseY, hierarchyX + 6, y, 66, 18)) {
            applyFields(false);
            String id = nextBoneId();
            String group = "decoration".equals(definition.deviceType) ? selectedBone().id
                    : CustomDeviceDefinition.isBoneGroup(selectedBone().id)
                    ? selectedBone().id : selectedBone().parent;
            definition.bones.add(CustomDeviceDefinition.Bone.child(id, group));
            selectedBone = definition.bones.size() - 1;
            loadFields();
            return true;
        }
        if (inside(mouseX, mouseY, hierarchyX + 78, y, 66, 18)
                && ("decoration".equals(definition.deviceType)
                || !CustomDeviceDefinition.isBoneGroup(selectedBone().id))) {
            removeSelectedBone();
            return true;
        }
        return false;
    }

    private boolean canManageFirepoints() {
        if (definition == null || "thruster".equals(definition.deviceType)
                || "decoration".equals(definition.deviceType)) {
            return false;
        }
        return "turret".equals(definition.deviceType) ? "long_cannon".equals(selectedBone().id)
                : CustomDeviceDefinition.isBoneGroup(selectedBone().id);
    }

    private boolean handlePropertyButtons(double mouseX, double mouseY) {
        int x = propertiesX + 7;
        int y = propertyActionY();
        int actionWidth = Math.min(150, propertiesWidth - 14);
        int parentWidth = Math.max(40, propertiesWidth - actionWidth - 27);
        if (isSelectedPointFxBone() && inside(mouseX, mouseY, x, pointFxPath.getY() - 1, 62, 18)) {
            openPointFxPicker();
            return true;
        }
        if (isSelectedBoneGroup()) {
            return handleRotationButtons(mouseX, mouseY, x, y);
        }
        if (inside(mouseX, mouseY, x, y, actionWidth, 18)) {
            minecraft.setScreen(new CustomDeviceAssetPackageScreen(definition.deviceType, this::selectAssetPackage, this));
            return true;
        }
        if (inside(mouseX, mouseY, x + actionWidth + 6, y, parentWidth, 18)) {
            cycleParent();
            return true;
        }
        if (inside(mouseX, mouseY, x, y + 24, 100, 18)) {
            transformEditMode = transformEditMode == TransformEditMode.POSITION
                    ? TransformEditMode.PIVOT : TransformEditMode.POSITION;
            return true;
        }
        if (inside(mouseX, mouseY, x + 106, y + 24, 80, 18)) {
            centerPivotOnModel();
            return true;
        }
        return handleRotationButtons(mouseX, mouseY, x, y + 29);
    }

    private boolean handleRotationButtons(double mouseX, double mouseY, int x, int labelY) {
        for (int axis = 0; axis < 3; axis++) {
            int buttonX = x + axis * 78;
            if (inside(mouseX, mouseY, buttonX, labelY + 18, 34, 18)) {
                rotateByQuarter(axis, -90.0F);
                return true;
            }
            if (inside(mouseX, mouseY, buttonX + 38, labelY + 18, 34, 18)) {
                rotateByQuarter(axis, 90.0F);
                return true;
            }
        }
        return false;
    }

    private boolean handleTurretPropertyClick(double mouseX, double mouseY) {
        if (!turretPropertiesOpen) {
            return false;
        }
        if (inside(mouseX, mouseY, turretPropertiesFieldX(), turretPropertyY(23),
                turretPropertiesFieldWidth(), 18)) {
            if (!"turret".equals(definition.deviceType)) {
                return false;
            }
            int index = CustomDeviceDefinition.TURRET_TYPES.indexOf(definition.turretType);
            definition.turretType = CustomDeviceDefinition.TURRET_TYPES.get(
                    (index + 1) % CustomDeviceDefinition.TURRET_TYPES.size());
            if ("heavyturret".equals(definition.turretType) && definition.hudShortName.isBlank()) {
                // Function: heavy custom turrets must always have a compact control-seat HUD label.
                definition.hudShortName = "TURRET";
                hudShortName.setValue(definition.hudShortName);
            }
            updateTurretFieldVisibility();
            clampFloatingPanels();
            return true;
        }
        if ("turret".equals(definition.deviceType)
                && insideBindButton(mouseX, mouseY, turretPropertyY(116))) {
            openFireAnimationPicker();
            return true;
        }
        if (inside(mouseX, mouseY, turretPropertiesFieldX(), turretPropertyY(131 + fireAnimationBindShift()),
                turretPropertiesFieldWidth(), 18)) {
            definition.weaponType = "energy".equals(definition.weaponType) ? "projectile" : "energy";
            updateTurretFieldVisibility();
            clampFloatingPanels();
            return true;
        }
        if ("energy".equals(definition.weaponType)
                && inside(mouseX, mouseY, turretPropertiesFieldX(), turretPropertyY(167 + fireAnimationBindShift()),
                turretPropertiesFieldWidth(), 18)) {
            minecraft.setScreen(new CustomTurretColorScreen(definition.laserColor, color -> {
                definition.laserColor = color;
                minecraft.setScreen(this);
            }, this));
            return true;
        }
        if (isWeaponLikeDefinition() && !"energy".equals(definition.weaponType)
                && insideBindButton(mouseX, mouseY, turretPropertyY(278 + fireAnimationBindShift()))) {
            openProjectileFxPicker();
            return true;
        }
        if ("thruster".equals(definition.deviceType)
                && inside(mouseX, mouseY, turretPropertiesFieldX(), turretPropertyY(149),
                turretPropertiesFieldWidth(), 18)) {
            minecraft.setScreen(new CustomTurretColorScreen(definition.flameColor, color -> {
                definition.flameColor = color;
                minecraft.setScreen(this);
            }, this));
            return true;
        }
        if ("thruster".equals(definition.deviceType)
                && inside(mouseX, mouseY, turretPropertiesFieldX(), turretPropertyY(171),
                turretPropertiesFieldWidth(), 18)) {
            minecraft.setScreen(new CustomTurretColorScreen(definition.trailColor, color -> {
                definition.trailColor = color;
                minecraft.setScreen(this);
            }, this));
            return true;
        }
        return false;
    }

    private void centerPivotOnModel() {
        applyFields(false);
        CustomDeviceDefinition.Bone bone = selectedBone();
        Vector3f center = CustomDeviceAssetCache.modelCenter(definition.deviceType, bone.model);
        if (center == null) {
            status = Component.translatable("screen.vsie.custom_turret_editor.center_pivot_unavailable");
            return;
        }
        // Function: the OBJ-local AABB center becomes the GeckoLib pivot retained in the definition.
        bone.pivot = new float[]{center.x, center.y, center.z};
        for (int axis = 0; axis < 3; axis++) {
            pivot[axis].setValue(format(bone.pivot[axis]));
        }
        transformEditMode = TransformEditMode.PIVOT;
        status = Component.translatable("screen.vsie.custom_turret_editor.center_pivot_done");
    }

    private void rotateByQuarter(int axis, float amount) {
        applyFields(false);
        CustomDeviceDefinition.Bone bone = selectedBone();
        bone.rotation[axis] = normalizeDegrees(bone.rotation[axis] + amount);
        rotation[axis].setValue(format(bone.rotation[axis]));
    }

    private void selectAssetPackage(CustomDeviceAssetPackage selected) {
        modelPath.setValue(selected.modelPath());
        texturePath.setValue(selected.texturePath());
        applyFields(false);
        CustomDeviceAssetCache.invalidateMeshes();
    }

    private void openPointFxPicker() {
        applyFields(false);
        minecraft.setScreen(new CustomFxSelectScreen(value -> {
            selectedBone().pointFx.fx = value;
            pointFxPath.setValue(value);
            minecraft.setScreen(this);
        }, () -> minecraft.setScreen(this)));
    }

    private void openProjectileFxPicker() {
        applyFields(false);
        minecraft.setScreen(new CustomFxSelectScreen(value -> {
            definition.projectileFx.fx = value;
            projectileFxPath.setValue(value);
            minecraft.setScreen(this);
        }, () -> minecraft.setScreen(this)));
    }

    private void openFireAnimationPicker() {
        applyFields(false);
        minecraft.setScreen(new CustomTurretAnimationSelectScreen(value -> {
            definition.fireAnimation = value;
            definition.fireAnimationName = value.isBlank() ? "" : CustomTurretAnimationResources.defaultAnimationName(value);
            fireAnimationPath.setValue(definition.fireAnimation);
            fireAnimationName.setValue(definition.fireAnimationName);
            updateTurretFieldVisibility();
            minecraft.setScreen(this);
        }, () -> minecraft.setScreen(this)));
    }

    private void synchronizePackageTexture() {
        // Function: a folder package owns exactly one same-named texture, so manual PNG mismatches cannot persist.
        CustomDeviceAssetPackage.fromModelPath(modelPath.getValue())
                .ifPresent(assetPackage -> texturePath.setValue(assetPackage.texturePath()));
    }

    private void cycleParent() {
        applyFields(false);
        CustomDeviceDefinition.Bone selected = selectedBone();
        if (!"decoration".equals(definition.deviceType)
                && CustomDeviceDefinition.isRequiredBoneGroup(selected.id)) {
            status = Component.translatable("screen.vsie.custom_turret_editor.required_bone_locked");
            return;
        }
        List<String> candidates = new ArrayList<>();
        if ("turret".equals(definition.deviceType)) {
            candidates.addAll(CustomDeviceDefinition.REQUIRED_BONES);
        } else {
            // Function: free-form devices may be rooted or parented to any non-descendant ordinary bone.
            candidates.add("");
            definition.bones.stream()
                    .filter(candidate -> candidate != selected && !isDescendant(candidate, selected.id))
                    .map(candidate -> candidate.id)
                    .forEach(candidates::add);
        }
        int index = candidates.indexOf(selected.parent);
        selected.parent = candidates.get((index + 1) % candidates.size());
        parentId.setValue(selected.parent);
    }

    private void saveDefinition(String folderPath, String fileName) {
        if (!applyFields(true)) {
            return;
        }
        try {
            definition.id = CustomDeviceStorage.definitionId(fileName);
            CustomDeviceStorage.saveDefinition(definition, folderPath, fileName);
            savedDefinitions = CustomDeviceStorage.loadAllDefinitions();
            CustomDeviceAssetCache.invalidateMeshes();
            status = Component.translatable("screen.vsie.custom_turret_editor.saved", definition.name);
        } catch (IOException | IllegalArgumentException exception) {
            status = exception.getMessage() == null
                    ? Component.translatable("screen.vsie.custom_turret_editor.save_failed")
                    : Component.literal(exception.getMessage());
        }
    }

    private void openSaveDialog() {
        if (!applyFields(false)) {
            return;
        }
        minecraft.setScreen(new CustomDeviceDefinitionSaveScreen(this,
                definition.id, (folderPath, fileName) -> saveDefinition(folderPath, fileName)));
    }

    private void openLoadDialog() {
        minecraft.setScreen(new CustomDeviceDefinitionLoadScreen(this, this::loadDefinition));
    }

    private void loadDefinition(CustomDeviceDefinition loaded) {
        definition = loaded.copy();
        if ("decoration".equals(definition.deviceType)) {
            turretPropertiesOpen = false;
        }
        selectedBone = 0;
        hierarchyScroll = 0;
        savedDefinitions = CustomDeviceStorage.loadAllDefinitions();
        CustomDeviceAssetCache.invalidateMeshes();
        loadFields();
        status = Component.translatable("screen.vsie.custom_turret_editor.loaded", definition.name);
    }

    private void removeSelectedBone() {
        applyFields(false);
        if (definition.bones.size() <= 1) {
            status = Component.translatable("screen.vsie.custom_turret_editor.minimum_one_bone");
            return;
        }
        CustomDeviceDefinition.Bone removed = selectedBone();
        String replacementParent = removed.parent;
        definition.bones.remove(selectedBone);
        for (CustomDeviceDefinition.Bone bone : definition.bones) {
            if (removed.id.equals(bone.parent)) {
                bone.parent = replacementParent;
            }
        }
        selectedBone = Math.max(0, selectedBone - 1);
        loadFields();
    }

    private AxisProjection projectGizmo(int centerX, int centerY) {
        Matrix4f parent = parentTransform(selectedBone());
        // Function: both edit modes display their axes at the selected bone pivot; only the edited data differs.
        Vector3f origin3 = parent.transformPosition(
                selectedBone().position[0] + selectedBone().pivot[0],
                selectedBone().position[1] + selectedBone().pivot[1],
                selectedBone().position[2] + selectedBone().pivot[2],
                new Vector3f());
        Matrix4f camera = new Matrix4f().rotateX((float) Math.toRadians(viewPitch))
                .rotateY((float) Math.toRadians(viewYaw));
        Vector3f cameraOrigin = camera.transformPosition(origin3, new Vector3f());
        Vector2f origin = screenPoint(cameraOrigin, centerX + (int) viewPanX, centerY + (int) viewPanY);
        Vector2f[] ends = new Vector2f[3];
        for (int axis = 0; axis < 3; axis++) {
            Vector4f direction = new Vector4f(axis == 0 ? 1 : 0, axis == 1 ? 1 : 0, axis == 2 ? 1 : 0, 0);
            if (transformEditMode == TransformEditMode.PIVOT) {
                float[] localDirection = CustomTurretPivotEdit.transformDirection(selectedBone(),
                        new float[]{direction.x, direction.y, direction.z});
                direction.set(localDirection[0], localDirection[1], localDirection[2], 0.0F);
            }
            parent.transform(direction);
            camera.transform(direction);
            ends[axis] = new Vector2f(origin.x + direction.x * 1.1F * viewScale,
                    origin.y - direction.y * 1.1F * viewScale);
        }
        return new AxisProjection(origin, ends);
    }

    private Matrix4f cameraTransform() {
        return new Matrix4f().rotateX((float) Math.toRadians(viewPitch))
                .rotateY((float) Math.toRadians(viewYaw));
    }

    private Matrix4f parentTransform(CustomDeviceDefinition.Bone bone) {
        Matrix4f transform = new Matrix4f();
        if (bone.parent.isEmpty()) {
            return transform;
        }
        CustomDeviceDefinition.Bone parent = definition.findBone(bone.parent);
        if (parent == null) {
            return transform;
        }
        transform.set(parentTransform(parent));
        transform.translate(parent.position[0], parent.position[1], parent.position[2]);
        transform.translate(parent.pivot[0], parent.pivot[1], parent.pivot[2]);
        transform.rotateZ((float) Math.toRadians(parent.rotation[2]));
        transform.rotateY((float) Math.toRadians(parent.rotation[1]));
        transform.rotateX((float) Math.toRadians(parent.rotation[0]));
        transform.scale(parent.scale[0], parent.scale[1], parent.scale[2]);
        transform.translate(-parent.pivot[0], -parent.pivot[1], -parent.pivot[2]);
        return transform;
    }

    private int nearestAxis(AxisProjection projection, float mouseX, float mouseY) {
        int nearest = -1;
        float distance = 8.0F;
        for (int axis = 0; axis < 3; axis++) {
            float candidate = pointSegmentDistance(mouseX, mouseY, projection.origin, projection.ends[axis]);
            if (candidate < distance) {
                distance = candidate;
                nearest = axis;
            }
        }
        return nearest;
    }

    private int boneDepth(CustomDeviceDefinition.Bone bone) {
        int depth = 0;
        String parent = bone.parent;
        while (!parent.isEmpty() && depth < 8) {
            CustomDeviceDefinition.Bone parentBone = definition.findBone(parent);
            if (parentBone == null) {
                break;
            }
            depth++;
            parent = parentBone.parent;
        }
        return depth;
    }

    private List<Integer> orderedBoneIndices() {
        List<Integer> ordered = new ArrayList<>();
        if (definition == null) {
            return ordered;
        }
        if ("turret".equals(definition.deviceType)) {
            appendBoneGroup(ordered, "root", "turret");
        } else {
            appendFlexibleBoneTree(ordered, "");
        }
        return ordered;
    }

    private void appendFlexibleBoneTree(List<Integer> ordered, String parentId) {
        for (int index = 0; index < definition.bones.size(); index++) {
            CustomDeviceDefinition.Bone bone = definition.bones.get(index);
            if (parentId.equals(bone.parent)) {
                ordered.add(index);
                appendFlexibleBoneTree(ordered, bone.id);
            }
        }
    }

    private void appendBoneGroup(List<Integer> ordered, String groupId, String childGroupId) {
        int groupIndex = definition.bones.indexOf(definition.findBone(groupId));
        if (groupIndex >= 0) {
            ordered.add(groupIndex);
        }
        for (int index = 0; index < definition.bones.size(); index++) {
            CustomDeviceDefinition.Bone bone = definition.bones.get(index);
            if (!CustomDeviceDefinition.isRequiredBoneGroup(bone.id) && groupId.equals(bone.parent)) {
                ordered.add(index);
            }
        }
        if (childGroupId != null) {
            String next = switch (childGroupId) {
                case "turret" -> "cannon";
                case "cannon" -> "long_cannon";
                default -> null;
            };
            appendBoneGroup(ordered, childGroupId, next);
        }
    }

    private void positionTurretPropertyFields() {
        clampTurretPropertiesScroll();
        int x = turretPropertiesFieldX();
        int width = turretPropertiesFieldWidth();
        moveField(fireCooldown, x, turretPropertyY(44), width);
        moveField(hudShortName, x, turretPropertyY(62), width);
        moveField(rotationSpeed, x, turretPropertyY(80), width);
        moveField(fireAnimationPath, x, turretPropertyY(98), width);
        moveField(fireAnimationName, x, turretPropertyY(116 + fireAnimationBindShift()), width);
        moveField(energyPerTick, x, turretPropertyY(152 + fireAnimationBindShift()), width);
        moveField(laserRadius, x, turretPropertyY(188 + fireAnimationBindShift()), width);
        moveField(ammoItemId, x, turretPropertyY(152 + fireAnimationBindShift()), width);
        moveField(projectileScale, x, turretPropertyY(170 + fireAnimationBindShift()), width);
        moveField(projectileDamage, x, turretPropertyY(188 + fireAnimationBindShift()), width);
        moveField(projectileExplosionRadius, x, turretPropertyY(206 + fireAnimationBindShift()), width);
        moveField(projectileLifetime, x, turretPropertyY(224 + fireAnimationBindShift()), width);
        moveField(projectileSpeed, x, turretPropertyY(242 + fireAnimationBindShift()), width);
        moveField(projectileFxPath, x, turretPropertyY(260 + fireAnimationBindShift()), width);
        moveField(projectileFxScale, x,
                turretPropertyY(278 + fireAnimationBindShift() + projectileFxBindShift()), width / 3 - 2);
        for (int axis = 0; axis < 3; axis++) {
            moveField(projectileFxRotation[axis], x + axis * (width / 3),
                    turretPropertyY(296 + fireAnimationBindShift() + projectileFxBindShift()), width / 3 - 2);
        }
        moveField(firepointInterval, x,
                turretPropertyY(314 + fireAnimationBindShift() + projectileFxBindShift()), width);
        moveField(thrusterThrust, x, turretPropertyY(44), width);
        moveField(thrusterFuelRate, x, turretPropertyY(62), width);
        moveField(thrusterFuel, x, turretPropertyY(80), width);
        moveField(flameSegments, x, turretPropertyY(98), width);
        moveField(flameRadius, x, turretPropertyY(116), width);
        moveField(trailRadius, x, turretPropertyY(134), width);
    }

    private int turretPropertiesFieldX() {
        return turretPropertiesX + TURRET_PROPERTIES_LABEL_WIDTH + 10;
    }

    private int turretPropertiesFieldWidth() {
        return Math.max(64, turretPropertiesWidth - TURRET_PROPERTIES_LABEL_WIDTH - 28);
    }

    private int turretPropertyY(int contentY) {
        return turretPropertiesY + contentY - turretPropertiesScroll;
    }

    private int fireAnimationBindShift() {
        return definition != null && "turret".equals(definition.deviceType) ? 18 : 0;
    }

    private int projectileFxBindShift() {
        return definition != null && isWeaponLikeDefinition() && !"energy".equals(definition.weaponType) ? 18 : 0;
    }

    private int turretPropertiesContentHeight() {
        if (definition == null) {
            return FLOATING_TITLE_HEIGHT;
        }
        if ("thruster".equals(definition.deviceType)) {
            return 196;
        }
        if ("energy".equals(definition.weaponType)) {
            return "turret".equals(definition.deviceType) ? 230 : 176;
        }
        return (definition.firepointCount > 1 ? 338 : 320)
                + fireAnimationBindShift() + projectileFxBindShift();
    }

    private int turretPropertiesVisibleContentHeight() {
        return Math.max(1, turretPropertiesPanelHeight() - FLOATING_TITLE_HEIGHT - 6);
    }

    private int turretPropertiesMaxScroll() {
        return Math.max(0, turretPropertiesContentHeight() - turretPropertiesVisibleContentHeight());
    }

    private void clampTurretPropertiesScroll() {
        turretPropertiesScroll = Math.max(0, Math.min(turretPropertiesScroll, turretPropertiesMaxScroll()));
    }

    private boolean isDescendant(CustomDeviceDefinition.Bone candidate, String ancestorId) {
        String parent = candidate.parent;
        while (!parent.isEmpty()) {
            if (ancestorId.equals(parent)) {
                return true;
            }
            CustomDeviceDefinition.Bone bone = definition.findBone(parent);
            if (bone == null) {
                break;
            }
            parent = bone.parent;
        }
        return false;
    }

    private String nextBoneId() {
        int suffix = 1;
        while (definition.findBone("bone_" + suffix) != null) {
            suffix++;
        }
        return "bone_" + suffix;
    }

    private String nextDefinitionId(String deviceType) {
        String prefix = switch (deviceType) {
            case "weapon" -> "custom_weapon_";
            case "thruster" -> "custom_thruster_";
            case "decoration" -> "custom_decoration_";
            default -> "custom_turret_";
        };
        int suffix = 1;
        while (containsDefinition(prefix + suffix)) {
            suffix++;
        }
        return prefix + suffix;
    }

    private boolean containsDefinition(String id) {
        return savedDefinitions.stream().anyMatch(definition -> definition.id.equals(id));
    }

    private CustomDeviceDefinition.Bone selectedBone() {
        return definition.bones.get(selectedBone);
    }

    private int propertyActionY() {
        return (isSelectedPointFxBone() ? pointFxRotation[0].getY() : scale[0].getY())
                + scale[0].getHeight() + 8;
    }

    private void beginPanelDrag(FloatingPanel panel, double mouseX, double mouseY, int panelX, int panelY) {
        draggedPanel = panel;
        raisePanel(panel);
        panelDragOffsetX = (int) mouseX - panelX;
        panelDragOffsetY = (int) mouseY - panelY;
    }

    /** Returns the floating panels bottom-to-top so the last-interacted panel and all its elements stay on top. */
    private List<FloatingPanel> panelZOrder() {
        List<FloatingPanel> order = new ArrayList<>();
        order.add(FloatingPanel.HIERARCHY);
        order.add(FloatingPanel.PROPERTIES);
        order.add(FloatingPanel.TURRET_PROPERTIES);
        order.remove(topmostPanel);
        order.add(topmostPanel);
        return order;
    }

    private void raisePanel(FloatingPanel panel) {
        topmostPanel = panel;
    }

    private boolean beginTurretPropertyResize(double mouseX, double mouseY) {
        int height = turretPropertiesPanelHeight();
        resizeTurretLeft = Math.abs(mouseX - turretPropertiesX) <= FLOATING_PANEL_EDGE;
        resizeTurretRight = Math.abs(mouseX - (turretPropertiesX + turretPropertiesWidth)) <= FLOATING_PANEL_EDGE;
        resizeTurretTop = Math.abs(mouseY - turretPropertiesY) <= FLOATING_PANEL_EDGE;
        resizeTurretBottom = Math.abs(mouseY - (turretPropertiesY + height)) <= FLOATING_PANEL_EDGE;
        resizingTurretProperties = resizeTurretLeft || resizeTurretRight || resizeTurretTop || resizeTurretBottom;
        return resizingTurretProperties;
    }

    private boolean beginPropertiesResize(double mouseX, double mouseY) {
        resizeLeft = Math.abs(mouseX - propertiesX) <= FLOATING_PANEL_EDGE;
        resizeRight = Math.abs(mouseX - (propertiesX + propertiesWidth)) <= FLOATING_PANEL_EDGE;
        resizeTop = Math.abs(mouseY - propertiesY) <= FLOATING_PANEL_EDGE;
        resizeBottom = Math.abs(mouseY - (propertiesY + propertiesPanelHeight())) <= FLOATING_PANEL_EDGE;
        resizedPanel = resizeLeft || resizeRight || resizeTop || resizeBottom
                ? ResizedPanel.PROPERTIES : ResizedPanel.NONE;
        return resizedPanel == ResizedPanel.PROPERTIES;
    }

    private void resizeProperties(double mouseX, double mouseY) {
        int oldRight = propertiesX + propertiesWidth;
        int oldBottom = propertiesY + propertiesPanelHeight();
        if (resizeLeft) {
            int nextLeft = Math.max(viewportLeft + 2, Math.min((int) mouseX, oldRight - PROPERTIES_MIN_WIDTH));
            propertiesWidth = oldRight - nextLeft;
            propertiesX = nextLeft;
        }
        if (resizeRight) {
            int nextRight = Math.min(viewportRight - 2, Math.max((int) mouseX, propertiesX + PROPERTIES_MIN_WIDTH));
            propertiesWidth = nextRight - propertiesX;
        }
        if (resizeTop) {
            int nextTop = Math.max(viewportTop + 2, Math.min((int) mouseY, oldBottom - PROPERTIES_MIN_HEIGHT));
            propertiesHeight = oldBottom - nextTop;
            propertiesY = nextTop;
        }
        if (resizeBottom) {
            int nextBottom = Math.min(viewportBottom - 2, Math.max((int) mouseY, propertiesY + PROPERTIES_MIN_HEIGHT));
            propertiesHeight = nextBottom - propertiesY;
        }
        clampFloatingPanels();
        positionPropertyFields();
    }

    private void resizeTurretProperties(double mouseX, double mouseY) {
        int oldRight = turretPropertiesX + turretPropertiesWidth;
        int oldBottom = turretPropertiesY + turretPropertiesPanelHeight();
        if (resizeTurretLeft) {
            int nextLeft = Math.max(viewportLeft + 2, Math.min((int) mouseX, oldRight - TURRET_PROPERTIES_MIN_WIDTH));
            turretPropertiesWidth = oldRight - nextLeft;
            turretPropertiesX = nextLeft;
        }
        if (resizeTurretRight) {
            int nextRight = Math.min(viewportRight - 2, Math.max((int) mouseX, turretPropertiesX + TURRET_PROPERTIES_MIN_WIDTH));
            turretPropertiesWidth = nextRight - turretPropertiesX;
        }
        if (resizeTurretTop) {
            int nextTop = Math.max(viewportTop + 2, Math.min((int) mouseY, oldBottom - TURRET_PROPERTIES_MIN_HEIGHT));
            turretPropertiesHeight = oldBottom - nextTop;
            turretPropertiesY = nextTop;
        }
        if (resizeTurretBottom) {
            int nextBottom = Math.min(viewportBottom - 2, Math.max((int) mouseY, turretPropertiesY + TURRET_PROPERTIES_MIN_HEIGHT));
            turretPropertiesHeight = nextBottom - turretPropertiesY;
        }
        clampFloatingPanels();
        positionTurretPropertyFields();
    }

    private void updatePropertyFieldVisibility() {
        boolean propertyPanelVisible = !propertiesCollapsed;
        boolean groupTransformOnly = isSelectedBoneGroup();
        boolean pointFx = isSelectedPointFxBone();
        for (EditBox field : allFields) {
            if (isTurretPropertyField(field)) {
                continue;
            }
            boolean visible = propertyPanelVisible
                    && (groupTransformOnly ? isGroupTransformField(field) || pointFx && isPointFxField(field)
                    : !isPointFxField(field) || pointFx);
            field.visible = visible;
            if (!visible) {
                field.setFocused(false);
            }
        }
    }

    private boolean isPivotField(EditBox candidate) {
        return candidate == pivot[0] || candidate == pivot[1] || candidate == pivot[2];
    }

    private boolean isGroupTransformField(EditBox candidate) {
        return isPivotField(candidate)
                || candidate == rotation[0] || candidate == rotation[1] || candidate == rotation[2]
                || candidate == scale[0] || candidate == scale[1] || candidate == scale[2];
    }

    private boolean isPointFxField(EditBox candidate) {
        return candidate == pointFxPath || candidate == pointFxScale
                || candidate == pointFxRotation[0] || candidate == pointFxRotation[1]
                || candidate == pointFxRotation[2];
    }

    private boolean isSelectedBoneGroup() {
        // The screen can reach floating-panel layout before a definition is loaded, so guard the empty editor state.
        return definition != null && !definition.bones.isEmpty()
                && !"decoration".equals(definition.deviceType)
                && CustomDeviceDefinition.isBoneGroup(selectedBone().id);
    }

    private boolean isSemanticBoneGroup(CustomDeviceDefinition.Bone bone) {
        return !"decoration".equals(definition.deviceType) && CustomDeviceDefinition.isBoneGroup(bone.id);
    }

    private boolean isSelectedPointFxBone() {
        if (definition == null || definition.bones.isEmpty()) {
            return false;
        }
        String id = selectedBone().id;
        return !"decoration".equals(definition.deviceType)
                && (CustomDeviceDefinition.isFirepointBoneGroup(id)
                || CustomDeviceDefinition.isFlamepointBoneGroup(id));
    }

    private boolean isTurretPropertyField(EditBox field) {
        return field == fireCooldown || field == hudShortName || field == rotationSpeed
                || field == fireAnimationPath || field == fireAnimationName
                || field == energyPerTick || field == laserRadius
                || field == ammoItemId || field == projectileScale || field == projectileDamage
                || field == projectileExplosionRadius || field == projectileLifetime || field == projectileSpeed
                || field == projectileFxPath || field == projectileFxScale
                || field == projectileFxRotation[0] || field == projectileFxRotation[1] || field == projectileFxRotation[2]
                || field == firepointInterval || field == thrusterThrust || field == thrusterFuelRate
                || field == thrusterFuel || field == flameSegments || field == flameRadius || field == trailRadius;
    }

    private void updateTurretFieldVisibility() {
        boolean energy = definition != null && "energy".equals(definition.weaponType);
        boolean thruster = definition != null && "thruster".equals(definition.deviceType);
        boolean weaponLike = definition != null
                && ("weapon".equals(definition.deviceType) || "turret".equals(definition.deviceType));
        boolean turret = definition != null && "turret".equals(definition.deviceType);
        fireCooldown.visible = turretPropertiesOpen && weaponLike;
        hudShortName.visible = turretPropertiesOpen && definitionRequiresHudShortName();
        rotationSpeed.visible = turretPropertiesOpen && turret;
        fireAnimationPath.visible = turretPropertiesOpen && turret;
        fireAnimationName.visible = turretPropertiesOpen && turret && !definition.fireAnimation.isBlank();
        energyPerTick.visible = turretPropertiesOpen && weaponLike && energy;
        laserRadius.visible = turretPropertiesOpen && weaponLike && energy;
        ammoItemId.visible = turretPropertiesOpen && weaponLike && !energy;
        projectileScale.visible = turretPropertiesOpen && weaponLike && !energy;
        projectileDamage.visible = turretPropertiesOpen && weaponLike && !energy;
        projectileExplosionRadius.visible = turretPropertiesOpen && weaponLike && !energy;
        projectileLifetime.visible = turretPropertiesOpen && weaponLike && !energy;
        projectileSpeed.visible = turretPropertiesOpen && weaponLike && !energy;
        projectileFxPath.visible = turretPropertiesOpen && weaponLike && !energy;
        projectileFxScale.visible = turretPropertiesOpen && weaponLike && !energy;
        for (EditBox field : projectileFxRotation) {
            field.visible = turretPropertiesOpen && weaponLike && !energy;
        }
        firepointInterval.visible = turretPropertiesOpen && weaponLike && !energy && definition.firepointCount > 1;
        thrusterThrust.visible = turretPropertiesOpen && thruster;
        thrusterFuelRate.visible = turretPropertiesOpen && thruster;
        thrusterFuel.visible = turretPropertiesOpen && thruster;
        flameSegments.visible = turretPropertiesOpen && thruster;
        flameRadius.visible = turretPropertiesOpen && thruster;
        trailRadius.visible = turretPropertiesOpen && thruster;
        clampTurretPropertiesScroll();
        positionTurretPropertyFields();
        if (!turretPropertiesOpen) {
            for (EditBox field : allFields) {
                if (isTurretPropertyField(field)) {
                    field.setFocused(false);
                }
            }
        }
    }

    private boolean definitionRequiresHudShortName() {
        return definition != null && ("weapon".equals(definition.deviceType)
                || ("turret".equals(definition.deviceType) && "heavyturret".equals(definition.turretType)));
    }

    private boolean isWeaponLikeDefinition() {
        return definition != null
                && ("weapon".equals(definition.deviceType) || "turret".equals(definition.deviceType));
    }

    private void closeTurretProperties() {
        turretPropertiesOpen = false;
        if (topmostPanel == FloatingPanel.TURRET_PROPERTIES) {
            topmostPanel = FloatingPanel.PROPERTIES;
        }
        if (draggedPanel == FloatingPanel.TURRET_PROPERTIES) {
            draggedPanel = null;
        }
        updateTurretFieldVisibility();
    }

    private int turretPropertiesToolX() {
        // Function: reserve the left header text area, then start a horizontal tool-icon strip.
        return panelLeft + 250;
    }

    private void toggleHierarchyPanel() {
        hierarchyCollapsed = !hierarchyCollapsed;
        raisePanel(FloatingPanel.HIERARCHY);
        clampFloatingPanels();
    }

    private void togglePropertiesPanel() {
        propertiesCollapsed = !propertiesCollapsed;
        raisePanel(FloatingPanel.PROPERTIES);
        updatePropertyFieldVisibility();
        clampFloatingPanels();
        // Function: expansion may move the window upward, so fields must follow in the same click.
        positionPropertyFields();
    }

    private int hierarchyPanelHeight() {
        return hierarchyCollapsed ? FLOATING_TITLE_HEIGHT : HIERARCHY_HEIGHT;
    }

    private int hierarchyVisibleRows() {
        // Function: reserve the bottom button rows so scrolled bones never overlap the bone actions.
        return Math.max(1, (HIERARCHY_HEIGHT - FLOATING_TITLE_HEIGHT - 62) / 17);
    }

    private int hierarchyMaxScroll(int boneCount) {
        return Math.max(0, boneCount - hierarchyVisibleRows());
    }

    private void drawHierarchyScrollBar(GuiGraphics graphics, int boneCount) {
        int maxScroll = hierarchyMaxScroll(boneCount);
        if (maxScroll <= 0) {
            return;
        }
        int trackTop = hierarchyY + FLOATING_TITLE_HEIGHT + 4;
        int trackBottom = hierarchyY + HIERARCHY_HEIGHT - 66;
        int trackHeight = Math.max(8, trackBottom - trackTop);
        int thumbHeight = Math.max(14, trackHeight * hierarchyVisibleRows() / Math.max(1, boneCount));
        int thumbY = trackTop + (trackHeight - thumbHeight) * hierarchyScroll / maxScroll;
        int x = hierarchyX + HIERARCHY_WIDTH - 6;
        graphics.fill(x, trackTop, x + 2, trackBottom, 0x33494D52);
        graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, 0x99565B61);
    }

    private int propertiesPanelHeight() {
        if (propertiesCollapsed) {
            return FLOATING_TITLE_HEIGHT;
        }
        int maxHeight = Math.max(PROPERTIES_MIN_HEIGHT, viewportBottom - viewportTop - 4);
        return Math.max(PROPERTIES_MIN_HEIGHT, Math.min(propertiesHeight, maxHeight));
    }

    private int turretPropertiesPanelHeight() {
        if (definition == null) {
            return FLOATING_TITLE_HEIGHT;
        }
        int maxHeight = Math.max(TURRET_PROPERTIES_MIN_HEIGHT, viewportBottom - viewportTop - 4);
        return Math.max(TURRET_PROPERTIES_MIN_HEIGHT, Math.min(turretPropertiesHeight, maxHeight));
    }

    private void clampFloatingPanels() {
        hierarchyX = Math.max(viewportLeft + 2, Math.min(viewportRight - HIERARCHY_WIDTH - 2, hierarchyX));
        hierarchyY = Math.max(viewportTop + 2,
                Math.min(viewportBottom - hierarchyPanelHeight() - 2, hierarchyY));
        propertiesWidth = Math.max(PROPERTIES_MIN_WIDTH,
                Math.min(propertiesWidth, viewportRight - viewportLeft - 4));
        propertiesHeight = Math.max(PROPERTIES_MIN_HEIGHT,
                Math.min(propertiesHeight, viewportBottom - viewportTop - 4));
        propertiesX = Math.max(viewportLeft + 2, Math.min(viewportRight - propertiesWidth - 2, propertiesX));
        propertiesY = Math.max(viewportTop + 2,
                Math.min(viewportBottom - propertiesPanelHeight() - 2, propertiesY));
        clampPropertiesScroll();
        turretPropertiesWidth = Math.max(TURRET_PROPERTIES_MIN_WIDTH,
                Math.min(turretPropertiesWidth, viewportRight - viewportLeft - 4));
        turretPropertiesHeight = Math.max(TURRET_PROPERTIES_MIN_HEIGHT,
                Math.min(turretPropertiesHeight, viewportBottom - viewportTop - 4));
        turretPropertiesX = Math.max(viewportLeft + 2,
                Math.min(viewportRight - turretPropertiesWidth - 2, turretPropertiesX));
        turretPropertiesY = Math.max(viewportTop + 2,
                Math.min(viewportBottom - turretPropertiesPanelHeight() - 2, turretPropertiesY));
        clampTurretPropertiesScroll();
    }

    private boolean insideViewport(double x, double y) {
        return x >= viewportLeft && x < viewportRight && y >= viewportTop && y < viewportBottom;
    }

    private boolean insideFloatingPanel(double x, double y) {
        return inside(x, y, hierarchyX, hierarchyY, HIERARCHY_WIDTH, hierarchyPanelHeight())
                || inside(x, y, propertiesX, propertiesY, propertiesWidth, propertiesPanelHeight())
                || turretPropertiesOpen && inside(x, y, turretPropertiesX, turretPropertiesY,
                turretPropertiesWidth, turretPropertiesPanelHeight());
    }

    private boolean insideTurretPropertiesContent(double x, double y) {
        return inside(x, y, turretPropertiesX + 3, turretPropertiesY + FLOATING_TITLE_HEIGHT + 2,
                turretPropertiesWidth - 6, turretPropertiesPanelHeight() - FLOATING_TITLE_HEIGHT - 5);
    }

    private boolean insidePropertiesContent(double x, double y) {
        return inside(x, y, propertiesX + 3, propertiesY + FLOATING_TITLE_HEIGHT + 2,
                propertiesWidth - 6, propertiesPanelHeight() - FLOATING_TITLE_HEIGHT - 5);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static float parse(EditBox field) {
        float value = Float.parseFloat(field.getValue());
        if (!Float.isFinite(value)) {
            throw new NumberFormatException("Non-finite number");
        }
        return value;
    }

    private static int parsePositiveInt(EditBox field) {
        int value = Integer.parseInt(field.getValue());
        if (value < 1 || value > 72_000) {
            throw new NumberFormatException("Cooldown outside 1-72000 ticks");
        }
        return value;
    }

    private static int parseNonNegativeInt(EditBox field) {
        int value = Integer.parseInt(field.getValue());
        if (value < 0) {
            throw new NumberFormatException("Negative value");
        }
        return value;
    }

    private static float normalizeDegrees(float value) {
        float normalized = value % 360.0F;
        return normalized <= -180.0F ? normalized + 360.0F : normalized > 180.0F ? normalized - 360.0F : normalized;
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private Vector2f screenPoint(Vector3f point, int centerX, int centerY) {
        return new Vector2f(centerX + point.x * viewScale, centerY - point.y * viewScale);
    }

    private static float pointSegmentDistance(float px, float py, Vector2f start, Vector2f end) {
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared < 0.001F) {
            return Float.MAX_VALUE;
        }
        float t = Math.max(0.0F, Math.min(1.0F, ((px - start.x) * dx + (py - start.y) * dy) / lengthSquared));
        float x = start.x + t * dx;
        float y = start.y + t * dy;
        return (float) Math.sqrt((px - x) * (px - x) + (py - y) * (py - y));
    }

    private static void drawButton(GuiGraphics graphics, int x, int y, int width, int height,
                                   Component label, boolean accent) {
        graphics.fill(x, y, x + width, y + height, accent ? 0xCC538CA5 : 0xCC565B61);
        graphics.fill(x, y + height - 2, x + width, y + height, accent ? ACCENT : 0xFF789B9D);
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, clipped(label, width - 8),
                x + width / 2, y + 6, accent ? TEXT : 0xFFAAC3C1);
    }

    private static Component clipped(Component label, int width) {
        String value = label.getString();
        var font = net.minecraft.client.Minecraft.getInstance().font;
        if (font.width(value) <= width) {
            return label;
        }
        return Component.literal(font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...");
    }

    /** Trims from the left so an overlong value's tail stays visible instead of running off the screen. */
    private static String clippedRight(String value, int maxWidth) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        if (value == null || value.isEmpty() || maxWidth <= 0 || font.width(value) <= maxWidth) {
            return value == null ? "" : value;
        }
        return "..." + font.plainSubstrByWidth(value, Math.max(0, maxWidth - font.width("...")), true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record AxisProjection(Vector2f origin, Vector2f[] ends) {
    }

    private enum FloatingPanel {
        HIERARCHY,
        PROPERTIES,
        TURRET_PROPERTIES
    }

    private enum ResizedPanel {
        NONE,
        PROPERTIES
    }

    private enum TransformEditMode {
        POSITION("screen.vsie.custom_turret_editor.adjust_position"),
        PIVOT("screen.vsie.custom_turret_editor.adjust_pivot");

        private final String translationKey;

        TransformEditMode(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
