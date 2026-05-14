package com.kodu16.vsie.content.weapon.infra_knife_accelerator;

import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class InfraKnifeAcceleratorBlockEntity extends AbstractWeaponBlockEntity {
    private static final DustParticleOptions RED_BEAM_PARTICLE = new DustParticleOptions(new Vector3f(1.0F, 0.02F, 0.0F), 2.0F);

    public InfraKnifeAcceleratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public float getmaxrange() {
        return 512;
    }

    @Override
    public int getcooldown() {
        return 4;
    }

    @Override
    public void fire() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        performRaycast(serverLevel);
        Vec3 beamStart = getRaycastStart();
        Vec3 beamEnd = getTargetpos();
        spawnRedBeam(serverLevel, beamStart, beamEnd);

        if (hasRaycastHit()) {
            LogUtils.getLogger().warn("explode at:" + targetpos);
            spawnHitParticles(serverLevel, targetpos);
            serverLevel.explode(
                    null,
                    targetpos.x, targetpos.y, targetpos.z,
                    3,
                    true,
                    Level.ExplosionInteraction.NONE
            );
        }
    }

    private void spawnRedBeam(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4D) {
            return;
        }
        int samples = Math.max(2, Math.min(128, (int) (length / 4.0D)));
        Vec3 step = delta.scale(1.0D / samples);
        // Function: draw the instantaneous infra-knife beam on clients without relying on block animation state.
        for (int i = 0; i <= samples; i++) {
            Vec3 point = from.add(step.scale(i));
            level.sendParticles(RED_BEAM_PARTICLE, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void spawnHitParticles(ServerLevel level, Vec3 hitPos) {
        // Function: reinforce the non-destructive explosion with visible impact and flame particles at the ray hit.
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, hitPos.x, hitPos.y, hitPos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME, hitPos.x, hitPos.y, hitPos.z, 48, 0.8D, 0.8D, 0.8D, 0.04D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, hitPos.x, hitPos.y, hitPos.z, 24, 0.8D, 0.8D, 0.8D, 0.02D);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("IFRA");
    }

    @Override
    public String getweapontype() {
        return "infra_knife_accelerator";
    }
}
