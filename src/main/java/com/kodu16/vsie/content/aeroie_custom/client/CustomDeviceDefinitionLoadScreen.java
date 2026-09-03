package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomDeviceResourceGuard;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceDefinition;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceStorage;
import com.kodu16.vsie.foundation.client.CrispScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/** Folder picker for loading one custom device definition from its own save directory. */
final class CustomDeviceDefinitionLoadScreen extends CrispScreen {
    private static final int FRAME = 0xFF2E3238;
    private static final int PANEL = 0xFF3A3D42;
    private static final int ACCENT = 0xFF87CEFA;
    private static final int TEXT = 0xFFE4FAF7;
    private static final int MUTED = 0xFF789B9D;
    private static final int ROW_HEIGHT = 18;
    private final Screen parent;
    private final Consumer<CustomDeviceDefinition> onLoad;
    private List<String> directories = List.of("");
    private int selected;
    private int firstVisible;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private Component status = Component.empty();

    CustomDeviceDefinitionLoadScreen(Screen parent, Consumer<CustomDeviceDefinition> onLoad) {
        super(Component.translatable("screen.vsie.custom_turret_load"));
        this.parent = parent;
        this.onLoad = onLoad;
    }

    @Override
    protected void init() {
        directories = CustomDeviceStorage.scanDirectories();
        panelWidth = Math.min(440, width - 32);
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
        graphics.fill(left, top + panelHeight - 38, left + panelWidth, top + panelHeight, FRAME);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_load.header"),
                left + 12, top + 14, ACCENT, false);
        graphics.drawString(font, Component.literal(CustomDeviceStorage.root().toString()),
                left + 10, top + 43, MUTED, false);
        drawDirectories(graphics, mouseX, mouseY);
        if (!status.getString().isBlank()) {
            graphics.drawCenteredString(font, status, left + panelWidth / 2, top + panelHeight / 2, 0xFFFF7C7C);
        }
        graphics.drawString(font, status, left + 10, top + panelHeight - 25, 0xFFFF7C7C, false);
        drawButton(graphics, left + panelWidth - 174, top + panelHeight - 27, 76,
                Component.translatable("screen.vsie.custom_turret_load.load"), true);
        drawButton(graphics, left + panelWidth - 90, top + panelHeight - 27, 76,
                Component.translatable("gui.cancel"), false);
    }

    private void drawDirectories(GuiGraphics graphics, int mouseX, int mouseY) {
        int listTop = top + 58;
        int visible = Math.max(1, (panelHeight - 104) / ROW_HEIGHT);
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
            graphics.drawString(font, Component.literal("> " + label), left + 14, y + 4,
                    active ? TEXT : 0xFFAAC3C1, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int listTop = top + 58;
            int row = (int) ((mouseY - listTop) / ROW_HEIGHT);
            int index = firstVisible + row;
            if (mouseX >= left + 8 && mouseX < left + panelWidth - 8 && row >= 0 && index < directories.size()) {
                selected = index;
                return true;
            }
            if (inside(mouseX, mouseY, left + panelWidth - 174, top + panelHeight - 27, 76, 20)) {
                load();
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
        int visible = Math.max(1, (panelHeight - 104) / ROW_HEIGHT);
        firstVisible = Math.max(0, Math.min(Math.max(0, directories.size() - visible),
                firstVisible + (scrollY < 0 ? 1 : -1)));
        return true;
    }

    private void load() {
        List<CustomDeviceStorage.DefinitionFile> files = CustomDeviceStorage.loadDefinitionFiles(directories.get(selected));
        if (files.isEmpty()) {
            status = Component.translatable("screen.vsie.custom_turret_load.empty_folder");
            return;
        }
        CustomDeviceDefinition definition = files.get(0).definition().copy();
        var missingObj = CustomDeviceResourceGuard.firstMissingObj(definition);
        if (missingObj.isPresent()) {
            status = Component.translatable("screen.vsie.custom_turret_load.missing_obj", missingObj.get());
            return;
        }
        onLoad.accept(definition);
        minecraft.setScreen(parent);
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
