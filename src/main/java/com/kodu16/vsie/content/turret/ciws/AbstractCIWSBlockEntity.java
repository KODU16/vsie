package com.kodu16.vsie.content.turret.ciws;

import com.kodu16.vsie.content.turret.AbstractTurretBlock;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.foundation.Vec;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.shadow.En;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.Comparator;
import java.util.List;

public abstract class AbstractCIWSBlockEntity extends AbstractTurretBlockEntity {
    protected AbstractCIWSBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 初始化 turretData
        this.turretData = new TurretData();
    }

    private @Nullable Entity targetprojectile = null;


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    @Override
    public void tick() {
        if(idleTicks > 1) {
            idleTicks = idleTicks-1;
            return;
        }
        // 功能：统一刷新炮塔世界坐标，减少 tick 主流程分支复杂度。
        refreshWorldPosition();
        tryInvalidateTarget();
        // 功能：统一处理目标搜索，若无有效目标则让炮塔回归默认角度。
        acquireTargetByAimType();
        if (hasValidTarget()) {
            // 功能：维护速度采样窗口，为弹道预测提供最近移动趋势。
            appendTargetVelocitySample();
            // 功能：统一更新当前目标点，避免实体/舰船重复分支代码。
            updateCurrentTargetPos();

            targetPos = getShootLocation(targetPos, targetPreVelocity, level, currentworldpos);
            updateTargetRot();
            this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot);
            this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot);
            if (xOK && yOK) {
                fireWhenLocked();
            }
        } else {
            // 功能：当周围没有有效敌人时，平滑回到用户设置的默认俯仰/偏航角。
            returnToDefaultRotation();
        }
        //LogUtils.getLogger().warn("targetx:"+targetxrot+"y:"+targetyrot+"currentx:"+xRot0+"y:"+yRot0+"OK?"+xOK+yOK);
        this.setAnimData(XROT, xRot0);
        this.setAnimData(YROT, yRot0);
        this.markUpdated();
    }

    private void tryInvalidateTarget() {
        if(aimtype==1) {
            if(!isValidTargetEntity(targetentity)) {
                setAnimData(HAS_TARGET, false);
                targetentity = null;
                targetdistance = 0;
                xRot0 = 0;
                yRot0 = 0;
                targetPreVelocity.clear();
            }
        }
        else if(aimtype==2) {
            if(!isValidTargetProjectile(targetprojectile)) {
                setAnimData(HAS_TARGET, false);
                targetprojectile = null;
                targetdistance = 0;
                xRot0 = 0;
                yRot0 = 0;
                targetPreVelocity.clear();
            }
        }
    }

    private boolean isValidTargetProjectile(@Nullable Entity e) {
        // 只负责实体判断，输入的只有实体
        if (e == null) {
            return false;
        }
        // 功能：目标实体已经消失（如雪球撞地）时立即判定失效，避免炮塔锁定到最后坐标抖动。
        if (e.isRemoved() || !e.isAlive()) {
            return false;
        }
        if (e instanceof LivingEntity) {
            return false;
        }

        //速度判断
        Vec3 center = new Vec3(0,0,0);
        boolean onship = VSGameUtilsKt.isBlockInShipyard(level,this.getBlockPos());
        if(onship) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
            Vector3dc center3d = ship.getTransform().getPositionInWorld();
            center = new Vec3(center3d.x(),center3d.y(),center3d.z());
        }
        else {
            center = new Vec3(this.currentworldpos.x, this.currentworldpos.y, this.currentworldpos.z);
        }
        if(isflyingtowards(e,center)){return false;}

        // 距离判断（用世界坐标）
        double distSq = e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        if (distSq > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }
        // 视线判断（眼睛位置更准）
        if (!canSeeTarget(new Vector3d(e.getX(), e.getY() + e.getEyeHeight(), e.getZ()))) {
            return false;
        }
        return true;
    }

    private boolean canSeeTarget(Vector3d pos) {

        Vec3 turretpos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        Vec3 targetPos = new Vec3(Math.round(pos.x()*10)/10.0, Math.round(pos.y()*10)/10.0, Math.round(pos.z()*10)/10.0);
        Vec3 lookVec = turretpos.vectorTo(targetPos).normalize().scale(0.75F);
        ClipContext ctx = new ClipContext(turretpos.add(lookVec), targetPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null);
        return level.clip(ctx).getType().equals(HitResult.Type.MISS);
    }

    private void updateTargetRot() {
        Direction facing = this.getBlockState().getValue(AbstractTurretBlock.FACING);
        // 1. 获取炮塔当前的朝向（方块的facing）
        Vec3 localUp  = VectorConversionsMCKt.toMinecraft(VectorConversionsMCKt.toJOMLD(facing.getOpposite().getNormal()));;
        // 2. 获取炮塔本地坐标系的 "前" 和 "上" 向量（世界坐标）
        Vec3 localForward = switch (facing) {
            case NORTH -> new Vec3(0, 1, 0);
            case SOUTH -> new Vec3(0, -1, 0);
            case WEST,EAST,UP,DOWN -> new Vec3(0,0,-1);
        };

        Vec3 localRight  = switch (facing) {
            case NORTH, DOWN, SOUTH -> new Vec3(1, 0, 0);
            case WEST -> new Vec3(0, -1, 0);
            case EAST -> new Vec3(0, 1, 0);
            case UP -> new Vec3(-1,0,0);
        };

        boolean onship = VSGameUtilsKt.isBlockInShipyard(level,this.getBlockPos());
        if(onship) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
            final ShipTransform transform = ship.getTransform();
            transform.getShipToWorld().transformDirection(new Vector3d(localForward.x,localForward.y,localForward.z), worldXDirection);
            worldXDirection.normalize();
            transform.getShipToWorld().transformDirection(new Vector3d(localUp.x,localUp.y,localUp.z), worldYDirection);
            worldYDirection.normalize();
            transform.getShipToWorld().transformDirection(new Vector3d(localRight.x,localRight.y,localRight.z), worldZDirection);
            worldZDirection.normalize();
        }
        else {
            worldXDirection = new Vector3d(localForward.x,localForward.y,localForward.z);
            worldYDirection = new Vector3d(localUp.x,localUp.y,localUp.z);
            worldZDirection = new Vector3d(localRight.x,localRight.y,localRight.z);

        }

        // 4. 目标相对炮塔中心的向量（世界坐标）
        Vec3 toTargetWorld = new Vec3(
                targetPos.x - currentworldpos.x,
                targetPos.y - currentworldpos.y,
                targetPos.z - currentworldpos.z
        ).normalize();   // 建议先normalize，减少浮点误差影响

        if (toTargetWorld.lengthSqr() < 1e-6) return; // 目标在正中心，放弃计算

        // 5. 把世界向量转换到炮塔本地坐标系（用基向量做点积）
        double localX = toTargetWorld.dot(VectorConversionsMCKt.toMinecraft(worldZDirection));     // 本地右
        double localY = toTargetWorld.dot(VectorConversionsMCKt.toMinecraft(worldYDirection));        // 本地向上
        double localZ = toTargetWorld.dot(VectorConversionsMCKt.toMinecraft(worldXDirection));   // 本地向前

        // 6. 现在就在本地坐标系了，计算角度（经典写法）
        // yaw   : 左右角度，atan2(x, z)
        // pitch : 上下角度，atan2(y, 平面距离)
        double yaw   = Math.atan2(localX, localZ);           // 注意atan2顺序
        double pitch = Math.atan2(localY, Math.sqrt(localX * localX + localZ * localZ));

        this.targetyrot = (float) -yaw;

        this.targetxrot = (float) pitch;
        //LogUtils.getLogger().warn("X:"+worldXDirection+"Y:"+worldYDirection+"Z:"+worldZDirection+"target:"+targetPos+"turret:"+currentworldpos +"yaw:"+yaw+"pitch:"+pitch);
    }

    private void acquireTargetByAimType() {
        if(aimtype == 1) {
            tryFindTargetEntity();
        }
        if(aimtype == 2){
            tryFindTargetProjectile();
        }

    }

    public void tryFindTargetProjectile() {
        // 功能：索敌阶段不再改动开火冷却，避免冷却与索敌共用计数器导致抖动。
        if (targetprojectile != null && !targetprojectile.isRemoved()) return; // 有活目标就不重复找

        if ((level.getGameTime() + this.hashCode()) % 3 != 0) return;

        AABB searchBox = new AABB(
                currentworldpos.x - SEARCH_RADIUS,
                currentworldpos.y - SEARCH_RADIUS,
                currentworldpos.z - SEARCH_RADIUS,
                currentworldpos.x + SEARCH_RADIUS,
                currentworldpos.y + SEARCH_RADIUS,
                currentworldpos.z + SEARCH_RADIUS
        );
        List<Entity> candidates = level.getEntitiesOfClass(Entity.class, searchBox, this::isValidTargetProjectile);

        if (candidates.isEmpty()) {
            return;
        }

        targetprojectile = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z)))
                .orElse(null);
        // 关键：这里一定要同步更新 targetPos！！
        this.targetPos = new Vector3d(
                targetprojectile.getX(),
                targetprojectile.getY(),
                targetprojectile.getZ()
        );
        setChanged();
    }

    // 功能：统一判断“当前是否持有有效目标”。
    private boolean hasValidTarget() {
        return (aimtype == 1 && isValidTargetEntity(targetentity))
                || (aimtype == 2 && isValidTargetProjectile(targetprojectile));
    }

    private void updateCurrentTargetPos() {
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPos = new Vector3d(
                    targetentity.getX(),
                    targetentity.getY() + targetentity.getEyeHeight(),
                    targetentity.getZ()
            );
        }
        if (aimtype == 2 && isValidTargetProjectile(targetprojectile)) {
            LogUtils.getLogger().warn("find target projectile:"+targetprojectile.getDisplayName()+"at:"+targetPos);
            targetPos = new Vector3d(
                    targetprojectile.getX(),
                    targetprojectile.getY(),
                    targetprojectile.getZ()
            );
        }
    }

    private void appendTargetVelocitySample() {
        if (targetPreVelocity.size() >= 5) {
            targetPreVelocity.remove(0);
        }
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPreVelocity.add(new Vector3d(targetentity.getDeltaMovement().x, targetentity.getDeltaMovement().y, targetentity.getDeltaMovement().z));
        } else if (aimtype == 2 && isValidTargetProjectile(targetprojectile)) {
            targetPreVelocity.add(new Vector3d(targetprojectile.getDeltaMovement().x, targetprojectile.getDeltaMovement().y, targetprojectile.getDeltaMovement().z));
        }
    }

    private void fireWhenLocked() {
        if (aimtype == 1) {
            shootentity();
            idleTicks = getCoolDown();
        } else if (aimtype == 2) {
            interceptprojectile();
            idleTicks = getCoolDown();
        }
    }

    private boolean isflyingtowards(Entity e, Vec3 center) {
        // 速度阈值，可调（单位：方块/刻）
        double speed = e.getDeltaMovement().length();
        if (speed < 0.25) return false; // 太慢的直接忽略（比如漂浮的物品）

        // 计算是否朝船本身飞来
        Vec3 toEntity = e.position().subtract(center);
        double dot = e.getDeltaMovement().normalize().dot(toEntity.normalize());
        return dot < -0.3; // 越负说明越正对护盾飞来（-0.3~0.6 之间调节手感）
    }

    public abstract void interceptprojectile();

    @Override
    public Component getDisplayName() {
        return Component.literal("CIWS Screen");
    }
}
