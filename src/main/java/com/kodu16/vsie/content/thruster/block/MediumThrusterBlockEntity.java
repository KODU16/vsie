package com.kodu16.vsie.content.thruster.block;

import com.kodu16.vsie.content.thruster.AbstractThrusterBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector4f;

import java.util.List;

public class MediumThrusterBlockEntity extends AbstractThrusterBlockEntity {
    //private final ControlSeatServerData serverData = new ControlSeatServerData();
    private boolean hasInitialized = false;
    //即使我不想写的这么恶心，为了跨维度我还是得干
    public SmartFluidTankBehaviour tank;

    public MediumThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }


    public float getMaxThrust() {return 200000;}

    @Override
    public float getflamewidth() {
        return 1.5f;
    }

    @Override
    public int fuelconsumptionperthrottle() {
        return 8;
    }

    ;

    @Override
    public float getZAxisOffset() {
        return -1.5f;
    }

    @Override
    public float getMaxFlameDistance() {
        return 30;
    }

    protected boolean isWorking() {
        return true;
    }

    @Override
    public String getthrustertype() {
        return "medium";
    }

    public Vector4f getStartColor() {
        return  new Vector4f(32.0f, 128.0f, 128.0f, 1.0f);
    }

    public Vector4f getEndColor() {
        return  new Vector4f(32.0f, 128.0f, 128.0f, 1.0f);
    }
}
