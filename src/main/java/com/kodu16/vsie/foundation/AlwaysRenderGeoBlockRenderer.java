package com.kodu16.vsie.foundation;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AlwaysRenderGeoBlockRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    private static final double GECKO_RENDER_BOUNDS_RADIUS = 128.0D;

    public AlwaysRenderGeoBlockRenderer(GeoModel<T> model) {
        super(model);
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        // Function: Sable/NeoForge frustum checks use this box before shouldRender(), so cover oversized Gecko models.
        return new AABB(blockEntity.getBlockPos()).inflate(GECKO_RENDER_BOUNDS_RADIUS);
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 cameraPos) {
        // Function: large Gecko block models must not disappear when only their origin block leaves the frustum.
        return true;
    }
}
