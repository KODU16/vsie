package com.kodu16.vsie.content.screen.block;

import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.List;

public class BasicScreenBlockEntity extends AbstractScreenBlockEntity {

    public BasicScreenBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public String getDisplaytype() {
        return "basic";
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {

    }

}
