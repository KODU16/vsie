package com.kodu16.vsie.content.thruster.block.basicthruster;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import org.joml.Vector4f;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BasicThrusterBlockEntity extends AbstractThrusterBlockEntity {
    //private final ControlSeatServerData serverData = new ControlSeatServerData();
    private boolean hasInitialized = false;
    //即使我不想写的这么恶心，为了跨维度我还是得干
    public SmartFluidTankBehaviour tank;

    public BasicThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }


    public float getMaxThrust() {return 100;};

    @Override
    public float getZAxisOffset() {
        return -0.125f;
    }

    @Override
    public float getMaxFlameDistance() {
        return 10;
    }

    protected boolean isWorking() {
        return true;
    }

    public Vector4f getStartColor() {
        return  new Vector4f(32.0f, 128.0f, 128.0f, 1.0f);
    }

    public Vector4f getEndColor() {
        return  new Vector4f(32.0f, 128.0f, 128.0f, 1.0f);
    }

}
