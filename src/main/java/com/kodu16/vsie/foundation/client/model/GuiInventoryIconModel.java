package com.kodu16.vsie.foundation.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public class GuiInventoryIconModel implements BakedModel {
    private final BakedModel heldModel;
    private final BakedModel guiModel;
    private final BakedModel heldContainerModel;

    public GuiInventoryIconModel(BakedModel heldModel, BakedModel guiModel, BakedModel heldContainerModel) {
        this.heldModel = heldModel;
        this.guiModel = guiModel;
        this.heldContainerModel = heldContainerModel;
    }

    // Function: split item rendering by context without changing the item or block model resources.
    @Override
    public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
        if (transformType == ItemDisplayContext.GUI) {
            return guiModel.applyTransform(transformType, poseStack, applyLeftHandTransform);
        }
        if (isHandContext(transformType)) {
            heldModel.applyTransform(transformType, poseStack, applyLeftHandTransform);
            return heldContainerModel;
        }
        return heldModel.applyTransform(transformType, poseStack, applyLeftHandTransform);
    }

    private static boolean isHandContext(ItemDisplayContext transformType) {
        return transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return heldModel.getQuads(state, direction, random);
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData data,
            @Nullable RenderType renderType) {
        return heldModel.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return heldModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return heldModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        // Function: GUI asks this before applyTransform, so report flat lighting for the replacement icon.
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return heldModel.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return heldModel.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return heldModel.getParticleIcon(data);
    }

    @Override
    public ItemTransforms getTransforms() {
        return heldModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return heldModel.getOverrides();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return heldModel.getRenderTypes(state, rand, data);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        return heldModel.getRenderTypes(itemStack, fabulous);
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack itemStack, boolean fabulous) {
        return heldModel.getRenderPasses(itemStack, fabulous);
    }
}
