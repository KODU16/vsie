package com.kodu16.vsie.content.aeroie_custom;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.List;

/** Stores and renders one definition-driven decoration without exposing gameplay behavior. */
public final class CustomDecorationBlockEntity extends SmartBlockEntity
        implements GeoBlockEntity, CustomDeviceBlockEntity {
    private static final String DEFINITION_TAG = "CustomDecorationDefinition";
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private CustomDeviceDefinition definition = CustomDeviceDefinition.createNew("missing_decoration", "decoration");
    private String definitionJson = definition.toJson();

    public CustomDecorationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public CustomDeviceDefinition getDefinition() {
        return definition;
    }

    @Override
    public void setDefinitionJson(String json) {
        CustomDeviceDefinition parsed = CustomDeviceDefinition.fromJson(json);
        if (!"decoration".equals(parsed.deviceType)) {
            throw new IllegalArgumentException("Custom decoration block requires a decoration definition");
        }
        restoreDefinition(parsed);
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            CustomDeviceResourceGuard.removePlacedDeviceIfMissingObj(level, worldPosition, definition);
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString(DEFINITION_TAG, definitionJson);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(DEFINITION_TAG)) {
            try {
                restoreDefinition(CustomDeviceDefinition.fromJson(tag.getString(DEFINITION_TAG)));
            } catch (IllegalArgumentException ignored) {
                // Invalid migrated data keeps the safe empty decoration definition.
            }
        }
    }

    private void restoreDefinition(CustomDeviceDefinition parsed) {
        if (!"decoration".equals(parsed.deviceType)) {
            throw new IllegalArgumentException("Custom decoration block requires a decoration definition");
        }
        definition = parsed;
        definitionJson = parsed.toJson();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
