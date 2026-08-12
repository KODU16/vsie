package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.bullet.BulletRenderer;
import com.kodu16.vsie.content.bullet.entity.CenixPlasmaBulletEntity;
import com.kodu16.vsie.content.bullet.entity.ElectroMagnetRailCannonBulletEntity;
import com.kodu16.vsie.content.bullet.entity.HeavyElectroMagnetBulletEntity;
import com.kodu16.vsie.content.bullet.entity.InfraKnifeBulletEntity;
import com.kodu16.vsie.content.bullet.entity.ParticleBulletEntity;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.custom_turret.CustomTurretProjectileEntity;
import com.kodu16.vsie.content.missile.entity.BasicMissileEntity;
import com.kodu16.vsie.content.warpprojectile.WarpProjecTileEntity;
import com.kodu16.vsie.vsie;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.EntityEntry;
import net.minecraft.world.entity.MobCategory;
import rbasamoyai.ritchiesprojectilelib.RPLTags;

public class vsieEntities {

    private static final CreateRegistrate REGISTRATE = vsie.registrate();

    public static void register() {}

    public static final EntityEntry<BasicMissileEntity> BASIC_MISSILE =
            REGISTRATE.entity("basic_missile", BasicMissileEntity::new, MobCategory.MISC)
                    // Function: missiles need a real hitbox and frequent sync for guided movement and impact.
                    .properties(builder -> builder.sized(0.6F, 0.6F).clientTrackingRange(16).updateInterval(1))
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<ParticleBulletEntity> PARTICLE_BULLET =
            REGISTRATE.entity("particle_bullet", ParticleBulletEntity::new, MobCategory.MISC)
                    // Function: bullets need long client tracking so they remain renderable in distant loaded chunks.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(256).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<CustomTurretProjectileEntity> CUSTOM_TURRET_PROJECTILE =
            REGISTRATE.entity("custom_turret_projectile", CustomTurretProjectileEntity::new, MobCategory.MISC)
                    // Function: configurable custom shots reuse the precise-motion bullet transport and renderer.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(256).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<CenixPlasmaBulletEntity> CENIX_PLASMA_BULLET =
            REGISTRATE.entity("cenix_plasma_bullet", CenixPlasmaBulletEntity::new, MobCategory.MISC)
                    // Function: plasma bullets share the same far-distance render requirement as the base bullet.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(256).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<HeavyElectroMagnetBulletEntity> HEAVY_ELECTROMAGNETIC_BULLET =
            REGISTRATE.entity("heavy_electromagnetic_bullet", HeavyElectroMagnetBulletEntity::new, MobCategory.MISC)
                    // Function: heavy electromagnetic bullets must keep syncing beyond normal combat distance.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(256).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<ElectroMagnetRailCannonBulletEntity> ELECTRO_MAGNET_RAIL_CANNON_BULLET =
            REGISTRATE.entity("electro_magnet_rail_cannon_bullet", ElectroMagnetRailCannonBulletEntity::new, MobCategory.MISC)
                    // Function: rail cannon bullets also need extended client tracking for remote rendering.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(256).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<InfraKnifeBulletEntity> INFRA_KNIFE_BULLET =
            REGISTRATE.entity("infra_knife_bullet", InfraKnifeBulletEntity::new, MobCategory.MISC)
                    // Function: infra-knife uses a 0.3 block collision cross-section; renderer stretches it along velocity.
                    .properties(builder -> builder.sized(0.3F, 0.3F).clientTrackingRange(256).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<WarpProjecTileEntity> WARP_PROJECTILE =
            REGISTRATE.entity("warp_projectile", WarpProjecTileEntity::new, MobCategory.MISC)
                    // Function: warp projectile FX must remain client-side even when the ship-target span is far beyond normal projectile range.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(2048).updateInterval(1))
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<ControlSeatMountEntity> CONTROL_SEAT_MOUNT_ENTITY =
            REGISTRATE.entity("control_seat_mount", ControlSeatMountEntity::new, MobCategory.MISC)
                    //.tag(RPLTags.PRECISE_MOTION)
                    .register();

}
