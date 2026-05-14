package com.kodu16.vsie.content.item.linker;

import com.kodu16.vsie.content.controlseat.AbstractControlSeatBlockEntity;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.shield.ShieldGeneratorBlockEntity;
import com.kodu16.vsie.content.storage.energybattery.AbstractEnergyBatteryBlockEntity;
import com.kodu16.vsie.content.storage.fueltank.AbstractFuelTankBlockEntity;
import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotBlockEntity;
import com.kodu16.vsie.content.weapon.missile_launcher.block.VerticleLaunchingSlotCoreBlockEntity;
import com.kodu16.vsie.utility.ItemStackNbt;
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
    private static final String STORED_TYPE_TAG = "StoredTargetType";
    private static final String TYPE_CONTROL_SEAT = "control_seat";
    private static final String TYPE_VERTICAL_LAUNCH_CORE = "verticle_launching_slot_core";

    public linker(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isRightClickingAir(level, player)) {
            if (!level.isClientSide) {
                // Function: Shift-right-clicking air clears every linker binding mode without touching unrelated item data.
                CompoundTag nbt = ItemStackNbt.getOrCreate(stack);
                clearStoredTargets(nbt);
                ItemStackNbt.set(stack, nbt);
                player.displayClientMessage(Component.literal("§aLinker 绑定已清空"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private boolean isRightClickingAir(Level level, Player player) {
        // Function: use() can be reached after some block interactions, so only clear when the player ray actually misses blocks.
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

        if (clickedBlockEntity instanceof VerticleLaunchingSlotCoreBlockEntity && !nbt.contains(CONTROL_SEAT_POS_TAG)) {
            // Function: recording a launch-slot core clears any existing control-seat target to avoid mixed linker modes.
            clearStoredTargets(nbt);
            putBlockPos(nbt, VERTICAL_LAUNCH_CORE_POS_TAG, clickedPos);
            nbt.putString(STORED_TYPE_TAG, TYPE_VERTICAL_LAUNCH_CORE);
            ItemStackNbt.set(stack, nbt);
            player.displayClientMessage(Component.literal("§a已记录垂直发射槽核心: " + clickedPos.toShortString()), true);
            return InteractionResult.CONSUME;
        }

        if (nbt.contains(VERTICAL_LAUNCH_CORE_POS_TAG)) {
            return handleVerticalLaunchSlotLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
        }

        if (!nbt.contains(CONTROL_SEAT_POS_TAG)) {
            return recordControlSeat(player, stack, nbt, clickedPos, clickedBlockEntity);
        }

        return handleControlSeatPeripheralLink(level, player, stack, nbt, clickedPos, clickedBlockEntity);
    }

    private InteractionResult recordControlSeat(ServerPlayer player, ItemStack stack, CompoundTag nbt, BlockPos clickedPos, BlockEntity clickedBlockEntity) {
        if (clickedBlockEntity instanceof AbstractControlSeatBlockEntity) {
            // Function: recording a control seat clears launch-slot-core data so the linker has one active target type.
            clearStoredTargets(nbt);
            putBlockPos(nbt, CONTROL_SEAT_POS_TAG, clickedPos);
            nbt.putString(STORED_TYPE_TAG, TYPE_CONTROL_SEAT);
            ItemStackNbt.set(stack, nbt);
            player.displayClientMessage(Component.literal("§a已记录控制椅: " + clickedPos.toShortString()), true);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.literal("§c请先右键控制椅或垂直发射槽核心进行记录"), true);
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
            player.displayClientMessage(Component.literal("§c记录的垂直发射槽核心已不存在，已清空记录"), true);
            return InteractionResult.CONSUME;
        }

        if (clickedBlockEntity instanceof VerticleLaunchingSlotBlockEntity) {
            // Function: each connected slot receives a stable 1-based number according to the core connection order.
            int slotIndex = core.addLinkedSlot(clickedPos);
            player.displayClientMessage(Component.literal("§b已将垂直发射槽 " + clickedPos.toShortString()
                    + " 连接到核心 " + corePos.toShortString() + "，编号 #" + slotIndex), true);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.literal("§c当前记录的是垂直发射槽核心，请右键垂直发射槽进行连接"), true);
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
            player.displayClientMessage(Component.literal("§c记录的控制椅已不存在，已清空记录"), true);
            return InteractionResult.CONSUME;
        }

        Vec3 peripheralPos = Vec3.atLowerCornerOf(clickedPos);
        if (clickedBlockEntity instanceof VerticleLaunchingSlotBlockEntity) {
            // Function: launch slots are linked through their core, never directly through a control seat.
            player.displayClientMessage(Component.literal("§cslot只能连接core而非直连控制椅"), true);
            return InteractionResult.CONSUME;
        }
        if (clickedBlockEntity instanceof AbstractThrusterBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, 0, "推进器");
        }
        if (clickedBlockEntity instanceof AbstractWeaponBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, 1, "武器");
        }
        if (clickedBlockEntity instanceof ShieldGeneratorBlockEntity shield) {
            shield.linkedcontrolseatpos = controllerPos;
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, 2, "护盾");
        }
        if (clickedBlockEntity instanceof AbstractTurretBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, 3, "炮塔");
        }
        if (clickedBlockEntity instanceof AbstractEnergyBatteryBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, 4, "电池");
        }
        if (clickedBlockEntity instanceof AbstractFuelTankBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, 5, "燃料罐");
        }
        if (clickedBlockEntity instanceof AbstractScreenBlockEntity) {
            return linkControlSeatPeripheral(player, controlSeat, controllerPos, peripheralPos, clickedPos, 7, "屏幕");
        }

        player.displayClientMessage(Component.literal("§c该方块不是可连接的控制椅外设"), true);
        return InteractionResult.CONSUME;
    }

    private InteractionResult linkControlSeatPeripheral(ServerPlayer player, AbstractControlSeatBlockEntity controlSeat,
                                                        BlockPos controllerPos, Vec3 peripheralPos, BlockPos clickedPos,
                                                        int type, String displayName) {
        // Function: keep the original control-seat linker peripheral type ids for existing control logic.
        controlSeat.addLinkedPeripheral(peripheralPos, type);
        player.displayClientMessage(Component.literal("§b已将控制椅 " + controllerPos.toShortString()
                + " 与" + displayName + " " + clickedPos.toShortString() + " 连接"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = ItemStackNbt.get(stack);
        if (tag != null && tag.contains(VERTICAL_LAUNCH_CORE_POS_TAG)) {
            BlockPos pos = getBlockPos(tag, VERTICAL_LAUNCH_CORE_POS_TAG);
            tooltip.add(Component.literal("§e已记录: 垂直发射槽核心 " + pos.toShortString()));
            tooltip.add(Component.literal("§7右键垂直发射槽可按顺序连接并分配编号"));
        } else if (tag != null && tag.contains(CONTROL_SEAT_POS_TAG)) {
            BlockPos pos = getBlockPos(tag, CONTROL_SEAT_POS_TAG);
            tooltip.add(Component.literal("§e已记录: 控制椅 " + pos.toShortString()));
            tooltip.add(Component.literal("§7右键推进器、武器、炮塔等外设进行连接"));
        } else {
            tooltip.add(Component.literal("§7右键控制椅或垂直发射槽核心进行记录"));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    private static void clearStoredTargets(CompoundTag nbt) {
        nbt.remove(CONTROL_SEAT_POS_TAG);
        nbt.remove(VERTICAL_LAUNCH_CORE_POS_TAG);
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
