package com.kodu16.vsie.network.fuel;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.HashMap;
import java.util.Map;

import com.kodu16.vsie.network.fuel.FluidThrusterProperties;
import com.kodu16.vsie.registries.fuel.ThrusterFuelManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.core.registries.BuiltInRegistries;

public class SyncThrusterFuelsPacket implements CustomPacketPayload {
    // 功能：NeoForge 1.21.1 payload 类型标识与编解码器注册入口。
    public static final CustomPacketPayload.Type<SyncThrusterFuelsPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("vsie", "fuel_syncthrusterfuelspacket"));
    private static final StreamCodec<ByteBuf, FluidThrusterProperties> FLUID_THRUSTER_PROPERTIES_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            properties -> properties.thrustMultiplier,
            ByteBufCodecs.FLOAT,
            properties -> properties.consumptionMultiplier,
            FluidThrusterProperties::new
    );
    private static final StreamCodec<ByteBuf, Map<ResourceLocation, FluidThrusterProperties>> FUEL_MAP_STREAM_CODEC = ByteBufCodecs.map(
            HashMap::new,
            ResourceLocation.STREAM_CODEC,
            FLUID_THRUSTER_PROPERTIES_STREAM_CODEC
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncThrusterFuelsPacket> STREAM_CODEC = StreamCodec.composite(
            FUEL_MAP_STREAM_CODEC,
            SyncThrusterFuelsPacket::fuelMap,
            SyncThrusterFuelsPacket::new
    );

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

    private Map<ResourceLocation, FluidThrusterProperties> fuelMap() {
        return this.fuelMap;
    }

    // 功能：NeoForge 1.21.1 处理器入口，复用旧版实例方法逻辑。
    public static void handle(SyncThrusterFuelsPacket pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            ThrusterFuelManager.updateClient(pkt.fuelMap);
        });
    }


    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
