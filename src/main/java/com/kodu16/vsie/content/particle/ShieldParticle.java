package com.kodu16.vsie.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class ShieldParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private final int startFrameTick;

    public ShieldParticle(ClientLevel level, double x, double y, double z,
                          double xSpeed, double ySpeed, double zSpeed,
                          ShieldParticleOptions options,
                          SpriteSet spriteSet) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.spriteSet = spriteSet;
        this.startFrameTick = options.getLifeOffset();

        this.quadSize = 0.03F;
        this.lifetime = this.startFrameTick + 3;
        this.setColor(1.0F, 1.0F, 1.0F);
        this.gravity = 0;
        this.hasPhysics = false;
        this.friction = 1.0F;
        this.setSprite(spriteSet.get(0, 4));
    }

    @Override
    public void tick() {
        this.setParticleSpeed(0,0,0);
        this.setSize(0.03F,0.03F);
        super.tick();

        int frameIndex;
        if (this.age < startFrameTick) {
            frameIndex = 0;
        } else {
            int elapsed = this.age - startFrameTick;
            int phase = elapsed / 2;
            frameIndex = Math.min(1 + phase, 4);
        }

        this.setSprite(spriteSet.get(frameIndex, 4));
        // Function: shield impact particles pulse outward as their short animation advances.
        this.quadSize = 0.08F + 0.10F * Mth.sqrt((float) this.age / this.lifetime);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_LIT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15728880;
    }

    public static class Provider implements ParticleProvider<ShieldParticleOptions> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(ShieldParticleOptions options,
                                       ClientLevel level,
                                       double x, double y, double z,
                                       double xSpeed, double ySpeed, double zSpeed) {
            return new ShieldParticle(level, x, y, z, xSpeed, ySpeed, zSpeed,
                    options, this.spriteSet);
        }
    }
}
