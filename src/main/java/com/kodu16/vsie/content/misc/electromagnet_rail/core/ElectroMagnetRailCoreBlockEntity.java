package com.kodu16.vsie.content.misc.electromagnet_rail.core;

import com.kodu16.vsie.content.misc.electromagnet_rail.top.ElectroMagnetRailTopBlock;
import com.kodu16.vsie.content.misc.electromagnet_rail.top.ElectroMagnetRailTopBlockEntity;
import com.kodu16.vsie.registries.vsieBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.constant.dataticket.SerializableDataTicket;

import javax.annotation.Nullable;
import java.util.List;

public class ElectroMagnetRailCoreBlockEntity extends SmartBlockEntity implements MenuProvider, IItemHandlerModifiable, GeoBlockEntity {
    private HolderLookup.Provider nbtRegistries;

    // 鎵弿缁撴灉鐘舵€佺爜锛氱敤浜庡悓姝ュ埌 GUI 骞舵樉绀烘娴嬫枃妗堛€?
    public static final int TERMINAL_STATUS_IDLE = 0;
    public static final int TERMINAL_STATUS_FOUND = 1;
    public static final int TERMINAL_STATUS_FACING_ERROR = 2;
    public static final int TERMINAL_STATUS_NOT_FOUND = 3;
    public static final int TERMINAL_STATUS_BLOCKED = 4;
    public static SerializableDataTicket<Boolean> IS_WORKING;
    // 鍔熻兘锛氱紦瀛樹笂涓€甯у乏鍙虫粦杞ㄧ殑 X 鍋忕Щ锛岀敤浜庡鎴风娓叉煋鏃跺仛骞虫粦鎻掑€笺€?
    public float prevRailOffsetX = 0.0f;
    // 鏍稿績浠撲粎鏈?4 涓Ы浣嶏紝涓斿彧鍏佽鏀惧叆 electromagnet_rail 鏂瑰潡鐗╁搧銆?
    public final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(vsieBlocks.ELECTRO_MAGNET_RAIL_BLOCK.asItem());
        }
    };

    // 鎻愪緵瀹瑰櫒鑿滃崟璇诲彇妫€娴嬬姸鎬併€?
    // 璁板綍鏈€杩戜竴娆♀€滅粓绔娴嬧€濈粨鏋滐紝渚涘鍣ㄨ彍鍗曞悓姝ョ粰瀹㈡埛绔?GUI銆?
    private int terminalStatus = TERMINAL_STATUS_IDLE;
    // 鎻愪緵瀹瑰櫒鑿滃崟璇诲彇妫€娴嬬粓绔潗鏍囥€?
    private BlockPos terminalPos = BlockPos.ZERO;
    // 鎻愪緵瀹㈡埛绔覆鏌撳眰璇诲彇褰撳墠鍏夋潫鎺ㄨ繘闀垮害銆?
    // 鍏夋潫褰撳墠鍙闀垮害锛堝崟浣嶏細鏂瑰潡锛夛紝姣?tick 鍚戠粓绔帹杩?10 鏍笺€?
    private float beamRenderDistance = 0.0f;

    public int getTerminalStatus() {
        return terminalStatus;
    }

    public BlockPos getTerminalPos() {
        return terminalPos;
    }

    public float getBeamRenderDistance() {
        return beamRenderDistance;
    }

    public ElectroMagnetRailCoreBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

    public void tick(){
        // 鍔熻兘锛氭瘡 tick 鏍￠獙宸茬粦瀹氱粓绔槸鍚︿粛鐒舵湁鏁堬紝鏃犳晥鍒欑珛鍗宠В缁戝苟鍋滄鍏夋潫娓叉煋銆?
        if (this.level == null || this.terminalStatus != TERMINAL_STATUS_FOUND || this.terminalPos.equals(BlockPos.ZERO)) {
            return;
        }

        if (!isTerminalBindingStillValid()) {
            setAnimData(IS_WORKING, false);
            updateTopBindingState(this.terminalPos, false);
            clearTerminalBinding();
            this.setChanged();
            this.sendData();
            return;
        }
        setAnimData(IS_WORKING,true);

        // 鍔熻兘锛氬厜鏉熷墠娌挎寜鍥哄畾閫熷害閫?tick 鎺ㄨ繘锛岀洿鍒板欢浼歌嚦 top銆?
        float maxDistance = (float) Math.sqrt(this.worldPosition.distSqr(this.terminalPos));
        this.beamRenderDistance = Math.min(maxDistance, this.beamRenderDistance + 2.0f);
    }

    // 鎻愪緵缁?GUI 涓庣孩鐭虫瘮杈冨櫒璇诲彇锛氱粺璁′粨鍐?rail 鎬绘暟銆?
    public int getStoredRailCount() {
        int total = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            total += inventory.getStackInSlot(i).getCount();
        }
        return total;
    }

    // 鎵ц缁堢妫€娴嬶細娌挎牳蹇冩湞鍚戝湪 rail 鏁伴噺鑼冨洿鍐呬粠杩戝埌杩滄煡鎵惧彲鍒拌揪鐨?top銆?
    public void detectTerminal() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        Direction facing = this.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING);
        int maxDistance = this.getStoredRailCount();

        BlockPos previousTerminalPos = this.terminalPos;
        this.terminalStatus = TERMINAL_STATUS_NOT_FOUND;
        this.terminalPos = BlockPos.ZERO;
        // 鍔熻兘锛氭瘡娆￠噸鏂版壂鎻忛兘鍏堥噸缃厜鏉熸帹杩涢暱搴︼紝閬垮厤娌跨敤鏃х粦瀹氱殑娓叉煋杩涘害銆?
        this.beamRenderDistance = 0.0f;

        for (int step = 1; step <= maxDistance; step++) {
            BlockPos checkPos = this.worldPosition.relative(facing, step);
            BlockState checkState = this.level.getBlockState(checkPos);

            if (checkState.is(vsieBlocks.ELECTRO_MAGNET_RAIL_TOP_BLOCK.get())) {
                Direction topFacing = checkState.getValue(ElectroMagnetRailTopBlock.FACING);
                if (topFacing == facing) {
                    // 鎵惧埌鍚堟硶缁堢锛氳褰曞潗鏍囩敤浜?GUI 灞曠ず銆?
                    this.terminalStatus = TERMINAL_STATUS_FOUND;
                    this.terminalPos = checkPos;
                    // 鍔熻兘锛氬悓姝?top 鐨勭粦瀹氱姸鎬侊紝璁?top 鍦ㄨ褰撳墠 core 缁戝畾鏃跺睍寮€宸﹀彸楠ㄩ銆?
                    if (!previousTerminalPos.equals(checkPos)) {
                        updateTopBindingState(previousTerminalPos, false);
                    }
                    updateTopBindingState(checkPos, true);
                    // 鍔熻兘锛氶噸鏂扮粦瀹氭椂閲嶇疆鍏夋潫闀垮害锛屼繚璇佷粠 core 閫愭寤朵几鍒?top銆?
                    this.beamRenderDistance = 0.0f;
                } else {
                    // 鎵惧埌缁堢浣嗘湞鍚戦敊璇€?
                    updateTopBindingState(previousTerminalPos, false);
                    this.terminalStatus = TERMINAL_STATUS_FACING_ERROR;
                    this.terminalPos = checkPos;
                    this.beamRenderDistance = 0.0f;
                }
                this.setChanged();
                this.sendData();
                return;
            }

            if (!checkState.isAir() && !checkState.is(vsieBlocks.ELECTRO_MAGNET_RAIL_BLOCK.get())) {
                // 鏍稿績涓庣粓绔箣闂村嚭鐜伴潪 rail 鐨勯殰纰嶆柟鍧楋紝鍒ゅ畾涓洪樆鎸°€?
                updateTopBindingState(previousTerminalPos, false);
                this.terminalStatus = TERMINAL_STATUS_BLOCKED;
                this.terminalPos = checkPos;
                this.beamRenderDistance = 0.0f;
                this.setChanged();
                this.sendData();
                return;
            }
        }

        // 鑼冨洿鍐呮湭鎵惧埌缁堢銆?
        updateTopBindingState(previousTerminalPos, false);
        this.setChanged();
        this.sendData();
    }


    // 鍔熻兘锛氬湪 core 琚牬鍧忔垨寮哄埗瑙ｇ粦鏃讹紝涓诲姩閫氱煡褰撳墠缁戝畾 top 鏀跺洖宸﹀彸楠ㄩ銆?
    public void releaseBoundTop() {
        updateTopBindingState(this.terminalPos, false);
        clearTerminalBinding();
    }

    // 鍔熻兘锛氭妸 core 鐨勭粦瀹氱粨鏋滃悓姝ョ粰鎸囧畾 top锛岄┍鍔?top 宸﹀彸楠ㄩ灞曞紑鎴栨敹鍥炪€?
    private void updateTopBindingState(BlockPos topPos, boolean bound) {
        if (this.level == null || topPos == null || topPos.equals(BlockPos.ZERO)) {
            return;
        }
        if (this.level.getBlockEntity(topPos) instanceof ElectroMagnetRailTopBlockEntity topBlockEntity) {
            topBlockEntity.setBoundToCore(bound);
        }
    }

    // 鎻愪緵娓叉煋灞傚揩閫熷垽鏂€滃彲娓叉煋鐨勭粦瀹氱姸鎬佲€濄€?
    public boolean hasValidTerminalBinding() {
        return this.terminalStatus == TERMINAL_STATUS_FOUND && !this.terminalPos.equals(BlockPos.ZERO) && isTerminalBindingStillValid();
    }

    // 鍔熻兘锛氭牎楠岃褰曠殑 top 鏄惁浠嶆槸鍚屽悜 top锛屼笖 core 鍒?top 涔嬮棿鏃犻殰纰嶃€?
    private boolean isTerminalBindingStillValid() {
        if (this.level == null || this.terminalPos.equals(BlockPos.ZERO)) {
            return false;
        }

        Direction facing = this.getBlockState().getValue(ElectroMagnetRailCoreBlock.FACING);
        BlockState topState = this.level.getBlockState(this.terminalPos);
        if (!topState.is(vsieBlocks.ELECTRO_MAGNET_RAIL_TOP_BLOCK.get())) {
            return false;
        }
        if (!(this.level.getBlockEntity(this.terminalPos) instanceof ElectroMagnetRailTopBlockEntity)) {
            return false;
        }
        if (topState.getValue(ElectroMagnetRailTopBlock.FACING) != facing) {
            return false;
        }

        int distance = getTerminalDistanceAlongFacing(facing, this.terminalPos);
        if (distance <= 0) {
            return false;
        }

        for (int step = 1; step < distance; step++) {
            BlockPos checkPos = this.worldPosition.relative(facing, step);
            BlockState checkState = this.level.getBlockState(checkPos);
            if (!checkState.isAir() && !checkState.is(vsieBlocks.ELECTRO_MAGNET_RAIL_BLOCK.get())) {
                return false;
            }
        }
        return true;
    }

    // 鍔熻兘锛氭牴鎹湞鍚戣绠楃粓绔湪鍓嶆柟鐨勮酱鍚戣窛绂伙紝鑻ユ柟鍚戦敊璇繑鍥?-1銆?
    private int getTerminalDistanceAlongFacing(Direction facing, BlockPos targetPos) {
        int dx = targetPos.getX() - this.worldPosition.getX();
        int dy = targetPos.getY() - this.worldPosition.getY();
        int dz = targetPos.getZ() - this.worldPosition.getZ();

        return switch (facing.getAxis()) {
            case X -> (dy == 0 && dz == 0 && Integer.signum(dx) == facing.getStepX()) ? Math.abs(dx) : -1;
            case Y -> (dx == 0 && dz == 0 && Integer.signum(dy) == facing.getStepY()) ? Math.abs(dy) : -1;
            case Z -> (dx == 0 && dy == 0 && Integer.signum(dz) == facing.getStepZ()) ? Math.abs(dz) : -1;
        };
    }

    // 鍔熻兘锛氭竻绌虹粦瀹氱姸鎬侊紝鍥炲埌鈥滄湭缁戝畾鈥濆苟鍏抽棴鍏夋潫娓叉煋銆?
    private void clearTerminalBinding() {
        this.terminalStatus = TERMINAL_STATUS_IDLE;
        this.terminalPos = BlockPos.ZERO;
        this.beamRenderDistance = 0.0f;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.vsie.electro_magnet_rail_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectroMagnetRailCoreContainerMenu(containerId, playerInventory, this);
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
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.read(tag, registries, clientpacket);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        // 鍚屾鏈€杩戜竴娆＄粓绔娴嬬粨鏋滐紝淇濊瘉 GUI 閲嶅紑鍚庝粛鑳藉睍绀恒€?
        if (tag.contains("TerminalStatus")) {
            this.terminalStatus = tag.getInt("TerminalStatus");
        }
        if (tag.contains("TerminalPos")) {
            this.terminalPos = BlockPos.of(tag.getLong("TerminalPos"));
        }
        if (tag.contains("BeamRenderDistance")) {
            this.beamRenderDistance = tag.getFloat("BeamRenderDistance");
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.write(tag, registries, clientpacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        // 鎸佷箙鍖栧苟鍚屾缁堢妫€娴嬬姸鎬佷笌鍧愭爣銆?
        tag.putInt("TerminalStatus", this.terminalStatus);
        tag.putLong("TerminalPos", this.terminalPos.asLong());
        tag.putFloat("BeamRenderDistance", this.beamRenderDistance);
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
        withNbtRegistries(registries, () -> read(tag, registries, true));
    }


    // 鍔熻兘锛氭彁渚涚粰 NeoForge 1.21.1 capability 娉ㄥ唽鍣ㄧ殑鐗╁搧澶勭悊鍣ㄥ疄渚嬨€?
    public IItemHandlerModifiable getItemHandler() {
        return this;
    }

    // IItemHandlerModifiable 鎺ュ彛杞彂鍒板唴閮?ItemStackHandler銆?
    @Override
    public int getSlots() {
        return inventory.getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return inventory.getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inventory.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return inventory.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
        setChanged();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
