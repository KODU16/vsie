package com.kodu16.vsie.content.custom_turret.client;

import com.kodu16.vsie.content.custom_turret.CustomTurretDefinition;
import com.kodu16.vsie.content.custom_turret.CustomTurretAssetPackage;
import com.kodu16.vsie.content.custom_turret.CustomTurretStorage;
import com.kodu16.vsie.content.custom_turret.CustomTurretRuntimePose;
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
public final class CustomTurretEditorScreen extends CrispScreen {
    private static final int FRAME = 0xFF2E3238;
    private static final int PANEL = 0xF23A3D42;
    private static final int SIDEBAR = 0xF2494D52;
    private static final int ACCENT = 0xFF87CEFA;
    private static final int TEXT = 0xFFE4FAF7;
    private static final int MUTED = 0xFF789B9D;
    private static final int FIELD_BACKGROUND = 0xCC272A2F;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_HEIGHT = 22;
    private static final int HIERARCHY_WIDTH = 150;
    private static final int HIERARCHY_HEIGHT = 228;
    private static final int PROPERTIES_WIDTH = 250;
    private static final int PROPERTIES_HEIGHT = 264;
    private static final int TURRET_PROPERTIES_WIDTH = 250;
    private static final int FLOATING_TITLE_HEIGHT = 20;
    private static final int ROW_HEIGHT = 18;
    private static final int AXIS_X = 0xFFFF6464;
    private static final int AXIS_Y = 0xFF79D27C;
    private static final int AXIS_Z = 0xFF75A7FF;
    private static final int BARREL_FORWARD = 0xFFFFC857;

    private CustomTurretDefinition definition;
    private int selectedBone;
    private List<CustomTurretDefinition> savedDefinitions = List.of();
    private final List<EditBox> allFields = new ArrayList<>();
    private EditBox definitionName;
    private EditBox boneId;
    private EditBox parentId;
    private EditBox modelPath;
    private EditBox texturePath;
    private final EditBox[] position = new EditBox[3];
    private final EditBox[] pivot = new EditBox[3];
    private final EditBox[] rotation = new EditBox[3];
    private final EditBox[] scale = new EditBox[3];
    private EditBox fireCooldown;
    private EditBox rotationSpeed;
    private EditBox energyPerTick;
    private EditBox laserRadius;
    private EditBox ammoItemId;
    private EditBox projectileScale;
    private EditBox projectileDamage;
    private EditBox projectileExplosionRadius;
    private EditBox projectileLifetime;
    private EditBox projectileSpeed;
    private EditBox firepointInterval;
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
    private int turretPropertiesX;
    private int turretPropertiesY;
    private boolean hierarchyCollapsed;
    private boolean propertiesCollapsed;
    private boolean turretPropertiesOpen;
    private FloatingPanel draggedPanel;
    private int panelDragOffsetX;
    private int panelDragOffsetY;
    private TransformEditMode transformEditMode = TransformEditMode.POSITION;
    private float viewYaw = CustomTurretEditorCamera.INITIAL_YAW_DEGREES;
    private float viewPitch = CustomTurretEditorCamera.INITIAL_PITCH_DEGREES;
    private float viewScale = 36.0F;
    private float viewPanX;
    private float viewPanY;
    private int activeAxis = -1;
    private boolean rotatingView;
    private boolean panningView;

    public CustomTurretEditorScreen() {
        super(Component.translatable("screen.vsie.custom_turret_editor"));
        savedDefinitions = CustomTurretStorage.loadAllDefinitions();
        definition = savedDefinitions.isEmpty()
                ? CustomTurretDefinition.createNew("custom_turret")
                : savedDefinitions.get(0).copy();
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
            propertiesX = viewportRight - PROPERTIES_WIDTH - 8;
            propertiesY = viewportTop + 8;
        }
        if (turretPropertiesX == 0 && turretPropertiesY == 0) {
            turretPropertiesX = viewportRight - TURRET_PROPERTIES_WIDTH - 8;
            turretPropertiesY = viewportBottom - turretPropertiesPanelHeight() - 8;
        }
        clampFloatingPanels();
        createFields();
        loadFields();
    }

    private void createFields() {
        allFields.clear();
        createPropertyFields();
        int x = turretPropertiesX + 122;
        int width = TURRET_PROPERTIES_WIDTH - 130;
        fireCooldown = addField(x, turretPropertiesY + 44, width, 6);
        rotationSpeed = addField(x, turretPropertiesY + 62, width, 12);
        energyPerTick = addField(x, turretPropertiesY + 98, width, 9);
        laserRadius = addField(x, turretPropertiesY + 134, width, 12);
        ammoItemId = addField(x, turretPropertiesY + 98, width, 128);
        projectileScale = addField(x, turretPropertiesY + 116, width, 12);
        projectileDamage = addField(x, turretPropertiesY + 134, width, 12);
        projectileExplosionRadius = addField(x, turretPropertiesY + 152, width, 12);
        projectileLifetime = addField(x, turretPropertiesY + 170, width, 6);
        projectileSpeed = addField(x, turretPropertiesY + 188, width, 12);
        firepointInterval = addField(x, turretPropertiesY + 206, width, 6);
    }

    private void createPropertyFields() {
        int fieldX = propertiesX + 70;
        int fieldWidth = PROPERTIES_WIDTH - 78;
        int y = propertiesY + FLOATING_TITLE_HEIGHT + 5;
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
    }

    private void positionPropertyFields() {
        int fieldX = propertiesX + 70;
        int fieldWidth = PROPERTIES_WIDTH - 78;
        if (isSelectedBoneGroup()) {
            // Function: the pivot-only group view places its sole vector directly below the panel title.
            for (int axis = 0; axis < 3; axis++) {
                moveField(pivot[axis], fieldX + axis * (fieldWidth / 3),
                        propertiesY + FLOATING_TITLE_HEIGHT + 5);
            }
            return;
        }
        int y = propertiesY + FLOATING_TITLE_HEIGHT + 5;
        moveField(definitionName, fieldX, y);
        moveField(boneId, fieldX, y += ROW_HEIGHT);
        moveField(parentId, fieldX, y += ROW_HEIGHT);
        moveField(modelPath, fieldX, y += ROW_HEIGHT);
        moveField(texturePath, fieldX, y += ROW_HEIGHT);
        for (EditBox field : position) {
            moveField(field, fieldX + java.util.Arrays.asList(position).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT);
        }
        y += ROW_HEIGHT;
        for (EditBox field : pivot) {
            moveField(field, fieldX + java.util.Arrays.asList(pivot).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT);
        }
        y += ROW_HEIGHT;
        for (EditBox field : rotation) {
            moveField(field, fieldX + java.util.Arrays.asList(rotation).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT);
        }
        y += ROW_HEIGHT;
        for (EditBox field : scale) {
            moveField(field, fieldX + java.util.Arrays.asList(scale).indexOf(field) * (fieldWidth / 3),
                    y + ROW_HEIGHT);
        }
    }

    private static void moveField(EditBox field, int x, int y) {
        field.setX(x);
        field.setY(y);
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
        if (definition.bones.isEmpty()) {
            definition = CustomTurretDefinition.createNew(definition.id);
        }
        selectedBone = Math.min(selectedBone, definition.bones.size() - 1);
        CustomTurretDefinition.Bone bone = selectedBone();
        boolean boneGroup = CustomTurretDefinition.isBoneGroup(bone.id);
        if (boneGroup) {
            // Function: fixed AeroIE bone groups expose only their pivot while remaining selectable in the hierarchy.
            transformEditMode = TransformEditMode.PIVOT;
        }
        definitionName.setValue(definition.name);
        definitionName.setEditable(!boneGroup);
        fireCooldown.setValue(Integer.toString(definition.fireCooldownTicks));
        rotationSpeed.setValue(format(definition.rotationSpeedDegreesPerTick));
        energyPerTick.setValue(Integer.toString(definition.energyPerTick));
        laserRadius.setValue(format(definition.laserRadius));
        ammoItemId.setValue(definition.ammoItemId);
        projectileScale.setValue(format(definition.projectileScale));
        projectileDamage.setValue(format(definition.projectileDamage));
        projectileExplosionRadius.setValue(format(definition.projectileExplosionRadius));
        projectileLifetime.setValue(Integer.toString(definition.projectileLifetimeTicks));
        projectileSpeed.setValue(format(definition.projectileSpeedBlocksPerSecond));
        firepointInterval.setValue(Integer.toString(definition.firepointIntervalTicks));
        boneId.setValue(bone.id);
        boneId.setEditable(!boneGroup);
        parentId.setValue(bone.parent);
        parentId.setEditable(!boneGroup);
        modelPath.setValue(bone.model);
        modelPath.setEditable(!boneGroup);
        // Function: imported/exported OBJ files populate an empty texture field from their MTL map_Kd link.
        texturePath.setValue(CustomTurretAssetCache.texturePath(bone.model, bone.texture));
        texturePath.setEditable(!boneGroup);
        for (int axis = 0; axis < 3; axis++) {
            position[axis].setValue(format(bone.position[axis]));
            pivot[axis].setValue(format(bone.pivot[axis]));
            rotation[axis].setValue(format(bone.rotation[axis]));
            scale[axis].setValue(format(bone.scale[axis]));
            position[axis].setEditable(!boneGroup);
            pivot[axis].setEditable(true);
            rotation[axis].setEditable(!boneGroup);
            scale[axis].setEditable(!boneGroup);
        }
        updatePropertyFieldVisibility();
        updateTurretFieldVisibility();
        clampFloatingPanels();
        positionPropertyFields();
    }

    private boolean applyFields(boolean validateDefinition) {
        try {
            synchronizePackageTexture();
            CustomTurretDefinition.Bone bone = selectedBone();
            String previousId = bone.id;
            definition.name = definitionName.getValue().trim();
            definition.fireCooldownTicks = parsePositiveInt(fireCooldown);
            definition.rotationSpeedDegreesPerTick = parse(rotationSpeed);
            definition.energyPerTick = parseNonNegativeInt(energyPerTick);
            definition.laserRadius = parse(laserRadius);
            definition.ammoItemId = ammoItemId.getValue().trim();
            definition.projectileScale = parse(projectileScale);
            definition.projectileDamage = parse(projectileDamage);
            definition.projectileExplosionRadius = parse(projectileExplosionRadius);
            definition.projectileLifetimeTicks = parsePositiveInt(projectileLifetime);
            definition.projectileSpeedBlocksPerSecond = parse(projectileSpeed);
            definition.firepointIntervalTicks = parsePositiveInt(firepointInterval);
            bone.id = boneId.getValue().trim().toLowerCase(Locale.ROOT);
            if (!CustomTurretDefinition.isFirepointBoneGroup(previousId) && bone.id.startsWith("firepoint")) {
                throw new IllegalArgumentException(Component.translatable(
                        "screen.vsie.custom_turret_editor.firepoint_prefix_reserved").getString());
            }
            bone.parent = parentId.getValue().trim().toLowerCase(Locale.ROOT);
            bone.model = modelPath.getValue().trim();
            bone.texture = texturePath.getValue().trim();
            for (int axis = 0; axis < 3; axis++) {
                bone.position[axis] = parse(position[axis]);
                bone.pivot[axis] = parse(pivot[axis]);
                bone.rotation[axis] = parse(rotation[axis]);
                bone.scale[axis] = parse(scale[axis]);
            }
            if (!previousId.equals(bone.id)) {
                for (CustomTurretDefinition.Bone child : definition.bones) {
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Function: blur the world before drawing the editor so the blur shader never processes GUI pixels.
        renderBackground(graphics, mouseX, mouseY, partialTick);
        RenderSystem.disableDepthTest();
        drawFrame(graphics);
        drawViewport(graphics);
        drawHierarchy(graphics, mouseX, mouseY);
        drawProperties(graphics, mouseX, mouseY);
        if (turretPropertiesOpen) {
            drawTurretProperties(graphics, mouseX, mouseY);
        }
        drawFieldBackgrounds(graphics);
        // Function: fields render last so their glyphs and borders remain pixel-sharp over custom panels.
        for (EditBox field : allFields) {
            field.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.flush();
        RenderSystem.enableDepthTest();
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
        drawTopToolIcons(graphics);
        graphics.drawString(font, status, panelLeft + 12, viewportBottom + 7, MUTED, false);
        Component viewHint = Component.translatable("screen.vsie.custom_turret_editor.view_hint");
        // Function: right-align the expanded pan/rotate/zoom hint without relying on language-specific width.
        graphics.drawString(font, viewHint, viewportRight - font.width(viewHint) - 12,
                viewportBottom + 7, MUTED, false);
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
        int y = hierarchyY + FLOATING_TITLE_HEIGHT + 4;
        List<Integer> orderedBones = orderedBoneIndices();
        for (int row = 0; row < orderedBones.size(); row++) {
            int index = orderedBones.get(row);
            CustomTurretDefinition.Bone bone = definition.bones.get(index);
            boolean selected = index == selectedBone;
            boolean hovered = mouseX >= hierarchyX + 4 && mouseX < hierarchyX + HIERARCHY_WIDTH - 4
                    && mouseY >= y && mouseY < y + 16;
            graphics.fill(hierarchyX + 4, y, hierarchyX + HIERARCHY_WIDTH - 4, y + 16,
                    selected ? 0xCC62686E : hovered ? 0x99565B61 : 0x00494D52);
            if (selected) {
                graphics.fill(hierarchyX + 4, y, hierarchyX + 7, y + 16, ACCENT);
            }
            int depth = boneDepth(bone);
            String marker = CustomTurretDefinition.isBoneGroup(bone.id) ? "▣ " : "◇ ";
            graphics.drawString(font, marker + bone.id,
                    hierarchyX + 10 + depth * 8, y + 4, selected ? TEXT : 0xFFAAC3C1, false);
            y += 17;
            if (y >= hierarchyY + HIERARCHY_HEIGHT - 49) {
                break;
            }
        }
        int buttonsY = hierarchyY + HIERARCHY_HEIGHT - 24;
        boolean managesFirepoints = "long_cannon".equals(selectedBone().id);
        drawButton(graphics, hierarchyX + 6, buttonsY - 21, 66, 18,
                Component.translatable("screen.vsie.custom_turret_editor.add_firepoint"), managesFirepoints);
        drawButton(graphics, hierarchyX + 78, buttonsY - 21, 66, 18,
                Component.translatable("screen.vsie.custom_turret_editor.remove_firepoint"), managesFirepoints);
        drawButton(graphics, hierarchyX + 6, buttonsY, 66, 18,
                Component.translatable("screen.vsie.custom_turret_editor.add_bone"), false);
        drawButton(graphics, hierarchyX + 78, buttonsY, 66, 18,
                Component.translatable("screen.vsie.custom_turret_editor.remove_bone"), false);
    }

    private void drawProperties(GuiGraphics graphics, int mouseX, int mouseY) {
        drawFloatingPanel(graphics, propertiesX, propertiesY, PROPERTIES_WIDTH, propertiesPanelHeight());
        int x = propertiesX + 7;
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.properties"),
                x, propertiesY + 6, ACCENT, false);
        drawCollapseIcon(graphics, propertiesX + PROPERTIES_WIDTH - 17, propertiesY + 3, propertiesCollapsed);
        if (propertiesCollapsed) {
            return;
        }
        if (isSelectedBoneGroup()) {
            // Function: fixed group selection renders a compact pivot-only property view.
            graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.pivot"),
                    x, pivot[0].getY() + 4, MUTED, false);
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
            graphics.drawString(font, labels[index], x, labelFields[index].getY() + 4, MUTED, false);
        }
        int actionY = propertyActionY();
        drawButton(graphics, x, actionY, 150, 18,
                Component.translatable("screen.vsie.custom_turret_editor.select_asset_package"), false);
        drawButton(graphics, x + 156, actionY, 80, 18,
                Component.translatable("screen.vsie.custom_turret_editor.parent_button"), false);
        drawButton(graphics, x, actionY + 24, 100, 18,
                Component.translatable(transformEditMode.translationKey), true);
        drawButton(graphics, x + 106, actionY + 24, 80, 18,
                Component.translatable("screen.vsie.custom_turret_editor.center_pivot"), false);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.rotate_quarter"),
                x + 192, actionY + 29, MUTED, false);
        String[] axisNames = {"X", "Y", "Z"};
        for (int axis = 0; axis < 3; axis++) {
            int buttonX = x + axis * 78;
            drawButton(graphics, buttonX, actionY + 47, 34, 18, Component.literal(axisNames[axis] + "-"), false);
            drawButton(graphics, buttonX + 38, actionY + 47, 34, 18,
                    Component.literal(axisNames[axis] + "+"), false);
        }
    }

    private void drawTurretProperties(GuiGraphics graphics, int mouseX, int mouseY) {
        drawFloatingPanel(graphics, turretPropertiesX, turretPropertiesY,
                TURRET_PROPERTIES_WIDTH, turretPropertiesPanelHeight());
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.turret_properties"),
                turretPropertiesX + 7, turretPropertiesY + 6, ACCENT, false);
        drawCloseIcon(graphics, turretPropertiesX + TURRET_PROPERTIES_WIDTH - 17, turretPropertiesY + 4);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.turret_type"),
                turretPropertiesX + 7, turretPropertiesY + 29, MUTED, false);
        drawButton(graphics, turretPropertiesX + 122, turretPropertiesY + 23, 120, 18,
                Component.translatable("screen.vsie.custom_turret_editor.turret_type." + definition.turretType), true);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.fire_cooldown"),
                turretPropertiesX + 7, fireCooldown.getY() + 4, MUTED, false);
        drawTurretFieldLabel(graphics, rotationSpeed, "rotation_speed");
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.weapon_type"),
                turretPropertiesX + 7, turretPropertiesY + 83, MUTED, false);
        drawButton(graphics, turretPropertiesX + 122, turretPropertiesY + 77, 120, 18,
                Component.translatable("screen.vsie.custom_turret_editor.weapon_type." + definition.weaponType), true);
        if ("energy".equals(definition.weaponType)) {
            drawTurretFieldLabel(graphics, energyPerTick, "energy_per_tick");
            graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.laser_color"),
                    turretPropertiesX + 7, turretPropertiesY + 119, MUTED, false);
            drawColorButton(graphics, turretPropertiesX + 122, turretPropertiesY + 113);
            drawTurretFieldLabel(graphics, laserRadius, "laser_radius");
        } else {
            drawTurretFieldLabel(graphics, ammoItemId, "ammo_item");
            drawTurretFieldLabel(graphics, projectileScale, "projectile_scale");
            drawTurretFieldLabel(graphics, projectileDamage, "projectile_damage");
            drawTurretFieldLabel(graphics, projectileExplosionRadius, "projectile_explosion_radius");
            drawTurretFieldLabel(graphics, projectileLifetime, "projectile_lifetime");
            drawTurretFieldLabel(graphics, projectileSpeed, "projectile_speed");
            if (definition.firepointCount > 1) {
                drawTurretFieldLabel(graphics, firepointInterval, "firepoint_interval");
            }
        }
    }

    private void drawCloseIcon(GuiGraphics graphics, int x, int y) {
        int color = 0xFFE4FAF7;
        for (int step = 0; step < 9; step++) {
            graphics.fill(x + step, y + step, x + step + 2, y + step + 2, color);
            graphics.fill(x + 8 - step, y + step, x + 10 - step, y + step + 2, color);
        }
    }

    private void drawTurretFieldLabel(GuiGraphics graphics, EditBox field, String key) {
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor." + key),
                turretPropertiesX + 7, field.getY() + 4, MUTED, false);
    }

    private void drawColorButton(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 120, y + 18, 0xFF565B61);
        graphics.fill(x + 2, y + 2, x + 118, y + 16, definition.laserColor);
    }

    private void drawFloatingPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x6687CEFA);
        graphics.fill(x, y, x + width, y + height, SIDEBAR);
        graphics.fill(x, y, x + width, y + FLOATING_TITLE_HEIGHT, FRAME);
        graphics.fill(x, y + FLOATING_TITLE_HEIGHT - 1, x + width, y + FLOATING_TITLE_HEIGHT, 0xAA87CEFA);
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

    private void drawFieldBackgrounds(GuiGraphics graphics) {
        for (EditBox field : allFields) {
            if (!field.visible) {
                continue;
            }
            graphics.fill(field.getX() - 2, field.getY() - 2,
                    field.getX() + field.getWidth() + 2, field.getY() + field.getHeight() + 2,
                    field.isFocused() ? ACCENT : 0xFF565B61);
            graphics.fill(field.getX(), field.getY(), field.getX() + field.getWidth(),
                    field.getY() + field.getHeight(), FIELD_BACKGROUND);
        }
    }

    private void drawViewport(GuiGraphics graphics) {
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
        CustomTurretMeshRenderer.render(definition, poseStack, graphics.bufferSource(), LightTexture.FULL_BRIGHT);
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
            if (inside(mouseX, mouseY, turretPropertiesToolX(), panelTop + 9, 24, 24)) {
                turretPropertiesOpen = true;
                updateTurretFieldVisibility();
                clampFloatingPanels();
                positionTurretPropertyFields();
                return true;
            }
            if (turretPropertiesOpen && inside(mouseX, mouseY,
                    turretPropertiesX + TURRET_PROPERTIES_WIDTH - 21, turretPropertiesY, 21, FLOATING_TITLE_HEIGHT)) {
                closeTurretProperties();
                return true;
            }
            if (inside(mouseX, mouseY, hierarchyX + HIERARCHY_WIDTH - 20, hierarchyY,
                    20, FLOATING_TITLE_HEIGHT)) {
                toggleHierarchyPanel();
                return true;
            }
            if (inside(mouseX, mouseY, propertiesX + PROPERTIES_WIDTH - 20, propertiesY,
                    20, FLOATING_TITLE_HEIGHT)) {
                togglePropertiesPanel();
                return true;
            }
            if (inside(mouseX, mouseY, hierarchyX, hierarchyY, HIERARCHY_WIDTH, FLOATING_TITLE_HEIGHT)) {
                beginPanelDrag(FloatingPanel.HIERARCHY, mouseX, mouseY, hierarchyX, hierarchyY);
                return true;
            }
            if (inside(mouseX, mouseY, propertiesX, propertiesY, PROPERTIES_WIDTH, FLOATING_TITLE_HEIGHT)) {
                beginPanelDrag(FloatingPanel.PROPERTIES, mouseX, mouseY, propertiesX, propertiesY);
                return true;
            }
            if (turretPropertiesOpen && inside(mouseX, mouseY, turretPropertiesX, turretPropertiesY,
                    TURRET_PROPERTIES_WIDTH, FLOATING_TITLE_HEIGHT)) {
                beginPanelDrag(FloatingPanel.TURRET_PROPERTIES, mouseX, mouseY,
                        turretPropertiesX, turretPropertiesY);
                return true;
            }
            if (handleHeaderClick(mouseX, mouseY)
                    || (!hierarchyCollapsed && handleHierarchyClick(mouseX, mouseY))
                    || (!propertiesCollapsed && handlePropertyButtons(mouseX, mouseY))
                    || handleTurretPropertyClick(mouseX, mouseY)) {
                return true;
            }
            if (insideFloatingPanel(mouseX, mouseY)) {
                // Function: fields receive the click, while unused panel space cannot manipulate the viewport.
                super.mouseClicked(mouseX, mouseY, button);
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
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
                CustomTurretDefinition.Bone bone = selectedBone();
                float[] values = transformEditMode == TransformEditMode.POSITION ? bone.position : bone.pivot;
                EditBox[] fields = transformEditMode == TransformEditMode.POSITION ? position : pivot;
                values[activeAxis] += amount;
                fields[activeAxis].setValue(format(values[activeAxis]));
            }
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && panningView) {
            float[] nextPan = CustomTurretEditorCamera.pan(viewPanX, viewPanY, (float) dragX, (float) dragY);
            viewPanX = nextPan[0];
            viewPanY = nextPan[1];
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void applyPreviewFields() {
        synchronizePackageTexture();
        CustomTurretDefinition.Bone bone = selectedBone();
        bone.model = modelPath.getValue().trim();
        bone.texture = texturePath.getValue().trim();
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
            bone.pivot = nextPivot;
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
            panningView = false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rotatingView = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
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
            loadNextDefinition();
        } else if (inside(mouseX, mouseY, buttonX + 60, panelTop + 10, 54, 22)) {
            definition = CustomTurretDefinition.createNew(nextDefinitionId());
            selectedBone = 0;
            status = Component.translatable("screen.vsie.custom_turret_editor.new_definition");
            loadFields();
        } else {
            openSaveDialog();
        }
        return true;
    }

    private boolean handleHierarchyClick(double mouseX, double mouseY) {
        int row = (int) ((mouseY - (hierarchyY + FLOATING_TITLE_HEIGHT + 4)) / 17);
        List<Integer> orderedBones = orderedBoneIndices();
        if (mouseX >= hierarchyX + 4 && mouseX < hierarchyX + HIERARCHY_WIDTH - 4
                && row >= 0 && row < orderedBones.size()) {
            applyFields(false);
            selectedBone = orderedBones.get(row);
            loadFields();
            return true;
        }
        int y = hierarchyY + HIERARCHY_HEIGHT - 24;
        if (inside(mouseX, mouseY, hierarchyX + 6, y - 21, 66, 18)) {
            if (!"long_cannon".equals(selectedBone().id)) {
                status = Component.translatable("screen.vsie.custom_turret_editor.select_long_cannon");
                return true;
            }
            applyFields(false);
            CustomTurretDefinition.Bone firepoint = definition.addFirepoint();
            selectedBone = definition.bones.indexOf(firepoint);
            loadFields();
            return true;
        }
        if (inside(mouseX, mouseY, hierarchyX + 78, y - 21, 66, 18)) {
            if (!"long_cannon".equals(selectedBone().id)) {
                status = Component.translatable("screen.vsie.custom_turret_editor.select_long_cannon");
                return true;
            }
            applyFields(false);
            CustomTurretDefinition.Bone removed = definition.removeLastFirepoint();
            if (removed != null && selectedBone >= definition.bones.size()) {
                selectedBone = definition.bones.indexOf(definition.findBone("firepoint" + definition.firepointCount));
            }
            loadFields();
            return true;
        }
        if (inside(mouseX, mouseY, hierarchyX + 6, y, 66, 18)) {
            applyFields(false);
            String id = nextBoneId();
            String group = CustomTurretDefinition.isRequiredBoneGroup(selectedBone().id)
                    ? selectedBone().id : selectedBone().parent;
            definition.bones.add(CustomTurretDefinition.Bone.child(id, group));
            selectedBone = definition.bones.size() - 1;
            loadFields();
            return true;
        }
        if (inside(mouseX, mouseY, hierarchyX + 78, y, 66, 18)
                && !CustomTurretDefinition.isBoneGroup(selectedBone().id)) {
            removeSelectedBone();
            return true;
        }
        return false;
    }

    private boolean handlePropertyButtons(double mouseX, double mouseY) {
        if (isSelectedBoneGroup()) {
            return false;
        }
        int x = propertiesX + 7;
        int y = propertyActionY();
        if (inside(mouseX, mouseY, x, y, 150, 18)) {
            minecraft.setScreen(new CustomTurretAssetPackageScreen(this::selectAssetPackage, this));
            return true;
        }
        if (inside(mouseX, mouseY, x + 156, y, 80, 18)) {
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
        for (int axis = 0; axis < 3; axis++) {
            int buttonX = x + axis * 78;
            if (inside(mouseX, mouseY, buttonX, y + 47, 34, 18)) {
                rotateByQuarter(axis, -90.0F);
                return true;
            }
            if (inside(mouseX, mouseY, buttonX + 38, y + 47, 34, 18)) {
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
        if (inside(mouseX, mouseY, turretPropertiesX + 122, turretPropertiesY + 23, 120, 18)) {
            int index = CustomTurretDefinition.TURRET_TYPES.indexOf(definition.turretType);
            definition.turretType = CustomTurretDefinition.TURRET_TYPES.get(
                    (index + 1) % CustomTurretDefinition.TURRET_TYPES.size());
            return true;
        }
        if (inside(mouseX, mouseY, turretPropertiesX + 122, turretPropertiesY + 77, 120, 18)) {
            definition.weaponType = "energy".equals(definition.weaponType) ? "projectile" : "energy";
            updateTurretFieldVisibility();
            clampFloatingPanels();
            return true;
        }
        if ("energy".equals(definition.weaponType)
                && inside(mouseX, mouseY, turretPropertiesX + 122, turretPropertiesY + 113, 120, 18)) {
            minecraft.setScreen(new CustomTurretColorScreen(definition.laserColor, color -> {
                definition.laserColor = color;
                minecraft.setScreen(this);
            }, this));
            return true;
        }
        return false;
    }

    private void centerPivotOnModel() {
        applyFields(false);
        CustomTurretDefinition.Bone bone = selectedBone();
        Vector3f center = CustomTurretAssetCache.modelCenter(bone.model);
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
        CustomTurretDefinition.Bone bone = selectedBone();
        bone.rotation[axis] = normalizeDegrees(bone.rotation[axis] + amount);
        rotation[axis].setValue(format(bone.rotation[axis]));
    }

    private void selectAssetPackage(CustomTurretAssetPackage selected) {
        modelPath.setValue(selected.modelPath());
        texturePath.setValue(selected.texturePath());
        applyFields(false);
        CustomTurretAssetCache.invalidateMeshes();
    }

    private void synchronizePackageTexture() {
        // Function: a folder package owns exactly one same-named texture, so manual PNG mismatches cannot persist.
        CustomTurretAssetPackage.fromModelPath(modelPath.getValue())
                .ifPresent(assetPackage -> texturePath.setValue(assetPackage.texturePath()));
    }

    private void cycleParent() {
        applyFields(false);
        CustomTurretDefinition.Bone selected = selectedBone();
        if (CustomTurretDefinition.isRequiredBoneGroup(selected.id)) {
            status = Component.translatable("screen.vsie.custom_turret_editor.required_bone_locked");
            return;
        }
        List<String> candidates = new ArrayList<>();
        candidates.addAll(CustomTurretDefinition.REQUIRED_BONES);
        int index = candidates.indexOf(selected.parent);
        selected.parent = candidates.get((index + 1) % candidates.size());
        parentId.setValue(selected.parent);
    }

    private void saveDefinition(String folderPath, String fileName) {
        if (!applyFields(true)) {
            return;
        }
        try {
            definition.id = CustomTurretStorage.definitionId(fileName);
            CustomTurretStorage.saveDefinition(definition, folderPath, fileName);
            savedDefinitions = CustomTurretStorage.loadAllDefinitions();
            CustomTurretAssetCache.invalidateMeshes();
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
        minecraft.setScreen(new CustomTurretDefinitionSaveScreen(this,
                definition.id, (folderPath, fileName) -> saveDefinition(folderPath, fileName)));
    }

    private void loadNextDefinition() {
        savedDefinitions = CustomTurretStorage.loadAllDefinitions();
        if (savedDefinitions.isEmpty()) {
            status = Component.translatable("screen.vsie.custom_turret_editor.no_definitions");
            return;
        }
        int current = -1;
        for (int index = 0; index < savedDefinitions.size(); index++) {
            if (savedDefinitions.get(index).id.equals(definition.id)) {
                current = index;
                break;
            }
        }
        definition = savedDefinitions.get((current + 1) % savedDefinitions.size()).copy();
        selectedBone = 0;
        loadFields();
        status = Component.translatable("screen.vsie.custom_turret_editor.loaded", definition.name);
    }

    private void removeSelectedBone() {
        applyFields(false);
        CustomTurretDefinition.Bone removed = selectedBone();
        String replacementParent = removed.parent;
        definition.bones.remove(selectedBone);
        for (CustomTurretDefinition.Bone bone : definition.bones) {
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

    private Matrix4f parentTransform(CustomTurretDefinition.Bone bone) {
        Matrix4f transform = new Matrix4f();
        if (bone.parent.isEmpty()) {
            return transform;
        }
        CustomTurretDefinition.Bone parent = definition.findBone(bone.parent);
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

    private int boneDepth(CustomTurretDefinition.Bone bone) {
        int depth = 0;
        String parent = bone.parent;
        while (!parent.isEmpty() && depth < 8) {
            CustomTurretDefinition.Bone parentBone = definition.findBone(parent);
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
        appendBoneGroup(ordered, "root", "turret");
        return ordered;
    }

    private void appendBoneGroup(List<Integer> ordered, String groupId, String childGroupId) {
        int groupIndex = definition.bones.indexOf(definition.findBone(groupId));
        if (groupIndex >= 0) {
            ordered.add(groupIndex);
        }
        for (int index = 0; index < definition.bones.size(); index++) {
            CustomTurretDefinition.Bone bone = definition.bones.get(index);
            if (!CustomTurretDefinition.isRequiredBoneGroup(bone.id) && groupId.equals(bone.parent)) {
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
        int x = turretPropertiesX + 122;
        moveField(fireCooldown, x, turretPropertiesY + 44);
        moveField(rotationSpeed, x, turretPropertiesY + 62);
        moveField(energyPerTick, x, turretPropertiesY + 98);
        moveField(laserRadius, x, turretPropertiesY + 134);
        moveField(ammoItemId, x, turretPropertiesY + 98);
        moveField(projectileScale, x, turretPropertiesY + 116);
        moveField(projectileDamage, x, turretPropertiesY + 134);
        moveField(projectileExplosionRadius, x, turretPropertiesY + 152);
        moveField(projectileLifetime, x, turretPropertiesY + 170);
        moveField(projectileSpeed, x, turretPropertiesY + 188);
        moveField(firepointInterval, x, turretPropertiesY + 206);
    }

    private boolean isDescendant(CustomTurretDefinition.Bone candidate, String ancestorId) {
        String parent = candidate.parent;
        while (!parent.isEmpty()) {
            if (ancestorId.equals(parent)) {
                return true;
            }
            CustomTurretDefinition.Bone bone = definition.findBone(parent);
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

    private String nextDefinitionId() {
        int suffix = 1;
        while (containsDefinition("custom_turret_" + suffix)) {
            suffix++;
        }
        return "custom_turret_" + suffix;
    }

    private boolean containsDefinition(String id) {
        return savedDefinitions.stream().anyMatch(definition -> definition.id.equals(id));
    }

    private CustomTurretDefinition.Bone selectedBone() {
        return definition.bones.get(selectedBone);
    }

    private int propertyActionY() {
        return scale[0].getY() + scale[0].getHeight() + 8;
    }

    private void beginPanelDrag(FloatingPanel panel, double mouseX, double mouseY, int panelX, int panelY) {
        draggedPanel = panel;
        panelDragOffsetX = (int) mouseX - panelX;
        panelDragOffsetY = (int) mouseY - panelY;
    }

    private void updatePropertyFieldVisibility() {
        boolean propertyPanelVisible = !propertiesCollapsed;
        boolean pivotOnly = isSelectedBoneGroup();
        for (EditBox field : allFields) {
            if (isTurretPropertyField(field)) {
                continue;
            }
            boolean visible = propertyPanelVisible && (!pivotOnly || isPivotField(field));
            field.visible = visible;
            if (!visible) {
                field.setFocused(false);
            }
        }
    }

    private boolean isPivotField(EditBox candidate) {
        return candidate == pivot[0] || candidate == pivot[1] || candidate == pivot[2];
    }

    private boolean isSelectedBoneGroup() {
        return !definition.bones.isEmpty()
                && CustomTurretDefinition.isBoneGroup(selectedBone().id);
    }

    private boolean isTurretPropertyField(EditBox field) {
        return field == fireCooldown || field == rotationSpeed || field == energyPerTick || field == laserRadius
                || field == ammoItemId || field == projectileScale || field == projectileDamage
                || field == projectileExplosionRadius || field == projectileLifetime || field == projectileSpeed
                || field == firepointInterval;
    }

    private void updateTurretFieldVisibility() {
        boolean energy = "energy".equals(definition.weaponType);
        fireCooldown.visible = turretPropertiesOpen;
        rotationSpeed.visible = turretPropertiesOpen;
        energyPerTick.visible = turretPropertiesOpen && energy;
        laserRadius.visible = turretPropertiesOpen && energy;
        ammoItemId.visible = turretPropertiesOpen && !energy;
        projectileScale.visible = turretPropertiesOpen && !energy;
        projectileDamage.visible = turretPropertiesOpen && !energy;
        projectileExplosionRadius.visible = turretPropertiesOpen && !energy;
        projectileLifetime.visible = turretPropertiesOpen && !energy;
        projectileSpeed.visible = turretPropertiesOpen && !energy;
        firepointInterval.visible = turretPropertiesOpen && !energy && definition.firepointCount > 1;
        if (!turretPropertiesOpen) {
            for (EditBox field : allFields) {
                if (isTurretPropertyField(field)) {
                    field.setFocused(false);
                }
            }
        }
    }

    private void closeTurretProperties() {
        turretPropertiesOpen = false;
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
        clampFloatingPanels();
    }

    private void togglePropertiesPanel() {
        propertiesCollapsed = !propertiesCollapsed;
        updatePropertyFieldVisibility();
        clampFloatingPanels();
        // Function: expansion may move the window upward, so fields must follow in the same click.
        positionPropertyFields();
    }

    private int hierarchyPanelHeight() {
        return hierarchyCollapsed ? FLOATING_TITLE_HEIGHT : HIERARCHY_HEIGHT;
    }

    private int propertiesPanelHeight() {
        if (propertiesCollapsed) {
            return FLOATING_TITLE_HEIGHT;
        }
        return isSelectedBoneGroup() ? FLOATING_TITLE_HEIGHT + 26 : PROPERTIES_HEIGHT;
    }

    private int turretPropertiesPanelHeight() {
        return "energy".equals(definition.weaponType) ? 158 : definition.firepointCount > 1 ? 230 : 212;
    }

    private void clampFloatingPanels() {
        hierarchyX = Math.max(viewportLeft + 2, Math.min(viewportRight - HIERARCHY_WIDTH - 2, hierarchyX));
        hierarchyY = Math.max(viewportTop + 2,
                Math.min(viewportBottom - hierarchyPanelHeight() - 2, hierarchyY));
        propertiesX = Math.max(viewportLeft + 2, Math.min(viewportRight - PROPERTIES_WIDTH - 2, propertiesX));
        propertiesY = Math.max(viewportTop + 2,
                Math.min(viewportBottom - propertiesPanelHeight() - 2, propertiesY));
        turretPropertiesX = Math.max(viewportLeft + 2,
                Math.min(viewportRight - TURRET_PROPERTIES_WIDTH - 2, turretPropertiesX));
        turretPropertiesY = Math.max(viewportTop + 2,
                Math.min(viewportBottom - turretPropertiesPanelHeight() - 2, turretPropertiesY));
    }

    private boolean insideViewport(double x, double y) {
        return x >= viewportLeft && x < viewportRight && y >= viewportTop && y < viewportBottom;
    }

    private boolean insideFloatingPanel(double x, double y) {
        return inside(x, y, hierarchyX, hierarchyY, HIERARCHY_WIDTH, hierarchyPanelHeight())
                || inside(x, y, propertiesX, propertiesY, PROPERTIES_WIDTH, propertiesPanelHeight())
                || turretPropertiesOpen && inside(x, y, turretPropertiesX, turretPropertiesY,
                TURRET_PROPERTIES_WIDTH, turretPropertiesPanelHeight());
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
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, label,
                x + width / 2, y + 6, accent ? TEXT : 0xFFAAC3C1);
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

    private enum TransformEditMode {
        POSITION("screen.vsie.custom_turret_editor.adjust_position"),
        PIVOT("screen.vsie.custom_turret_editor.adjust_pivot");

        private final String translationKey;

        TransformEditMode(String translationKey) {
            this.translationKey = translationKey;
        }
    }
}
