package com.kodu16.vsie.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;

public class CannonMuzzleSmokeParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected CannonMuzzleSmokeParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.gravity = 0.0F;
        this.friction = 0.94F;
        this.hasPhysics = false;
        this.quadSize = 0.8F + this.random.nextFloat() * 0.35F;
        this.lifetime = 32 + this.random.nextInt(18);
        // Function: keep the server-provided plume velocity so muzzle smoke is pushed along the barrel like CBC smoke.
        this.setParticleSpeed(dx, dy, dz);
        this.setSpriteFromAge(sprites);
        this.setAlpha(0.82F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float progress = this.age / (float) Math.max(1, this.lifetime);
        // Function: copied CBC smoke sprites expand and fade as the muzzle plume dissipates.
        this.quadSize *= 1.018F;
        this.setAlpha(Mth.clamp(0.82F * (1.0F - progress), 0.3F, 0.82F));
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<net.minecraft.core.particles.SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(net.minecraft.core.particles.SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new CannonMuzzleSmokeParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
