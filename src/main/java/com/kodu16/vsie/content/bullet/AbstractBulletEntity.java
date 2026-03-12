package com.kodu16.vsie.content.bullet;

import com.kodu16.vsie.utility.FxData;
import com.kodu16.vsie.utility.vsieFxHelper;
import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FXHelper;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import software.bernie.example.registry.EntityRegistry;

import java.util.List;
import java.util.Optional;

import static net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel;

public abstract class AbstractBulletEntity extends Projectile {

    private int lifeTime = 0;
    private double damage = 1.0;
    // 功能：获取子弹数据，供 tick 中读取 FX 配置。
    // 功能：存储当前子弹的数据配置，默认携带 particle_cannon_fire 的 awake FX。
    @Getter
    private BulletData dataBase = BulletData.createParticleCannonDefault();

    public AbstractBulletEntity(EntityType<? extends AbstractBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;           // 保持高速、无重力
        this.setNoGravity(true);         // 推荐一起设置
    }

    @Override
    public void tick() {

        if (this.tickCount >= 1 && this.tickCount <= 8) {
            if (this.level().isClientSide()) {
                vsieFxHelper.extractFxUnit(getDataBase().getFxData(), FxData::getAwakeFx)
                        .map(FxData.FxUnit::getId).map(FXHelper::getFX)
                        .ifPresent(fx -> {
                            var effect = new EntityEffect(fx, this.level(), this, EntityEffect.AutoRotate.XROT);
                            effect.setForcedDeath(true);
                            effect.start();
                        });
            }
        }

        Vec3 movement = this.getDeltaMovement();
        // ===== 5 tick 后速度 ×10 =====
        if (this.tickCount == 8) {
            this.setDeltaMovement(movement.scale(30));
            movement = this.getDeltaMovement();
        }
        Vec3 start = this.position();
        Vec3 end = start.add(movement);

        // 客户端只负责表现
        if (this.level().isClientSide()) {
            this.setPos(end);
            return;
        }

        // ===== 1 标准射线检测 =====
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(
                this,
                this::canHitEntity
        );

        // ===== 2 防止高速漏判 =====
        if (hitResult.getType() == HitResult.Type.MISS) {

            List<Entity> entities = this.level().getEntities(
                    this,
                    this.getBoundingBox().expandTowards(movement).inflate(1.5)
            );

            Entity closest = null;
            double closestDistSq = Double.MAX_VALUE;

            for (Entity entity : entities) {

                if (!this.canHitEntity(entity)) continue;

                Optional<Vec3> intercept = entity.getBoundingBox().clip(start, end);

                if (intercept.isPresent()) {

                    double distSq = intercept.get().distanceToSqr(start);

                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = entity;
                    }
                }
            }

            if (closest != null) {
                hitResult = new EntityHitResult(closest);
            }
        }

        // ===== 处理命中 =====
        if (hitResult.getType() == HitResult.Type.ENTITY) {

            this.onHitEntity((EntityHitResult) hitResult);
            this.discard();
            return;

        } else if (hitResult.getType() == HitResult.Type.BLOCK) {

            this.onHitBlock((BlockHitResult) hitResult);
            this.discard();
            return;
        }

        // ===== 最后移动 =====
        this.setPos(end);

        lifeTime++;

        if (lifeTime > 60) {
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        BlockState state = (level().getBlockState(pResult.getBlockPos()));
        if(state.isCollisionShapeFullBlock(level(), pResult.getBlockPos())) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity target = pResult.getEntity();
        target.hurt(this.level().damageSources().onFire(),15);
        this.discard();
    }


    // 功能：外部可覆盖子弹数据；传入 null 时回退默认 particle_cannon_fire 配置。
    public void setDataBase(BulletData dataBase) {
        this.dataBase = dataBase == null ? BulletData.createParticleCannonDefault() : dataBase;
    }

    @Override
    protected void defineSynchedData() {

    }
}
