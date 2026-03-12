package com.kodu16.vsie.content.weapon;

import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.RenderUtils;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AbstractWeaponBlockEntity extends SmartBlockEntity implements GeoBlockEntity, MenuProvider {
    // Constants

    //variables
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public WeaponData weaponData;//注意这个data不存固有属性比如射速射程，只存频道之类的
    public boolean hasInitialized;//防止莫名其妙的重置导致变砖
    @Getter
    private float raycastDistance = 513.0f;//武器的raycast和推进器不太一样，武器是射线检测目标的距离，如果是射弹武器也检测，但不会利用
    @Getter
    public Vec3 targetpos = new Vec3(0,0,0);
    public Vec3 weaponpos;
    public int currentTick = -1;
    public String weapontype = "";


    public abstract float getmaxrange(); //获取最大射程
    public abstract int getcooldown(); //每两次射击间最小间隔的tick数

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
        currentTick++;
        if (currentTick < getcooldown()) return;
        currentTick = getcooldown();
        this.raycastDistance = 0;
        if(!needtofire()) {
            getData().isfiring = false;
            return;
        }
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (hasInitialized)
        {
            currentTick = 0;
            getData().isfiring = true;
            BlockPos pos = this.getBlockPos();
            boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, pos);
            if (onShip) {
                weaponpos = VSGameUtilsKt.toWorldCoordinates(level, pos);
                fire();
            }
        }
    }

    public WeaponData getData() {
        if (weaponData == null) {
            weaponData = new WeaponData();
        }
        return weaponData;
    }

    public abstract void fire();

    public void receivechannel(int encode) {
        getData().receivingchannel = encode;
    }

    public void receivetarget(Ship ship) {
        getData().targetship = ship;
    }

    public void modifychannel(int type) {
        if (level == null || level.isClientSide) {
            return; // 客户端完全不许改！
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

        // 默认使用最大射程：当射线没有命中任何方块时，激光会显示为武器的最大长度。
        this.raycastDistance = effectiveMaxDistance;
        // 默认目标点设置为最大射程末端，便于保持客户端/服务端状态一致。
        this.targetpos = worldTo;

        // Perform raycast using world coordinates
        ClipContext.Fluid clipFluid = ClipContext.Fluid.ANY;
        ClipContext context = new ClipContext(worldFrom, worldTo, ClipContext.Block.COLLIDER, clipFluid, null);
        BlockHitResult hit = level.clip(context);

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();

            // 命中方块时，激光长度严格使用“武器位置 -> 命中位置”的实际距离。
            float distance = (float)worldFrom.distanceTo(hitPos);
            this.raycastDistance = Math.min(distance, effectiveMaxDistance);
            // 命中后将目标点改为真实命中点，用于后续爆炸等逻辑。
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

        Vec3 localFromCenter = Vec3.atLowerCornerWithOffset(worldPosition, 0.5, 0.5, 0.5);
        Vec3 localDisplacement = localDirectionVector.scale(maxRaycastDistance);

        Vec3 worldFrom;
        Vec3 worldDisplacement;

        boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, localBlockPos);

        if (onShip) {
            LoadedShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, localBlockPos);
            if (ship != null) {
                worldFrom = VSGameUtilsKt.toWorldCoordinates(ship, localFromCenter);

                Quaterniondc shipRotation = ship.getTransform().getShipToWorldRotation();
                Vector3d rotatedDisplacementJOML = new Vector3d();
                shipRotation.transform(localDisplacement.x, localDisplacement.y, localDisplacement.z, rotatedDisplacementJOML);
                worldDisplacement = new Vec3(rotatedDisplacementJOML.x, rotatedDisplacementJOML.y, rotatedDisplacementJOML.z);
            } else {
                worldFrom = localFromCenter;
                worldDisplacement = localDisplacement;
            }
        } else {
            worldFrom = localFromCenter;
            worldDisplacement = localDisplacement;
        }

        Vec3 worldTo = worldFrom.add(worldDisplacement);
        return new Pair<>(worldFrom, worldTo);
    }

    public Vec3 getWeaponPos() {
        BlockPos pos = this.getBlockPos();
        LoadedShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
        if(ship!=null) {return VSGameUtilsKt.toWorldCoordinates(level, pos);}
        else {return new Vec3(pos.getX(),pos.getY(), pos.getZ());}
    }

    @Override
    public double getTick(Object BlockEntity) {
        return RenderUtils.getCurrentTick();
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
        tag.putFloat("raycastDistance", this.getRaycastDistance());
        tag.putDouble("target_x",this.targetpos.x);
        tag.putDouble("target_y",this.targetpos.y);
        tag.putDouble("target_z",this.targetpos.z);
        tag.putBoolean("channel1",weaponData.getChannel1());
        tag.putBoolean("channel2",weaponData.getChannel2());
        tag.putBoolean("channel3",weaponData.getChannel3());
        tag.putBoolean("channel4",weaponData.getChannel4());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (this.weaponData == null) {
            this.weaponData = new WeaponData();
        }
        if (tag.contains("raycastDistance", CompoundTag.TAG_FLOAT)) {this.raycastDistance = tag.getFloat("raycastDistance");}
        if(tag.contains("target_x") && tag.contains("target_y") && tag.contains("target_z")) {this.targetpos = new Vec3(tag.getDouble("target_x"), tag.getDouble("target_y"), tag.getDouble("target_z"));}
        if (tag.contains("channel1")) {weaponData.setChannel1(tag.getBoolean("channel1"));}
        if (tag.contains("channel2")) {weaponData.setChannel2(tag.getBoolean("channel2"));}
        if (tag.contains("channel3")) {weaponData.setChannel3(tag.getBoolean("channel3"));}
        if (tag.contains("channel4")) {weaponData.setChannel4(tag.getBoolean("channel4"));}
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
