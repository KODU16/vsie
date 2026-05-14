package com.kodu16.vsie.content.weapon.arc_emitter;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ArcEmitterBlockEntity extends AbstractWeaponBlockEntity {
    private static final ResourceLocation ARC_LIGHTENING_FX = ResourceLocation.fromNamespaceAndPath("vsie", "arc_lightening");
    private static final int BLOCK_BREAK_RADIUS = 7;
    private static final float ARC_LIGHTENING_DEFAULT_LENGTH = 5.0F;

    public ArcEmitterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 20;
    }

    @Override
    public void fire() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        performRaycast(serverLevel);
        Vec3 firePos = getRaycastStart();
        Vec3 hitPos = getTargetpos();
        playArcLighteningFx(firePos, hitPos);
        if (hasRaycastHit()) {
            LogUtils.getLogger().warn("arc emitter explode at:" + hitPos);
            destroyBlocksInRadius(serverLevel, BlockPos.containing(hitPos), BLOCK_BREAK_RADIUS);
            serverLevel.explode(
                    null,
                    hitPos.x, hitPos.y, hitPos.z,
                    3.0F,
                    false,
                    Level.ExplosionInteraction.NONE
            );
        }
    }

    private void destroyBlocksInRadius(ServerLevel level, BlockPos center, int radius) {
        int radiusSqr = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z > radiusSqr) {
                        continue;
                    }
                    BlockPos targetPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(targetPos);
                    if (state.isAir() || state.getDestroySpeed(level, targetPos) < 0.0F) {
                        continue;
                    }
                    level.destroyBlock(targetPos, true);
                }
            }
        }
    }

    private void playArcLighteningFx(Vec3 firePos, Vec3 hitPos) {
        Vec3 beam = hitPos.subtract(firePos);
        double length = beam.length();
        if (length <= 1.0E-4D) {
            return;
        }
        Vec3 direction = beam.normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0F, 1.0F, 0.0F,
                (float) direction.x, (float) direction.y, (float) direction.z
        );
        float yScale = Math.max(0.01F, (float) length / ARC_LIGHTENING_DEFAULT_LENGTH);
        // Function: arc_lightening is authored as a 5-block Y segment, so scale only local Y from muzzle to hit point.
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                ARC_LIGHTENING_FX,
                firePos.x, firePos.y, firePos.z,
                rotation,
                new Vector3f(1.0F, yScale, 1.0F),
                false
        ));
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("ARCE");
    }

    @Override
    public String getweapontype() {
        return "arc_emitter";
    }
}
