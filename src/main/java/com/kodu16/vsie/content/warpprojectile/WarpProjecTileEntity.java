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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WarpProjecTileEntity extends Projectile {
    // 功能：同步弹射物总飞行距离，保证客户端和服务端都能按相同距离自动消失。
    private static final EntityDataAccessor<Float> MAX_TRAVEL_DISTANCE =
            SynchedEntityData.defineId(WarpProjecTileEntity.class, EntityDataSerializers.FLOAT);
    // 功能：记录是否已经播放过生成时的 Photon 特效，避免同一个实体重复触发。
    private boolean hasPlayedSpawnFx = false;
    // 功能：累计当前实体已经飞行的距离，用于达到上限时自动移除。
    private double travelledDistance = 0.0D;
    // 功能：统一管理跃迁弹射物的生成特效资源，生成时播放 warp_projectile.fx。
    private static final ResourceLocation WARP_PROJECTILE_FX = new ResourceLocation("vsie", "warp_projectile");

    @Override
    protected void defineSynchedData() {
        // 功能：为最大飞行距离提供默认值，避免未配置时出现 0 距离立刻消失。
        this.entityData.define(MAX_TRAVEL_DISTANCE, 64.0F);
    }

    public WarpProjecTileEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
        this.noPhysics = true;           // 保持高速、无重力
        this.setNoGravity(true);         // 推荐一起设置
    }

    // 功能：在生成前一次性配置方向、速度和最大飞行距离，调用方式与 bullet 的生成流程保持一致。
    public void configureLaunch(Vec3 direction, double speed, double maxTravelDistance) {
        Vec3 safeDirection = direction == null || direction.lengthSqr() < 1.0E-7D ? Vec3.ZERO : direction.normalize();
        this.setDeltaMovement(safeDirection.scale(Math.max(0.0D, speed)));
        this.setMaxTravelDistance(maxTravelDistance);
        this.travelledDistance = 0.0D;
    }

    // 功能：允许外部在生成时单独修改速度；会保留原有方向，只更新飞行速度。
    public void setSpeed(double speed) {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-7D) {
            return;
        }
        this.setDeltaMovement(movement.normalize().scale(Math.max(0.0D, speed)));
    }

    // 功能：允许外部在生成时单独指定消失前的最大前进距离。
    public void setMaxTravelDistance(double maxTravelDistance) {
        this.entityData.set(MAX_TRAVEL_DISTANCE, (float) Math.max(0.0D, maxTravelDistance));
    }

    // 功能：提供当前配置的最大飞行距离读取接口，便于外部逻辑复用。
    public double getMaxTravelDistance() {
        return this.entityData.get(MAX_TRAVEL_DISTANCE);
    }

    @Override
    public void tick() {
        super.tick();

        // 功能：实体首次更新时播放生成特效，使用 warp_projectile.fx 提供视觉表现。
        if (!this.hasPlayedSpawnFx && this.level().isClientSide()) {
            this.hasPlayedSpawnFx = true;
            var fx = FXHelper.getFX(WARP_PROJECTILE_FX);
            if (fx != null) {
                var effect = new EntityEffect(fx, this.level(), this, EntityEffect.AutoRotate.XROT);
                effect.setForcedDeath(true);
                effect.start();
            }
        }

        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-7D) {
            return;
        }

        Vec3 nextPos = this.position().add(movement);

        // 功能：客户端只负责表现移动，命中与销毁判定交给服务端统一处理。
        if (this.level().isClientSide()) {
            this.setPos(nextPos);
            this.travelledDistance += movement.length();
            if (this.travelledDistance >= this.getMaxTravelDistance()) {
                this.discard();
            }
            return;
        }

        // 功能：沿运动方向做射线检测，命中实体或方块后立即结束飞行。
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) hitResult);
            return;
        }
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) hitResult);
            return;
        }

        // 功能：未命中时推进实体位置，并累计已飞行距离用于距离销毁。
        this.setPos(nextPos);
        this.travelledDistance += movement.length();
        if (this.travelledDistance >= this.getMaxTravelDistance()) {
            this.discard();
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
