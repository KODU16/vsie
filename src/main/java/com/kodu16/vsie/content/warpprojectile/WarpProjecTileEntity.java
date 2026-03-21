package com.kodu16.vsie.content.warpprojectile;

import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("removal")
public class WarpProjecTileEntity extends Projectile {
    // 功能：同步弹射物总飞行距离，保证客户端和服务端都能按相同距离自动消失。
    private static final EntityDataAccessor<Float> MAX_LIFE =
            SynchedEntityData.defineId(WarpProjecTileEntity.class, EntityDataSerializers.FLOAT);
    // 功能：记录是否已经在客户端播放过生成特效，避免每 tick 重复创建 Photon 特效实例。
    private boolean spawnedClientFx = false;
    // 功能：记录是否已经播放过消失特效，避免实体移除流程中重复触发 warp_projectile_vanish.fx。
    private boolean vanishedClientFx = false;
    // 功能：保存跃迁弹的最大寿命/半径，用于服务端销毁判定与客户端消失平面半径统一。
    public double maxlife;
    private int lifeTime = 0;
    // 功能：缓存最后一次有效速度方向，供消失特效在弹体停下后仍可按飞行法线方向对齐。
    private Vec3 lastNonZeroVelocity = Vec3.ZERO;
    // 功能：统一管理跃迁弹射物的生成特效资源，生成时播放 warp_projectile.fx。
    private static final ResourceLocation WARP_PROJECTILE_FX = new ResourceLocation("vsie", "warp_projectile");
    // 功能：统一管理跃迁弹射物的消失特效资源，实体销毁时播放 warp_projectile_vanish.fx。
    private static final ResourceLocation WARP_PROJECTILE_VANISH_FX = new ResourceLocation("vsie", "warp_projectile_vanish");

    @Override
    protected void defineSynchedData() {
        // 功能：为最大飞行距离提供默认值，避免未配置时出现 0 距离立刻消失。
        this.entityData.define(MAX_LIFE, 64.0F);
    }

    public WarpProjecTileEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;           // 保持高速、无重力
        this.setNoGravity(true);         // 推荐一起设置
    }

    // 功能：在生成前一次性配置方向、速度和最大飞行距离，调用方式与 bullet 的生成流程保持一致。
    public void configureLaunch(Vec3 direction, double life) {
        this.setDeltaMovement(direction.scale(0.2));
        this.maxlife = life/3;
    }

    @Override
    public void tick() {
        super.tick();

        // 功能：实体首次更新时播放生成特效，使用 warp_projectile.fx 提供视觉表现。
        if (this.level().isClientSide() && !this.spawnedClientFx) {
            this.spawnedClientFx = true;
            var fx = FXHelper.getFX(WARP_PROJECTILE_FX);
            if (fx != null) {
                var effect = new EntityEffect(fx, this.level(), this, EntityEffect.AutoRotate.FORWARD);
                effect.setForcedDeath(true);
                effect.start();
            }
        }
        this.maxlife = this.entityData.get(MAX_LIFE);

        Vec3 movement = this.getDeltaMovement();
        this.updateRotationFromVelocity(movement);

        Vec3 nextPos = this.position().add(movement);

        // 功能：客户端只负责表现移动，命中与销毁判定交给服务端统一处理。
        if (this.level().isClientSide()) {
            this.setPos(nextPos);
            return;
        }

        // 功能：未命中时推进实体位置，并累计已飞行距离用于距离销毁。
        this.setPos(nextPos);
        lifeTime++;
        if (lifeTime >= this.maxlife) {
            // 功能：先按最后一次飞行方向刷新朝向，再停止位移，保证消失特效垂直于当前速度播放。
            this.updateRotationFromVelocity(this.lastNonZeroVelocity);
            this.setDeltaMovement(Vec3.ZERO);
            this.playVanishFx();
        }
        if(lifeTime>=this.maxlife+80) {
            this.discard();
        }
    }



    // 功能：根据当前速度同步实体的俯仰与偏航，让 Photon 的 AutoRotate.FORWARD 能获取完整三维朝向。
    private void updateRotationFromVelocity(Vec3 velocity) {
        if (velocity.lengthSqr() < 1.0E-6D) {
            return;
        }

        this.lastNonZeroVelocity = velocity;
        float yaw = (float) Math.atan2(velocity.x, velocity.z) * Mth.RAD_TO_DEG;
        float pitch = (float) Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)) * Mth.RAD_TO_DEG;
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
    }

    // 功能：在客户端播放一次消失特效，确保跃迁弹移除时能释放 warp_projectile_vanish.fx。
    private void playVanishFx() {
        var fx = FXHelper.getFX(WARP_PROJECTILE_VANISH_FX);
        if (fx != null) {
            var effect = new EntityEffect(fx, this.level(), this, EntityEffect.AutoRotate.FORWARD);
            effect.setForcedDeath(false);
            effect.start();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // 功能：命中实体后结束弹射物；预留伤害或传送逻辑给后续扩展。
        Entity target = result.getEntity();
        if (target != null) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // 功能：命中方块后直接移除，行为与常规 bullet 一致。
        this.discard();
    }
}
