package com.kodu16.vsie.content.weapon;

import com.kodu16.vsie.content.cooldown.FireCooldown;
import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.kodu16.vsie.foundation.LoadedChunkRaycast;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import com.kodu16.vsie.registries.vsieSounds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public abstract class AbstractWeaponBlockEntity extends SmartBlockEntity implements GeoBlockEntity, MenuProvider, IItemHandlerModifiable {
    // Constants
    private static final String AMMO_INVENTORY_TAG = "AmmoInventory";
    protected static final int DEFAULT_CONTROL_SEAT_ENERGY_COST_PER_TICK = 50;

    //variables
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public WeaponData weaponData;
    public boolean hasInitialized;
    private float raycastDistance = 0.0f;
    public Vec3 targetpos = new Vec3(0,0,0);
    public Vec3 weaponpos;
    protected Vec3 raycastStart = Vec3.ZERO;
    protected Vec3 raycastEnd = Vec3.ZERO;
    private boolean raycastHit = false;
    private float lastSyncedRaycastDistance = Float.NaN;
    private boolean lastSyncedRaycastHit = false;
    public int currentTick = -1;
    protected double fireCooldownValue = -1.0D;
    public String weapontype = "";
    // Function: every ordinary weapon gets a shared 9-slot ammo buffer; energy weapons reject all inserts.
    private final ItemStackHandler ammoInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return acceptsAmmoStack(stack);
        }
    };

    public float getRaycastDistance() {
        return raycastDistance;
    }

    public Vec3 getTargetpos() {
        return targetpos;
    }

    public boolean hasRaycastHit() {
        return raycastHit;
    }

    public boolean supportsBlockDestructionToggle() {
        return true;
    }

    public boolean breaksBlocksEnabled() {
        return getData().isBreaksBlocks();
    }

    public void setBreaksBlocksEnabled(boolean enabled) {
        getData().setBreaksBlocks(enabled);
    }

    public void toggleBreaksBlocksEnabled() {
        setBreaksBlocksEnabled(!breaksBlocksEnabled());
    }


    public abstract float getmaxrange();
    public abstract int getcooldown();

    public boolean isEnergyWeapon() {
        return true;
    }

    public @Nullable Item getAmmoItem() {
        return null;
    }

    public FireCooldown getFireCooldown() {
        return FireCooldown.cool1(getcooldown());
    }

    public int getControlSeatEnergyCostPerTick() {
        // Function: linked weapons consume a baseline control-seat FE upkeep even before subclasses tune it.
        return DEFAULT_CONTROL_SEAT_ENERGY_COST_PER_TICK;
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
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        boolean fireRequested = needtofire();
        tickFireCooldown(fireRequested);
        if (level.isClientSide()) {
            return;
        }
        if(!fireRequested) {
            getData().isfiring = false;
            clearRaycastVisualState(level);
            return;
        }
        if (!isFireCooldownReady()) {
            clearRaycastVisualState(level);
            return;
        }
        if (hasInitialized)
        {
            BlockPos pos = this.getBlockPos();
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level,pos);;
            if (subLevel!=null) {
                weaponpos = ServerShipUtils.getBlockCenterWorld(subLevel, pos);
            } else {
                // Function: weapons placed in the normal level still need to fire and use normal-world coordinates.
                weaponpos = Vec3.atCenterOf(pos);
            }
            if (!consumeAmmoForShot()) {
                getData().isfiring = false;
                clearRaycastVisualState(level);
                return;
            }
            consumeFireCooldown();
            getData().isfiring = true;
            playFireSound(level);
            fire();
        }
    }

    private void clearRaycastVisualState(@Nonnull Level level) {
        BlockState state = this.getBlockState();
        this.raycastDistance = 0.0F;
        this.raycastStart = Vec3.ZERO;
        this.raycastEnd = Vec3.ZERO;
        this.targetpos = Vec3.ZERO;
        this.raycastHit = false;
        syncRaycastStateIfChanged(level, state);
    }

    public WeaponData getData() {
        if (weaponData == null) {
            weaponData = new WeaponData();
        }
        return weaponData;
    }

    public abstract void fire();

    protected void playFireSound(Level level) {
        SoundEvent soundEvent = getFireSoundEvent();
        if (soundEvent == null || weaponpos == null) {
            return;
        }

        // Function: weapon fire sounds use the already-resolved world muzzle area so sublevel weapons sound anchored in place.
        level.playSound(
                null,
                weaponpos.x,
                weaponpos.y,
                weaponpos.z,
                soundEvent,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
    }

    protected @Nullable SoundEvent getFireSoundEvent() {
        return switch (getweapontype()) {
            case "arc_emitter" -> vsieSounds.ARC_EMITTER_FIRE.get();
            case "electro_magnet_rail_cannon" -> vsieSounds.ELECTRO_MAGNET_RAIL_CANNON_FIRE.get();
            default -> null;
        };
    }

    public boolean hasAmmoInventorySlots() {
        return !isEnergyWeapon() && getAmmoItem() != null;
    }

    protected boolean hasAmmoReady() {
        if (!hasAmmoInventorySlots()) {
            return true;
        }
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            if (!ammoInventory.extractItem(slot, 1, true).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    protected boolean consumeAmmoForShot() {
        if (!hasAmmoInventorySlots()) {
            return true;
        }
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            ItemStack extracted = ammoInventory.extractItem(slot, 1, false);
            if (!extracted.isEmpty()) {
                setChanged();
                return true;
            }
        }
        return false;
    }

    protected boolean acceptsAmmoStack(ItemStack stack) {
        Item ammoItem = getAmmoItem();
        return ammoItem != null && hasAmmoInventorySlots() && stack.is(ammoItem);
    }

    public void dropStoredAmmo(Level level, BlockPos pos) {
        if (!hasAmmoInventorySlots()) {
            return;
        }
        // Function: preserve buffered ammo when a non-energy weapon is broken.
        for (int slot = 0; slot < ammoInventory.getSlots(); slot++) {
            ItemStack stack = ammoInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack.copy());
                ammoInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    public IItemHandlerModifiable getItemHandler() {
        return this;
    }

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
        return cooldown.usesValue() ? (int) Math.floor(fireCooldownValue) : currentTick;
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
            return;
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
        this.raycastDistance = effectiveMaxDistance;
        this.targetpos = worldTo;
        // Function: misses still use the max-range endpoint; this flag separates visual target from real block hit.
        this.raycastHit = false;

        // Perform raycast using world coordinates
        ClipContext.Fluid clipFluid = ClipContext.Fluid.ANY;
        BlockHitResult hit = LoadedChunkRaycast.clipIgnoringUnloadedChunks(
                level,
                worldFrom,
                worldTo,
                ClipContext.Block.COLLIDER,
                clipFluid,
                CollisionContext.empty()
        );

        if (hit.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = hit.getLocation();
            this.raycastHit = true;
            float distance = (float)worldFrom.distanceTo(hitPos);
            this.raycastDistance = Math.min(distance, effectiveMaxDistance);
            this.targetpos = hitPos;
        }
        // Function: ray weapons can fire rapidly, so only resync the block entity when visible ray state actually changes.
        syncRaycastStateIfChanged(level, state);
    }

    private void syncRaycastStateIfChanged(@Nonnull Level level, @Nonnull BlockState state) {
        // Function: the server owns beam visibility; clients only render the last synced ray length.
        if (!level.isClientSide() && shouldSyncRaycastState()) {
            lastSyncedRaycastDistance = this.raycastDistance;
            lastSyncedRaycastHit = this.raycastHit;
            setChanged();
            level.sendBlockUpdated(this.worldPosition, state, state, 3);
        }
    }

    private boolean shouldSyncRaycastState() {
        return Float.isNaN(lastSyncedRaycastDistance)
                || Math.abs(lastSyncedRaycastDistance - this.raycastDistance) > 0.01F
                || lastSyncedRaycastHit != this.raycastHit;
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
        tag.put(AMMO_INVENTORY_TAG, ammoInventory.serializeNBT(registries));
        tag.putFloat("raycastDistance", this.getRaycastDistance());
        tag.putDouble("target_x",this.targetpos.x);
        tag.putDouble("target_y",this.targetpos.y);
        tag.putDouble("target_z",this.targetpos.z);
        tag.putBoolean("channel1",weaponData.getChannel1());
        tag.putBoolean("channel2",weaponData.getChannel2());
        tag.putBoolean("channel3",weaponData.getChannel3());
        tag.putBoolean("channel4",weaponData.getChannel4());
        tag.putBoolean("breaksBlocks", weaponData.isBreaksBlocks());
        // Function: sync firing state so client-only loop sounds can stop on the same tick as the server.
        tag.putBoolean("isfiring", weaponData.isfiring);
        tag.putDouble("fireCooldownValue", this.fireCooldownValue);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains(AMMO_INVENTORY_TAG)) {
            ammoInventory.deserializeNBT(registries, tag.getCompound(AMMO_INVENTORY_TAG));
        }
        if (this.weaponData == null) {
            this.weaponData = new WeaponData();
        }
        if (tag.contains("raycastDistance", Tag.TAG_FLOAT)) {this.raycastDistance = tag.getFloat("raycastDistance");}
        if(tag.contains("target_x") && tag.contains("target_y") && tag.contains("target_z")) {this.targetpos = new Vec3(tag.getDouble("target_x"), tag.getDouble("target_y"), tag.getDouble("target_z"));}
        if (tag.contains("channel1")) {weaponData.setChannel1(tag.getBoolean("channel1"));}
        if (tag.contains("channel2")) {weaponData.setChannel2(tag.getBoolean("channel2"));}
        if (tag.contains("channel3")) {weaponData.setChannel3(tag.getBoolean("channel3"));}
        if (tag.contains("channel4")) {weaponData.setChannel4(tag.getBoolean("channel4"));}
        if (tag.contains("breaksBlocks")) {weaponData.setBreaksBlocks(tag.getBoolean("breaksBlocks"));}
        if (tag.contains("isfiring")) {weaponData.isfiring = tag.getBoolean("isfiring");}
        if (tag.contains("fireCooldownValue")) {this.fireCooldownValue = tag.getDouble("fireCooldownValue");}
    }

    //geckolib

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public int getSlots() {
        return ammoInventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return ammoInventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return ammoInventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ammoInventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return ammoInventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return ammoInventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        ammoInventory.setStackInSlot(slot, stack);
        setChanged();
    }
}


