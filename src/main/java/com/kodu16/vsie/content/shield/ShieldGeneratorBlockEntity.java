package com.kodu16.vsie.content.shield;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class ShieldGeneratorBlockEntity extends SmartBlockEntity {
    public SmartFluidTankBehaviour tank;
    public ShieldGeneratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }
    double RADIUS = 10;

    public void tick(Level level, BlockPos pos, BlockState state, ShieldGeneratorBlockEntity be) {
        if (level.isClientSide || level.getGameTime() % 2 != 0) return;

        Vec3 center = Vec3.atCenterOf(pos);
        AABB searchBox = new AABB(this.getBlockPos()).inflate(RADIUS + 6.0); // 多搜一点，防止高速实体一帧穿过去

        // 核心：只筛选“没有生命值 + 速度够快 + 不是玩家也不是盔甲架”之类的实体
        level.getEntitiesOfClass(Entity.class, searchBox, entity -> {
            if (entity.isRemoved() || entity instanceof LivingEntity)
                return false;

            // 速度阈值，可调（单位：方块/刻）
            double speed = entity.getDeltaMovement().length();
            if (speed < 0.25) return false; // 太慢的直接忽略（比如漂浮的物品）

            // 计算是否朝护盾飞来
            Vec3 toEntity = entity.position().subtract(center);
            double dot = entity.getDeltaMovement().normalize().dot(toEntity.normalize());
            return dot < -0.3; // 越负说明越正对护盾飞来（-0.3~0.6 之间调节手感）
        }).forEach(entity -> {

            Vec3 toEntity = entity.position().subtract(center);
            double distSq = toEntity.lengthSqr();

            if (distSq > RADIUS * RADIUS || distSq < 0.25) return;

            // 拦截！
            entity.discard(); // 直接删除，兼容 99% 的模组实体

            // 粒子交点
            Vec3 hitDir = toEntity.normalize();
            Vec3 hitPoint = center.add(hitDir.scale(RADIUS));
            spawnRippleParticles((ServerLevel) level, hitPoint, hitDir);

            // 可选：播放音效
            level.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS,
                    1.0f, 1.2f + level.random.nextFloat() * 0.4f);
        });
    }

    private static void spawnRippleParticles(ServerLevel level, Vec3 hitPoint, Vec3 hitDir) {
        int numRings = 3;
        int particlesPerRing = 16;
        double speed = 0.02;

        // 构造一个与 hitDir 垂直的局部坐标系 (u, v)
        Vec3 w = hitDir.normalize();
        Vec3 arbitrary = Math.abs(w.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = w.cross(arbitrary).normalize();   // 第一个正交向量
        Vec3 v = w.cross(u).normalize();           // 第二个正交向量（也垂直于 w）

        for (int ring = 0; ring < numRings; ring++) {
            double ringRadius = 0.2 + ring * 0.4;
            for (int i = 0; i < particlesPerRing; i++) {
                double angle = (i * 2 * Math.PI) / particlesPerRing;
                Vec3 localOffset = u.scale(Math.cos(angle) * ringRadius)
                        .add(v.scale(Math.sin(angle) * ringRadius));

                // 计算粒子位置
                Vec3 pos = hitPoint.add(localOffset);

                // 计算漂浮速度
                Vec3 outwardMovement = hitDir.scale(0.05); // 向外漂浮的速度

                // 使用 DustParticleOptions（浅蓝色）
                Vector3f dustColor = new Vector3f(1.0f, 0.8f, 1.0f);  // 浅蓝色 RGB
                DustParticleOptions dustParticle = new DustParticleOptions(dustColor, 1.0f); // 浅蓝色
                level.sendParticles(dustParticle,
                        pos.x, pos.y, pos.z,
                        1, outwardMovement.x, outwardMovement.y, outwardMovement.z, speed);

                // 使用 Glow 发光粒子
                level.sendParticles(ParticleTypes.GLOW,
                        pos.x, pos.y, pos.z,
                        1, outwardMovement.x, outwardMovement.y, outwardMovement.z, speed);
            }
        }
    }
}
