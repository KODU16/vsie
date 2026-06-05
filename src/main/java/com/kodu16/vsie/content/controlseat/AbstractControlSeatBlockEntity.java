package com.kodu16.vsie.content.controlseat;

import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractControlSeatBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, GeoBlockEntity {

    // Common State
    protected ControlSeatServerData controlseatData;
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public float calculatedstrength = 0;

    //energy
    public int energyspendpertick = 10;
    public int capacitorenergy = 0;
    public int totalenergy = 100;
    public int totalenergyavalible = 0;
    public boolean linkedBatteryPowerAvailableThisTick = false;

    //fuel
    public int fuelspendcurrenttick = 0;
    public int capacitorfuel = 0;
    public int totalfuel = 100;
    public int totalfuelavalible = 0;

    //shield
    public double avalibleshield = 0;

    //Links(nbt:true)
    private static final int LINKED_PERIPHERAL_MISSING_TICKS_BEFORE_REMOVAL = 5;
    private final List<Vec3> linkedThrusters = new ArrayList<>();
    public final List<Vec3> linkedWeapons = new ArrayList<>();
    public final List<Vec3> linkedShields = new ArrayList<>();
    private final List<Vec3> linkedTurrets = new ArrayList<>();
    private final List<Vec3> linkedBatteries = new ArrayList<>();
    public final List<Vec3> linkedFuelTanks = new ArrayList<>();
    public final List<Vec3> linkedAmmoboxes = new ArrayList<>();
    public final List<Vec3> linkedScreens = new ArrayList<>();
    private final Map<String, MissingLinkedPeripheralState> missingLinkedPeripheralStates = new HashMap<>();

    public AbstractControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        controlseatData = new ControlSeatServerData();
    }

    protected abstract boolean isWorking();

    public ControlSeatServerData getControlSeatData() {
        return controlseatData;
    }

    public abstract void onRemove();

    public abstract String getcontrolseattype();

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.putString("enemy", controlseatData.enemy);
        nbt.putString("ally", controlseatData.ally);

        writeVec3List(nbt, "Thrusters", linkedThrusters);
        writeVec3List(nbt, "Weapons", linkedWeapons);
        writeVec3List(nbt, "Shields", linkedShields);
        writeVec3List(nbt, "Turrets", linkedTurrets);
        writeVec3List(nbt, "Batteries", linkedBatteries);
        writeVec3List(nbt, "Fueltanks", linkedFuelTanks);
        writeVec3List(nbt, "Ammoboxes", linkedAmmoboxes);
        writeVec3List(nbt, "Screens", linkedScreens);
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(nbt, registries, clientPacket);
        if (this.controlseatData == null) {
            this.controlseatData = new ControlSeatServerData();
        }
        if (nbt.contains("enemy")) {
            this.controlseatData.enemy = nbt.getString("enemy");
        }
        if (nbt.contains("ally")) {
            this.controlseatData.ally = nbt.getString("ally");
        }

        linkedThrusters.clear();
        linkedWeapons.clear();
        linkedShields.clear();
        linkedTurrets.clear();
        linkedBatteries.clear();
        linkedFuelTanks.clear();
        linkedAmmoboxes.clear();
        linkedScreens.clear();
        missingLinkedPeripheralStates.clear();

        readVec3List(nbt, "Thrusters", linkedThrusters);
        readVec3List(nbt, "Weapons", linkedWeapons);
        readVec3List(nbt, "Shields", linkedShields);
        readVec3List(nbt, "Turrets", linkedTurrets);
        readVec3List(nbt, "Batteries", linkedBatteries);
        readVec3List(nbt, "Fueltanks", linkedFuelTanks);
        readVec3List(nbt, "Ammoboxes", linkedAmmoboxes);
        readVec3List(nbt, "Screens", linkedScreens);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // Function: linker overlays need the control seat's linked peripheral lists on the client.
        write(tag, registries, true);
        return tag;
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

    public void setEnemy(String str) {
        controlseatData.enemy = str;
    }

    public void setAlly(String str) {
        SubLevel subLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            serverSubLevel.setName(processSlug(serverSubLevel.getName(), str));
        }
        controlseatData.ally = str;
    }

    public void addLinkedPeripheral(Vec3 pos, int type) {
        List<Vec3> linkedPeripherals = getLinkedPeripheralOffsets(type);
        if (linkedPeripherals == null) {
            return;
        }

        Vec3 relativeOffset = toRelativeLinkedPeripheralOffset(pos);
        Vec3 storedOffset = findStoredLinkedOffset(linkedPeripherals, relativeOffset);
        if (storedOffset == null) {
            linkedPeripherals.add(relativeOffset);
            LogUtils.getLogger().warn("adding linked peripheral offset to controlseat: " + relativeOffset);
            setChanged();
        } else {
            clearMissingLinkedPeripheralState(storedOffset, type);
        }
        // Function: push updated relative linker data to clients after peripheral link changes.
        sendData();
    }

    public void removeLinkedPeripheral(Vec3 pos, int type) {
        Vec3 storedOffset = findStoredLinkedOffset(pos, type);
        if (storedOffset == null || !shouldRemoveMissingLinkedPeripheral(storedOffset, type)) {
            return;
        }

        List<Vec3> linkedPeripherals = getLinkedPeripheralOffsets(type);
        if (linkedPeripherals == null || !linkedPeripherals.remove(storedOffset)) {
            return;
        }

        clearMissingLinkedPeripheralState(storedOffset, type);
        setChanged();
        // Function: keep client-side linker overlays consistent after a missing peripheral expires.
        sendData();
    }

    public void confirmLinkedPeripheralPresent(Vec3 pos, int type) {
        Vec3 storedOffset = findStoredLinkedOffset(pos, type);
        if (storedOffset != null) {
            clearMissingLinkedPeripheralState(storedOffset, type);
        }
    }

    public void forEachLinkedPeripheral(Consumer<Vec3> action, int type) {
        List<Vec3> linkedPeripherals = getLinkedPeripheralOffsets(type);
        if (linkedPeripherals == null) {
            return;
        }

        for (Vec3 relativeOffset : new ArrayList<>(linkedPeripherals)) {
            action.accept(toAbsoluteLinkedPeripheralPos(relativeOffset));
        }
    }

    public List<BlockPos> getLinkedTurretPositionsInOrder() {
        List<BlockPos> turretPositions = new ArrayList<>(linkedTurrets.size());
        for (Vec3 relativeOffset : linkedTurrets) {
            turretPositions.add(BlockPos.containing(toAbsoluteLinkedPeripheralPos(relativeOffset)));
        }
        // Function: HUD weapon markers use the saved linker order as the stable external peripheral index.
        return turretPositions;
    }

    private List<Vec3> getLinkedPeripheralOffsets(int type) {
        return switch (type) {
            case 0 -> linkedThrusters;
            case 1 -> linkedWeapons;
            case 2 -> linkedShields;
            case 3 -> linkedTurrets;
            case 4 -> linkedBatteries;
            case 5 -> linkedFuelTanks;
            case 6 -> linkedAmmoboxes;
            case 7 -> linkedScreens;
            default -> null;
        };
    }

    private Vec3 toRelativeLinkedPeripheralOffset(Vec3 absolutePos) {
        BlockPos peripheralPos = BlockPos.containing(absolutePos);
        BlockPos seatPos = getBlockPos();
        // Function: persist links as seat-relative block offsets so sublevel pose rebuilds cannot invalidate them.
        return new Vec3(
                peripheralPos.getX() - seatPos.getX(),
                peripheralPos.getY() - seatPos.getY(),
                peripheralPos.getZ() - seatPos.getZ()
        );
    }

    private Vec3 toAbsoluteLinkedPeripheralPos(Vec3 relativeOffset) {
        BlockPos offset = BlockPos.containing(relativeOffset);
        BlockPos absolutePos = getBlockPos().offset(offset.getX(), offset.getY(), offset.getZ());
        return Vec3.atLowerCornerOf(absolutePos);
    }

    private Vec3 findStoredLinkedOffset(Vec3 pos, int type) {
        List<Vec3> linkedPeripherals = getLinkedPeripheralOffsets(type);
        if (linkedPeripherals == null) {
            return null;
        }

        Vec3 relativeOffset = toRelativeLinkedPeripheralOffset(pos);
        Vec3 storedOffset = findStoredLinkedOffset(linkedPeripherals, relativeOffset);
        return storedOffset != null ? storedOffset : findStoredLinkedOffset(linkedPeripherals, pos);
    }

    private Vec3 findStoredLinkedOffset(List<Vec3> linkedPeripherals, Vec3 relativeOffset) {
        BlockPos targetOffset = BlockPos.containing(relativeOffset);
        for (Vec3 linkedOffset : linkedPeripherals) {
            if (BlockPos.containing(linkedOffset).equals(targetOffset)) {
                return linkedOffset;
            }
        }
        return null;
    }

    private boolean shouldRemoveMissingLinkedPeripheral(Vec3 relativeOffset, int type) {
        String key = getMissingLinkedPeripheralKey(relativeOffset, type);
        long currentTick = level == null ? Long.MIN_VALUE : level.getGameTime();
        MissingLinkedPeripheralState state = missingLinkedPeripheralStates.computeIfAbsent(
                key,
                ignored -> new MissingLinkedPeripheralState()
        );

        if (state.lastMissingTick == currentTick) {
            return state.missingTicks >= LINKED_PERIPHERAL_MISSING_TICKS_BEFORE_REMOVAL;
        }

        state.missingTicks = state.lastMissingTick == currentTick - 1 ? state.missingTicks + 1 : 1;
        state.lastMissingTick = currentTick;
        return state.missingTicks >= LINKED_PERIPHERAL_MISSING_TICKS_BEFORE_REMOVAL;
    }

    private void clearMissingLinkedPeripheralState(Vec3 relativeOffset, int type) {
        missingLinkedPeripheralStates.remove(getMissingLinkedPeripheralKey(relativeOffset, type));
    }

    private String getMissingLinkedPeripheralKey(Vec3 relativeOffset, int type) {
        BlockPos offset = BlockPos.containing(relativeOffset);
        return type + ":" + offset.getX() + "," + offset.getY() + "," + offset.getZ();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    public static String processSlug(String a, String b) {
        if (a != null && a.matches("^\\[.*?\\].*")) {
            int endIndex = a.indexOf(']');
            if (endIndex != -1) {
                String suffix = a.substring(endIndex + 1);
                return "[" + b + "]" + suffix;
            }
        }
        return "[" + b + "]" + a;
    }

    private void writeVec3List(CompoundTag nbt, String key, List<Vec3> positions) {
        ListTag list = new ListTag();
        for (Vec3 vec : positions) {
            CompoundTag vecTag = new CompoundTag();
            vecTag.putDouble("x", vec.x);
            vecTag.putDouble("y", vec.y);
            vecTag.putDouble("z", vec.z);
            list.add(vecTag);
        }
        nbt.put(key, list);
    }

    private void readVec3List(CompoundTag nbt, String key, List<Vec3> targetList) {
        if (!nbt.contains(key, Tag.TAG_LIST)) return;

        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag vecTag = list.getCompound(i);
            double x = vecTag.getDouble("x");
            double y = vecTag.getDouble("y");
            double z = vecTag.getDouble("z");
            targetList.add(new Vec3(x, y, z));
        }
    }

    private static class MissingLinkedPeripheralState {
        private int missingTicks;
        private long lastMissingTick = Long.MIN_VALUE;
    }
}
