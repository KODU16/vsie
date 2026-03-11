package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.foundation.Vec;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.network.SerializableDataTicket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.util.RenderUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;

public abstract class AbstractTurretBlockEntity extends SmartBlockEntity implements GeoBlockEntity, MenuProvider {
    Logger LOGGER = LogUtils.getLogger();
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    public static SerializableDataTicket<Boolean> HAS_TARGET;
    public Vector3d targetPos = new Vector3d(0,0,0); //这是被选择的那个目标的位置
    public static SerializableDataTicket<Float> XROT; //这是动画计算用的
    public static SerializableDataTicket<Float> YROT;
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public double targetdistance;
    public @Nullable LivingEntity targetentity;
    private @Nullable Ship selectedtargetShip;
    public int aimtype = 0; //0：空 1：实体 2：船只
    public List<Vector3d> targetPreVelocity = new ArrayList<Vector3d>();
    public float xRot0 = 0;
    public float yRot0 = 0;
    public float prevxrot = 0;
    public float prevyrot = 0;
    public boolean xOK = false;
    public boolean yOK = false;
    public float targetxrot = 0;
    public float targetyrot = 0;
    public int idleTicks = 0;
    // 功能：记录炮口火焰剩余显示时间（单位：tick），用于实现“开火后延迟熄灭”效果。
    public int muzzleFlashTicks = 0;
    private static final double SEARCH_RADIUS = 128.0;
    public int defaultspinx = 0;
    public int defaultspiny = 0;
    public boolean onShip = false;
    public Vector3d currentworldpos = new Vector3d(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
    protected TurretData turretData;

    public Vector3d worldXDirection = new Vector3d();
    public Vector3d worldYDirection = new Vector3d();
    public Vector3d worldZDirection = new Vector3d();

    protected AbstractTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 初始化 turretData
        this.turretData = new TurretData();
    }


    public TurretData getData() {
        if (turretData == null) {
            turretData = new TurretData();
        }
        return turretData;
    }

    public double getTargetdistance() {return targetdistance;}

    public void modifytargettype(int type) {
        if (level == null || level.isClientSide) {
            return; // 客户端完全不许改！
        }
        TurretData data = getData(); // 使用防护性方法
        if(type==4){
            this.aimtype = 2;
            data.setTargetsShip(!data.getTargetsShip());
            if (data.getTargetsShip()){
                data.setTargetsHostile(false);
                data.setTargetsPassive(false);
                data.setTargetsPlayers(false);
            }
        }
        else{
            this.aimtype = 1;
            if(type==1){
                data.setTargetsHostile(!data.getTargetsHostile());
            }
            if(type==2){
                data.setTargetsPassive(!data.getTargetsPassive());
            }
            if(type==3){
                data.setTargetsPlayers(!data.getTargetsPlayers());
            }
        }
        if (!data.getTargetsHostile() && !data.getTargetsPassive() && !data.getTargetsPlayers() && !data.getTargetsShip())
            this.aimtype = 0;
        else if (data.getTargetsHostile() || data.getTargetsPassive() || data.getTargetsPlayers()){
            data.setTargetsShip(false);
        }
    }

    public void modifydefaultspin(int spinx, int spiny) {
        this.defaultspinx = spinx;
        this.defaultspiny = spiny;
    }

    public void tick() {
        // 功能：冷却时间仅用于禁止开火，不再阻断索敌与转向逻辑。
        if (idleTicks > 0) {
            idleTicks = idleTicks - 1;
        }
        // 功能：每 tick 衰减炮口火焰显示计时，计时结束后自动隐藏火焰层。
        if (muzzleFlashTicks > 0) {
            muzzleFlashTicks = muzzleFlashTicks - 1;
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

    // 功能：根据方块是否在船上，更新炮塔在世界中的实际发射原点。
    public void refreshWorldPosition() {
        onShip = VSGameUtilsKt.isBlockInShipyard(level, this.getBlockPos());
        if (onShip) {
            Ship ship = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
            Vector3d center = VSGameUtilsKt.toWorldCoordinates(ship, this.getBlockPos().getX(), this.getBlockPos().getY() + getYAxisOffset(), this.getBlockPos().getZ());
            currentworldpos = new Vector3d(center.x, center.y, center.z);
            return;
        }
        currentworldpos = new Vector3d(this.getBlockPos().getX(), this.getBlockPos().getY() + getYAxisOffset(), this.getBlockPos().getZ());
    }

    // 功能：按当前索敌模式尝试获取目标，避免在 tick 主逻辑中散落多层 if。
    private void acquireTargetByAimType() {
        if (aimtype == 1) {
            tryFindTargetEntity();
        } else if (aimtype == 2 && !getData().enemyshipsData.isEmpty()) {
            tryFindtargetShip();
        }
    }

    // 功能：统一判断“当前是否持有有效目标”。
    private boolean hasValidTarget() {
        return (aimtype == 1 && isValidTargetEntity(targetentity))
                || (aimtype == 2 && isValidTargetShip(selectedtargetShip));
    }

    // 功能：维护最多 5 条目标速度历史，供预测弹道时使用。
    private void appendTargetVelocitySample() {
        if (targetPreVelocity.size() >= 5) {
            targetPreVelocity.remove(0);
        }
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPreVelocity.add(new Vector3d(targetentity.getDeltaMovement().x, targetentity.getDeltaMovement().y, targetentity.getDeltaMovement().z));
        } else if (aimtype == 2 && isValidTargetShip(selectedtargetShip)) {
            targetPreVelocity.add(new Vector3d(selectedtargetShip.getVelocity()));
        }
    }

    // 功能：根据目标类型更新当前瞄准点，供后续弹道预测与旋转计算使用。
    private void updateCurrentTargetPos() {
        if (aimtype == 1 && isValidTargetEntity(targetentity)) {
            targetPos = new Vector3d(
                    targetentity.getX(),
                    targetentity.getY(),
                    targetentity.getZ()
            );
            return;
        }
        if (aimtype == 2 && isValidTargetShip(selectedtargetShip)) {
            targetPos = (Vector3d) selectedtargetShip.getTransform().getPositionInWorld();
        }
    }

    // 功能：在炮口完成对准时触发开火，并设置统一冷却。
    private void fireWhenLocked() {
        // 功能：冷却期间允许继续索敌与旋转，但禁止重复开火。
        if (idleTicks > 0) {
            return;
        }
        if (aimtype == 1) {
            targetdistance = Vec.Distance(currentworldpos, targetPos);
            shootentity();
            idleTicks = getCoolDown();
            // 功能：实体目标开火后保持 0.5 秒炮口火焰显示（20tick/s * 0.5s = 10tick）。
            muzzleFlashTicks = 10;
        } else if (aimtype == 2) {
            targetdistance = Vec.Distance(currentworldpos, targetPos);
            shootship();
            idleTicks = getCoolDown();
            // 功能：舰船目标开火后同样保持 0.5 秒炮口火焰显示。
            muzzleFlashTicks = 10;
        }
    }

    // 功能：提供统一的炮口火焰可见判定，支持“开火后延时显示”。
    public boolean shouldRenderMuzzleFlash() {
        return muzzleFlashTicks > 0;
    }

    // 功能：无有效目标时将炮塔朝向平滑回归到默认角度（defaultxrot/defaultyrot）。
    public void returnToDefaultRotation() {
        this.targetxrot = this.defaultspinx;
        this.targetyrot = this.defaultspiny;
        this.xRot0 = closestReachableX(xRot0, getMaxSpinSpeed(), targetxrot*Mth.PI/180);
        this.yRot0 = closestReachableY(yRot0, getMaxSpinSpeed(), targetyrot*Mth.PI/180);
    }

    //use 5 ticks' velocity data to predict movement,providing more accurate prediction
    public abstract Vector3d getShootLocation(Vector3d vec, List<Vector3d> preV, Level lv, Vector3d pos);

    //船不是实体，需要一套单独的索敌逻辑，这也是为啥不能同时索敌实体和船

    public abstract String getturrettype();

    public abstract double getYAxisOffset();

    public abstract float getMaxSpinSpeed();

    public abstract int getCoolDown();

    public abstract int getenergypertick();

    public abstract void shootentity();

    public abstract void shootship();

    public void updateenemy(ArrayList<Ship> enemyshipsData) {
        this.getData().enemyshipsData = enemyshipsData;
    }

    private void tryInvalidateTarget() {
        if(aimtype==1) {
            if(!isValidTargetEntity(targetentity)) {
                setAnimData(HAS_TARGET, false);
                targetentity = null;
                targetdistance = 0;
                targetPreVelocity.clear();
            }
        }
        else if(aimtype==2) {
            if(!isValidTargetShip(selectedtargetShip)) {
                setAnimData(HAS_TARGET, false);
                selectedtargetShip = null;
                targetdistance = 0;
                targetPreVelocity.clear();
            }
        }
    }

    public void tryFindTargetEntity() {
        // 功能：索敌阶段不再改动开火冷却，避免冷却与索敌共用计数器导致抖动。
        if (targetentity != null && targetentity.isAlive()) return; // 有活目标就不重复找

        if ((level.getGameTime() + this.hashCode()) % 5 != 0) return;

        AABB searchBox = new AABB(
                currentworldpos.x - SEARCH_RADIUS,
                currentworldpos.y - SEARCH_RADIUS,
                currentworldpos.z - SEARCH_RADIUS,
                currentworldpos.x + SEARCH_RADIUS,
                currentworldpos.y + SEARCH_RADIUS,
                currentworldpos.z + SEARCH_RADIUS
        );

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox, this::isValidTargetEntity);

        if (candidates.isEmpty()) {
            return;
        }

        targetentity = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z)))
                .orElse(null);
        // 关键：这里一定要同步更新 targetPos！！
        this.targetPos = new Vector3d(
                targetentity.getX(),
                targetentity.getY(),
                targetentity.getZ()
        );
        setChanged();
    }


    public void tryFindtargetShip() {
        // 功能：索敌阶段不再改动开火冷却，避免冷却与索敌共用计数器导致抖动。
        ArrayList<Ship> enemylist = getData().enemyshipsData;
        if (enemylist.isEmpty()) return;
        if (isValidTargetShip(selectedtargetShip)) return;

        this.selectedtargetShip = enemylist.stream()
                .filter(this::isValidTargetShip)
                .min(Comparator.comparingDouble(ship -> {
                    Vector3d shipPos = new Vector3d(ship.getTransform().getPositionInWorld());
                    return currentworldpos.distanceSquared(shipPos);
                }))
                .orElse(null);

        if (this.selectedtargetShip != null) {
            // 功能：舰船目标改为“可见瞄准点”（优先可见外表面），避免目标点落在船体内部导致永远无法锁定。
            this.targetPos = getShipAimPoint(this.selectedtargetShip);
            setChanged();
        }
    }



    public boolean isValidTargetEntity(@Nullable LivingEntity e) {
        // 只负责实体判断，输入的只有实体
        if (e == null) {
            return false;
        }
        if (!e.isAlive()) {
            return false;
        }
        MobCategory category = e.getType().getCategory();
        if (    getData().getTargetsHostile() && category.isFriendly() ||
                getData().getTargetsPassive() && !category.isFriendly() ||
                getData().getTargetsPlayers() && e instanceof Player player && player.isCreative()) {
            return false;
        }

        // 距离判断（用世界坐标）
        double distSq = e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        if (distSq > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }
        // 视线判断（眼睛位置更准）
        if (!canSeeTarget(new Vector3d(e.getX(), e.getY(), e.getZ()))) {
            return false;
        }
        return true;
    }


    private boolean isValidTargetShip(Ship ship) {
        if(ship == null) {
            return false;
        }
        Vector3d shippos = new Vector3d (ship.getTransform().getPositionInWorld());
        Vector3d pos = new Vector3d(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        double distance = Vec.Distance(pos, shippos);
        if(distance > 1280) {
            return false;
        }
        // 功能：舰船使用专门的可见性判定（多采样点），避免只检测质心时被船体自身遮挡。
        if(!canSeeShipTarget(ship)) {
            return false;
        }
        return true;
    }

    private boolean canSeeShipTarget(Ship ship) {
        // 功能：对舰船AABB的多个外表面点做视线检测，只要有一个可见点即判定可见。
        AABBdc worldAabb = ship.getWorldAABB();
        double centerX = (worldAabb.minX() + worldAabb.maxX()) * 0.5;
        double centerY = (worldAabb.minY() + worldAabb.maxY()) * 0.5;
        double centerZ = (worldAabb.minZ() + worldAabb.maxZ()) * 0.5;

        Vector3d[] samplePoints = new Vector3d[] {
                new Vector3d(centerX, centerY, centerZ),
                new Vector3d(worldAabb.minX(), centerY, centerZ),
                new Vector3d(worldAabb.maxX(), centerY, centerZ),
                new Vector3d(centerX, worldAabb.minY(), centerZ),
                new Vector3d(centerX, worldAabb.maxY(), centerZ),
                new Vector3d(centerX, centerY, worldAabb.minZ()),
                new Vector3d(centerX, centerY, worldAabb.maxZ())
        };

        for (Vector3d samplePoint : samplePoints) {
            if (canSeeTarget(samplePoint)) {
                return true;
            }
        }
        return false;
    }

    private Vector3d getShipAimPoint(Ship ship) {
        // 功能：返回一个优先可见的舰船瞄准点，减少炮塔在“不可见质心”上反复重选目标的抽搐。
        AABBdc worldAabb = ship.getWorldAABB();
        double centerX = (worldAabb.minX() + worldAabb.maxX()) * 0.5;
        double centerY = (worldAabb.minY() + worldAabb.maxY()) * 0.5;
        double centerZ = (worldAabb.minZ() + worldAabb.maxZ()) * 0.5;

        Vector3d[] samplePoints = new Vector3d[] {
                new Vector3d(centerX, worldAabb.maxY(), centerZ),
                new Vector3d(worldAabb.minX(), centerY, centerZ),
                new Vector3d(worldAabb.maxX(), centerY, centerZ),
                new Vector3d(centerX, centerY, worldAabb.minZ()),
                new Vector3d(centerX, centerY, worldAabb.maxZ()),
                new Vector3d(centerX, centerY, centerZ)
        };

        for (Vector3d samplePoint : samplePoints) {
            if (canSeeTarget(samplePoint)) {
                return samplePoint;
            }
        }

        return new Vector3d(centerX, centerY, centerZ);
    }

    private boolean canSeeTarget(Vector3d pos) {

        Vec3 turretpos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        // 功能：移除坐标四舍五入，避免视线判断在边界处抖动导致炮塔抽搐。
        Vec3 targetPos = new Vec3(pos.x(), pos.y(), pos.z());
        Vec3 lookVec = turretpos.vectorTo(targetPos).normalize().scale(0.75F);
        ClipContext ctx = new ClipContext(turretpos.add(lookVec), targetPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, null);
        return level.clip(ctx).getType().equals(HitResult.Type.MISS);
    }

    @Override
    public double getTick(Object BlockEntity) {
        return RenderUtils.getCurrentTick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 确保 turretData 被正确初始化
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        markUpdated();
    }

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        //if(!this.level.isClientSide()) sendUpdatePacket();
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


    @Override
    public Component getDisplayName() {
        return Component.literal("Turret Screen");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TurretContainerMenu(containerId, inv, this);
    }

    public static int getRandomInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        write(tag, true);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        read(tag, true);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt("aimtype", aimtype);
        tag.putBoolean("hostile",turretData.getTargetsHostile());
        tag.putBoolean("passive",turretData.getTargetsPassive());
        tag.putBoolean("player",turretData.getTargetsPlayers());
        tag.putBoolean("ship",turretData.getTargetsShip());
        tag.putDouble("distance", this.getTargetdistance());
        tag.putFloat("xrot",this.targetxrot);
        tag.putFloat("yrot",this.targetyrot);
        tag.putInt("defaultxrot",this.defaultspinx);
        tag.putInt("defaultyrot",this.defaultspiny);
        // 功能：同步炮口火焰剩余时间到客户端，确保渲染层可按时显示/熄灭。
        tag.putInt("muzzleFlashTicks", this.muzzleFlashTicks);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        // 确保 turretData 不为 null
        if (this.turretData == null) {
            this.turretData = new TurretData();
        }
        if (tag.contains("aimtype")) {this.aimtype = tag.getInt("aimtype");}
        if (tag.contains("hostile")) {turretData.setTargetsHostile(tag.getBoolean("hostile"));}
        if (tag.contains("passive")) {turretData.setTargetsPassive(tag.getBoolean("passive"));}
        if (tag.contains("player")) {turretData.setTargetsPlayers(tag.getBoolean("player"));}
        if (tag.contains("ship")) {turretData.setTargetsShip(tag.getBoolean("ship"));}
        if (tag.contains("distance")) {this.targetdistance = tag.getDouble("distance");}
        if (tag.contains("xrot")) {this.targetxrot = tag.getFloat("xrot");}
        if (tag.contains("yrot")) {this.targetyrot = tag.getFloat("yrot");}
        if (tag.contains("defaultyrot")) {this.defaultspiny = tag.getInt("defaultyrot");}
        if (tag.contains("defaultxrot")) {this.defaultspinx = tag.getInt("defaultxrot");}
        if (tag.contains("muzzleFlashTicks")) {this.muzzleFlashTicks = tag.getInt("muzzleFlashTicks");}
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public float closestReachableX(float current, float maxChange, float target) {
        // 先把 target 拉到 current ±180° 范围内
        float delta = target - current;
        delta = (delta + Mth.PI) % (Mth.TWO_PI) - Mth.PI;  // -π ~ +π

        float minAllowed = -maxChange;
        float maxAllowed = maxChange;

        float move;
        if (delta < minAllowed) {
            move = minAllowed;
            this.xOK = false;
        } else if (delta > maxAllowed) {
            move = maxAllowed;
            this.xOK = false;
        } else {
            move = delta;
            this.xOK = true;
        }

        return current + move;
    }

    public float closestReachableY(float current, float maxChange, float target) {
        // 先把 target 拉到 current ±180° 范围内
        float delta = target - current;
        delta = (delta + Mth.PI) % (Mth.TWO_PI) - Mth.PI;  // -π ~ +π

        float minAllowed = -maxChange;
        float maxAllowed = maxChange;

        float move;
        if (delta < minAllowed) {
            move = minAllowed;
            this.yOK = false;
        } else if (delta > maxAllowed) {
            move = maxAllowed;
            this.yOK = false;
        } else {
            move = delta;
            this.yOK = true;
        }

        return current + move;
    }
}
