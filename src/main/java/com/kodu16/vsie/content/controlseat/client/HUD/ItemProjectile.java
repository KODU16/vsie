package com.kodu16.vsie.content.controlseat.client.HUD;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector4f;
import com.mojang.datafixers.util.Pair;
import org.valkyrienskies.core.impl.shadow.lp;

import javax.annotation.Nullable;
import java.util.Map;

public class ItemProjectile {
    public static void drawforeach(Vec3 seatpos, Map<String, Object> shipsData, Vec3 seatdirection, GuiGraphics gg, int argb) {
        for (Map.Entry<String, Object> entry : shipsData.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> attr = (Map<String, Object>) entry.getValue();

            long id = ((Number) attr.get("id")).longValue();
            String slug = (String) attr.get("slug");
            String dim = (String) attr.get("dimension");
            double x = (double) attr.get("x");
            double y = (double) attr.get("y");
            double z = (double) attr.get("z");
            // 渲染用
            String displayName = slug != null && !slug.isBlank() ? slug : "Ship " + id;
            Vec3 pos = new Vec3(x, y, z);
        }
    }



}
