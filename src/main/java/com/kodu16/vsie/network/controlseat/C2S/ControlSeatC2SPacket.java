package com.kodu16.vsie.network.controlseat.C2S;

import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.entity.ControlSeatMountEntity;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3d;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ControlSeatC2SPacket implements CustomPacketPayload {
    // Function: NeoForge 1.21.1 payload id and stream codec registration entry.
    public static final CustomPacketPayload.Type<ControlSeatC2SPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "controlseat_c2s_controlseatc2spacket"));
    public static final StreamCodec<FriendlyByteBuf, ControlSeatC2SPacket> STREAM_CODEC = CustomPacketPayload.codec(ControlSeatC2SPacket::encode, ControlSeatC2SPacket::decode);
    private static final float CONTROL_DEADZONE = 0.025F;
    private static final float KEY_ROLL_TORQUE_SCALE = 0.02F;
    public static final float MOUSE_TORQUE_CURVE_EXPONENT = 2.0F;

    public static final Logger LOGGER = LogUtils.getLogger();
    public final BlockPos pos;
    public final float mousex;
    public final float mousey;
    public final float roll;
    public final int keys;
    public final boolean mouseLpress;
    public final boolean isViewLocked;

    public ControlSeatC2SPacket(BlockPos pos, float mousex, float mousey, float roll, int keys, boolean mouseLpress, boolean isViewLocked) {
        this.pos = pos;
        this.mousex = mousex;
        this.mousey = mousey;
        this.roll = roll;
        this.keys = keys;
        this.mouseLpress = mouseLpress;
        this.isViewLocked = isViewLocked;
    }

    public static void encode(ControlSeatC2SPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeFloat(pkt.mousex);
        buf.writeFloat(pkt.mousey);
        buf.writeFloat(pkt.roll);
        buf.writeVarInt(pkt.keys);
        buf.writeBoolean(pkt.mouseLpress);
        buf.writeBoolean(pkt.isViewLocked);
    }

    public static ControlSeatC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        float mousex = buf.readFloat();
        float mousey = buf.readFloat();
        float roll = buf.readFloat();
        int keys = buf.readVarInt();
        boolean mouseLpress = buf.readBoolean();
        boolean isViewLocked = buf.readBoolean();
        return new ControlSeatC2SPacket(pos, mousex, mousey, roll, keys, mouseLpress, isViewLocked);
    }

    // Function: NeoForge handler entry that reuses the existing Supplier<NetworkEvent.Context> path.
    public static void handle(ControlSeatC2SPacket pkt, IPayloadContext context) {
        handle(pkt, () -> new NetworkEvent.Context(context));
    }

    public static void handle(ControlSeatC2SPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            ServerLevel level = sender.serverLevel();
            BlockPos pos = pkt.pos;
            int keys = pkt.keys;
            BlockEntity seat = level.getBlockEntity(pos);
            if (!(seat instanceof ControlSeatBlockEntity controlSeat)) {
                sender.sendSystemMessage(Component.literal("Invalid control seat at " + pos));
                return;
            }
            if (!(sender.getVehicle() instanceof ControlSeatMountEntity mount) || !mount.getBoundBlockPos().equals(pos)) {
                PlayerMayClearInput(sender, controlSeat);
                return;
            }

            boolean isThrottlePressed = (keys & Keys.THROTTLE) != 0;
            boolean isBrakePressed = (keys & Keys.BRAKE) != 0;
            boolean isRollLeftPressed = (keys & Keys.ROLLL) != 0;
            boolean isRollRightPressed = (keys & Keys.ROLLR) != 0;
            boolean isWPressed = (keys & Keys.KEY_W) != 0;
            boolean isSPressed = (keys & Keys.KEY_S) != 0;
            boolean isControlLeftPressed = (keys & Keys.KEY_CONTROL_LEFT) != 0;
            boolean isControlRightPressed = (keys & Keys.KEY_CONTROL_RIGHT) != 0;
            int finalthrottledelta = isThrottlePressed ? 1 : (isBrakePressed ? -1 : 0);
            float yawInput = sanitizeMouseTorqueAxis(pkt.mousex);
            float pitchInput = sanitizeMouseTorqueAxis(pkt.mousey);
            // Function: A/D roll is intentionally weaker than mouse torque so it does not snap large sublevels.
            float rollInput = clampControlAxis(((isRollRightPressed ? 1.0F : 0.0F) - (isRollLeftPressed ? 1.0F : 0.0F)) * KEY_ROLL_TORQUE_SCALE + sanitizeControlAxis(pkt.roll));

            if (!Float.isFinite(pkt.mousex) || !Float.isFinite(pkt.mousey) || !Float.isFinite(pkt.roll)) {
                sender.sendSystemMessage(Component.literal("Invalid torque input! check packet"));
                return;
            }

            ControlSeatServerData serverData = controlSeat.getServerData();
            serverData.isviewlocked = pkt.isViewLocked;
            if (serverData.isWarpPreparing) {
                serverData.setTorque(new Vector3d(0, 0, 0));
                serverData.setForce(new Vector3d(0, 0, 0));
                serverData.setThrottle(0);
            } else if (pkt.isViewLocked) {
                int finalthrottle = Math.max(-100, Math.min(serverData.getThrottle() + finalthrottledelta, 100));
                // Function: locked view uses W/S and Z/C for translation; A/D always stays roll.
                double verticalInput = (isWPressed ? 1.0D : 0.0D) - (isSPressed ? 1.0D : 0.0D);
                double lateralInput = (isControlRightPressed ? 1.0D : 0.0D) - (isControlLeftPressed ? 1.0D : 0.0D);
                serverData.setForce(normalizeFlatTranslationInput(verticalInput, lateralInput));
                serverData.setTorque(new Vector3d(rollInput, -yawInput, pitchInput));
                serverData.setThrottle(finalthrottle);
            } else {
                // Function: unlocked view maps W/S to pitch, A/D to roll, and Z/C to yaw.
                float pitchKeyInput = clampControlAxis((isWPressed ? 1.0F : 0.0F) - (isSPressed ? 1.0F : 0.0F));
                float yawKeyInput = clampControlAxis((isControlRightPressed ? 1.0F : 0.0F) - (isControlLeftPressed ? 1.0F : 0.0F));
                int finalthrottle = Math.max(-100, Math.min(serverData.getThrottle() + finalthrottledelta, 100));
                serverData.setForce(new Vector3d(0, 0, 0));
                serverData.setTorque(new Vector3d(rollInput, -yawKeyInput, pitchKeyInput));
                serverData.setThrottle(finalthrottle);
            }

            serverData.isfiring = pkt.mouseLpress;
            controlSeat.setChanged();
        });
        ctx.setPacketHandled(true);
    }

    private static void PlayerMayClearInput(ServerPlayer sender, ControlSeatBlockEntity controlSeat) {
        net.minecraft.world.entity.player.Player currentPlayer = controlSeat.getServerData().getPlayer();
        if (currentPlayer == null || currentPlayer.getUUID().equals(sender.getUUID())) {
            controlSeat.clearControlInput();
        }
    }

    private static float sanitizeControlAxis(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        float clamped = clampControlAxis(value);
        return Math.abs(clamped) < CONTROL_DEADZONE ? 0.0F : clamped;
    }

    private static float sanitizeMouseTorqueAxis(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }

        float clamped = clampControlAxis(value);
        float magnitude = Math.abs(clamped);
        if (magnitude < CONTROL_DEADZONE) {
            return 0.0F;
        }

        float normalized = (magnitude - CONTROL_DEADZONE) / (1.0F - CONTROL_DEADZONE);
        float curved = (float) Math.pow(normalized, Math.max(0.1F, MOUSE_TORQUE_CURVE_EXPONENT));
        return Math.copySign(curved, clamped);
    }

    private static float clampControlAxis(float value) {
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    private static Vector3d normalizeFlatTranslationInput(double verticalInput, double lateralInput) {
        Vector3d input = new Vector3d(0.0D, verticalInput, lateralInput);
        // Function: diagonal translation keeps the same 10-percent-throttle authority as a single axis.
        if (input.lengthSquared() > 1.0D) {
            input.normalize();
        }
        return input;
    }

    public static final class Keys {
        public static final int THROTTLE = 1 << 0;
        public static final int BRAKE = 1 << 1;
        public static final int SCAN_PERIPHERAL = 1 << 2;
        public static final int ROLLL = 1 << 3;
        public static final int ROLLR = 1 << 4;
        public static final int SPACE = 1 << 5;
        public static final int SHIFT = 1 << 6;
        public static final int CTRL = 1 << 7;
        public static final int MOUSEL = 1 << 8;
        public static final int MOUSER = 1 << 9;
        public static final int KEY_W = 1 << 10;
        public static final int KEY_S = 1 << 11;
        public static final int KEY_CONTROL_LEFT = 1 << 12;
        public static final int KEY_CONTROL_RIGHT = 1 << 13;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
