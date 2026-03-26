package com.kodu16.vsie.content.missile;

import com.mojang.logging.LogUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.network.SerializableDataTicket;

public abstract class AbstractMissileEntity extends AbstractHurtingProjectile implements GeoEntity {
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(AbstractMissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_AGE = SynchedEntityData.defineId(AbstractMissileEntity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private Ship target;
    private float speed = 1.5f;
    private static final int MAX_LIFETIME_TICKS = 20 * 10;

    // ===== 新增：转向相关参数 =====
    private Vec3 currentDirection = null;  // 当前飞行方向
    private float maxTurnRatePerTick = 0.05f;  // 每tick最大转向角度（弧度）
    // 对应约2.86度/tick，或约57度/秒 @ 20 TPS
    // 可根据需要调整，值越小转弯半径越大

    public float xRot0 = 0f;
    public float yRot0 = 0f;
    public static SerializableDataTicket<Double> MISSILE_MOMENTUM_X;
    public static SerializableDataTicket<Double> MISSILE_MOMENTUM_Y;
    public static SerializableDataTicket<Double> MISSILE_MOMENTUM_Z;

    public AbstractMissileEntity(EntityType<? extends AbstractMissileEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AGE, 0);
    }

    public void setTarget(Ship ship) {
        this.target = ship;
    }

    public abstract String getmissiletype();

    /**
     * 设置最大转向速率（弧度/tick）
     * 例如：0.05 弧度/tick ≈ 2.86度/tick ≈ 57度/秒
     * 值越小，转弯半径越大
     */
    public void setMaxTurnRate(float radiansPerTick) {
        this.maxTurnRatePerTick = radiansPerTick;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) return;

        int age = this.entityData.get(DATA_AGE);
        age++;
        this.entityData.set(DATA_AGE, age);

        if (age >= MAX_LIFETIME_TICKS) {
            this.explodeAndDiscard();
            return;
        }

        // ───────────────────────────────
        // 追踪目标部分（添加转向限制）
        if(target == null) {
            LogUtils.getLogger().warn("NO target!");
            // 没有目标时保持直线飞行
            if(currentDirection != null) {
                this.setDeltaMovement(currentDirection.scale(speed));
                this.move(MoverType.SELF, this.getDeltaMovement());
            }
        } else {
            Vector3dc targetpos = target.getTransform().getPosition();

            // 计算理想方向（指向目标）
            Vec3 idealDirection = new Vec3(
                    targetpos.x() - this.getX(),
                    targetpos.y() - this.getY(),
                    targetpos.z() - this.getZ()
            ).normalize();

            // 初始化当前方向（第一次tick）
            if(currentDirection == null) {
                currentDirection = idealDirection;
            }

            // ===== 应用转向限制 =====
            currentDirection = limitTurnRate(currentDirection, idealDirection, maxTurnRatePerTick);

            // 应用速度和移动
            this.setDeltaMovement(currentDirection.scale(speed));
            setAnimData(MISSILE_MOMENTUM_X, currentDirection.x());
            setAnimData(MISSILE_MOMENTUM_Y, currentDirection.y());
            setAnimData(MISSILE_MOMENTUM_Z, currentDirection.z());
            this.move(MoverType.SELF, this.getDeltaMovement());
        }

        super.tick();
    }

    /**
     * 限制从当前方向到理想方向的转向速率
     * @param current 当前方向（已归一化）
     * @param ideal 理想方向（已归一化）
     * @param maxTurnRate 最大转向角度（弧度/tick）
     * @return 应用转向限制后的新方向（已归一化）
     */
    private Vec3 limitTurnRate(Vec3 current, Vec3 ideal, float maxTurnRate) {
        // 计算两向量之间的夹角
        double dotProduct = current.dot(ideal);
        // 防止数值误差导致acos域错误
        dotProduct = Math.max(-1.0, Math.min(1.0, dotProduct));
        double angle = Math.acos(dotProduct);

        // 如果夹角小于最大转向角，直接返回理想方向
        if(angle <= maxTurnRate) {
            return ideal;
        }

        // 否则，只转动最大允许角度
        // 使用球面线性插值（Slerp）
        double t = maxTurnRate / angle;  // 插值参数

        // Slerp公式: (sin((1-t)*angle)/sin(angle)) * v1 + (sin(t*angle)/sin(angle)) * v2
        double sinAngle = Math.sin(angle);
        double factorCurrent = Math.sin((1.0 - t) * angle) / sinAngle;
        double factorIdeal = Math.sin(t * angle) / sinAngle;

        Vec3 result = new Vec3(
                current.x * factorCurrent + ideal.x * factorIdeal,
                current.y * factorCurrent + ideal.y * factorIdeal,
                current.z * factorCurrent + ideal.z * factorIdeal
        );

        return result.normalize();
    }

    protected void explodeAndDiscard() {
        this.level().explode(this,
                this.getX(), this.getY(), this.getZ(),
                4.0F,
                Level.ExplosionInteraction.BLOCK
        );
        this.discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
