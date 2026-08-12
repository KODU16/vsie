package com.kodu16.vsie.content.misc.enemy_autocannon;

import com.kodu16.vsie.network.misc.EnemyAutocannonSettingsC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class EnemyAutocannonScreen extends AbstractContainerScreen<EnemyAutocannonContainerMenu> {
    private EditBox spreadInput;
    private EditBox intervalInput;
    private EditBox burstInput;
    private @Nullable Component validationMessage;

    public EnemyAutocannonScreen(EnemyAutocannonContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 188;
        inventoryLabelY = 95;
    }

    @Override
    protected void init() {
        super.init();
        spreadInput = addNumberInput(108, 20, formatNumber(menu.getSpreadAngle()), 7, true);
        intervalInput = addNumberInput(108, 42, Integer.toString(menu.getShotIntervalTicks()), 4, false);
        burstInput = addNumberInput(108, 64, Integer.toString(menu.getBurstShots()), 4, false);
        addRenderableWidget(Button.builder(Component.translatable("gui.vsie.common.save"), button -> saveAndClose())
                .bounds(leftPos + 18, topPos + 76, 50, 20)
                .build());
    }

    private EditBox addNumberInput(int x, int y, String value, int maxLength, boolean decimal) {
        EditBox input = new EditBox(font, leftPos + x, topPos + y, 50, 15, Component.empty());
        input.setFilter(text -> text.isEmpty() || (decimal ? text.matches("\\d+(\\.\\d*)?") : text.chars().allMatch(Character::isDigit)));
        input.setMaxLength(maxLength);
        input.setValue(value);
        addRenderableWidget(input);
        return input;
    }

    private void saveAndClose() {
        try {
            double spread = Double.parseDouble(spreadInput.getValue());
            int interval = Integer.parseInt(intervalInput.getValue());
            int shots = Integer.parseInt(burstInput.getValue());
            if (!Double.isFinite(spread)
                    || spread < EnemyAutocannonBlockEntity.MIN_SPREAD_ANGLE
                    || spread > EnemyAutocannonBlockEntity.MAX_SPREAD_ANGLE
                    || interval < EnemyAutocannonBlockEntity.MIN_SHOT_INTERVAL_TICKS
                    || interval > EnemyAutocannonBlockEntity.MAX_SHOT_INTERVAL_TICKS
                    || shots < EnemyAutocannonBlockEntity.MIN_BURST_SHOTS
                    || shots > EnemyAutocannonBlockEntity.MAX_BURST_SHOTS) {
                throw new NumberFormatException();
            }
            validationMessage = null;
            ModNetworking.sendToServer(new EnemyAutocannonSettingsC2SPacket(
                    menu.getBlockPosition(), spread, interval, shots
            ));
            minecraft.player.closeContainer();
        } catch (NumberFormatException ignored) {
            validationMessage = Component.translatable("gui.vsie.enemy_autocannon.invalid_values");
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + imageHeight - 3, 0xFF8B8B8B);
        graphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFFC6C6C6);
        drawSlot(graphics, 24, 38);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, 8 + column * 18, 106 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, 8 + column * 18, 164);
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_autocannon.projectile.label"), 8, 27, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_autocannon.spread.label"), 58, 23, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_autocannon.interval.label"), 58, 45, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_autocannon.burst.label"), 58, 67, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        if (validationMessage != null) {
            graphics.drawString(font, validationMessage, 71, 82, 0xCC3333, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Function: AbstractContainerScreen supplies the only required background pass.
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            saveAndClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
