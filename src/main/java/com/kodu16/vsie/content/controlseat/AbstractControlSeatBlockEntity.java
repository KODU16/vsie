package com.kodu16.vsie.content.controlseat;

import com.mojang.logging.LogUtils;
import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.kodu16.vsie.foundation.ServerShipUtils;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractControlSeatBlockEntity extends SmartBlockEntity {
    public static final int ENEMY_CANNON_PERIPHERAL_TYPE = 8;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LINK_TRACE_PREFIX = "[VSIE-LINK-TRACE]";

    // Common State
    protected ControlSeatServerData controlseatData;

    //Links(nbt:true)
    private static final int LINKED_PERIPHERAL_MISSING_TICKS_BEFORE_REMOVAL = 5;
    private static final String LINKED_PERIPHERAL_POSITION_FORMAT_TAG = "LinkedPeripheralPositionsRelative";
    private static final int SABLE_PLOT_COORDINATE_THRESHOLD = 1_000_000;
    private final List<Vec3> linkedThrusters = new ArrayList<>();
    public final List<Vec3> linkedWeapons = new ArrayList<>();
    public final List<Vec3> linkedShields = new ArrayList<>();
    private final List<Vec3> linkedTurrets = new ArrayList<>();
    private final List<Vec3> linkedBatteries = new ArrayList<>();
    public final List<Vec3> linkedFuelTanks = new ArrayList<>();
    public final List<Vec3> linkedAmmoboxes = new ArrayList<>();
    public final List<Vec3> linkedScreens = new ArrayList<>();
    private final List<Vec3> linkedEnemyCannons = new ArrayList<>();
    private final Map<String, MissingLinkedPeripheralState> missingLinkedPeripheralStates = new HashMap<>();
    private String lastLinkedPeripheralResolutionSignature = "";

    public AbstractControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        controlseatData = new ControlSeatServerData();
    }

    public ControlSeatServerData getControlSeatData() {
        return controlseatData;
    }

    // Function: concrete controllers explicitly whitelist their compatible peripheral families.
    public abstract boolean supportsLinkedPeripheralType(int type);

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(nbt, registries, clientPacket);
        nbt.putString("enemy", controlseatData.enemy);
        nbt.putString("ally", controlseatData.ally);

        writeSupportedVec3List(nbt, "Thrusters", linkedThrusters, 0);
        writeSupportedVec3List(nbt, "Weapons", linkedWeapons, 1);
        writeSupportedVec3List(nbt, "Shields", linkedShields, 2);
        writeSupportedVec3List(nbt, "Turrets", linkedTurrets, 3);
        writeSupportedVec3List(nbt, "Batteries", linkedBatteries, 4);
        writeSupportedVec3List(nbt, "Fueltanks", linkedFuelTanks, 5);
        writeSupportedVec3List(nbt, "Ammoboxes", linkedAmmoboxes, 6);
        writeSupportedVec3List(nbt, "Screens", linkedScreens, 7);
        writeSupportedVec3List(nbt, "EnemyCannons", linkedEnemyCannons, ENEMY_CANNON_PERIPHERAL_TYPE);
        // Function: distinguish portable seat-relative offsets from legacy absolute Sable plot coordinates.
        nbt.putBoolean(LINKED_PERIPHERAL_POSITION_FORMAT_TAG, true);
        if (!clientPacket || getLinkedPeripheralCount() > 0) {
            traceLinkState("WRITE", clientPacket, getLinkedPeripheralCount(), nbt);
        }
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

        int previousCount = getLinkedPeripheralCount();
        if (!containsLinkedPeripheralPayload(nbt)) {
            // Function: partial update tags must not erase persistent links they do not carry.
            LOGGER.warn("{} phase=READ_SKIPPED_PARTIAL context={} clientPacket={} previous={} keys={}",
                    LINK_TRACE_PREFIX, linkTraceContext(), clientPacket, linkedPeripheralSummary(), nbt.getAllKeys());
            return;
        }

        linkedThrusters.clear();
        linkedWeapons.clear();
        linkedShields.clear();
        linkedTurrets.clear();
        linkedBatteries.clear();
        linkedFuelTanks.clear();
        linkedAmmoboxes.clear();
        linkedScreens.clear();
        linkedEnemyCannons.clear();
        missingLinkedPeripheralStates.clear();
        boolean relativeFormat = nbt.getBoolean(LINKED_PERIPHERAL_POSITION_FORMAT_TAG);
        readSupportedVec3List(nbt, "Thrusters", linkedThrusters, 0, relativeFormat);
        readSupportedVec3List(nbt, "Weapons", linkedWeapons, 1, relativeFormat);
        readSupportedVec3List(nbt, "Shields", linkedShields, 2, relativeFormat);
        readSupportedVec3List(nbt, "Turrets", linkedTurrets, 3, relativeFormat);
        readSupportedVec3List(nbt, "Batteries", linkedBatteries, 4, relativeFormat);
        readSupportedVec3List(nbt, "Fueltanks", linkedFuelTanks, 5, relativeFormat);
        readSupportedVec3List(nbt, "Ammoboxes", linkedAmmoboxes, 6, relativeFormat);
        readSupportedVec3List(nbt, "Screens", linkedScreens, 7, relativeFormat);
        readSupportedVec3List(nbt, "EnemyCannons", linkedEnemyCannons, ENEMY_CANNON_PERIPHERAL_TYPE, relativeFormat);
        traceLinkState("READ_APPLY", clientPacket, previousCount, nbt);
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
            setChanged();
            LOGGER.info("{} phase=LINK_ADD context={} type={} target={} offset={} totals={}",
                    LINK_TRACE_PREFIX, linkTraceContext(), type, BlockPos.containing(pos),
                    BlockPos.containing(relativeOffset), linkedPeripheralSummary());
        } else {
            traceMissingRecovery(storedOffset, type, clearMissingLinkedPeripheralState(storedOffset, type));
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

        MissingLinkedPeripheralState removedState = clearMissingLinkedPeripheralState(storedOffset, type);
        LOGGER.warn("{} phase=MISSING_DELETE context={} type={} target={} offset={} missingTicks={} totals={}",
                LINK_TRACE_PREFIX, linkTraceContext(), type,
                BlockPos.containing(toAbsoluteLinkedPeripheralPos(storedOffset)), BlockPos.containing(storedOffset),
                removedState == null ? LINKED_PERIPHERAL_MISSING_TICKS_BEFORE_REMOVAL : removedState.missingTicks,
                linkedPeripheralSummary());
        setChanged();
        // Function: keep client-side linker overlays consistent after a missing peripheral expires.
        sendData();
    }

    public boolean removeLinkedPeripheralImmediately(Vec3 pos, int type) {
        Vec3 storedOffset = findStoredLinkedOffset(pos, type);
        if (storedOffset == null) {
            return false;
        }

        List<Vec3> linkedPeripherals = getLinkedPeripheralOffsets(type);
        if (linkedPeripherals == null || !linkedPeripherals.remove(storedOffset)) {
            return false;
        }

        clearMissingLinkedPeripheralState(storedOffset, type);
        LOGGER.info("{} phase=LINK_REMOVE_EXPLICIT context={} type={} target={} offset={} totals={}",
                LINK_TRACE_PREFIX, linkTraceContext(), type,
                BlockPos.containing(toAbsoluteLinkedPeripheralPos(storedOffset)), BlockPos.containing(storedOffset),
                linkedPeripheralSummary());
        setChanged();
        // Function: explicit linker unlink should propagate to client-side overlays immediately instead of waiting for missing-tick cleanup.
        sendData();
        return true;
    }

    public boolean hasLinkedPeripheral(Vec3 pos, int type) {
        // Function: linker toggle logic needs to distinguish active links from stale peripheral-side saved positions.
        return findStoredLinkedOffset(pos, type) != null;
    }

    public void confirmLinkedPeripheralPresent(Vec3 pos, int type) {
        Vec3 storedOffset = findStoredLinkedOffset(pos, type);
        if (storedOffset != null) {
            traceMissingRecovery(storedOffset, type, clearMissingLinkedPeripheralState(storedOffset, type));
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

    protected void traceLinkedPeripheralResolutionIfChanged() {
        if (level == null) {
            return;
        }

        int saved = getLinkedPeripheralCount();
        int loaded = 0;
        int sameSubLevel = 0;
        SubLevel seatSubLevel = ServerShipUtils.getSubLevelAtBlockPos(level, getBlockPos());
        StringBuilder failures = new StringBuilder();
        for (int type = 0; type <= ENEMY_CANNON_PERIPHERAL_TYPE; type++) {
            List<Vec3> offsets = getLinkedPeripheralOffsets(type);
            if (offsets == null) {
                continue;
            }
            for (Vec3 offset : offsets) {
                BlockPos targetPos = BlockPos.containing(toAbsoluteLinkedPeripheralPos(offset));
                BlockEntity target = level.getBlockEntity(targetPos);
                boolean isLoaded = target != null;
                boolean sharesSubLevel = isLoaded && areSameSubLevel(
                        seatSubLevel, ServerShipUtils.getSubLevelAtBlockPos(level, targetPos)
                );
                if (isLoaded) {
                    loaded++;
                }
                if (sharesSubLevel) {
                    sameSubLevel++;
                }
                if (!isLoaded || !sharesSubLevel) {
                    if (!failures.isEmpty()) {
                        failures.append(';');
                    }
                    failures.append(type).append('@').append(BlockPos.containing(offset))
                            .append("->").append(targetPos)
                            .append(isLoaded ? ":wrong_sublevel" : ":missing");
                }
            }
        }

        String signature = level.dimension().location() + "|" + getBlockPos() + "|" + saved + "|" + loaded
                + "|" + sameSubLevel + "|" + failures;
        if (signature.equals(lastLinkedPeripheralResolutionSignature)) {
            return;
        }
        lastLinkedPeripheralResolutionSignature = signature;
        // Function: distinguish persisted link counts from links that can actually resolve after a dimension handoff.
        if (saved > 0 && (loaded != saved || sameSubLevel != saved)) {
            LOGGER.warn("{} phase=RESOLUTION_SNAPSHOT context={} saved={} loaded={} sameSubLevel={} failures={}",
                    LINK_TRACE_PREFIX, linkTraceContext(), saved, loaded, sameSubLevel, failures);
        } else {
            LOGGER.info("{} phase=RESOLUTION_SNAPSHOT context={} saved={} loaded={} sameSubLevel={} failures={}",
                    LINK_TRACE_PREFIX, linkTraceContext(), saved, loaded, sameSubLevel, failures);
        }
    }

    private static boolean areSameSubLevel(SubLevel first, SubLevel second) {
        if (first == null || second == null) {
            return false;
        }
        if (first == second) {
            return true;
        }
        return first.getUniqueId() != null && first.getUniqueId().equals(second.getUniqueId());
    }

    public List<BlockPos> getLinkedTurretPositionsInOrder() {
        List<BlockPos> turretPositions = new ArrayList<>(linkedTurrets.size());
        for (Vec3 relativeOffset : linkedTurrets) {
            turretPositions.add(BlockPos.containing(toAbsoluteLinkedPeripheralPos(relativeOffset)));
        }
        // Function: HUD weapon markers use the saved linker order as the stable external peripheral index.
        return turretPositions;
    }

    public List<BlockPos> getLinkedWeaponPositionsInOrder() {
        List<BlockPos> weaponPositions = new ArrayList<>(linkedWeapons.size());
        for (Vec3 relativeOffset : linkedWeapons) {
            weaponPositions.add(BlockPos.containing(toAbsoluteLinkedPeripheralPos(relativeOffset)));
        }
        // Function: ammo boxes refill linked weapons in the same order saved by the linker.
        return weaponPositions;
    }

    public List<BlockPos> getLinkedEnemyCannonPositionsInOrder() {
        List<BlockPos> cannonPositions = new ArrayList<>(linkedEnemyCannons.size());
        for (Vec3 relativeOffset : linkedEnemyCannons) {
            cannonPositions.add(BlockPos.containing(toAbsoluteLinkedPeripheralPos(relativeOffset)));
        }
        return cannonPositions;
    }

    private List<Vec3> getLinkedPeripheralOffsets(int type) {
        if (!supportsLinkedPeripheralType(type)) {
            return null;
        }
        return switch (type) {
            case 0 -> linkedThrusters;
            case 1 -> linkedWeapons;
            case 2 -> linkedShields;
            case 3 -> linkedTurrets;
            case 4 -> linkedBatteries;
            case 5 -> linkedFuelTanks;
            case 6 -> linkedAmmoboxes;
            case 7 -> linkedScreens;
            case ENEMY_CANNON_PERIPHERAL_TYPE -> linkedEnemyCannons;
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
        if (state.missingTicks == 1) {
            LOGGER.warn("{} phase=MISSING_BEGIN context={} type={} target={} offset={} gameTick={}",
                    LINK_TRACE_PREFIX, linkTraceContext(), type,
                    BlockPos.containing(toAbsoluteLinkedPeripheralPos(relativeOffset)),
                    BlockPos.containing(relativeOffset), currentTick);
        }
        return state.missingTicks >= LINKED_PERIPHERAL_MISSING_TICKS_BEFORE_REMOVAL;
    }

    private MissingLinkedPeripheralState clearMissingLinkedPeripheralState(Vec3 relativeOffset, int type) {
        return missingLinkedPeripheralStates.remove(getMissingLinkedPeripheralKey(relativeOffset, type));
    }

    private void traceMissingRecovery(Vec3 relativeOffset, int type, MissingLinkedPeripheralState state) {
        if (state == null || state.missingTicks <= 0) {
            return;
        }
        LOGGER.info("{} phase=MISSING_RECOVERED context={} type={} target={} offset={} missingTicks={}",
                LINK_TRACE_PREFIX, linkTraceContext(), type,
                BlockPos.containing(toAbsoluteLinkedPeripheralPos(relativeOffset)), BlockPos.containing(relativeOffset),
                state.missingTicks);
    }

    private String getMissingLinkedPeripheralKey(Vec3 relativeOffset, int type) {
        BlockPos offset = BlockPos.containing(relativeOffset);
        return type + ":" + offset.getX() + "," + offset.getY() + "," + offset.getZ();
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

    private void writeSupportedVec3List(CompoundTag nbt, String key, List<Vec3> positions, int type) {
        if (supportsLinkedPeripheralType(type)) {
            writeVec3List(nbt, key, positions);
        }
    }

    private void readSupportedVec3List(
            CompoundTag nbt,
            String key,
            List<Vec3> targetList,
            int type,
            boolean relativeFormat
    ) {
        if (supportsLinkedPeripheralType(type)) {
            readVec3List(nbt, key, targetList, relativeFormat);
        }
    }

    private void readVec3List(CompoundTag nbt, String key, List<Vec3> targetList, boolean relativeFormat) {
        if (!nbt.contains(key, Tag.TAG_LIST)) return;

        ListTag list = nbt.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag vecTag = list.getCompound(i);
            double x = vecTag.getDouble("x");
            double y = vecTag.getDouble("y");
            double z = vecTag.getDouble("z");
            targetList.add(normalizeLoadedLinkedOffset(new Vec3(x, y, z), relativeFormat));
        }
    }

    private Vec3 normalizeLoadedLinkedOffset(Vec3 storedPosition, boolean relativeFormat) {
        if (relativeFormat || !looksLikeLegacySablePlotPosition(storedPosition)) {
            return storedPosition;
        }

        BlockPos absolute = BlockPos.containing(storedPosition);
        BlockPos seat = getBlockPos();
        // Function: migrate legacy absolute plot positions once, then persist them with the format marker.
        Vec3 migratedOffset = new Vec3(
                absolute.getX() - seat.getX(),
                absolute.getY() - seat.getY(),
                absolute.getZ() - seat.getZ()
        );
        LOGGER.info("{} phase=LEGACY_POSITION_MIGRATED context={} absolute={} offset={}",
                LINK_TRACE_PREFIX, linkTraceContext(), absolute, BlockPos.containing(migratedOffset));
        return migratedOffset;
    }

    private static boolean looksLikeLegacySablePlotPosition(Vec3 storedPosition) {
        return Math.abs(storedPosition.x) >= SABLE_PLOT_COORDINATE_THRESHOLD
                || Math.abs(storedPosition.z) >= SABLE_PLOT_COORDINATE_THRESHOLD;
    }

    private boolean containsLinkedPeripheralPayload(CompoundTag nbt) {
        return nbt.contains(LINKED_PERIPHERAL_POSITION_FORMAT_TAG)
                || nbt.contains("Thrusters", Tag.TAG_LIST)
                || nbt.contains("Weapons", Tag.TAG_LIST)
                || nbt.contains("Shields", Tag.TAG_LIST)
                || nbt.contains("Turrets", Tag.TAG_LIST)
                || nbt.contains("Batteries", Tag.TAG_LIST)
                || nbt.contains("Fueltanks", Tag.TAG_LIST)
                || nbt.contains("Ammoboxes", Tag.TAG_LIST)
                || nbt.contains("Screens", Tag.TAG_LIST)
                || nbt.contains("EnemyCannons", Tag.TAG_LIST);
    }

    private int getLinkedPeripheralCount() {
        return linkedThrusters.size() + linkedWeapons.size() + linkedShields.size() + linkedTurrets.size()
                + linkedBatteries.size() + linkedFuelTanks.size() + linkedAmmoboxes.size()
                + linkedScreens.size() + linkedEnemyCannons.size();
    }

    private String linkedPeripheralSummary() {
        return "thrusters=" + linkedThrusters.size()
                + ",weapons=" + linkedWeapons.size()
                + ",shields=" + linkedShields.size()
                + ",turrets=" + linkedTurrets.size()
                + ",batteries=" + linkedBatteries.size()
                + ",fuelTanks=" + linkedFuelTanks.size()
                + ",ammoBoxes=" + linkedAmmoboxes.size()
                + ",screens=" + linkedScreens.size()
                + ",enemyCannons=" + linkedEnemyCannons.size();
    }

    private String linkTraceContext() {
        String levelName = level == null
                ? "unbound"
                : level.dimension().location() + "/" + level.getClass().getSimpleName();
        return levelName + "@" + getBlockPos();
    }

    private void traceLinkState(String phase, boolean clientPacket, int previousCount, CompoundTag nbt) {
        LOGGER.info("{} phase={} context={} clientPacket={} previousCount={} formatRelative={} totals={} keys={}",
                LINK_TRACE_PREFIX, phase, linkTraceContext(), clientPacket, previousCount,
                nbt.getBoolean(LINKED_PERIPHERAL_POSITION_FORMAT_TAG), linkedPeripheralSummary(), nbt.getAllKeys());
    }

    private static class MissingLinkedPeripheralState {
        private int missingTicks;
        private long lastMissingTick = Long.MIN_VALUE;
    }
}
