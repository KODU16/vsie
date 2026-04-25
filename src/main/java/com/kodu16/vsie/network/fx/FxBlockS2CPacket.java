package com.kodu16.vsie.network.fx;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.kodu16.vsie.utility.vsieFxHelper;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.parameter.ExposedParameter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.valkyrienskies.core.impl.shadow.Bl;

import java.util.function.Supplier;

@Getter
@AllArgsConstructor
public class FxBlockS2CPacket implements CustomPacketPayload
{
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<FxBlockS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "fx_fxblocks2cpacket"));
    public static final StreamCodec<FriendlyByteBuf, FxBlockS2CPacket> STREAM_CODEC = CustomPacketPayload.codec((buf, pkt) -> pkt.encode(buf), FxBlockS2CPacket::decode);

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

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版实例方法逻辑。
    public static void handle(FxBlockS2CPacket pkt, IPayloadContext context) {
        pkt.handle(() -> new net.minecraftforge.network.NetworkEvent.Context(context));
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


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
