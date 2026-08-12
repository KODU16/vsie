package com.kodu16.vsie.content.custom_turret.client;

import com.kodu16.vsie.content.custom_turret.CustomTurretDefinition;
import com.kodu16.vsie.content.custom_turret.CustomTurretStorage;
import com.kodu16.vsie.foundation.client.CrispScreen;
import com.kodu16.vsie.network.custom_turret.SelectCustomTurretC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Folder-and-file definition browser used by the custom turret placer. */
public final class CustomTurretSelectorScreen extends CrispScreen {
    private static final int FRAME = 0xFF2E3238;
    private static final int PANEL = 0xF23A3D42;
    private static final int SIDEBAR = 0xF2494D52;
    private static final int ACCENT = 0xFF87CEFA;
    private static final int TEXT = 0xFFE4FAF7;
    private static final int MUTED = 0xFF789B9D;
    private static final int ROW_HEIGHT = 22;
    private static final int FOLDER_WIDTH = 170;
    private final InteractionHand hand;
    private List<String> directories = List.of("");
    private List<CustomTurretStorage.DefinitionFile> files = List.of();
    private int selectedDirectory;
    private int selectedFile;
    private int firstVisibleDirectory;
    private int firstVisibleFile;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    public CustomTurretSelectorScreen(InteractionHand hand) {
        super(Component.translatable("screen.vsie.custom_turret_selector"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        directories = CustomTurretStorage.scanDirectories();
        selectedDirectory = Math.min(selectedDirectory, Math.max(0, directories.size() - 1));
        refreshFiles();
        panelWidth = Math.min(620, width - 36);
        panelHeight = Math.min(400, height - 32);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
    }

    private void refreshFiles() {
        files = CustomTurretStorage.loadDefinitionFiles(directories.get(selectedDirectory));
        selectedFile = Math.min(selectedFile, Math.max(0, files.size() - 1));
        firstVisibleFile = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xA61C1E22);
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        graphics.fill(left - 3, top - 3, right + 3, bottom + 3, 0xAA87CEFA);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left, top, right, top + 42, FRAME);
        graphics.fill(left, top + 42, left + FOLDER_WIDTH, bottom - 24, SIDEBAR);
        graphics.fill(left, bottom - 24, right, bottom, FRAME);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_selector.header"),
                left + 14, top + 15, ACCENT, false);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_selector.controls"),
                left + 12, bottom - 16, MUTED, false);
        drawFolders(graphics, mouseX, mouseY);
        drawFiles(graphics, mouseX, mouseY);
        drawDetails(graphics);
    }

    private void drawFolders(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_selector.folders"),
                left + 10, top + 49, MUTED, false);
        int visible = visibleRows();
        for (int row = 0; row < visible; row++) {
            int index = firstVisibleDirectory + row;
            if (index >= directories.size()) {
                break;
            }
            int y = top + 64 + row * ROW_HEIGHT;
            boolean active = index == selectedDirectory;
            boolean hovered = inside(mouseX, mouseY, left + 4, y, FOLDER_WIDTH - 8, ROW_HEIGHT - 2);
            graphics.fill(left + 4, y, left + FOLDER_WIDTH - 4, y + ROW_HEIGHT - 2,
                    active ? 0xCC62686E : hovered ? 0x99565B61 : 0x00494D52);
            String folder = directories.get(index);
            graphics.drawString(font, "▸ " + (folder.isEmpty() ? "/" : folder), left + 10, y + 6,
                    active ? TEXT : 0xFFAAC3C1, false);
        }
    }

    private void drawFiles(GuiGraphics graphics, int mouseX, int mouseY) {
        int fileLeft = left + FOLDER_WIDTH + 8;
        int fileWidth = 210;
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_selector.files"),
                fileLeft, top + 49, MUTED, false);
        for (int row = 0; row < visibleRows(); row++) {
            int index = firstVisibleFile + row;
            if (index >= files.size()) {
                break;
            }
            int y = top + 64 + row * ROW_HEIGHT;
            boolean active = index == selectedFile;
            boolean hovered = inside(mouseX, mouseY, fileLeft, y, fileWidth, ROW_HEIGHT - 2);
            graphics.fill(fileLeft, y, fileLeft + fileWidth, y + ROW_HEIGHT - 2,
                    active ? 0xCC62686E : hovered ? 0x99565B61 : 0x332E3238);
            graphics.drawString(font, files.get(index).fileName(), fileLeft + 7, y + 6,
                    active ? TEXT : 0xFFAAC3C1, false);
        }
        if (files.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("screen.vsie.custom_turret_selector.empty_folder"),
                    fileLeft, top + 68, fileWidth, MUTED);
        }
    }

    private void drawDetails(GuiGraphics graphics) {
        if (files.isEmpty()) {
            return;
        }
        int x = left + FOLDER_WIDTH + 232;
        int y = top + 65;
        CustomTurretDefinition definition = files.get(selectedFile).definition();
        graphics.drawString(font, definition.name, x, y, TEXT, false);
        graphics.fill(x, y + 17, left + panelWidth - 16, y + 18, 0x6687CEFA);
        graphics.drawWordWrap(font, Component.translatable("screen.vsie.custom_turret_selector.folder",
                directories.get(selectedDirectory).isEmpty() ? "/" : directories.get(selectedDirectory)),
                x, y + 28, panelWidth - (x - left) - 16, MUTED);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_selector.id", definition.id),
                x, y + 55, MUTED, false);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_selector.bones", definition.bones.size()),
                x, y + 72, MUTED, false);
        graphics.drawWordWrap(font, Component.translatable("screen.vsie.custom_turret_selector.hint"),
                x, y + 98, panelWidth - (x - left) - 16, 0xFFAAC3C1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int row = (int) ((mouseY - (top + 64)) / ROW_HEIGHT);
            if (row >= 0 && inside(mouseX, mouseY, left + 4, top + 64, FOLDER_WIDTH - 8,
                    visibleRows() * ROW_HEIGHT)) {
                int index = firstVisibleDirectory + row;
                if (index < directories.size()) {
                    selectedDirectory = index;
                    selectedFile = 0;
                    refreshFiles();
                    return true;
                }
            }
            if (row >= 0 && inside(mouseX, mouseY, left + FOLDER_WIDTH + 8, top + 64, 210,
                    visibleRows() * ROW_HEIGHT)) {
                int index = firstVisibleFile + row;
                if (index < files.size()) {
                    selectedFile = index;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int direction = scrollY < 0 ? 1 : -1;
        if (mouseX < left + FOLDER_WIDTH) {
            firstVisibleDirectory = clampScroll(firstVisibleDirectory + direction, directories.size());
        } else {
            firstVisibleFile = clampScroll(firstVisibleFile + direction, files.size());
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (!files.isEmpty()) {
                ModNetworking.sendToServer(new SelectCustomTurretC2SPacket(hand,
                        files.get(selectedFile).definition().toJson()));
                onClose();
            }
            return true;
        }
        if (!files.isEmpty() && (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN)) {
            selectedFile = Math.floorMod(selectedFile + (keyCode == GLFW.GLFW_KEY_UP ? -1 : 1), files.size());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private int visibleRows() {
        return Math.max(1, (panelHeight - 96) / ROW_HEIGHT);
    }

    private int clampScroll(int value, int size) {
        return Math.max(0, Math.min(Math.max(0, size - visibleRows()), value));
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
