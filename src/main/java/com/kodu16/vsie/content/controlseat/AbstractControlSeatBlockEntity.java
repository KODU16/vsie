package com.kodu16.vsie.content.controlseat;


import com.kodu16.vsie.content.controlseat.server.ControlSeatServerData;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

        import java.util.List;

@SuppressWarnings({"deprecation", "unchecked"})
public abstract class AbstractControlSeatBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    // Constants
    protected static final int OBSTRUCTION_LENGTH = 10;
    protected static final int TICKS_PER_ENTITY_CHECK = 5;
    protected static final int LOWEST_POWER_THRSHOLD = 5;
    private static final float PARTICLE_VELOCITY = 4;
    private static final double NOZZLE_OFFSET_FROM_CENTER = 0.9;
    private static final double SHIP_VELOCITY_INHERITANCE = 0.5;

    // Common State
    protected ControlSeatServerData controlseatData;

    // Ticking
    private int currentTick = 0;
    private int clientTick = 0;
    private float particleSpawnAccumulator = 0.0f;

    // Particles
    //protected ParticleType<PlumeParticleData> particleType;


    public AbstractControlSeatBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        controlseatData = new ControlSeatServerData();
        //particleType = (ParticleType<PlumeParticleData>) ParticleTypes.getPlumeType();
        //this.damager = new ThrusterDamager(this);
    }

    // 修改 tick 方法，在此方法中确保座椅输入与对应玩家的 UUID 匹配
    public abstract void serverTick();

    protected abstract boolean isWorking();

    //protected abstract LangBuilder getGoggleStatus();

    //@Nullable
    //protected abstract Direction getFluidCapSide();

    public ControlSeatServerData getControlSeatData() {
        return controlseatData;
    }

    // 在移除座椅时清除控制记录
    public abstract void onRemove();
}
