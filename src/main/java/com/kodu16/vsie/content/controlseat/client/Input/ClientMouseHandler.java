package com.kodu16.vsie.content.controlseat.client.Input;

import com.kodu16.vsie.content.controlseat.client.ControlSeatClientData;
import com.kodu16.vsie.content.controlseat.client.ControlSeatWarpSelectionScreen;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatInputC2SPacket;
import com.kodu16.vsie.network.controlseat.C2S.ControlSeatWarpCancelC2SPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.kodu16.vsie.registries.vsieKeyMappings;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class ClientMouseHandler {
    private static final double MANUAL_AIM_DISTANCE = 1024.0D;
    private static final double CONTROL_MOUSE_X_RANGE = 2560.0D;
    private static final double CONTROL_MOUSE_Y_RANGE = 1440.0D;

    public static final Logger LOGGER = LogUtils.getLogger();
    public boolean viewlock = false;

    public static void clearInactiveSeatState(LocalPlayer player) {
        if (player == null) {
            return;
        }
        ControlSeatClientData data = ClientDataManager.getClientData(player);
        if (data == null || player.getVehicle() instanceof ControlSeatMountEntity) {
            return;
        }
        if (data.isDimensionTransferPending()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientDataManager.clearSeatBinding(player);
        if (minecraft.screen instanceof ControlSeatWarpSelectionScreen) {
            minecraft.setScreen(null);
        }
    }

    public static void handle(LocalPlayer player, BlockPos pos) {
        if (player == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(player.getVehicle() instanceof ControlSeatMountEntity seat)) {
            clearInactiveSeatState(player);
            return;
        }
        if (!seat.getBoundBlockPos().equals(pos)) {
            return;
        }

        ControlSeatClientData data = ClientDataManager.getClientDataForSeat(player, pos);
        if (data == null) {
            return;
        }

        data.setUserUUID(player.getUUID());
        if (data.consumePendingViewRecentering() && data.isViewLocked()) {
            centerLockedView(player, minecraft, pos);
        }
        handleMouseLock(player, data, minecraft, pos);
        handleWarpSelection(data, minecraft, pos);

        Vec3 aimTargetPos = calculateManualAimTargetPos(player);
        if (data.isWarpPreparing || data.hasPendingWarpTeleport) {
            // Function: warp owns rotation, so clear stale mouse offset before manual control resumes.
            data.reset();
            ClientSeatInputSender.tickSend(pos, player.getUUID(), 0, 0, 0, false, data.viewLock, aimTargetPos);
            return;
        }

        double dx = data.getAccumulatedMousex();
        double dy = data.getAccumulatedMousey();
        if (data.isViewLocked()) {
            double controlX = normalizeControlInput(dx, CONTROL_MOUSE_X_RANGE);
            double controlY = normalizeControlInput(dy, CONTROL_MOUSE_Y_RANGE);
            ClientSeatInputSender.tickSend(pos, player.getUUID(), controlX, controlY, 0, data.mouseLpress, data.viewLock, aimTargetPos);
        } else {
            // Function: unlocked view still uses left click to fire active weapons and manual heavy turrets.
            ClientSeatInputSender.tickSend(pos, player.getUUID(), 0, 0, 0, data.mouseLpress, data.viewLock, aimTargetPos);
            data.reset();
        }
    }

    private static Vec3 calculateManualAimTargetPos(LocalPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle().normalize();
        return eyePos.add(lookVec.scale(MANUAL_AIM_DISTANCE));
    }

    private static double normalizeControlInput(double value, double range) {
        return Math.max(-1.0D, Math.min(1.0D, value / range));
    }

    private static void handleWarpSelection(ControlSeatClientData data, Minecraft minecraft, BlockPos pos) {
        if (!vsieKeyMappings.KEY_START_WARP.consumeClick()) {
            return;
        }
        if (minecraft.level == null) {
            return;
        }
        if (data.isWarpPreparing) {
            // Function: pressing warp again cancels this chair's active preparation instead of opening a second menu.
            ModNetworking.sendToServer(new ControlSeatWarpCancelC2SPacket(pos, currentSeatEntityId(playerFromMinecraft(minecraft))));
            if (minecraft.screen instanceof ControlSeatWarpSelectionScreen) {
                minecraft.setScreen(null);
            }
            return;
        }
        if (data.hasPendingWarpTeleport) {
            if (minecraft.screen instanceof ControlSeatWarpSelectionScreen) {
                minecraft.setScreen(null);
            }
            return;
        }
        if (minecraft.screen instanceof ControlSeatWarpSelectionScreen) {
            minecraft.setScreen(null);
            return;
        }
        minecraft.setScreen(new ControlSeatWarpSelectionScreen(pos));
    }

    public static void handleMouseLock(LocalPlayer player, ControlSeatClientData data, Minecraft minecraft, BlockPos pos) {
        KeyMapping jumpKey = vsieKeyMappings.KEY_TOGGLE_LOCK;
        if (player == null || !(player.getVehicle() instanceof ControlSeatMountEntity)) {
            data.disableViewLock();
            data.clearUserUUID();
            if (minecraft.screen instanceof ControlSeatWarpSelectionScreen) {
                minecraft.setScreen(null);
            }
            return;
        }

        if (jumpKey.isDown() && System.currentTimeMillis() - data.getLastKeyPressTime() > 800) {
            data.requestViewLock(!data.isViewLocked());
            data.reset();
            if (data.isViewLocked()) {
                player.displayClientMessage(Component.translatable("message.vsie.control_seat.view_lock.enabled"), true);
                centerLockedView(player, minecraft, pos);
            } else {
                player.displayClientMessage(Component.translatable("message.vsie.control_seat.view_lock.disabled"), true);
            }
            Vec3 aimTargetPos = calculateManualAimTargetPos(player);
            ModNetworking.sendToServer(new ControlSeatInputC2SPacket(
                    pos,
                    currentSeatEntityId(player),
                    0,
                    data.viewLock,
                    aimTargetPos.x,
                    aimTargetPos.y,
                    aimTargetPos.z
            ));
            data.updatelastKeyPressTime();
        }
    }

    /**
     * Reuses the seat-facing center used by manual view locking after a dimension remount.
     */
    private static void centerLockedView(LocalPlayer player, Minecraft minecraft, BlockPos pos) {
        Level level = minecraft.level;
        if (level == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(BlockStateProperties.FACING)) {
            return;
        }
        Direction facing = state.getValue(BlockStateProperties.FACING);
        int yRot;
        if (facing == Direction.NORTH) {
            yRot = 0;
        } else if (facing == Direction.SOUTH) {
            yRot = 180;
        } else if (facing == Direction.EAST) {
            yRot = 90;
        } else {
            yRot = 270;
        }
        player.setYRot(yRot);
        player.setXRot(0);
        player.setYHeadRot(yRot);
        player.setYBodyRot(0);
    }

    private static LocalPlayer playerFromMinecraft(Minecraft minecraft) {
        return minecraft == null ? null : minecraft.player;
    }

    private static java.util.UUID currentSeatEntityId(LocalPlayer player) {
        if (player != null && player.getVehicle() instanceof ControlSeatMountEntity mount) {
            return mount.getUUID();
        }
        return new java.util.UUID(0L, 0L);
    }
}
