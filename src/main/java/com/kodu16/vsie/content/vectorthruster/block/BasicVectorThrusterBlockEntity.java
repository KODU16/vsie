package com.kodu16.vsie.content.vectorthruster.block;

import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BasicVectorThrusterBlockEntity extends AbstractVectorThrusterBlockEntity {
    private boolean hasInitialized = false;
    //即使我不想写的这么恶心，为了跨维度我还是得干
    public SmartFluidTankBehaviour tank;

    @Override
    public float getMaxFlameDistance() {
        return 0;
    }

    public BasicVectorThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    @Override
    public float getZAxisOffset() {
        return 0;
    }

    @Override
    public float getMaxThrust() {
        return 0;
    }

    @Override
    protected boolean isWorking() {
        return false;
    }

    @Override
    public String getvectorthrustertype() {
        return "basic";
    }
}
