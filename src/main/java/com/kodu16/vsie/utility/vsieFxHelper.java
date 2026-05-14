package com.kodu16.vsie.utility;

import com.kodu16.vsie.network.fx.FxBlockS2CPacket;
import com.kodu16.vsie.network.fx.FxEntityS2CPacket;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.lowdragmc.photon.client.fx.BlockEffectExecutor;
import com.lowdragmc.photon.client.fx.EntityEffectExecutor;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import com.lowdragmc.photon.client.gameobject.IFXObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Function;

public final class vsieFxHelper {
    // Extracts FX units while supporting both flat data and nested "fx" data.
    public static Optional<FxData.FxUnit> extractFxUnit(@Nullable FxData fxData, Function<FxData, FxData.FxUnit> mapper) {
        return Optional.ofNullable(fxData).map(data -> data.resolveUnit(mapper));
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientTriggerEntityFx(FxEntityS2CPacket triggerPacket) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Entity entity = level.getEntity(triggerPacket.getEntityID());
            if (entity != null) {
                FX fx = FXHelper.getFX(triggerPacket.getFx());
                if (fx != null) {
                    var effect = new EntityEffectExecutor(fx, level, entity, EntityEffectExecutor.AutoRotate.NONE);
                    effect.setForcedDeath(triggerPacket.isForceDead());
                    effect.start();
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientTriggerBlockEffectFx(FxBlockS2CPacket triggerPacket) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            BlockPos pos = triggerPacket.getBlockPos();
            FX fx = FXHelper.getFX(triggerPacket.getFx());
            if (fx != null) {
                var effect = new BlockEffectExecutor(fx, level, pos);
                effect.setForcedDeath(triggerPacket.isForceDead());
                effect.start();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientTriggerPositionEffectFx(FxPositionS2CPacket triggerPacket) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            FX fx = FXHelper.getFX(triggerPacket.getFx());
            if (fx != null) {
                BlockPos blockPos = BlockPos.containing(triggerPacket.getX(), triggerPacket.getY(), triggerPacket.getZ());
                Vector3f velocity = new Vector3f(
                        (float) triggerPacket.getVelocityX(),
                        (float) triggerPacket.getVelocityY(),
                        (float) triggerPacket.getVelocityZ()
                );
                var effect = new MovingBlockEffectExecutor(fx, level, blockPos, velocity);
                // BlockEffectExecutor adds 0.5 to offsets, so subtract it to preserve exact world coordinates.
                effect.setOffset(new Vector3f(
                        (float) (triggerPacket.getX() - blockPos.getX() - 0.5D),
                        (float) (triggerPacket.getY() - blockPos.getY() - 0.5D),
                        (float) (triggerPacket.getZ() - blockPos.getZ() - 0.5D)
                ));
                effect.setRotation(new Quaternionf(triggerPacket.getRotation()));
                effect.setScale(triggerPacket.getScale());
                effect.setForcedDeath(triggerPacket.isForceDead());
                // Function: Photon normally gates duplicate block FX; selected effects can opt into overlap.
                effect.setAllowMulti(triggerPacket.isAllowMulti());
                effect.setCheckState(false);
                effect.start();
            }
        }
    }

    private static class MovingBlockEffectExecutor extends BlockEffectExecutor {
        private static final float TICKS_PER_SECOND = 20.0F;
        private final Vector3f velocityPerTick;
        private final Vector3f basePosition = new Vector3f();
        private int ageTicks;

        private MovingBlockEffectExecutor(FX fx, Level level, BlockPos pos, Vector3f velocityPerSecond) {
            super(fx, level, pos);
            this.velocityPerTick = new Vector3f(velocityPerSecond).div(TICKS_PER_SECOND);
        }

        @Override
        public void start() {
            super.start();
            this.basePosition.set(
                    pos.getX() + 0.5F + offset.x,
                    pos.getY() + 0.5F + offset.y,
                    pos.getZ() + 0.5F + offset.z
            );
        }

        @Override
        public void updateFXObjectTick(IFXObject fxObject) {
            super.updateFXObjectTick(fxObject);
            if (runtime != null && fxObject == runtime.root) {
                ageTicks++;
                // Keep position FX moving with the server supplied sublevel linear velocity.
                fxObject.updatePos(new Vector3f(basePosition).fma(ageTicks, velocityPerTick));
            }
        }
    }
}
