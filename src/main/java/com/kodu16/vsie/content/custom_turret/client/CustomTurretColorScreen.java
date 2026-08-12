package com.kodu16.vsie.content.custom_turret.client;

import com.kodu16.vsie.foundation.client.CrispScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.function.IntConsumer;

/** Compact HSV selector used by custom energy-turret laser definitions. */
public final class CustomTurretColorScreen extends CrispScreen {
    private static final int PICKER_SIZE = 160;
    private static final int HUE_WIDTH = 18;
    private final IntConsumer onSelected;
    private final Screen parent;
    private float hue;
    private float saturation;
    private float brightness;
    private boolean draggingSquare;
    private boolean draggingHue;

    public CustomTurretColorScreen(int argb, IntConsumer onSelected, Screen parent) {
        super(Component.translatable("screen.vsie.custom_turret_color"));
        this.onSelected = onSelected;
        this.parent = parent;
        float[] hsv = Color.RGBtoHSB(argb >> 16 & 255, argb >> 8 & 255, argb & 255, null);
        hue = hsv[0];
        saturation = hsv[1];
        brightness = hsv[2];
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (width - PICKER_SIZE - HUE_WIDTH - 18) / 2;
        int top = (height - PICKER_SIZE) / 2;
        graphics.fill(left - 12, top - 34, left + PICKER_SIZE + HUE_WIDTH + 30,
                top + PICKER_SIZE + 38, 0xF23A3D42);
        graphics.drawString(font, title, left, top - 24, 0xFF87CEFA, false);
        drawSaturationBrightness(graphics, left, top);
        drawHue(graphics, left + PICKER_SIZE + 10, top);
        int color = selectedColor();
        graphics.fill(left, top + PICKER_SIZE + 10, left + 72, top + PICKER_SIZE + 28, color);
        graphics.drawString(font, Component.literal(String.format("#%06X", color & 0xFFFFFF)),
                left + 80, top + PICKER_SIZE + 15, 0xFFE4FAF7, false);
        graphics.drawString(font, Component.translatable("screen.vsie.custom_turret_color.confirm"),
                left + PICKER_SIZE - 34, top + PICKER_SIZE + 15, 0xFF87CEFA, false);
    }

    private void drawSaturationBrightness(GuiGraphics graphics, int left, int top) {
        for (int y = 0; y < PICKER_SIZE; y++) {
            float value = 1.0F - y / (float) (PICKER_SIZE - 1);
            for (int x = 0; x < PICKER_SIZE; x++) {
                float sat = x / (float) (PICKER_SIZE - 1);
                graphics.fill(left + x, top + y, left + x + 1, top + y + 1,
                        0xFF000000 | Color.HSBtoRGB(hue, sat, value));
            }
        }
        int markerX = left + Math.round(saturation * (PICKER_SIZE - 1));
        int markerY = top + Math.round((1.0F - brightness) * (PICKER_SIZE - 1));
        graphics.renderOutline(markerX - 3, markerY - 3, 7, 7, 0xFFFFFFFF);
    }

    private void drawHue(GuiGraphics graphics, int left, int top) {
        for (int y = 0; y < PICKER_SIZE; y++) {
            float rowHue = y / (float) (PICKER_SIZE - 1);
            graphics.fill(left, top + y, left + HUE_WIDTH, top + y + 1,
                    0xFF000000 | Color.HSBtoRGB(rowHue, 1.0F, 1.0F));
        }
        int markerY = top + Math.round(hue * (PICKER_SIZE - 1));
        graphics.renderOutline(left - 2, markerY - 2, HUE_WIDTH + 4, 5, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int left = (width - PICKER_SIZE - HUE_WIDTH - 18) / 2;
        int top = (height - PICKER_SIZE) / 2;
        draggingSquare = inside(mouseX, mouseY, left, top, PICKER_SIZE, PICKER_SIZE);
        draggingHue = inside(mouseX, mouseY, left + PICKER_SIZE + 10, top, HUE_WIDTH, PICKER_SIZE);
        if (draggingSquare || draggingHue) {
            updateSelection(mouseX, mouseY, left, top);
            return true;
        }
        if (inside(mouseX, mouseY, left + PICKER_SIZE - 40, top + PICKER_SIZE + 8, 74, 24)) {
            onSelected.accept(selectedColor());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && (draggingSquare || draggingHue)) {
            int left = (width - PICKER_SIZE - HUE_WIDTH - 18) / 2;
            int top = (height - PICKER_SIZE) / 2;
            updateSelection(mouseX, mouseY, left, top);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSquare = false;
        draggingHue = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateSelection(double mouseX, double mouseY, int left, int top) {
        if (draggingSquare) {
            saturation = clamp((float) (mouseX - left) / (PICKER_SIZE - 1));
            brightness = 1.0F - clamp((float) (mouseY - top) / (PICKER_SIZE - 1));
        } else if (draggingHue) {
            hue = clamp((float) (mouseY - top) / (PICKER_SIZE - 1));
        }
    }

    private int selectedColor() {
        return 0xFF000000 | Color.HSBtoRGB(hue, saturation, brightness);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
