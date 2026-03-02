package com.kodu16.vsie.content.item.shieldtool;

//import com.kodu16.vsie.network.packet.IFFC2SPacket;
import com.kodu16.vsie.vsie;
import net.minecraft.client.gui.GuiGraphics;
        import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

@SuppressWarnings({"removal"})
public class shieldtoolScreen extends AbstractContainerScreen<ShieldToolContainerMenu> {

    private EditBox editBoxA;
    private EditBox editBoxB;

    public shieldtoolScreen(ShieldToolContainerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
    }

    private void saveAndClose() {
        this.minecraft.player.closeContainer();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = new ResourceLocation(vsie.ID, "textures/gui/iff/iff_gui.png");
        guiGraphics.blit(texture,   // 用实例字段
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题（保持原样）
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        int startX = 8;           // 左边距（可调）
        int startY = 24;          // 第一行开始的 Y 坐标（标题下面一点，可调）
        int lineHeight = 10;      // 每行间隔（字体高度通常9–10）

        // ──────────────────────────────────────────────
        // 准备要显示的几行内容 和 对应的详细 tooltip
        // ──────────────────────────────────────────────
        record LineData(String displayText, Component detailedTooltip) {}

        var lines = new LineData[] {
                new LineData("§0Max shield generator Distance: " + menu.dmax,
                        Component.literal("§3绑定的护盾发生器之间的最大距离")),
                new LineData("§0Min shield generator Distance: " + menu.dmin,
                        Component.literal("§3绑定的护盾发生器之间的最小距离")),

                new LineData("Max shield amount: " + menu.maxShield,
                        Component.literal("§7所有护盾发生器FE容量总和，能量消耗在护盾发生器之间均分")),

                new LineData("Shield radius: " + menu.radius,
                        Component.literal("§7绑定的所有护盾发生器之间的最大距离*0.75")),

                new LineData("Energy cost per intercept: " + menu.costPerProjectile,
                        Component.literal("§7（护盾发生器之间的最大距离^2/护盾发生器之间的最小距离）*护盾发生器数*1000")),

                new LineData("Energy regenerate per tick: " + menu.regenPerTick,
                        Component.literal("§7（护盾发生器之间的最大距离*护盾发生器个数）* 500 FE")),

                new LineData("Overload cooldown time: " + menu.maxCooldown+"ticks",
                        Component.literal("§7(护盾发生器之间的最大距离/护盾发生器之间的最小距离)*100 ticks"))
        };

        // 当前鼠标在 GUI 内的相对坐标
        int relMouseX = mouseX - leftPos;
        int relMouseY = mouseY - topPos;

        Component hoveredTooltip = null;

        for (int i = 0; i < lines.length; i++) {
            LineData line = lines[i];
            int y = startY + i * lineHeight;

            // 绘制带颜色的文字（§b 会生效）
            guiGraphics.drawString(font, line.displayText, startX, y, 0x404040, false);  // true = drop shadow

            // 判断鼠标是否在这个文字行上（宽松一点的判定区域）
            boolean isHovered =
                    relMouseX >= startX - 4 &&
                            relMouseX <= startX + 160 &&     // 假设最长一行不超过这个宽度
                            relMouseY >= y - 2 &&
                            relMouseY <= y + lineHeight + 1;

            if (isHovered) {
                hoveredTooltip = line.detailedTooltip;
            }
        }

        // 如果有悬停的 tooltip，就在最后渲染（最上层）
        if (hoveredTooltip != null) {
            // 可以用 renderTooltip，也可以自己控制位置
            guiGraphics.renderTooltip(font, hoveredTooltip, relMouseX, relMouseY);
            // 或者偏右上一点：guiGraphics.renderTooltip(font, hoveredTooltip, relMouseX + 12, relMouseY - 12);
        }
    }

}
