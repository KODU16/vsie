package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceAssetPackage;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceStorage;
import com.kodu16.vsie.foundation.client.CrispScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/** Photon-style in-game folder chooser restricted to complete custom-turret model packages. */
final class CustomDeviceAssetPackageScreen extends CrispScreen {
    private static final int FRAME = 0xFF2E3238;
    private static final int PANEL = 0xF23A3D42;
    private static final int ACCENT = 0xFF87CEFA;
    private static final int TEXT = 0xFFE4FAF7;
    private static final int MUTED = 0xFF789B9D;
    private static final int ROW_HEIGHT = 20;
    private final String deviceType;
    private final Consumer<CustomDeviceAssetPackage> onSelected;
    private final Screen parent;
    private List<CustomDeviceAssetPackage> packages = List.of();
    private int selected;
    private int firstVisible;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;

    CustomDeviceAssetPackageScreen(String deviceType, Consumer<CustomDeviceAssetPackage> onSelected, Screen parent) {
        super(Component.translatable("screen.vsie.custom_turret_asset_package"));
        this.deviceType = deviceType;
        this.onSelected = onSelected;
        this.parent = parent;
    }

    @Override
    protected void init() {
        packages = CustomDeviceStorage.scanAssetPackages(deviceType);
        selected = Math.min(selected, Math.max(0, packages.size() - 1));
        panelWidth = Math.min(430, width - 32);
        panelHeight = Math.min(300, height - 28);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xA61C1E22);
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xAA87CEFA);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left, top, left + panelWidth, top + 38, FRAME);
        graphics.fill(left, top + panelHeight - 34, left + panelWidth, top + panelHeight, FRAME);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_asset_package.header"),
                left + 12, top + 14, ACCENT, false);
        graphics.drawString(font, Component.literal(CustomDeviceStorage.rootForDeviceType(deviceType).toString()),
                left + 10, top + 43, MUTED, false);
        drawRows(graphics, mouseX, mouseY);
        drawButton(graphics, left + panelWidth - 174, top + panelHeight - 27, 76,
                Component.translatable("screen.vsie.custom_turret_asset_package.select"), true);
        drawButton(graphics, left + panelWidth - 90, top + panelHeight - 27, 76,
                Component.translatable("gui.cancel"), false);
    }

    private void drawRows(GuiGraphics graphics, int mouseX, int mouseY) {
        int listTop = top + 58;
        int visible = Math.max(1, (panelHeight - 100) / ROW_HEIGHT);
        if (packages.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("screen.vsie.custom_turret_asset_package.empty"),
                    left + 12, listTop + 8, panelWidth - 24, MUTED);
            return;
        }
        for (int row = 0; row < visible; row++) {
            int index = firstVisible + row;
            if (index >= packages.size()) {
                break;
            }
            int y = listTop + row * ROW_HEIGHT;
            boolean active = index == selected;
            boolean hovered = inside(mouseX, mouseY, left + 8, y, panelWidth - 16, ROW_HEIGHT - 2);
            graphics.fill(left + 8, y, left + panelWidth - 8, y + ROW_HEIGHT - 2,
                    active ? 0xCC62686E : hovered ? 0x99565B61 : 0x332E3238);
            graphics.drawString(font, Component.literal("▸ " + packages.get(index).name()),
                    left + 15, y + 5, active ? TEXT : 0xFFAAC3C1, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int listTop = top + 58;
            int row = (int) ((mouseY - listTop) / ROW_HEIGHT);
            int index = firstVisible + row;
            if (mouseX >= left + 8 && mouseX < left + panelWidth - 8 && row >= 0 && index < packages.size()) {
                selected = index;
                return true;
            }
            if (inside(mouseX, mouseY, left + panelWidth - 174, top + panelHeight - 27, 76, 20)) {
                choose();
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
        int visible = Math.max(1, (panelHeight - 100) / ROW_HEIGHT);
        firstVisible = Math.max(0, Math.min(Math.max(0, packages.size() - visible),
                firstVisible + (scrollY < 0 ? 1 : -1)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            choose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void choose() {
        if (!packages.isEmpty()) {
            onSelected.accept(packages.get(selected));
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
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
}
