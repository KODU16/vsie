package com.kodu16.vsie.content.weapon;

import com.kodu16.vsie.content.cooldown.FireCooldown;
import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.slf4j.Logger;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.RenderUtil;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AbstractWeaponBlockEntity extends SmartBlockEntity implements GeoBlockEntity, MenuProvider {
    // Constants

    //variables
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public WeaponData weaponData;//娉ㄦ剰杩欎釜data涓嶅瓨鍥烘湁灞炴€ф瘮濡傚皠閫熷皠绋嬶紝鍙瓨棰戦亾涔嬬被鐨?
    public boolean hasInitialized;//闃叉鑾悕鍏跺鐨勯噸缃鑷村彉鐮?
    private float raycastDistance = 513.0f;//姝﹀櫒鐨剅aycast鍜屾帹杩涘櫒涓嶅お涓€鏍凤紝姝﹀櫒鏄皠绾挎娴嬬洰鏍囩殑璺濈锛屽鏋滄槸灏勫脊姝﹀櫒涔熸娴嬶紝浣嗕笉浼氬埄鐢?
    public Vec3 targetpos = new Vec3(0,0,0);
    public Vec3 weaponpos;
    protected Vec3 raycastStart = Vec3.ZERO;
    protected Vec3 raycastEnd = Vec3.ZERO;
    private boolean raycastHit = false;
    public int currentTick = -1;
    protected int fireCooldownValue = -1;
    public String weapontype = "";

    public float getRaycastDistance() {
        return raycastDistance;
    }

    public Vec3 getTargetpos() {
        return targetpos;
    }

    public boolean hasRaycastHit() {
        return raycastHit;
    }


    public abstract float getmaxrange(); //鑾峰彇鏈€澶у皠绋?
    public abstract int getcooldown(); //姣忎袱娆″皠鍑婚棿鏈€灏忛棿闅旂殑tick鏁?

    public FireCooldown getFireCooldown() {
        return FireCooldown.cool1(getcooldown());
    }

    public String getweapontype() {
        return null;
    }

    public AbstractWeaponBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        this.weaponData = new WeaponData();
        this.hasInitialized = true;
    }


    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public void tick() {
        super.tick();
        this.raycastDistance = 0;
        boolean fireRequested = needtofire();
        tickFireCooldown(fireRequested);
        if(!fireRequested) {
            getData().isfiring = false;
            return;
        }
        if (!isFireCooldownReady()) {
            return;
        }
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (hasInitialized)
        {
            consumeFireCooldown();
            getData().isfiring = true;
            BlockPos pos = this.getBlockPos();
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,pos);;
            if (subLevel!=null) {
                weaponpos = ServerShipUtils.getBlockCenterWorld(subLevel, pos);
            } else {
                // Function: weapons placed in the normal level still need to fire and use normal-world coordinates.
                weaponpos = Vec3.atCenterOf(pos);
            }
            fire();
        }
    }

    public WeaponData getData() {
        if (weaponData == null) {
            weaponData = new WeaponData();
        }
        return weaponData;
    }

    public abstract void fire();

    protected void tickFireCooldown(boolean fireRequested) {
        FireCooldown cooldown = getFireCooldown();
        currentTick = Math.min(currentTick + 1, cooldown.intervalTicks());
        ensureFireCooldownValue(cooldown);
        if (cooldown.usesValue() && !fireRequested) {
            fireCooldownValue = Math.min(cooldown.maxValue(), fireCooldownValue + cooldown.recoveryPerTick());
        }
    }

    protected boolean isFireCooldownReady() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        return currentTick >= cooldown.intervalTicks() && (!cooldown.usesValue() || fireCooldownValue > 0);
    }

    protected void consumeFireCooldown() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        currentTick = 0;
        if (cooldown.usesValue()) {
            fireCooldownValue = Math.max(0, fireCooldownValue - 1);
        }
    }

    public int getCooldownHudValue() {
        FireCooldown cooldown = getFireCooldown();
        ensureFireCooldownValue(cooldown);
        return cooldown.usesValue() ? fireCooldownValue : currentTick;
    }

    public int getCooldownHudMax() {
        FireCooldown cooldown = getFireCooldown();
        return cooldown.usesValue() ? cooldown.maxValue() : cooldown.intervalTicks();
    }

    public boolean isCooldownHudRemaining() {
        return false;
    }

    private void ensureFireCooldownValue(FireCooldown cooldown) {
        if (!cooldown.usesValue()) {
            return;
        }
        if (fireCooldownValue < 0) {
            fireCooldownValue = cooldown.maxValue();
        } else if (fireCooldownValue > cooldown.maxValue()) {
            fireCooldownValue = cooldown.maxValue();
        }
    }

    public void receivechannel(int encode) {
        getData().receivingchannel = encode;
    }

    public void receivetarget(SubLevel subLevel) {
        getData().targetship = subLevel;
    }

    public void modifychannel(int type) {
        if (level == null || level.isClientSide) {
            return; // 瀹㈡埛绔畬鍏ㄤ笉璁告敼锛?
        }
        WeaponData data = getData();
        if(type==1){
            data.setChannel1(!data.getChannel1());
            if(data.channel1) {
                data.channel2 = false;
                data.channel3 = false;
                data.channel4 = false;
            }
        }
        if(type==2){
            data.setChannel2(!data.getChannel2());
            if(data.channel2) {
                data.channel1 = false;
                data.channel3 = false;
                data.channel4 = false;
            }
        }
        if(type==3){
            data.setChannel3(!data.getChannel3());
            if(data.channel3) {
                data.channel1 = false;
                data.channel2 = false;
                data.channel4 = false;
            }
        }
        if(type==4){
            data.setChannel4(!data.getChannel4());
            if(data.channel4) {
                data.channel1 = false;
                data.channel2 = false;
                data.channel3 = false;
            }
        }
    }

    public boolean needtofire() {
        boolean ans = false;
        for (int i = 0; i < 4; i++) {
            boolean flag = ((getData().receivingchannel >> i) &1) == 1;
            if (flag && i == 0 && getData().channel1) {
                ans = true;
                break;
            }
            if (flag && i == 1 && getData().channel2) {
                ans = true;
                break;
            }
            if (flag && i == 2 && getData().channel3) {
                ans = true;
                break;
            }
            if (flag && i == 3 && getData().channel4) {
                ans = true;
                break;
            }
        }
        return ans;
    }

    @SuppressWarnings("null")
    public void performRaycast(@Nonnull Level level) {
        if(!getData().isfiring) {return;}
        BlockState state = this.getBlockState();
        BlockPos currentBlockPos = this.getBlockPos();

        Direction facingDirection = state.getValue(AbstractWeaponBlock.FACING);
        Vec3 localDirectionVector = new Vec3(facingDirection.step());

        float effectiveMaxDistance = getmaxrange();

        Pair<Vec3, Vec3> raycastPositions = calculateRaycastPositions(currentBlockPos, localDirectionVector, effectiveMaxDistance);
        Vec3 worldFrom = raycastPositions.getFirst();
        Vec3 worldTo = raycastPositions.getSecond();
        this.raycastStart = worldFrom;
        this.raycastEnd = worldTo;

        // 榛樿浣跨敤鏈€澶у皠绋嬶細褰撳皠绾挎病鏈夊懡涓换浣曟柟鍧楁椂锛屾縺鍏変細鏄剧ず涓烘鍣ㄧ殑鏈€澶ч暱搴︺€?
        this.raycastDistance = effectiveMaxDistance;
        // 榛樿鐩爣鐐硅缃负鏈€澶у皠绋嬫湯绔紝渚夸簬淇濇寔瀹㈡埛绔?鏈嶅姟绔姸鎬佷竴鑷淬€?
        this.targetpos = worldTo;
        // Function: misses still use the max-range endpoint; this flag separates visual target from real block hit.
        this.raycastHit = false;

        // Perform raycast using world coordinates
        ClipContext.Fluid clipFluid = ClipContext.Fluid.ANY;
        ClipContext context = new ClipContext(worldFrom, worldTo, ClipContext.Block.COLLIDER, clipFluid, CollisionContext.empty());
        BlockHitResult hit = level.clip(context);

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();
            this.raycastHit = true;

            // 鍛戒腑鏂瑰潡鏃讹紝婵€鍏夐暱搴︿弗鏍间娇鐢ㄢ€滄鍣ㄤ綅缃?-> 鍛戒腑浣嶇疆鈥濈殑瀹為檯璺濈銆?
            float distance = (float)worldFrom.distanceTo(hitPos);
            this.raycastDistance = Math.min(distance, effectiveMaxDistance);
            // 鍛戒腑鍚庡皢鐩爣鐐规敼涓虹湡瀹炲懡涓偣锛岀敤浜庡悗缁垎鐐哥瓑閫昏緫銆?
            this.targetpos = hitPos;
            LogUtils.getLogger().warn("raycast pose from clip:"+this.targetpos);
        }
        setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    private Pair<Vec3, Vec3> calculateRaycastPositions(BlockPos localBlockPos, Vec3 localDirectionVector, float maxRaycastDistance) {
        Level level = getLevel();

        Vec3 localFromCenter = Vec3.atCenterOf(localBlockPos);
        Vec3 localDisplacement = localDirectionVector.scale(maxRaycastDistance);

        Vec3 worldFrom;
        Vec3 worldDisplacement;

        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,this.getBlockPos());;
        if (subLevel!=null) {
            Quaterniondc shipRotation = subLevel.logicalPose().orientation();
            Vector3d rotatedDisplacementJOML = new Vector3d();
            shipRotation.transform(localDisplacement.x, localDisplacement.y, localDisplacement.z, rotatedDisplacementJOML);
            worldFrom = subLevel.logicalPose().transformPosition(localFromCenter);
            worldDisplacement = new Vec3(rotatedDisplacementJOML.x, rotatedDisplacementJOML.y, rotatedDisplacementJOML.z);
        } else {
            worldFrom = localFromCenter;
            worldDisplacement = localDisplacement;
        }

        // Function: start slightly outside the weapon block so the ray does not collide with its own collider.
        if (worldDisplacement.lengthSqr() > 1.0E-6D) {
            worldFrom = worldFrom.add(worldDisplacement.normalize().scale(0.75D));
        }
        Vec3 worldTo = worldFrom.add(worldDisplacement);
        return new Pair<>(worldFrom, worldTo);
    }

    public Vec3 getRaycastStart() {
        return raycastStart;
    }

    public Vec3 getWeaponPos() {
        BlockPos pos = this.getBlockPos();
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,pos);;
        return ServerShipUtils.getBlockCenterWorld(subLevel, pos);
    }

    @Override
    public double getTick(Object BlockEntity) {
        return RenderUtil.getCurrentTick();
    }

    //menu

    @Override
    public @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new WeaponContainerMenu(containerId, inv, this);
    }

    // Networking and nbt

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.weaponData == null) {
            this.weaponData = new WeaponData();
        }
        markUpdated();
    }

    public void markUpdated() {
        this.setChanged();
        this.getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        //if(!this.level.isClientSide()) sendUpdatePacket();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return super.getUpdateTag(registries);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        read(tag, registries, true);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putFloat("raycastDistance", this.getRaycastDistance());
        tag.putDouble("target_x",this.targetpos.x);
        tag.putDouble("target_y",this.targetpos.y);
        tag.putDouble("target_z",this.targetpos.z);
        tag.putBoolean("channel1",weaponData.getChannel1());
        tag.putBoolean("channel2",weaponData.getChannel2());
        tag.putBoolean("channel3",weaponData.getChannel3());
        tag.putBoolean("channel4",weaponData.getChannel4());
        tag.putInt("fireCooldownValue", this.fireCooldownValue);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (this.weaponData == null) {
            this.weaponData = new WeaponData();
        }
        // 鍔熻兘锛氶€傞厤 1.21.1 NeoForge 鐨?NBT 绫诲瀷妫€鏌ュ父閲忥紝浣跨敤 Tag.TAG_FLOAT 璇诲彇娴偣灏勭嚎璺濈銆?
        if (tag.contains("raycastDistance", Tag.TAG_FLOAT)) {this.raycastDistance = tag.getFloat("raycastDistance");}
        if(tag.contains("target_x") && tag.contains("target_y") && tag.contains("target_z")) {this.targetpos = new Vec3(tag.getDouble("target_x"), tag.getDouble("target_y"), tag.getDouble("target_z"));}
        if (tag.contains("channel1")) {weaponData.setChannel1(tag.getBoolean("channel1"));}
        if (tag.contains("channel2")) {weaponData.setChannel2(tag.getBoolean("channel2"));}
        if (tag.contains("channel3")) {weaponData.setChannel3(tag.getBoolean("channel3"));}
        if (tag.contains("channel4")) {weaponData.setChannel4(tag.getBoolean("channel4"));}
        if (tag.contains("fireCooldownValue", Tag.TAG_INT)) {this.fireCooldownValue = tag.getInt("fireCooldownValue");}
    }

    //geckolib

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }
}
