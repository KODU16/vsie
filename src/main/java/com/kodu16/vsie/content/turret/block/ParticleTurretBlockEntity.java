package com.kodu16.vsie.content.turret.block;

import com.kodu16.vsie.content.bullet.BulletData;
import com.kodu16.vsie.content.bullet.entity.ParticleBulletEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.registries.vsieEntities;
import com.kodu16.vsie.registries.vsieItems;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class ParticleTurretBlockEntity extends AbstractTurretBlockEntity implements IItemHandlerModifiable {
    private HolderLookup.Provider nbtRegistries;

    // 鍔熻兘锛氱矑瀛愮偖鍐呴儴 3x3 寮硅嵂浠擄紝浠呭厑璁告斁鍏?particle_container銆?
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

    @Override
    public Vec3 getShootLocation(Vec3 vec, List<Vector3d> preV, Level lv, Vec3 pos) {
        return vec;
    }

    public String getturrettype() {
        return "particle";
    }

    public double getYAxisOffset() {return 3.3d;}

    @Override
    protected Vector3d getTurretPivotInGeoPixels() {
        // 鍔熻兘锛氳繑鍥?particle turret 妯″瀷涓?turret 楠ㄩ鐨勬灑杞寸偣锛岀敤浜庤绠楃湡瀹炰笘鐣岀偖鍙ｅ熀鍑嗙偣銆?
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
        // 鍔熻兘锛氭病鏈夊彲鐢ㄧ矑瀛愬鍣ㄦ椂锛岀偖濉斿彧淇濇寔鐬勫噯锛屼笉鎵ц寮€鐏€?
        return hasStoredContainer();
    }

    @Override
    public void shootentity() {
        Level level = this.getLevel();
        // 鍔熻兘锛氫粎鍏佽鏈嶅姟绔墽琛屽紑鐏€昏緫锛岄伩鍏嶅鎴风鍦ㄧ储鏁?棰勬祴鍒嗘敮璇Е鍙戜竴娆″皠鍑诲姩鐢汇€?
        if (level == null || level.isClientSide) {
            return;
        }
        // 鍔熻兘锛氭瘡娆＄湡姝ｅ紑鐏墠娑堣€?1 涓矑瀛愬鍣紝鑻ユ秷鑰楀け璐ュ垯缁堟鏈灏勫嚮銆?
        if (!consumeOneContainer()) {
            return;
        }
        if(getFirePoint() != null){
            triggerAnim("controller", "shoot");
            Vec3 center = new Vec3(this.getBlockPos().getX()+getFirePoint().x, this.getBlockPos().getY()+getFirePoint().y+getYAxisOffset(), this.getBlockPos().getZ()+getFirePoint().z);
            SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, this.getBlockPos());
            Vec3 firepoint = subLevel == null ? center : subLevel.logicalPose().transformPosition(center);
            ParticleBulletEntity bullet = new ParticleBulletEntity(vsieEntities.PARTICLE_BULLET.get(), level);
            // 鍔熻兘锛氫负绮掑瓙鐐瓙寮瑰啓鍏ユ爣鍑?data锛岀‘淇濆瓙寮圭 1 tick 浣跨敤 particle_cannon_fire 瑙﹀彂 awake FX銆?
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

    // 鍔熻兘锛氬垽鏂脊鑽粨涓槸鍚︿粛鏈夊彲鐢ㄧ矑瀛愬鍣ㄣ€?
    private boolean hasStoredContainer() {
        for (int slot = 0; slot < containerInventory.getSlots(); slot++) {
            if (!containerInventory.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // 鍔熻兘锛氫粠寮硅嵂浠撴寜妲戒綅椤哄簭娑堣€?1 涓矑瀛愬鍣紝浣滀负涓€娆″皠鍑绘垚鏈€?
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

    // 鍔熻兘锛氭柟鍧楄鐮村潖鏃跺皢绮掑瓙瀹瑰櫒鎺夎惤鍒颁笘鐣岋紝閬垮厤鐗╁搧涓㈠け銆?
    public void dropStoredContainers(Level level, BlockPos pos) {
        for (int slot = 0; slot < containerInventory.getSlots(); slot++) {
            ItemStack stack = containerInventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack.copy());
                containerInventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private HolderLookup.Provider currentNbtRegistries() {
        return nbtRegistries != null ? nbtRegistries : this.level.registryAccess();
    }

    private void withNbtRegistries(HolderLookup.Provider registries, Runnable action) {
        HolderLookup.Provider previous = this.nbtRegistries;
        this.nbtRegistries = registries;
        try {
            action.run();
        } finally {
            this.nbtRegistries = previous;
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        // 鍔熻兘锛氫繚瀛樼矑瀛愮偖 3x3 寮硅嵂浠撴暟鎹紝淇濊瘉閲嶈繘涓栫晫鍚庝笉涓粨鍐呯墿鍝併€?
        tag.put("ParticleContainerInventory", containerInventory.serializeNBT(registries));
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("ParticleContainerInventory")) {
            // 鍔熻兘锛氳鍙栫矑瀛愮偖 3x3 寮硅嵂浠撴暟鎹紝鐢ㄤ簬鏈嶅姟绔€昏緫涓?GUI 鍚屾銆?
            containerInventory.deserializeNBT(registries, tag.getCompound("ParticleContainerInventory"));
        }
    }

    // 鍔熻兘锛氭彁渚涚粰 NeoForge 1.21.1 capability 娉ㄥ唽鍣ㄧ殑鐗╁搧澶勭悊鍣ㄥ疄渚嬨€?
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        withNbtRegistries(registries, () -> read(tag, registries, true));
    }

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
