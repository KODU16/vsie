package com.kodu16.vsie.content.aeroie_custom.client;

import com.kodu16.vsie.content.aeroie_custom.CustomTurretAnimationResources;
import com.kodu16.vsie.content.aeroie_custom.CustomDeviceStorage;
import com.kodu16.vsie.foundation.client.CrispScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

/** Simple folder-rooted picker for GeckoLib .animation.json files under AeroIE_custom/turret. */
final class CustomTurretAnimationSelectScreen extends CrispScreen {
    private static final int FRAME = 0xFF2E3238;
    private static final int PANEL = 0xF2494D52;
    private static final int ACCENT = 0xFF87CEFA;
    private static final int TEXT = 0xFFE4FAF7;
    private static final int MUTED = 0xFF789B9D;

    private final Consumer<String> onSelect;
    private final Runnable onCancel;
    private List<String> files = List.of();
    private int selected;
    private int firstVisible;

    CustomTurretAnimationSelectScreen(Consumer<String> onSelect, Runnable onCancel) {
        super(Component.translatable("screen.vsie.custom_turret_editor.fire_animation_select"));
        this.onSelect = onSelect;
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        files = CustomTurretAnimationResources.scanAnimationFiles();
        selected = Math.min(selected, Math.max(0, files.size() - 1));
        firstVisible = Math.min(firstVisible, Math.max(0, files.size() - 1));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int width = Math.min(460, this.width - 30);
        int height = Math.min(260, this.height - 30);
        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;
        graphics.fill(left - 2, top - 2, left + width + 2, top + height + 2, 0xAA87CEFA);
        graphics.fill(left, top, left + width, top + height, PANEL);
        graphics.fill(left, top, left + width, top + 24, FRAME);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_editor.fire_animation_select"),
                left + 8, top + 8, ACCENT, false);
        graphics.drawString(font, Component.literal(CustomDeviceStorage.turretRoot().toString()),
                left + 8, top + height - 18, MUTED, false);
        if (files.isEmpty()) {
            graphics.drawWordWrap(font, Component.translatable("screen.vsie.custom_turret_editor.fire_animation_empty"),
                    left + 10, top + 42, width - 20, MUTED);
        } else {
            int rowY = top + 32;
            int visible = Math.min(10, files.size() - firstVisible);
            for (int row = 0; row < visible; row++) {
                int index = firstVisible + row;
                boolean active = index == selected;
                graphics.fill(left + 8, rowY, left + width - 8, rowY + 16,
                        active ? 0xCC62686E : mouseY >= rowY && mouseY < rowY + 16 ? 0x99565B61 : 0);
                graphics.drawString(font, files.get(index), left + 12, rowY + 4,
                        active ? TEXT : 0xFFAAC3C1, false);
                rowY += 17;
            }
        }
        drawButton(graphics, left + width - 168, top + height - 44, 76, 20,
                Component.translatable("screen.vsie.custom_turret_editor.fire_animation_clear"), false);
        drawButton(graphics, left + width - 86, top + height - 44, 76, 20,
                Component.translatable("screen.vsie.custom_turret_editor.fire_animation_bind"), !files.isEmpty());
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height,
                            Component label, boolean active) {
        graphics.fill(x, y, x + width, y + height, active ? 0xFF4F86A0 : 0xFF565B61);
        graphics.fill(x, y + height - 2, x + width, y + height, ACCENT);
        graphics.drawCenteredString(font, label, x + width / 2, y + 6, TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int width = Math.min(460, this.width - 30);
        int height = Math.min(260, this.height - 30);
        int left = (this.width - width) / 2;
        int top = (this.height - height) / 2;
        if (inside(mouseX, mouseY, left + width - 168, top + height - 44, 76, 20)) {
            onSelect.accept("");
            return true;
        }
        if (!files.isEmpty() && inside(mouseX, mouseY, left + width - 86, top + height - 44, 76, 20)) {
            onSelect.accept(files.get(selected));
            return true;
        }
        int row = (int) ((mouseY - (top + 32)) / 17);
        int index = firstVisible + row;
        if (mouseX >= left + 8 && mouseX < left + width - 8 && row >= 0 && index < files.size()) {
            selected = index;
            return true;
        }
        if (!inside(mouseX, mouseY, left, top, width, height)) {
            onCancel.run();
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!files.isEmpty()) {
            firstVisible = Math.max(0, Math.min(Math.max(0, files.size() - 10),
                    firstVisible + (scrollY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onCancel.run();
            return true;
        }
        if (!files.isEmpty() && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            onSelect.accept(files.get(selected));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
