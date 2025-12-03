package com.kodu16.vsie.content.turret;

import com.kodu16.vsie.content.turret.server.TurretContainerMenu;
import com.kodu16.vsie.foundation.Vec;
import com.kodu16.vsie.utility.AttachmentUtils;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import mekanism.api.providers.IBlockProvider;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.primitives.AABBdc;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
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

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;

public abstract class AbstractTurretBlockEntity extends SmartBlockEntity implements GeoBlockEntity, MenuProvider {
    Logger LOGGER = LogUtils.getLogger();
    @WrappingComputerMethod(wrapper = SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    EnergyInventorySlot energySlot;
    public static SerializableDataTicket<Boolean> HAS_TARGET;
    Map<String, Object> shipmapper = new HashMap<>();
    Map<String, Object> shipattr = new HashMap<>();
    public Vector3d targetPos = new Vector3d(0,0,0); //这是被选择的那个目标的位置
    public static SerializableDataTicket<Double> TARGET_POS_X; //这是列表
    public static SerializableDataTicket<Double> TARGET_POS_Y;
    public static SerializableDataTicket<Double> TARGET_POS_Z;
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    public double targetdistance;
    public @Nullable LivingEntity targetentity;
    public @Nullable ServerShip targetShip;
    private int aimtype = 0; //0：空 1：实体 2：船只
    private List<Vector3d> targetPreVelocity = new ArrayList<Vector3d>();
    public float xRot0 = 0;
    public float yRot0 = 0;
    private int coolDown = 0;
    private int alignmentDelay = 0;  // 专门负责“对准后必须再等15tick”
    private int idleTicks = 0;
    private static final double SEARCH_RADIUS = 100.0;
    public boolean onShip = false;
    public Vector3d currentworldpos = new Vector3d(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
    protected TurretData turretData;
    private boolean hasInitialized = false;//值得被写入abstract类被所有人学习！

    protected AbstractTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 初始化 turretData
        this.turretData = new TurretData();
        this.hasInitialized = false;
    }


    public TurretData getData() {
        if (turretData == null) {
            turretData = new TurretData();
        }
        return turretData;
    }

    public double getTargetdistance() {return targetdistance;}

    public Vector3d getTargetPos() {return targetPos;}

    public Vector3d getTurretPos() {return currentworldpos;}

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
    }

    public void tick() {
        if (!level.isClientSide) {
            //LOGGER.warn("targets hostile: " + turretData.getTargetsHostile());
        }
        onShip = VSGameUtilsKt.isBlockInShipyard(level, this.getBlockPos());
        if (onShip) {
            LoadedShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, this.getBlockPos());
            Vec3 center = VectorConversionsMCKt.toMinecraft(ship.getTransform().getShipToWorld().transformPosition(VectorConversionsMCKt.toJOML(getBlockPos().getCenter())));
            currentworldpos = new Vector3d(center.x, center.y+getYAxisOffset(), center.z);
        }
        else {
            currentworldpos = new Vector3d(this.getBlockPos().getX(), this.getBlockPos().getY()+getYAxisOffset(), this.getBlockPos().getZ());
        }
        LOGGER.warn("turret pos: " + currentworldpos + " aimtype: " + this.aimtype);
        tryInvalidateTarget();
        if(aimtype == 1)
            tryFindTargetEntity();
        if(aimtype == 2)
            tryFindtargetShip();
        if(aimtype!=0) {
            LOGGER.warn("targeting entity: " + targetentity);
            if (targetPreVelocity.size()>=5){
                targetPreVelocity.remove(0);
            }
            if (aimtype==1 && isValidTargetEntity(targetentity)) {
                targetPreVelocity.add(new Vector3d(targetentity.getDeltaMovement().x, targetentity.getDeltaMovement().y, targetentity.getDeltaMovement().z));
            }
            if (aimtype==2 && isValidTargetShip(targetShip)) {
                targetPreVelocity.add(new Vector3d(targetShip.getVelocity()));
            }
            if(aimtype==1 && isValidTargetEntity(targetentity) || aimtype==2 && isValidTargetShip(targetShip)) {
                targetPos = new Vector3d(
                        targetentity.getX(),
                        targetentity.getY() + targetentity.getEyeHeight(),
                        targetentity.getZ()
                );
                targetPos = getShootLocation(targetPos, targetPreVelocity, level, currentworldpos);
                targetdistance = Vec.Distance(currentworldpos, targetPos);
                setAnimData(TARGET_POS_X, targetPos.x);
                setAnimData(TARGET_POS_Y, targetPos.y);
                setAnimData(TARGET_POS_Z, targetPos.z);
                setAnimData(HAS_TARGET, targetPos != null);

                // 1. 先处理射击后强制冷却
                if (coolDown > 0) {
                    coolDown--;
                    alignmentDelay = 0;  // 冷却期间，延迟计时直接清零，防止误射
                }

                // 2. 冷却结束了，且当前有有效目标
                if (coolDown <= 0) {
                    if (alignmentDelay > 0) {
                        alignmentDelay--;  // 正在稳定对准倒计时
                    } else {
                        // 第一次进入“冷却结束 + 有目标”的状态，开始15tick倒计时
                        alignmentDelay = 15;
                    }
                }

                // 3. 只有当 coolDown 结束 且 alignmentDelay 也倒计时完毕，才开火
                if (coolDown <= 0 && alignmentDelay == 1) {  // 倒数到1的那一tick开火（或用 <=0 看你习惯）
                    shoot();
                    coolDown = 10;       // 根据炮的射速设置，比如 20~60
                    alignmentDelay = 0;  // 重置
                }
            }
            else {
                alignmentDelay = 0;  // 关键！失去目标就立刻重置延迟计时
                targetdistance = 0;
                setAnimData(HAS_TARGET, false);
            }
        }
        this.markUpdated();
    }

    //use 5 ticks' velocity data to predict movement,providing more accurate prediction
    public abstract Vector3d getShootLocation(Vector3d vec, List<Vector3d> preV, Level lv, Vector3d pos);

    //船不是实体，需要一套单独的索敌逻辑，这也是为啥不能同时索敌实体和船

    public abstract String getturrettype();

    public abstract double getYAxisOffset();

    public abstract void shoot();

    public void tryInvalidateTarget() {
        if(aimtype==1) {
            if(!isValidTargetEntity(targetentity)) {
                setAnimData(HAS_TARGET, false);
                targetentity = null;
                targetPreVelocity.clear();
            }
        }
        else if(aimtype==2) {
            if(!isValidTargetShip(targetShip)) {
                setAnimData(HAS_TARGET, false);
                targetShip = null;
                targetPreVelocity.clear();
            }
        }
    }

    private void tryFindTargetEntity() {
        if (idleTicks-- > 0) return;
        if (targetentity != null && targetentity.isAlive()) return; // 有活目标就不重复找

        if ((level.getGameTime() + this.hashCode()) % 3 != 0) return;

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
            idleTicks = 80;
            return;
        }

        targetentity = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z)))
                .orElse(null);
        if (targetentity != null) {
            // 关键：这里一定要同步更新 targetPos！！
            this.targetPos = new Vector3d(
                    targetentity.getX(),
                    targetentity.getY() + targetentity.getEyeHeight(),
                    targetentity.getZ()
            );
            LOGGER.info("成功锁定目标: {}", targetentity);
            setChanged();
        } else {
            idleTicks = 20;
        }
    }


    private void tryFindtargetShip() {
        if(idleTicks-- > 0) {
            return;
        }
        if (targetShip == null && (level.getGameTime() + this.hashCode()) % 3 == 0) {
            int scope = 256;
            Map<String, Double> cmap = getCoordinate();
            BlockPos startBlockPos = new BlockPos((int) (Math.floor(cmap.get("x")) + scope), (int) (Math.floor(cmap.get("y")) + scope), (int) (Math.floor(cmap.get("z")) + scope));
            BlockPos endBlockPos = new BlockPos((int) (cmap.get("x") - scope), (int) (Math.floor(cmap.get("y")) - scope), (int) (Math.floor(cmap.get("z")) - scope));
            QueryableShipData<Ship> qsd = VSGameUtilsKt.getAllShips(level);
            try {
                qsd.iterator().forEachRemaining(e -> {
                    AABBdc p = e.getWorldAABB();
                    double[] c = getAABBdcCenter(p);
                    AABB aabb = new AABB(startBlockPos, endBlockPos);
                    BlockPos blockPos = new BlockPos((int) Math.floor(c[0]), (int) Math.floor(c[1]), (int) Math.floor(c[2]));
                    boolean filterFlag = level.canSeeSky(blockPos);

                    if (filterFlag && aabb.contains(c[0], c[1], c[2])) {
                        // Check if the ship ID already exists in the shipmapper
                        if (!shipmapper.containsKey(String.valueOf(e.getId()))) {
                            shipattr.put("id", e.getId());
                            shipattr.put("slug", e.getSlug());
                            shipattr.put("dimension", e.getChunkClaimDimension());
                            shipattr.put("x", c[0]);
                            shipattr.put("y", c[1]);
                            shipattr.put("z", c[2]);
                            AABBdc box = e.getWorldAABB();
                            shipattr.put("max_x", box.maxX());
                            shipattr.put("max_y", box.maxY());
                            shipattr.put("max_z", box.maxZ());
                            shipattr.put("min_x", box.minX());
                            shipattr.put("min_y", box.minY());
                            shipattr.put("min_z", box.minZ());
                            shipmapper.put(String.valueOf(e.getId()), shipattr);
                        }
                    }
                });
            } catch (RuntimeException ex) {
                // Handle exceptions, if any
            }
        }
        if (targetShip != null) {
            // 获取船只的速度
            Vector3d velocity = new Vector3d(targetShip.getVelocity().x(), targetShip.getVelocity().y(), targetShip.getVelocity().z());

            // 使用5个时刻的速度数据来预测船只位置
            targetPos = getShootLocation(new Vector3d(targetShip.getTransform().getPositionInWorld().x(), targetShip.getTransform().getPositionInWorld().y(), targetShip.getTransform().getPositionInWorld().z()), targetPreVelocity, level, currentworldpos);

            // 更新射击目标位置
            setAnimData(TARGET_POS_X, targetPos.x);
            setAnimData(TARGET_POS_Y, targetPos.y);
            setAnimData(TARGET_POS_Z, targetPos.z);
        }
    }



    private boolean isValidTargetEntity(@Nullable LivingEntity e) {
        // 必须第一行就判断 null，否则 100% 崩溃
        if (e == null) {
            return false;
        }
        if (!e.isAlive()) {
            return false;
        }
        // 如果你还想排除玩家创造模式、旁观者等
        if (e.isInvulnerable() || e.isSpectator() || (e instanceof Player player && player.isCreative())) {
            return false;
        }

        // 距离判断（用世界坐标）
        double distSq = e.distanceToSqr(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        if (distSq > SEARCH_RADIUS * SEARCH_RADIUS) {
            return false;
        }

        // 视线判断（眼睛位置更准）
        if (!canSeeTarget(new Vector3d(e.getX(), e.getY() + e.getEyeHeight(), e.getZ()))) {
            return false;
        }

        // 如果你还保留了原来的 canBeSeenAsEnemy 判断（可选）
        // if (!e.canBeSeenAsEnemy()) return false;

        return true;
    }


    private boolean isValidTargetShip(ServerShip ship) {
        if(ship == null) {
            return false;
        }
        Vector3d shippos = new Vector3d (ship.getTransform().getPositionInWorld());
        Vector3d pos = new Vector3d(currentworldpos.x, currentworldpos.y, currentworldpos.z);
        double distance = Vec.Distance(pos, shippos);
        if(distance > 128) {
            return false;
        }
        if(!canSeeTarget(pos)) {
            return false;
        }
        return true;
    }

    private boolean canSeeTarget(Vector3d pos) {
        Vec3 turretpos = new Vec3(currentworldpos.x, currentworldpos.y, currentworldpos.z);
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

    private double[] getAABBdcCenter(AABBdc aabb) {
        double width = aabb.maxX() - aabb.minX();
        double len = aabb.maxZ() - aabb.minZ();
        double hight = aabb.maxY() - aabb.minY();
        double centerX = aabb.minX() + width / 2;
        double centerY = aabb.minY() + hight / 2;
        double centerZ = aabb.minZ() + len / 2;
        return new double[]{centerX, centerY, centerZ};
    }

    private Map<String, Double> getCoordinate()
    {
        Map<String, Double> map = new HashMap<>();
        BlockPos pos = this.getBlockPos();
        boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, pos);
        if (onShip) {
            ServerShip ship = AttachmentUtils.getShipAt((ServerLevel) level, pos);
            Vector3d v3d = VSGameUtilsKt.toWorldCoordinates(ship, pos);
            map.put("x", v3d.x);
            map.put("y", v3d.y);
            map.put("z", v3d.z);
        } else {
            map.put("x", (double) pos.getX());
            map.put("y", (double) pos.getY());
            map.put("z", (double) pos.getZ());
        }
        return map;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Turret_Screen");
    }

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new TurretContainerMenu(containerId, inv, this);
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
        if (tag.contains("distance")) {this.targetdistance = tag.getDouble("distance");}
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE)
                .triggerableAnim("shoot", SHOOT_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
