package com.kodu16.vsie.network.fx;

import com.kodu16.vsie.utility.vsieFxHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public class FxEntityS2CPacket
{
    private final ResourceLocation fx;
    private final int entityID;
    private final boolean forceDead;

    public FxEntityS2CPacket(ResourceLocation fx, int entityID, Boolean forceDead)
    {
        this.fx = fx;
        this.entityID = entityID;
        this.forceDead = forceDead;
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(fx);
        buffer.writeInt(entityID);
        buffer.writeBoolean(forceDead);
    }

    public static FxEntityS2CPacket decode(FriendlyByteBuf buf) {
        ResourceLocation rl = buf.readResourceLocation();
        int entityid = buf.readInt();
        boolean forcedead = buf.readBoolean();
        return new FxEntityS2CPacket(rl,entityid,forcedead);
    }

    @OnlyIn(Dist.CLIENT)
    public void handle(Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    vsieFxHelper.clientTriggerEntityFx(this);
                })
        );
        ctx.get().setPacketHandled(true);
    }
}
