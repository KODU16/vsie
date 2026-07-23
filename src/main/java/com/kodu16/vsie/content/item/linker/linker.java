package com.kodu16.vsie.content.item.linker;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.structure.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxBlockEntity;
import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryBlockEntity;
import com.kodu16.vsie.content.storage.fueltank.AbstractFuelTankBlockEntity;
import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.electro_magnet_rail_accelerator.ElectromagnetRailAcceleratorBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.kodu16.vsie.utility.ItemStackNbt;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class linker extends Item {
    public static final String CONTROL_SEAT_POS_TAG = "ControlSeatPos";
    public static final String VERTICAL_LAUNCH_CORE_POS_TAG = "VerticleLaunchingSlotCorePos";
    public static final String ELECTRO_MAGNET_RAIL_CORE_POS_TAG = "ElectroMagnetRailCorePos";
    private static final String STORED_TYPE_TAG = "StoredTargetType";
    private static final String TYPE_CONTROL_SEAT = "control_seat";
    private static final String TYPE_VERTICAL_LAUNCH_CORE = "verticle_launching_slot_core";
    private static final String TYPE_ELECTRO_MAGNET_RAIL_CORE = "electro_magnet_rail_core";

    public linker(Properties pProperties) {
        super(pProperties);
    }

    // Function: keep linker user-facing text in lang files instead of hard-coded English.
    private static Component linkerText(String key, Object... args) {
        return Component.translatable("item.vsie.linker." + key, args);
    }

    private static Component linkerTargetText(String key) {
        return Component.translatable("item.vsie.linker.target." + key);
    }

    private static void showLinkerMessage(Player player, String key, Object... args) {
        player.displayClientMessage(linkerText(key, args), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isRightClickingAir(level, player)) {
            if (!level.isClientSide) {
                // Function: shift-right-clicking air clears all linker modes without touching unrelated item data.
                CompoundTag nbt = ItemStackNbt.getOrCreate(stack);
                clearStoredTargets(nbt);
                ItemStackNbt.set(stack, nbt);
                showLinkerMessage(player, "cleared");
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private boolean isRightClickingAir(Level level, Player player) {
        // Function: use() can be reached after some block interactions, so only clear when the ray actually misses blocks.
        return getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE).getType() == HitResult.Type.MISS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        BlockPos clickedPos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        CompoundTag nbt = ItemStackNbt.getOrCreate(stack);
        BlockEntity clickedBlockEntity = level.getBlockEntity(clickedPos);

        if (player.isShiftKeyDown()) {
            InteractionResult toggleResult = tryToggleStoredLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
            if (toggleResult.consumesAction()) {
                return toggleResult;
            }
        }

        if (clickedBlockEntity instanceof VerticleLaunchingSlotCoreBlockEntity && !nbt.contains(CONTROL_SEAT_POS_TAG)) {
            clearStoredTargets(nbt);
            putBlockPos(nbt, VERTICAL_LAUNCH_CORE_POS_TAG, clickedPos);
            nbt.putString(STORED_TYPE_TAG, TYPE_VERTICAL_LAUNCH_CORE);
            ItemStackNbt.set(stack, nbt);
            showLinkerMessage(player, "recorded_vertical_launch_core", clickedPos.toShortString());
            return InteractionResult.CONSUME;
        }

        if (clickedBlockEntity instanceof ElectroMagnetRailCoreBlockEntity && !nbt.contains(CONTROL_SEAT_POS_TAG)) {
            clearStoredTargets(nbt);
            putBlockPos(nbt, ELECTRO_MAGNET_RAIL_CORE_POS_TAG, clickedPos);
            nbt.putString(STORED_TYPE_TAG, TYPE_ELECTRO_MAGNET_RAIL_CORE);
            ItemStackNbt.set(stack, nbt);
            showLinkerMessage(player, "recorded_rail_core", clickedPos.toShortString());
            return InteractionResult.CONSUME;
        }

        if (nbt.contains(VERTICAL_LAUNCH_CORE_POS_TAG)) {
            return handleVerticalLaunchSlotLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
        }

        if (nbt.contains(ELECTRO_MAGNET_RAIL_CORE_POS_TAG)) {
            return handleElectroMagnetRailAcceleratorLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
        }

        if (!nbt.contains(CONTROL_SEAT_POS_TAG)) {
            return recordControlSeat(player, stack, nbt, clickedPos, clickedBlockEntity);
        }

        return handleControlSeatPeripheralLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
    }

    private InteractionResult recordControlSeat(ServerPlayer player, ItemStack stack, CompoundTag nbt, BlockPos clickedPos, BlockEntity clickedBlockEntity) {
        if (clickedBlockEntity instanceof AbstractControlSeatBlockEntity) {
            clearStoredTargets(nbt);
            putBlockPos(nbt, CONTROL_SEAT_POS_TAG, clickedPos);
            nbt.putString(STORED_TYPE_TAG, TYPE_CONTROL_SEAT);
            ItemStackNbt.set(stack, nbt);
            showLinkerMessage(player, "recorded_control_seat", clickedPos.toShortString());
            return InteractionResult.CONSUME;
        }

        showLinkerMessage(player, "record_first_target");
        return InteractionResult.CONSUME;
    }

    private InteractionResult handleVerticalLaunchSlotLink(Level level, ServerPlayer player, ItemStack stack, CompoundTag nbt,
                                                           BlockPos clickedPos, BlockEntity clickedBlockEntity) {
        BlockPos corePos = getBlockPos(nbt, VERTICAL_LAUNCH_CORE_POS_TAG);
        BlockEntity coreBlockEntity = level.getBlockEntity(corePos);
        if (!(coreBlockEntity instanceof VerticleLaunchingSlotCoreBlockEntity core)) {
            nbt.remove(VERTICAL_LAUNCH_CORE_POS_TAG);
            nbt.remove(STORED_TYPE_TAG);
            ItemStackNbt.set(stack, nbt);
            showLinkerMessage(player, "missing_vertical_launch_core");
            return InteractionResult.CONSUME;
        }

        if (clickedBlockEntity instanceof VerticleLaunchingSlotBlockEntity) {
            if (!ensureSameSubLevel(level, corePos, clickedPos, player,
                    "vertical_launch_same_sublevel")) {
                return InteractionResult.CONSUME;
            }
            // Function: each connected slot receives a stable 1-based number according to the core connection order.
            int slotIndex = core.addLinkedSlot(clickedPos);
            showLinkerMessage(player, "bound_launch_slot", clickedPos.toShortString(), corePos.toShortString(), slotIndex);
            return InteractionResult.CONSUME;
        }

        showLinkerMessage(player, "finish_vertical_launch_slot");
        return InteractionResult.CONSUME;
    }

    private InteractionResult handleControlSeatPeripheralLink(Level level, ServerPlayer player, ItemStack stack, CompoundTag nbt,
                                                              BlockPos clickedPos, BlockEntity clickedBlockEntity) {
        BlockPos controllerPos = getBlockPos(nbt, CONTROL_SEAT_POS_TAG);
        BlockEntity controllerBlockEntity = level.getBlockEntity(controllerPos);

        if (!(controllerBlockEntity instanceof AbstractControlSeatBlockEntity controlSeat)) {
            nbt.remove(CONTROL_SEAT_POS_TAG);
            nbt.remove(STORED_TYPE_TAG);
            ItemStackNbt.set(stack, nbt);
            showLinkerMessage(player, "missing_control_seat");
            return InteractionResult.CONSUME;
        }

        if (clickedBlockEntity instanceof ElectroMagnetRailCoreBlockEntity core) {
            if (!ensureSameSubLevel(level, controllerPos, clickedPos, player,
                    "control_seat_rail_core_same_sublevel")) {
                return InteractionResult.CONSUME;
            }
            core.setLinkedControlSeatPos(controllerPos);
            core.sendData();
            showLinkerMessage(player, "linked_rail_core_to_control_seat", clickedPos.toShortString(), controllerPos.toShortString());
            return InteractionResult.CONSUME;
        }

        if (!ensureSameSubLevel(level, controllerPos, clickedPos, player,
                "control_seat_peripheral_same_sublevel")) {
            return InteractionResult.CONSUME;
        }

        Vec3 peripheralPos = Vec3.atLowerCornerOf(clickedPos);
        if (clickedBlockEntity instanceof VerticleLaunchingSlotBlockEntity) {
            showLinkerMessage(player, "vertical_slots_core_only");
            return InteractionResult.CONSUME;
        }
        if (clickedBlockEntity instanceof AbstractThrusterBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, clickedBlockEntity, 0, "thruster");
        }
        if (clickedBlockEntity instanceof AbstractWeaponBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, clickedBlockEntity, 1, "weapon");
        }
        if (clickedBlockEntity instanceof ShieldGeneratorBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, clickedBlockEntity, 2, "shield");
        }
        if (clickedBlockEntity instanceof AbstractTurretBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, clickedBlockEntity, 3, "turret");
        }
        if (clickedBlockEntity instanceof AbstractEnergyBatteryBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, clickedBlockEntity, 4, "battery");
        }
        if (clickedBlockEntity instanceof AbstractFuelTankBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, clickedBlockEntity, 5, "fuel_tank");
        }
        if (clickedBlockEntity instanceof AmmoBoxBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, clickedBlockEntity, 6, "ammo_box");
        }
        if (clickedBlockEntity instanceof AbstractScreenBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, clickedBlockEntity, 7, "screen");
        }

        showLinkerMessage(player, "cannot_link_to_control_seat");
        return InteractionResult.CONSUME;
    }

    private InteractionResult handleElectroMagnetRailAcceleratorLink(Level level, ServerPlayer player, ItemStack stack, CompoundTag nbt,
                                                                     BlockPos clickedPos, BlockEntity clickedBlockEntity) {
        BlockPos corePos = getBlockPos(nbt, ELECTRO_MAGNET_RAIL_CORE_POS_TAG);
        BlockEntity coreBlockEntity = level.getBlockEntity(corePos);
        if (!(coreBlockEntity instanceof ElectroMagnetRailCoreBlockEntity)) {
            nbt.remove(ELECTRO_MAGNET_RAIL_CORE_POS_TAG);
            nbt.remove(STORED_TYPE_TAG);
            ItemStackNbt.set(stack, nbt);
            showLinkerMessage(player, "missing_rail_core");
            return InteractionResult.CONSUME;
        }

        if (clickedBlockEntity instanceof ElectromagnetRailAcceleratorBlockEntity accelerator) {
            if (!ensureSeparatedRailBinding(level, corePos, clickedPos, player)) {
                return InteractionResult.CONSUME;
            }
            // Function: the accelerator stores one explicit rail-core binding that its fire logic resolves later.
            accelerator.setLinkedCorePos(corePos);
            showLinkerMessage(player, "bound_accelerator_to_core", clickedPos.toShortString(), corePos.toShortString());
            return InteractionResult.CONSUME;
        }

        showLinkerMessage(player, "finish_accelerator");
        return InteractionResult.CONSUME;
    }

    private InteractionResult linkControlSeatPeripheral(ServerPlayer player, AbstractControlSeatBlockEntity controlSeat,
                                                        BlockPos controllerPos, Vec3 peripheralPos, BlockPos clickedPos, BlockEntity clickedBlockEntity,
                                                        int type, String displayName) {
        setStoredPeripheralControlSeat(clickedBlockEntity, controllerPos);
        // Function: keep the original control-seat linker peripheral type ids for existing control logic.
        controlSeat.addLinkedPeripheral(peripheralPos, type);
        showLinkerMessage(player, "linked_peripheral", linkerTargetText(displayName), clickedPos.toShortString(), controllerPos.toShortString());
        return InteractionResult.CONSUME;
    }

    private InteractionResult tryToggleStoredLink(Level level, ServerPlayer player, ItemStack stack, CompoundTag nbt,
                                                  BlockPos clickedPos, BlockEntity clickedBlockEntity) {
        if (nbt.contains(ELECTRO_MAGNET_RAIL_CORE_POS_TAG)) {
            return tryToggleRailCoreAcceleratorLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
        }
        if (nbt.contains(CONTROL_SEAT_POS_TAG)) {
            return tryToggleControlSeatPeripheralLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult tryToggleControlSeatPeripheralLink(Level level, ServerPlayer player, ItemStack stack, CompoundTag nbt,
                                                                 BlockPos clickedPos, BlockEntity clickedBlockEntity) {
        if (clickedBlockEntity instanceof ElectroMagnetRailCoreBlockEntity core) {
            return tryToggleRailCoreControlSeatLink(level, player, stack, nbt, clickedPos, core);
        }

        int peripheralType = getControlSeatPeripheralType(clickedBlockEntity);
        if (peripheralType < 0) {
            return InteractionResult.PASS;
        }

        BlockPos linkedControlSeatPos = getStoredLinkedControlSeatPos(clickedBlockEntity);
        if (linkedControlSeatPos != null && !linkedControlSeatPos.equals(BlockPos.ZERO)) {
            if (isStoredControlSeatPeripheralLinkValid(level, linkedControlSeatPos, clickedPos, peripheralType)) {
                return tryUnlinkControlSeatPeripheral(level, player, clickedPos, clickedBlockEntity, peripheralType, linkedControlSeatPos);
            }
            // Function: stale peripheral-side control-seat coordinates should not block shift-right-click from creating a fresh link.
            clearStoredPeripheralControlSeat(clickedBlockEntity);
        }

        if (!nbt.contains(CONTROL_SEAT_POS_TAG)) {
            showLinkerMessage(player, "record_control_seat_before_peripheral");
            return InteractionResult.CONSUME;
        }

        // Function: shift-right-click now toggles peripherals, so an unlinked block binds to the currently recorded control seat.
        return handleControlSeatPeripheralLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
    }

    private InteractionResult tryToggleRailCoreControlSeatLink(Level level, ServerPlayer player, ItemStack stack, CompoundTag nbt,
                                                               BlockPos clickedPos, ElectroMagnetRailCoreBlockEntity core) {
        BlockPos linkedControlSeatPos = core.getLinkedControlSeatPos();
        if (linkedControlSeatPos != null && !linkedControlSeatPos.equals(BlockPos.ZERO)) {
            if (level.getBlockEntity(linkedControlSeatPos) instanceof AbstractControlSeatBlockEntity
                    && areOnSameSubLevel(level, linkedControlSeatPos, clickedPos)) {
                core.setLinkedControlSeatPos(BlockPos.ZERO);
                core.sendData();
                showLinkerMessage(player, "removed_rail_core_from_control_seat", clickedPos.toShortString());
                return InteractionResult.CONSUME;
            }
            // Function: stale rail-core control-seat coordinates should not block the next bind attempt.
            core.setLinkedControlSeatPos(BlockPos.ZERO);
            core.sendData();
        }

        if (!nbt.contains(CONTROL_SEAT_POS_TAG)) {
            showLinkerMessage(player, "record_control_seat_before_rail_core");
            return InteractionResult.CONSUME;
        }

        return handleControlSeatPeripheralLink(level, player, stack, nbt, clickedPos, core);
    }

    private InteractionResult tryToggleRailCoreAcceleratorLink(Level level, ServerPlayer player, ItemStack stack, CompoundTag nbt,
                                                               BlockPos clickedPos, BlockEntity clickedBlockEntity) {
        if (!(clickedBlockEntity instanceof ElectromagnetRailAcceleratorBlockEntity accelerator)) {
            return InteractionResult.PASS;
        }

        BlockPos linkedCorePos = accelerator.getLinkedCorePos();
        if (linkedCorePos != null && !linkedCorePos.equals(BlockPos.ZERO)) {
            if (isStoredRailCoreAcceleratorLinkValid(level, linkedCorePos, accelerator)) {
                accelerator.setLinkedCorePos(BlockPos.ZERO);
                showLinkerMessage(player, "removed_accelerator_from_rail_core", clickedPos.toShortString());
                return InteractionResult.CONSUME;
            }
            // Function: stale accelerator-side rail-core coordinates should not block shift-right-click from creating a fresh link.
            accelerator.setLinkedCorePos(BlockPos.ZERO);
        }

        return handleElectroMagnetRailAcceleratorLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
    }

    private InteractionResult tryUnlinkControlSeatPeripheral(Level level, ServerPlayer player, BlockPos clickedPos,
                                                             BlockEntity clickedBlockEntity, int peripheralType, BlockPos linkedControlSeatPos) {
        boolean removed = false;
        if (linkedControlSeatPos != null && !linkedControlSeatPos.equals(BlockPos.ZERO)) {
            removed = unlinkFromControlSeatAt(level, linkedControlSeatPos, clickedPos, peripheralType);
        }
        clearStoredPeripheralControlSeat(clickedBlockEntity);

        if (removed) {
            showLinkerMessage(player, "removed_peripheral_from_control_seat", clickedPos.toShortString());
        } else {
            showLinkerMessage(player, "no_stored_control_seat_link");
        }
        return InteractionResult.CONSUME;
    }

    private int getControlSeatPeripheralType(BlockEntity blockEntity) {
        if (blockEntity instanceof VerticleLaunchingSlotBlockEntity) {
            return -1;
        }
        if (blockEntity instanceof AbstractThrusterBlockEntity) {
            return 0;
        }
        if (blockEntity instanceof AbstractWeaponBlockEntity) {
            return 1;
        }
        if (blockEntity instanceof ShieldGeneratorBlockEntity) {
            return 2;
        }
        if (blockEntity instanceof AbstractTurretBlockEntity) {
            return 3;
        }
        if (blockEntity instanceof AbstractEnergyBatteryBlockEntity) {
            return 4;
        }
        if (blockEntity instanceof AbstractFuelTankBlockEntity) {
            return 5;
        }
        if (blockEntity instanceof AmmoBoxBlockEntity) {
            return 6;
        }
        if (blockEntity instanceof AbstractScreenBlockEntity) {
            return 7;
        }
        return -1;
    }

    private BlockPos getStoredLinkedControlSeatPos(BlockEntity blockEntity) {
        if (blockEntity instanceof ShieldGeneratorBlockEntity shield) {
            return shield.linkedcontrolseatpos;
        }
        if (blockEntity instanceof AbstractEnergyBatteryBlockEntity battery) {
            return battery.linkedcontrolseatpos;
        }
        if (blockEntity instanceof AbstractFuelTankBlockEntity fuelTank) {
            return fuelTank.linkedcontrolseatpos;
        }
        if (blockEntity instanceof AmmoBoxBlockEntity ammoBox) {
            return ammoBox.getLinkedControlSeatPos();
        }
        if (blockEntity instanceof AbstractThrusterBlockEntity thruster) {
            return thruster.getLinkedControlSeatPos();
        }
        if (blockEntity instanceof AbstractWeaponBlockEntity weapon) {
            return weapon.getLinkedControlSeatPos();
        }
        if (blockEntity instanceof AbstractTurretBlockEntity turret) {
            return turret.getLinkedControlSeatPos();
        }
        if (blockEntity instanceof AbstractScreenBlockEntity screen) {
            return screen.getLinkedControlSeatPos();
        }
        return null;
    }

    private boolean isStoredControlSeatPeripheralLinkValid(Level level, BlockPos controlSeatPos, BlockPos peripheralPos, int peripheralType) {
        BlockEntity blockEntity = level.getBlockEntity(controlSeatPos);
        if (!(blockEntity instanceof AbstractControlSeatBlockEntity controlSeat)) {
            return false;
        }
        // Function: only a control-seat list entry counts as an active peripheral link; peripheral-side NBT alone may be stale.
        return controlSeat.hasLinkedPeripheral(Vec3.atLowerCornerOf(peripheralPos), peripheralType);
    }

    private boolean isStoredRailCoreAcceleratorLinkValid(Level level, BlockPos corePos, ElectromagnetRailAcceleratorBlockEntity accelerator) {
        BlockEntity blockEntity = level.getBlockEntity(corePos);
        if (!(blockEntity instanceof ElectroMagnetRailCoreBlockEntity)) {
            return false;
        }
        // Function: only a still-loaded rail core at the stored position counts as an active accelerator core link.
        return corePos.equals(accelerator.getLinkedCorePos());
    }

    private boolean ensureSameSubLevel(Level level, BlockPos sourcePos, BlockPos targetPos, ServerPlayer player, String failureMessageKey) {
        if (areOnSameSubLevel(level, sourcePos, targetPos)) {
            return true;
        }
        // Function: most linker bindings are local to one assembled sublevel; reject cross-sublevel links at bind time.
        showLinkerMessage(player, failureMessageKey);
        return false;
    }

    private boolean areOnSameSubLevel(Level level, BlockPos firstPos, BlockPos secondPos) {
        SubLevel firstSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, firstPos);
        SubLevel secondSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, secondPos);
        if (firstSubLevel == null || secondSubLevel == null) {
            return false;
        }
        return firstSubLevel == secondSubLevel || firstSubLevel.getUniqueId().equals(secondSubLevel.getUniqueId());
    }

    private boolean ensureSeparatedRailBinding(Level level, BlockPos corePos, BlockPos acceleratorPos, ServerPlayer player) {
        SubLevel coreSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, corePos);
        SubLevel acceleratorSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, acceleratorPos);
        if (coreSubLevel == null || acceleratorSubLevel == null) {
            showLinkerMessage(player, "rail_core_accelerator_mounted_sublevels");
            return false;
        }
        if (coreSubLevel == acceleratorSubLevel || coreSubLevel.getUniqueId().equals(acceleratorSubLevel.getUniqueId())) {
            showLinkerMessage(player, "rail_core_accelerator_different_sublevels");
            return false;
        }
        return true;
    }

    private void setStoredPeripheralControlSeat(BlockEntity blockEntity, BlockPos controlSeatPos) {
        if (blockEntity instanceof ShieldGeneratorBlockEntity shield) {
            shield.linkedcontrolseatpos = controlSeatPos.immutable();
            shield.setChanged();
            shield.sendData();
            return;
        }
        if (blockEntity instanceof AbstractEnergyBatteryBlockEntity battery) {
            battery.setLinkedcontrolseatpos(controlSeatPos);
            battery.sendData();
            return;
        }
        if (blockEntity instanceof AbstractFuelTankBlockEntity fuelTank) {
            fuelTank.setLinkedcontrolseatpos(controlSeatPos);
            fuelTank.sendData();
            return;
        }
        if (blockEntity instanceof AmmoBoxBlockEntity ammoBox) {
            ammoBox.setLinkedControlSeatPos(controlSeatPos);
            return;
        }
        if (blockEntity instanceof AbstractThrusterBlockEntity thruster) {
            thruster.setLinkedControlSeatPos(controlSeatPos);
            thruster.sendData();
            return;
        }
        if (blockEntity instanceof AbstractWeaponBlockEntity weapon) {
            weapon.setLinkedControlSeatPos(controlSeatPos);
            weapon.sendData();
            return;
        }
        if (blockEntity instanceof AbstractTurretBlockEntity turret) {
            turret.setLinkedControlSeatPos(controlSeatPos);
            turret.sendData();
            return;
        }
        if (blockEntity instanceof AbstractScreenBlockEntity screen) {
            screen.setLinkedControlSeatPos(controlSeatPos);
            screen.sendData();
        }
    }

    private void clearStoredPeripheralControlSeat(BlockEntity blockEntity) {
        if (blockEntity instanceof ShieldGeneratorBlockEntity shield) {
            shield.linkedcontrolseatpos = BlockPos.ZERO;
            shield.setChanged();
            shield.sendData();
            return;
        }
        if (blockEntity instanceof AbstractEnergyBatteryBlockEntity battery) {
            battery.setLinkedcontrolseatpos(BlockPos.ZERO);
            battery.sendData();
            return;
        }
        if (blockEntity instanceof AbstractFuelTankBlockEntity fuelTank) {
            fuelTank.setLinkedcontrolseatpos(BlockPos.ZERO);
            fuelTank.sendData();
            return;
        }
        if (blockEntity instanceof AmmoBoxBlockEntity ammoBox) {
            ammoBox.setLinkedControlSeatPos(null);
            return;
        }
        if (blockEntity instanceof AbstractThrusterBlockEntity thruster) {
            thruster.setLinkedControlSeatPos(BlockPos.ZERO);
            thruster.sendData();
            return;
        }
        if (blockEntity instanceof AbstractWeaponBlockEntity weapon) {
            weapon.setLinkedControlSeatPos(BlockPos.ZERO);
            weapon.sendData();
            return;
        }
        if (blockEntity instanceof AbstractTurretBlockEntity turret) {
            turret.setLinkedControlSeatPos(BlockPos.ZERO);
            turret.sendData();
            return;
        }
        if (blockEntity instanceof AbstractScreenBlockEntity screen) {
            screen.setLinkedControlSeatPos(BlockPos.ZERO);
            screen.sendData();
        }
    }

    private boolean unlinkFromControlSeatAt(Level level, BlockPos controlSeatPos, BlockPos peripheralPos, int peripheralType) {
        BlockEntity blockEntity = level.getBlockEntity(controlSeatPos);
        if (!(blockEntity instanceof AbstractControlSeatBlockEntity controlSeat)) {
            return false;
        }
        // Function: explicit peripheral unlink must bypass delayed missing-tick cleanup and remove the saved link immediately.
        return controlSeat.removeLinkedPeripheralImmediately(Vec3.atLowerCornerOf(peripheralPos), peripheralType);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = ItemStackNbt.get(stack);
        if (tag != null && tag.contains(VERTICAL_LAUNCH_CORE_POS_TAG)) {
            BlockPos pos = getBlockPos(tag, VERTICAL_LAUNCH_CORE_POS_TAG);
            tooltip.add(linkerText("tooltip.recorded_vertical_launch_core", pos.toShortString()));
            tooltip.add(linkerText("tooltip.link_launch_slot"));
        } else if (tag != null && tag.contains(ELECTRO_MAGNET_RAIL_CORE_POS_TAG)) {
            BlockPos pos = getBlockPos(tag, ELECTRO_MAGNET_RAIL_CORE_POS_TAG);
            tooltip.add(linkerText("tooltip.recorded_rail_core", pos.toShortString()));
            tooltip.add(linkerText("tooltip.bind_accelerator"));
        } else if (tag != null && tag.contains(CONTROL_SEAT_POS_TAG)) {
            BlockPos pos = getBlockPos(tag, CONTROL_SEAT_POS_TAG);
            tooltip.add(linkerText("tooltip.recorded_control_seat", pos.toShortString()));
            tooltip.add(linkerText("tooltip.link_peripheral"));
        } else {
            tooltip.add(linkerText("tooltip.record_first_target"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static void clearStoredTargets(CompoundTag nbt) {
        nbt.remove(CONTROL_SEAT_POS_TAG);
        nbt.remove(VERTICAL_LAUNCH_CORE_POS_TAG);
        nbt.remove(ELECTRO_MAGNET_RAIL_CORE_POS_TAG);
        nbt.remove(STORED_TYPE_TAG);
    }

    private static void putBlockPos(CompoundTag nbt, String key, BlockPos pos) {
        nbt.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }

    public static BlockPos getBlockPos(CompoundTag nbt, String key) {
        int[] pos = nbt.getIntArray(key);
        if (pos.length < 3) {
            return BlockPos.ZERO;
        }
        return new BlockPos(pos[0], pos[1], pos[2]);
    }
}
