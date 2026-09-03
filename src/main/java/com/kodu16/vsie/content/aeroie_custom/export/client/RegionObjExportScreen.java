package com.kodu16.vsie.content.aeroie_custom.export.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceStorage;
import com.kodu16.vsie.content.aeroie_custom.export.RegionExportSelection;
import com.kodu16.vsie.foundation.client.CrispScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** EBL-styled export screen with an air-surround warning and a Components-root folder picker. */
public final class RegionObjExportScreen extends CrispScreen {
    private static final int FRAME = 0xFF2E3238;
    private static final int PANEL = 0xF23A3D42;
    private static final int ACCENT = 0xFF87CEFA;
    private static final int TEXT = 0xFFE4FAF7;
    private static final int MUTED = 0xFF789B9D;
    private static final int WARNING = 0xFFFFD27C;
    private static final int ROW_HEIGHT = 18;
    private static final DateTimeFormatter DEFAULT_NAME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final RegionExportSelection selection;
    private List<String> directories = List.of("");
    private int selected;
    private int firstVisible;
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
        directories = CustomDeviceStorage.scanComponentsDirectories();
        panelWidth = Math.min(520, width - 32);
        panelHeight = Math.min(360, height - 28);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        nameField = new EditBox(font, left + 88, top + panelHeight - 57, panelWidth - 112, 18, Component.empty());
        nameField.setMaxLength(64);
        nameField.setValue("structure_" + DEFAULT_NAME.format(LocalDateTime.now()));
        nameField.setTextColor(TEXT);
        nameField.setBordered(false);
        addRenderableWidget(nameField);
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
        graphics.fill(left, bottom - 68, right, bottom, FRAME);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.header"),
                left + 14, top + 15, ACCENT, false);

        int bodyX = left + 24;
        int bodyY = top + 58;
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.corner_1",
                        selection.first().toShortString()),
                bodyX, bodyY, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.corner_2",
                        selection.second().toShortString()),
                bodyX, bodyY + 17, TEXT, false);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.volume", selection.volume()),
                bodyX, bodyY + 34, MUTED, false);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.air_warning"),
                bodyX, bodyY + 51, WARNING, false);
        graphics.drawString(font, Component.literal(CustomDeviceStorage.componentsRoot().toString()),
                left + 10, top + 116, MUTED, false);
        drawDirectories(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("screen.vsie.region_obj_export.name"),
                left + 12, top + panelHeight - 54, MUTED, false);
        graphics.fill(nameField.getX() - 2, nameField.getY() - 2,
                nameField.getX() + nameField.getWidth() + 2, nameField.getY() + nameField.getHeight() + 2,
                nameField.isFocused() ? ACCENT : 0xFF565B61);
        graphics.fill(nameField.getX(), nameField.getY(), nameField.getX() + nameField.getWidth(),
                nameField.getY() + nameField.getHeight(), 0xCC272A2F);
        nameField.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, status, left + 10, top + panelHeight - 25, statusColor, false);
        drawButton(graphics, left + panelWidth - 174, top + panelHeight - 27, 76,
                Component.translatable("screen.vsie.region_obj_export.export"), true);
        drawButton(graphics, left + panelWidth - 90, top + panelHeight - 27, 76,
                Component.translatable("gui.cancel"), false);
    }

    private void drawDirectories(GuiGraphics graphics, int mouseX, int mouseY) {
        int listTop = top + 128;
        int visible = Math.max(1, (panelHeight - 206) / ROW_HEIGHT);
        for (int row = 0; row < visible; row++) {
            int index = firstVisible + row;
            if (index >= directories.size()) {
                break;
            }
            int y = listTop + row * ROW_HEIGHT;
            boolean active = index == selected;
            boolean hovered = inside(mouseX, mouseY, left + 8, y, panelWidth - 16, ROW_HEIGHT - 1);
            graphics.fill(left + 8, y, left + panelWidth - 8, y + ROW_HEIGHT - 1,
                    active ? 0xCC62686E : hovered ? 0x99565B61 : 0x332E3238);
            String path = directories.get(index);
            String label = path.isEmpty() ? "/" : "/" + path;
            graphics.drawString(font, Component.literal("▸ " + label), left + 14, y + 4,
                    active ? TEXT : 0xFFAAC3C1, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int listTop = top + 128;
            int row = (int) ((mouseY - listTop) / ROW_HEIGHT);
            int index = firstVisible + row;
            if (mouseX >= left + 8 && mouseX < left + panelWidth - 8 && row >= 0 && index < directories.size()) {
                selected = index;
                return true;
            }
            if (inside(mouseX, mouseY, left + panelWidth - 174, top + panelHeight - 27, 76, 20)) {
                export();
                return true;
            }
            if (inside(mouseX, mouseY, left + panelWidth - 90, top + panelHeight - 27, 76, 20)) {
                onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int visible = Math.max(1, (panelHeight - 206) / ROW_HEIGHT);
        firstVisible = Math.max(0, Math.min(Math.max(0, directories.size() - visible),
                firstVisible + (scrollY < 0 ? 1 : -1)));
        return true;
    }

    private void export() {
        if (minecraft == null || minecraft.level == null) {
            setError(Component.translatable("screen.vsie.region_obj_export.no_world"));
            return;
        }
        try {
            String name = CustomDeviceStorage.definitionId(nameField.getValue());
            status = Component.translatable("screen.vsie.region_obj_export.exporting");
            statusColor = ACCENT;
            RegionObjExporter.ExportResult result = RegionObjExporter.export(
                    minecraft.level, selection, directories.get(selected), name);
            status = Component.translatable("screen.vsie.region_obj_export.success",
                    result.blocks(), result.quads(), result.skippedDynamicBlocks(), result.skippedUnloadedBlocks());
            statusColor = 0xFF79D27C;
        } catch (IOException | IllegalArgumentException exception) {
            setError(Component.literal(
                    exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
        }
    }

    private void setError(Component message) {
        status = message;
        statusColor = 0xFFFF7C7C;
    }

    private static void drawButton(GuiGraphics graphics, int x, int y, int width, Component label, boolean accent) {
        graphics.fill(x, y, x + width, y + 20, accent ? 0xCC538CA5 : 0xCC565B61);
        graphics.fill(x, y + 18, x + width, y + 20, accent ? ACCENT : MUTED);
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, label, x + width / 2, y + 6,
                accent ? TEXT : 0xFFAAC3C1);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
