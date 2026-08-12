package com.kodu16.vsie.content.misc.enemy_cannon;

import com.kodu16.vsie.network.misc.EnemyCannonSettingsC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class EnemyCannonScreen extends AbstractContainerScreen<EnemyCannonContainerMenu> {
    private EditBox chargeInput;
    private EditBox cooldownInput;
    private @Nullable Component validationMessage;

    public EnemyCannonScreen(EnemyCannonContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 180;
        inventoryLabelY = 87;
    }

    @Override
    protected void init() {
        super.init();
        chargeInput = addIntegerInput(108, 27, menu.getChargeCount(), 3);
        cooldownInput = addIntegerInput(108, 52, menu.getCooldownTicks(), 5);
        addRenderableWidget(Button.builder(Component.translatable("gui.vsie.common.save"), button -> saveAndClose())
                .bounds(leftPos + 108, topPos + 73, 50, 20)
                .build());
    }

    private EditBox addIntegerInput(int x, int y, int value, int maxLength) {
        EditBox input = new EditBox(font, leftPos + x, topPos + y, 50, 15, Component.empty());
        input.setFilter(text -> text.isEmpty() || text.chars().allMatch(Character::isDigit));
        input.setMaxLength(maxLength);
        input.setValue(Integer.toString(value));
        addRenderableWidget(input);
        return input;
    }

    private void saveAndClose() {
        try {
            int charge = Integer.parseInt(chargeInput.getValue());
            int cooldown = Integer.parseInt(cooldownInput.getValue());
            if (charge < EnemyCannonBlockEntity.MIN_CHARGE_COUNT
                    || charge > EnemyCannonBlockEntity.MAX_CHARGE_COUNT
                    || cooldown < EnemyCannonBlockEntity.MIN_COOLDOWN_TICKS
                    || cooldown > EnemyCannonBlockEntity.MAX_COOLDOWN_TICKS) {
                throw new NumberFormatException();
            }
            validationMessage = null;
            // Function: item movement remains vanilla menu traffic while only numeric settings use this packet.
            ModNetworking.sendToServer(new EnemyCannonSettingsC2SPacket(menu.getBlockPosition(), charge, cooldown));
            minecraft.player.closeContainer();
        } catch (NumberFormatException ignored) {
            validationMessage = Component.translatable("gui.vsie.enemy_cannon.invalid_values");
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
                drawSlot(graphics, 8 + column * 18, 98 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, 8 + column * 18, 156);
        }
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF373737);
        graphics.fill(leftPos + x, topPos + y, leftPos + x + 16, topPos + y + 16, 0xFF8B8B8B);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_cannon.projectile.label"), 8, 27, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_cannon.charge.label"), 57, 30, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.vsie.enemy_cannon.cooldown.label"), 57, 55, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
        if (validationMessage != null) {
            graphics.drawString(font, validationMessage, 8, 78, 0xCC3333, false);
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
}
