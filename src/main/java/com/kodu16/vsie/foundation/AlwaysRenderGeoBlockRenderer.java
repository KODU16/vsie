package com.kodu16.vsie.foundation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AlwaysRenderGeoBlockRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    private static final double GECKO_RENDER_BOUNDS_RADIUS = 128.0D;
    private static final int LIGHT_SAMPLE_HORIZONTAL_RADIUS = 2;
    private static final int LIGHT_SAMPLE_DOWN = 1;
    private static final int LIGHT_SAMPLE_UP = 4;

    public AlwaysRenderGeoBlockRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        // Function: oversized Gecko models should use nearby visible light, not only the origin block's light.
        super.render(blockEntity, partialTick, poseStack, bufferSource, sampledModelLight(blockEntity, packedLight), packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        // Function: Sable/NeoForge frustum checks use this box before shouldRender(), so cover oversized Gecko models.
        return new AABB(blockEntity.getBlockPos()).inflate(GECKO_RENDER_BOUNDS_RADIUS);
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        // Function: large Gecko models should not disappear when their origin block leaves the camera frustum.
        return true;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        // Function: large Gecko block models must not disappear when only their origin block leaves the frustum.
        return true;
    }

    private static int sampledModelLight(BlockEntity blockEntity, int fallbackLight) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return fallbackLight;
        }

        int maxBlockLight = LightTexture.block(fallbackLight);
        int maxSkyLight = LightTexture.sky(fallbackLight);
        BlockPos origin = blockEntity.getBlockPos();
        BlockPos.MutableBlockPos samplePos = new BlockPos.MutableBlockPos();

        for (int x = -LIGHT_SAMPLE_HORIZONTAL_RADIUS; x <= LIGHT_SAMPLE_HORIZONTAL_RADIUS; x++) {
            for (int y = -LIGHT_SAMPLE_DOWN; y <= LIGHT_SAMPLE_UP; y++) {
                for (int z = -LIGHT_SAMPLE_HORIZONTAL_RADIUS; z <= LIGHT_SAMPLE_HORIZONTAL_RADIUS; z++) {
                    samplePos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.isLoaded(samplePos)) {
                        continue;
                    }
                    int sampledLight = LevelRenderer.getLightColor(level, samplePos);
                    maxBlockLight = Math.max(maxBlockLight, LightTexture.block(sampledLight));
                    maxSkyLight = Math.max(maxSkyLight, LightTexture.sky(sampledLight));
                }
            }
        }

        return LightTexture.pack(maxBlockLight, maxSkyLight);
    }
}
