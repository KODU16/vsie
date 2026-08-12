package com.kodu16.vsie.foundation.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Base for code-rendered AeroIE screens that must never pass their GUI pixels through the world blur shader. */
public abstract class CrispScreen extends Screen {
    protected CrispScreen(Component title) {
        super(title);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Function: retain a readable world overlay without invoking Screen.processBlurEffect on the main target.
        renderTransparentBackground(graphics);
    }
}
