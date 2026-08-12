package com.kodu16.vsie.content.custom_turret.export.client;

import com.kodu16.vsie.content.custom_turret.CustomTurretStorage;
import com.kodu16.vsie.content.custom_turret.export.RegionExportSelection;
import com.kodu16.vsie.foundation.client.CrispScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** EBL-styled confirmation screen for naming and writing one world-region export. */
public final class RegionObjExportScreen extends CrispScreen {
    private static final int FRAME = 0xFF2E3238;
    private static final int PANEL = 0xF23A3D42;
    private static final int ACCENT = 0xFF87CEFA;
    private static final int TEXT = 0xFFE4FAF7;
    private static final int MUTED = 0xFF789B9D;
    private static final DateTimeFormatter DEFAULT_NAME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final RegionExportSelection selection;
    private EditBox nameField;
    private Component status = Component.empty();
    private int statusColor = MUTED;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public RegionObjExportScreen(RegionExportSelection selection) {
        super(Component.translatable("screen.vsie.region_obj_export"));
        this.selection = selection;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 32);
        panelHeight = 250;
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        nameField = new EditBox(font, left + 120, top + 104, panelWidth - 144, 18, Component.empty());
        nameField.setMaxLength(64);
        nameField.setValue("structure_" + DEFAULT_NAME.format(LocalDateTime.now()));
        nameField.setTextColor(TEXT);
        nameField.setBordered(false);
        addRenderableWidget(nameField);

        addRenderableWidget(Button.builder(Component.translatable("screen.vsie.region_obj_export.export"), button -> export())
                .bounds(left + panelWidth - 220, top + panelHeight - 39, 96, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(left + panelWidth - 114, top + panelHeight - 39, 90, 20).build());
        setInitialFocus(nameField);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Function: apply world blur before any exporter panel or widget is written to the main render target.
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xA61C1E22);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        graphics.fill(left - 3, top - 3, right + 3, bottom + 3, 0xAA87CEFA);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 42, FRAME);
        graphics.fill(left, bottom - 52, right, bottom, FRAME);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.header"),
                left + 14, top + 15, ACCENT, false);

        int bodyX = left + 24;
        int bodyY = top + 58;
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.corner_1", selection.first().toShortString()),
                bodyX, bodyY, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.corner_2", selection.second().toShortString()),
                bodyX, bodyY + 17, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.volume", selection.volume()),
                bodyX, bodyY + 34, MUTED, false);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.name"), bodyX, top + 109, MUTED, false);
        graphics.fill(left + 116, top + 101, right - 20, top + 125, 0xCC272A2F);
        graphics.drawString(font, Component.literal(CustomTurretStorage.root().toString()), bodyX, top + 139, MUTED, false);
        graphics.drawWordWrap(font, status, bodyX, top + 157, panelWidth - 48, statusColor);
        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void export() {
        if (minecraft == null || minecraft.level == null) {
            setError(Component.translatable("screen.vsie.region_obj_export.no_world"));
            return;
        }
        status = Component.translatable("screen.vsie.region_obj_export.exporting");
        statusColor = ACCENT;
        try {
            RegionObjExporter.ExportResult result = RegionObjExporter.export(minecraft.level, selection, nameField.getValue());
            status = Component.translatable(
                    "screen.vsie.region_obj_export.success",
                    result.blocks(), result.quads(), result.skippedDynamicBlocks(), result.skippedUnloadedBlocks()
            );
            statusColor = 0xFF79D27C;
        } catch (IOException | IllegalArgumentException exception) {
            setError(Component.literal(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
        }
    }

    private void setError(Component message) {
        status = message;
        statusColor = 0xFFFF7C7C;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
