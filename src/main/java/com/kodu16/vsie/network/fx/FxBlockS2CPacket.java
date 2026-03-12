package com.kodu16.vsie.network.fx;


import com.kodu16.vsie.utility.vsieFxHelper;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.valkyrienskies.core.impl.shadow.Bl;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public class FxBlockS2CPacket
{
    private final ResourceLocation fx;
    private final BlockPos blockPos;
    private final boolean forceDead;

    public FxBlockS2CPacket(ResourceLocation fx, BlockPos pos, Boolean forceDead)
    {
        this.fx = fx;
        this.blockPos = pos;
        this.forceDead = forceDead;
    }
    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(fx);
        buffer.writeBlockPos(blockPos);
        buffer.writeBoolean(forceDead);
    }

    public static FxBlockS2CPacket decode(FriendlyByteBuf buf){
        ResourceLocation rl = buf.readResourceLocation();
        BlockPos pos = buf.readBlockPos();
        boolean forcedead = buf.readBoolean();
        return new FxBlockS2CPacket(rl,pos,forcedead);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                vsieFxHelper.clientTriggerBlockEffectFx(this);
            })
        );
        ctx.get().setPacketHandled(true);
    }
}
