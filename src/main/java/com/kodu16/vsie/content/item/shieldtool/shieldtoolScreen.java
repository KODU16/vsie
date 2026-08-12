package com.kodu16.vsie.content.item.shieldtool;

import com.kodu16.vsie.foundation.client.GuiTooltipHelper;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

@SuppressWarnings({"removal"})
public class shieldtoolScreen extends AbstractContainerScreen<ShieldToolContainerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(vsie.ID, "textures/gui/iff/iff_gui.png");

    public shieldtoolScreen(ShieldToolContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Function: the container parent owns the single background render pass.
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderLineTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        int startX = 8;
        int startY = 24;
        int lineHeight = 12;
        int color = 0x202020;

        for (int i = 0; i < createLines().size(); i++) {
            ShieldLine line = createLines().get(i);
            guiGraphics.drawString(this.font, line.display(), startX, startY + i * lineHeight, color, false);
        }
    }

    private void renderLineTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startX = this.leftPos + 8;
        int startY = this.topPos + 24;
        int lineHeight = 12;

        List<ShieldLine> lines = createLines();
        for (int i = 0; i < lines.size(); i++) {
            ShieldLine line = lines.get(i);
            int y = startY + i * lineHeight;
            int width = Math.min(this.imageWidth - 16, this.font.width(line.display()) + 6);
            if (!GuiTooltipHelper.isMouseWithin(mouseX, mouseY, startX - 2, y - 1, width, lineHeight)) {
                continue;
            }
            guiGraphics.renderComponentTooltip(this.font, line.tooltip(), mouseX, mouseY);
            return;
        }
    }

    private List<ShieldLine> createLines() {
        return List.of(
                createLine(
                        Component.translatable("gui.vsie.shield_tool.max_distance.label", formatDistance(menu.getMaxDistance())),
                        Component.translatable("gui.vsie.shield_tool.max_distance.tooltip")
                ),
                createLine(
                        Component.translatable("gui.vsie.shield_tool.min_distance.label", formatDistance(menu.getMinDistance())),
                        Component.translatable("gui.vsie.shield_tool.min_distance.tooltip")
                ),
                createLine(
                        Component.translatable("gui.vsie.shield_tool.max_shield.label", menu.getMaxShield()),
                        Component.translatable("gui.vsie.shield_tool.max_shield.tooltip")
                ),
                createLine(
                        Component.translatable("gui.vsie.shield_tool.radius.label", menu.getRadius()),
                        Component.translatable("gui.vsie.shield_tool.radius.tooltip")
                ),
                createLine(
                        Component.translatable("gui.vsie.shield_tool.cost.label", menu.getCostPerProjectile()),
                        Component.translatable("gui.vsie.shield_tool.cost.tooltip")
                ),
                createLine(
                        Component.translatable("gui.vsie.shield_tool.regen.label", menu.getRegenPerTick()),
                        Component.translatable("gui.vsie.shield_tool.regen.tooltip")
                ),
                createLine(
                        Component.translatable("gui.vsie.shield_tool.cooldown.label", menu.getMaxCooldown()),
                        Component.translatable("gui.vsie.shield_tool.cooldown.tooltip")
                )
        );
    }

    private ShieldLine createLine(Component display, Component formulaTooltip) {
        // 把当前显示值和计算说明一起放进 tooltip，便于直接核对公式与结果。
        return new ShieldLine(
                display,
                List.of(
                        display,
                        formulaTooltip
                )
        );
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.ROOT, "%.2f", distance);
    }

    private record ShieldLine(Component display, List<Component> tooltip) {
    }
}
