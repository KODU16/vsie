package com.kodu16.vsie.content.screen.client.functions;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

public class ServerInfo {

    // 功能：在屏幕 layer 上按行渲染服务器信息文本（信息类型-数值）。
    public static void renderServerInfo(PoseStack poseStack,
                                        AbstractScreenBlockEntity screen,
                                        MultiBufferSource bufferSource,
                                        Font font) {
        // 功能：固定信息文案，满足“每行一个信息”的显示要求。
        String[] lines = new String[]{
                "TPS-" + screen.tps,
                "PhysTPS-" + screen.phystps,
                "内存使用-" + Math.round(screen.serverJVMpercentage * 100) + "%"
        };

        // 功能：将文本放到屏幕上端起始位置，并逐行向下偏移。
        float startX = -0.48f;
        float startY = -0.45f;
        float lineHeight = 0.12f;

        poseStack.pushPose();
        // 功能：缩放字体到屏幕模型坐标系，避免文字超出屏幕范围。
        poseStack.scale(0.01f, -0.01f, 0.01f);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            float drawX = startX * 100.0f;
            float drawY = (startY + i * lineHeight) * 100.0f;
            // 功能：绘制白色文本到当前屏幕 layer。
            font.drawInBatch(line, drawX, drawY, 0xFFFFFFFF, false,
                    poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL,
                    0, 0x00F000F0);
        }

        poseStack.popPose();
    }
}
