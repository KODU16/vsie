package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.bullet.BulletRenderer;
import com.kodu16.vsie.content.bullet.entity.ParticleBulletEntity;
import com.kodu16.vsie.content.bullet.entity.CenixPlasmaBulletEntity;
import com.kodu16.vsie.content.bullet.entity.HeavyElectroMagnetBulletEntity;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
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
                    // Use a compact hitbox so the projectile no longer inherits a tall default entity box.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(10).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<CenixPlasmaBulletEntity> CENIX_PLASMA_BULLET =
            REGISTRATE.entity("cenix_plasma_bullet", CenixPlasmaBulletEntity::new, MobCategory.MISC)
                    // Function: fast plasma bullets need immediate client tracking so entity-attached FX can start reliably.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(10).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<HeavyElectroMagnetBulletEntity> HEAVY_ELECTROMAGNETIC_BULLET =
            REGISTRATE.entity("heavy_electromagnetic_bullet", HeavyElectroMagnetBulletEntity::new, MobCategory.MISC)
                    // Function: heavy electromagnetic bullets currently share particle bullet size and tracking.
                    .properties(builder -> builder.sized(0.35F, 0.35F).clientTrackingRange(10).updateInterval(1))
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<WarpProjecTileEntity> WARP_PROJECTILE =
            REGISTRATE.entity("warp_projectile", WarpProjecTileEntity::new, MobCategory.MISC)
                    // 功能：注册 warp 特效弹体实体，供控制椅自动对准完成后在服务器生成。
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<ControlSeatMountEntity> CONTROL_SEAT_MOUNT_ENTITY =
            REGISTRATE.entity("control_seat_mount", ControlSeatMountEntity::new, MobCategory.MISC)
                    // 功能：注册 warp 特效弹体实体，供控制椅自动对准完成后在服务器生成。
                    //.tag(RPLTags.PRECISE_MOTION)
                    .register();

}
