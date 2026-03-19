package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.bullet.BulletRenderer;
import com.kodu16.vsie.content.bullet.entity.ParticleBulletEntity;
import com.kodu16.vsie.content.bullet.entity.CenixPlasmaBulletEntity;
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
                    .register();
    public static final EntityEntry<ParticleBulletEntity> PARTICLE_BULLET =
            REGISTRATE.entity("particle_bullet", ParticleBulletEntity::new, MobCategory.MISC)
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<CenixPlasmaBulletEntity> CENIX_PLASMA_BULLET =
            REGISTRATE.entity("plasma_bullet", CenixPlasmaBulletEntity::new, MobCategory.MISC)
                    .renderer(() -> BulletRenderer::new)
                    .tag(RPLTags.PRECISE_MOTION)
                    .register();
    public static final EntityEntry<WarpProjecTileEntity> WARP_PROJECTILE =
            REGISTRATE.entity("warp_projectile", WarpProjecTileEntity::new, MobCategory.MISC)
                    // 功能：注册 warp 特效弹体实体，供控制椅自动对准完成后在服务器生成。
                    .register();

}
