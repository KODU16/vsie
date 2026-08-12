package com.kodu16.vsie.content.misc.enemy_core;

import com.kodu16.vsie.network.misc.EnemyCoreSettingsC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@SuppressWarnings("removal")
public class EnemyCoreScreen extends AbstractContainerScreen<EnemyCoreContainerMenu> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/iff/iff_gui.png");

    private EditBox enemyInput;
    private EditBox allyInput;
    private EditBox radiusInput;
    private EditBox speedInput;
    private @Nullable Component validationMessage;

    public EnemyCoreScreen(EnemyCoreContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        enemyInput = addTextInput(76, 23, Component.translatable("gui.vsie.enemy_core.enemy.tooltip"));
        allyInput = addTextInput(76, 48, Component.translatable("gui.vsie.enemy_core.ally.tooltip"));
        radiusInput = addTextInput(76, 73, Component.translatable("gui.vsie.enemy_core.radius.tooltip"));
        speedInput = addTextInput(76, 98, Component.translatable("gui.vsie.enemy_core.speed.tooltip"));

        enemyInput.setMaxLength(EnemyCoreBlockEntity.MAX_PATTERN_LENGTH);
        allyInput.setMaxLength(EnemyCoreBlockEntity.MAX_PATTERN_LENGTH);
        radiusInput.setMaxLength(12);
        speedInput.setMaxLength(12);
        radiusInput.setFilter(EnemyCoreScreen::isUnsignedDecimal);
        speedInput.setFilter(EnemyCoreScreen::isUnsignedDecimal);

        enemyInput.setValue(menu.getEnemyPattern());
        allyInput.setValue(menu.getAllyPattern());
        radiusInput.setValue(formatNumber(menu.getOrbitRadius()));
        speedInput.setValue(formatNumber(menu.getOrbitSpeed()));

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.vsie.common.save"),
                        button -> saveAndClose())
                .bounds(leftPos + 32, topPos + 128, 40, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.vsie.common.cancel"),
                        button -> minecraft.player.closeContainer())
                .bounds(leftPos + 104, topPos + 128, 40, 20)
                .build());
    }

    private EditBox addTextInput(int x, int y, Component hint) {
        EditBox input = new EditBox(font, leftPos + x, topPos + y, 72, 15, hint);
        addRenderableWidget(input);
        return input;
    }

    private void saveAndClose() {
        try {
            double radius = Double.parseDouble(radiusInput.getValue());
            double speed = Double.parseDouble(speedInput.getValue());
            if (!Double.isFinite(radius) || !Double.isFinite(speed)
                    || radius < EnemyCoreBlockEntity.MIN_ORBIT_RADIUS
                    || radius > EnemyCoreBlockEntity.MAX_ORBIT_RADIUS
                    || speed < EnemyCoreBlockEntity.MIN_ORBIT_SPEED
                    || speed > EnemyCoreBlockEntity.MAX_ORBIT_SPEED) {
                throw new NumberFormatException();
            }

            validationMessage = null;
            ModNetworking.sendToServer(new EnemyCoreSettingsC2SPacket(
                    menu.getBlockPosition(),
                    enemyInput.getValue(),
                    allyInput.getValue(),
                    radius,
                    speed
            ));
            minecraft.player.closeContainer();
        } catch (NumberFormatException ignored) {
            // Function: retain the open screen so invalid numeric settings are never silently submitted.
            validationMessage = Component.translatable("gui.vsie.enemy_core.invalid_values");
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_core.enemy.label"), 18, 26, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_core.ally.label"), 18, 51, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_core.radius.label"), 18, 76, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_core.speed.label"), 18, 101, 0x404040, false);
        if (validationMessage != null) {
            graphics.drawString(font, validationMessage, 18, 116, 0xCC3333, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Function: AbstractContainerScreen supplies the only required background pass.
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderInputTooltip(graphics, mouseX, mouseY);
    }

    private void renderInputTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (InputTooltip tooltip : List.of(
                new InputTooltip(enemyInput, "gui.vsie.enemy_core.enemy.tooltip"),
                new InputTooltip(allyInput, "gui.vsie.enemy_core.ally.tooltip"),
                new InputTooltip(radiusInput, "gui.vsie.enemy_core.radius.tooltip"),
                new InputTooltip(speedInput, "gui.vsie.enemy_core.speed.tooltip")
        )) {
            EditBox input = tooltip.input;
            if (mouseX >= input.getX() && mouseX < input.getX() + input.getWidth()
                    && mouseY >= input.getY() && mouseY < input.getY() + input.getHeight()) {
                graphics.renderTooltip(font, Component.translatable(tooltip.translationKey), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            minecraft.player.closeContainer();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveAndClose();
            return true;
        }
        if (isEditingText()) {
            focusedInput().keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (isEditingText()) {
            focusedInput().charTyped(codePoint, modifiers);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private boolean isEditingText() {
        return enemyInput.isFocused() || allyInput.isFocused() || radiusInput.isFocused() || speedInput.isFocused();
    }

    private EditBox focusedInput() {
        if (enemyInput.isFocused()) {
            return enemyInput;
        }
        if (allyInput.isFocused()) {
            return allyInput;
        }
        if (radiusInput.isFocused()) {
            return radiusInput;
        }
        return speedInput;
    }

    private static boolean isUnsignedDecimal(String value) {
        return value.isEmpty() || value.matches("\\d+(\\.\\d*)?");
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private record InputTooltip(EditBox input, String translationKey) {
    }
}
