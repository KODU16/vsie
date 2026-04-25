package net.minecraftforge.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 功能：提供给旧版 1.20.1 数据包处理器的上下文兼容层，
 * 让原有 `NetworkEvent.Context` 代码可在 NeoForge 1.21.1 的 IPayloadContext 上继续运行。
 */
public final class NetworkEvent {
    private NetworkEvent() {
    }

    public static final class Context {
        private final IPayloadContext payloadContext;

        public Context(IPayloadContext payloadContext) {
            this.payloadContext = payloadContext;
        }

        public ServerPlayer getSender() {
            return payloadContext.player() instanceof ServerPlayer player ? player : null;
        }

        public void enqueueWork(Runnable task) {
            payloadContext.enqueueWork(task);
        }

        public void setPacketHandled(boolean ignored) {
            // 功能：NeoForge 1.21.1 的 payload 处理无需手动 setPacketHandled，这里保留空实现兼容旧调用。
        }
    }
}
