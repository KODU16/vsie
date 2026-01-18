package com.kodu16.vsie.content.weapon;

import com.kodu16.vsie.content.thruster.ThrusterData;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.valkyrienskies.core.api.ships.LoadedShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AbstractWeaponBlockEntity extends SmartBlockEntity {
    // Constants
    public ServerShip ship = VSGameUtilsKt.getShipManagingPos((ServerLevel) level, getBlockPos());

    public WeaponData weaponData;//注意这个data不存固有属性，只存频道之类的
    public boolean hasInitialized = false;//防止莫名其妙的重置导致变砖
    private float raycastDistance = 0.0f;//武器的raycast和推进器不太一样，武器是射线检测目标的距离，如果是射弹武器也检测，但不会利用
    private Vec3 targetpos = new Vec3(0,0,0);
    private int currentTick = -1;


    public abstract float getmaxrange(); //获取最大射程
    public abstract int getcooldown(); //每两次射击间最小间隔的tick数

    public int channel; //指定开火分组

    public AbstractWeaponBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        weaponData = new WeaponData();
    }

    public WeaponData getData() {return weaponData;}

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    public float getRaycastDistance() {
        return raycastDistance;
    }

    public void tick() {
        super.tick();
        currentTick++;
        if (currentTick % getcooldown() != 0) return;

        // Reset tick counter to prevent overflow
        if (currentTick >= getcooldown()) {
            currentTick = 0;
        }
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        Logger LOGGER = LogUtils.getLogger();
        if (hasInitialized)
        {
            BlockPos pos = this.getBlockPos();
            boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, pos);
            if (onShip) {
                LoadedShip Ship = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
                if (Ship == null) return;
                performRaycast(level);
            }
        }
    }

    @SuppressWarnings("null")
    private void performRaycast(@Nonnull Level level) {
        BlockState state = this.getBlockState();
        BlockPos currentBlockPos = this.getBlockPos();

        Direction facingDirection = state.getValue(AbstractWeaponBlock.FACING);
        Vec3 localDirectionVector = new Vec3(facingDirection.step());

        float effectiveMaxDistance = getmaxrange();

        Pair<Vec3, Vec3> raycastPositions = calculateRaycastPositions(currentBlockPos, localDirectionVector, effectiveMaxDistance);
        Vec3 worldFrom = raycastPositions.getFirst();
        Vec3 worldTo = raycastPositions.getSecond();

        // Perform raycast using world coordinates
        ClipContext.Fluid clipFluid = ClipContext.Fluid.ANY;
        ClipContext context = new ClipContext(worldFrom, worldTo, ClipContext.Block.COLLIDER, clipFluid, null);
        BlockHitResult hit = level.clip(context);

        // Calculate power based on world distance
        float distance = effectiveMaxDistance;
        BlockPos hitBlockPos = null;

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();
            hitBlockPos = hit.getBlockPos();

            distance = (float)worldFrom.distanceTo(hitPos);
            distance = Math.min(distance, effectiveMaxDistance);
        }

        updateRaycast(level, state, distance, worldTo);
    }

    private Pair<Vec3, Vec3> calculateRaycastPositions(BlockPos localBlockPos, Vec3 localDirectionVector, float maxRaycastDistance) {
        Level level = getLevel();

        Vec3 localFromCenter = getStartingPoint(localDirectionVector);
        Vec3 localDisplacement = localDirectionVector.scale(maxRaycastDistance);

        Vec3 worldFrom;
        Vec3 worldDisplacement;

        boolean onShip = VSGameUtilsKt.isBlockInShipyard(level, localBlockPos);

        if (onShip) {
            LoadedShip ship = VSGameUtilsKt.getShipObjectManagingPos(level, localBlockPos);
            if (ship != null && ship.getTransform() != null) {
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

    protected Vec3 getStartingPoint(Vec3 directionVec) {
        Vec3 blockCenter = Vec3.atLowerCornerWithOffset(worldPosition, 0.5, 0.5, 0.5);
        return blockCenter;
    }

    private void updateRaycast(@Nonnull Level level, @Nonnull BlockState state, float distance, Vec3 worldTo) {
        if (Math.abs(this.raycastDistance - distance) > 0.01f) {
            this.raycastDistance = distance;
            this.targetpos = worldTo;
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(this.worldPosition, state, state, 3);
            }
        }
    }

    // Networking and nbt

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
        tag.putFloat("raycastDistance", this.raycastDistance);
        tag.putDouble("targetx",this.targetpos.x);
        tag.putDouble("targety",this.targetpos.y);
        tag.putDouble("targetz",this.targetpos.z);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);

        if (tag.contains("raycastDistance", CompoundTag.TAG_FLOAT)) {
            this.raycastDistance = tag.getFloat("raycastDistance");
        } else {
            this.raycastDistance = getmaxrange();
        }
        if (tag.contains("targetx", CompoundTag.TAG_DOUBLE)
                && tag.contains("targety", CompoundTag.TAG_DOUBLE)
                && tag.contains("targetz", CompoundTag.TAG_DOUBLE)) {
            this.targetpos = new Vec3(tag.getDouble("targetx"),tag.getDouble("targety"),tag.getDouble("targetz"));

        }
    }
}
