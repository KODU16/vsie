package com.kodu16.vsie.content.shield;

import com.kodu16.vsie.foundation.RelativeBlockPosNbt;

import com.kodu16.vsie.network.fx.FxPositionS2CPacket;
import com.kodu16.vsie.registries.ModNetworking;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class ShieldGeneratorBlockEntity extends SmartBlockEntity {
    private static final ResourceLocation SHIELD_HIT_FX = ResourceLocation.fromNamespaceAndPath("vsie", "shield_hit");
    public SmartFluidTankBehaviour tank;
    public BlockPos linkedcontrolseatpos = new BlockPos(0,0,0);
    double RADIUS = 3;
    public int maxreceiverate = 100;
    public EnergyStorage energyStorage = new ShieldEnergyStorage();

    public ShieldGeneratorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    private class ShieldEnergyStorage extends EnergyStorage {
        private ShieldEnergyStorage() {
            super(100000, Integer.MAX_VALUE, Integer.MAX_VALUE, 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // The control seat updates this limit every tick, so apply it at receive time.
            int dynamicReceive = Math.max(0, ShieldGeneratorBlockEntity.this.maxreceiverate);
            return super.receiveEnergy(Math.min(maxReceive, dynamicReceive), simulate);
        }

        public void setEnergyStored(int energy) {
            // NBT restores exact stored shield FE and should not be capped by the current receive rate.
            this.energy = Mth.clamp(energy, 0, this.capacity);
        }
    }

    public void tick(Level level, BlockPos pos, BlockState state, ShieldGeneratorBlockEntity be) {
        if (level.isClientSide || level.getGameTime() % 2 != 0) return;

        Vec3 center = Vec3.atCenterOf(pos);
        AABB searchBox = ShieldInterception.searchBox(center, RADIUS);
        level.getEntitiesOfClass(Entity.class, searchBox, ShieldInterception::isCandidate).forEach(entity -> {
            ShieldInterception.Hit shieldHit = ShieldInterception.findHit(entity, center, RADIUS);
            if (shieldHit == null) return;

            if(getEnergy().getEnergyStored()>20000)
            {
                entity.discard();
                Vec3 hitDir = shieldHit.normal();
                Vec3 hitPoint = shieldHit.point();
                playShieldHitFx((ServerLevel) level, hitPoint, hitDir);

                level.playSound(null, hitPoint.x, hitPoint.y, hitPoint.z,
                        SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS,
                        1.0f, 1.2f + level.random.nextFloat() * 0.4f);
                getEnergyStorage().extractEnergy(20,false);
            }
        });
    }

    private static void playShieldHitFx(ServerLevel level, Vec3 hitPoint, Vec3 normal) {
        if (normal.lengthSqr() <= 1.0E-6D) {
            return;
        }
        Vec3 normalized = normal.normalize();
        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0F, 1.0F, 0.0F,
                (float) normalized.x, (float) normalized.y, (float) normalized.z
        );
        // Align shield_hit local Y axis with the impact normal and play it at the shield surface point.
        ModNetworking.sendToAll(new FxPositionS2CPacket(
                SHIELD_HIT_FX,
                hitPoint.x, hitPoint.y, hitPoint.z,
                0.0D, 0.0D, 0.0D,
                rotation,
                new Vector3f(1.0F, 1.0F, 1.0F),
                true,
                true
        ));
    }

    public IEnergyStorage getEnergyCapability() {
        return energyStorage;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.write(tag, registries, clientpacket);
        tag.putInt("Energy", getEnergy().getEnergyStored());
        RelativeBlockPosNbt.write(tag, "controlpos", getBlockPos(), linkedcontrolseatpos);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientpacket) {
        super.read(tag, registries, clientpacket);
        if (tag.contains("Energy")) {
            ((ShieldEnergyStorage) energyStorage).setEnergyStored(tag.getInt("Energy"));
        }
        linkedcontrolseatpos = RelativeBlockPosNbt.read(tag, "controlpos", getBlockPos(), false);
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
        read(tag, registries, true);
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IEnergyStorage getEnergy() {
        return energyStorage;
    }

}
