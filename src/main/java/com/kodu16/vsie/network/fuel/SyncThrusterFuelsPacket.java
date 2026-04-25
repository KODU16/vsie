package com.kodu16.vsie.network.fuel;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.kodu16.vsie.network.fuel.FluidThrusterProperties;
import com.kodu16.vsie.registries.fuel.ThrusterFuelManager;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.core.registries.BuiltInRegistries;

public class SyncThrusterFuelsPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<SyncThrusterFuelsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "fuel_syncthrusterfuelspacket"));
    public static final StreamCodec<FriendlyByteBuf, SyncThrusterFuelsPacket> STREAM_CODEC = CustomPacketPayload.codec((buf, pkt) -> pkt.encode(buf), SyncThrusterFuelsPacket::decode);

    private final Map<ResourceLocation, FluidThrusterProperties> fuelMap;

    public static SyncThrusterFuelsPacket create(Map<Fluid, FluidThrusterProperties> mapToSync) {
        Map<ResourceLocation, FluidThrusterProperties> networkSafeMap = new HashMap<>();
        mapToSync.forEach((fluid, props) -> {
            ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
            if (key != null) {
                networkSafeMap.put(key, props);
            }
        });
        return new SyncThrusterFuelsPacket(networkSafeMap);
    }

    private SyncThrusterFuelsPacket(Map<ResourceLocation, FluidThrusterProperties> fuelMap) {
        this.fuelMap = fuelMap;
    }

    public static SyncThrusterFuelsPacket decode(FriendlyByteBuf buf) {
        Map<ResourceLocation, FluidThrusterProperties> map = buf.readMap(FriendlyByteBuf::readResourceLocation, FluidThrusterProperties::decode);
        return new SyncThrusterFuelsPacket(map);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(this.fuelMap, FriendlyByteBuf::writeResourceLocation, (b, props) -> props.encode(b));
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版实例方法逻辑。
    public static void handle(SyncThrusterFuelsPacket pkt, IPayloadContext context) {
        pkt.handle(() -> new net.minecraftforge.network.NetworkEvent.Context(context));
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ThrusterFuelManager.updateClient(this.fuelMap);
        });
        context.setPacketHandled(true);
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
