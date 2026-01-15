package com.kodu16.vsie.content.controlseat.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ShipForcesInducer;
import org.valkyrienskies.core.api.ships.ShipPhysicsListener;
import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl;

import com.kodu16.vsie.utility.AttachmentUtils;

@SuppressWarnings("deprecation")
public final class ControlSeatForceAttachment implements ShipPhysicsListener {
    public Map<BlockPos, ServerShipHandler> appliersMapping = new ConcurrentHashMap<>();
    public ControlSeatForceAttachment() {}

    @Override
    public void physTick(@NotNull PhysShip Physship, @NotNull PhysLevel physLevel) {
        PhysShipImpl ship = (PhysShipImpl)Physship;
        appliersMapping.forEach((pos, applier) -> {
            applier.getandsendshipdata(ship);
            applier.applyForceAndTorque(ship);
        });
    }

    public void addApplier(BlockPos pos, ServerShipHandler applier){
        appliersMapping.put(pos, applier);
    }

    public void removeApplier(ServerLevel level, BlockPos pos){
        appliersMapping.remove(pos);
        //Remove attachment by using passing null as attachment instance in order to clean up after ourselves
        if (appliersMapping.isEmpty()) {
            LoadedServerShip ship = AttachmentUtils.getShipAt(level, pos);
            if (ship != null) {
                // Remove attachment by passing null as the instance
                ship.setAttachment(ControlSeatForceAttachment.class, null);
            }
        }
    }

    //Getters
    public static ControlSeatForceAttachment getOrCreateAsAttachment(LoadedServerShip ship) {
        return AttachmentUtils.getOrCreate(ship, ControlSeatForceAttachment.class, ControlSeatForceAttachment::new);
    }

    public static ControlSeatForceAttachment get(Level level, BlockPos pos) {
        return AttachmentUtils.get(level, pos, ControlSeatForceAttachment.class, ControlSeatForceAttachment::new);
    }
}
