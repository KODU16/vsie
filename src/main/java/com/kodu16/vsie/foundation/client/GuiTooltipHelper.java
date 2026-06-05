package com.kodu16.vsie.foundation.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class GuiTooltipHelper {
    private GuiTooltipHelper() {
    }

    public static boolean isMouseWithin(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static boolean renderTooltipIfHovered(GuiGraphics guiGraphics, Font font, int mouseX, int mouseY,
                                                 int x, int y, int width, int height, Component tooltip) {
        if (!isMouseWithin(mouseX, mouseY, x, y, width, height)) {
            return false;
        }
        // Function: centralize simple rectangular hover tooltips so every GUI uses the same hit-test rules.
        guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
        return true;
    }
}
