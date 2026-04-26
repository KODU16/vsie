package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.content.bullet.BulletData;
import com.kodu16.vsie.content.bullet.entity.ParticleBulletEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieItems;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ParticleTurretBlockEntity extends AbstractTurretBlockEntity implements IItemHandlerModifiable {
    // 功能：粒子炮内部 3x3 弹药仓，仅允许放入 particle_container。
    private final ItemStackHandler containerInventory = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(vsieItems.PARTICLE_CONTAINER.get());
        }
    };
    public ParticleTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SHOOT_ANIMATION = RawAnimation.begin().then("shoot", Animation.LoopType.PLAY_ONCE);
    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public Vector3d getShootLocation(Vector3d vec, List<Vector3d> preV, Level lv, Vector3d pos) {
        return vec;
    }

    public String getturrettype() {
        return "particle";
    }

    public double getYAxisOffset() {return 3.3d;}

    @Override
    protected Vector3d getTurretPivotInGeoPixels() {
        // 功能：返回 particle turret 模型中 turret 骨骼的枢轴点，用于计算真实世界炮口基准点。
        return new Vector3d(0.0, 0.0, 0);
    }

    @Override
    public double getcannonlength() {
        return 4;
    }

    @Override
    public float getMaxSpinSpeed() {
        return Mth.PI/64;
    }

    @Override
    public int getCoolDown() {
        return 60;
    }

    @Override
    public int getenergypertick() {
        return 100;
    }

    @Override
    protected boolean canShootCurrentTarget() {
        // 功能：没有可用粒子容器时，炮塔只保持瞄准，不执行开火。
        return hasStoredContainer();
    }

    @Override
    public void shootentity() {
        Level level = this.getLevel();
        // 功能：仅允许服务端执行开火逻辑，避免客户端在索敌/预测分支误触发一次射击动画。
        if (level == null || level.isClientSide) {
            return;
        }
        // 功能：每次真正开火前消耗 1 个粒子容器，若消耗失败则终止本次射击。
        if (!consumeOneContainer()) {
            return;
        }
        boolean onship = VSGameUtilsKt.isBlockInShipyard(level,this.getBlockPos());
        if(onship && getFirePoint() != null){
            triggerAnim("controller", "shoot");
            Ship ship = VSGameUtilsKt.getShipManagingPos(level,this.getBlockPos());
            Vec3 center = new Vec3(this.getBlockPos().getX()+getFirePoint().x, this.getBlockPos().getY()+getFirePoint().y+getYAxisOffset(), this.getBlockPos().getZ()+getFirePoint().z);
            Vec3 firepoint = VSGameUtilsKt.toWorldCoordinates(ship,center);
            ParticleBulletEntity bullet = new ParticleBulletEntity(vsieEntities.PARTICLE_BULLET.get(), level);
            // 功能：为粒子炮子弹写入标准 data，确保子弹第 1 tick 使用 particle_cannon_fire 触发 awake FX。
            bullet.setDataBase(BulletData.createParticleCannonDefault());
            bullet.setPos(firepoint);
            bullet.setDeltaMovement(new Vec3(targetPos.x-firepoint.x,targetPos.y-firepoint.y,targetPos.z-firepoint.z).normalize().scale(1.0F));
            this.getLevel().addFreshEntity(bullet);
        }
    }

    @Override
    public void shootship() {

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

    // 功能：判断弹药仓中是否仍有可用粒子容器。
    private boolean hasStoredContainer() {
        for (int slot = 0; slot < containerInventory.getSlots(); slot++) {
            if (!containerInventory.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // 功能：从弹药仓按槽位顺序消耗 1 个粒子容器，作为一次射击成本。
    private boolean consumeOneContainer() {
        for (int slot = 0; slot < containerInventory.getSlots(); slot++) {
            ItemStack extracted = containerInventory.extractItem(slot, 1, false);
            if (!extracted.isEmpty()) {
                setChanged();
                return true;
            }
        }
        return false;
    }

    // 功能：方块被破坏时将粒子容器掉落到世界，避免物品丢失。
    public void dropStoredContainers(Level level, BlockPos pos) {
        for (int slot = 0; slot < containerInventory.getSlots(); slot++) {
            ItemStack stack = containerInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack.copy());
                containerInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        // 功能：保存粒子炮 3x3 弹药仓数据，保证重进世界后不丢仓内物品。
        tag.put("ParticleContainerInventory", containerInventory.serializeNBT());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        if (tag.contains("ParticleContainerInventory")) {
            // 功能：读取粒子炮 3x3 弹药仓数据，用于服务端逻辑与 GUI 同步。
            containerInventory.deserializeNBT(tag.getCompound("ParticleContainerInventory"));
        }
    }

    // 功能：提供给 NeoForge 1.21.1 capability 注册器的物品处理器实例。
    public IItemHandlerModifiable getItemHandler() {
        return this;
    }

    @Override
    public int getSlots() {
        return containerInventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return containerInventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return containerInventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return containerInventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return containerInventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return containerInventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        containerInventory.setStackInSlot(slot, stack);
        setChanged();
    }
}
