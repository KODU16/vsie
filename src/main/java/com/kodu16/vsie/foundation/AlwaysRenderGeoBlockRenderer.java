package com.kodu16.vsie.foundation;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class AlwaysRenderGeoBlockRenderer<T extends BlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public AlwaysRenderGeoBlockRenderer(GeoModel<T> model) {
        super(model);
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
