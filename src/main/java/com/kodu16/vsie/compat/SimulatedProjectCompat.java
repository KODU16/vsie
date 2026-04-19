package com.kodu16.vsie.compat;

import com.kodu16.vsie.content.controlseat.server.ControlSeatForceAttachment;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * 功能：集中封装 Simulated-Project 的接入逻辑，避免业务代码继续直接依赖 Valkyrien Skies API。
 */
public final class SimulatedProjectCompat {

    private SimulatedProjectCompat() {
    }

    /**
     * 功能：在模组初始化阶段注册控制席推进相关附件（后续对接 Simulated-Project 的 attachment API）。
     */
    public static void registerControlSeatAttachment() {
        // 功能：迁移占位实现；当前版本先保留调用入口，后续在此处直接绑定 Simulated-Project 附件注册代码。
        // 功能：通过引用附件类型确保迁移目标类在初始化阶段被加载。
        ControlSeatForceAttachment.class.getName();
    }

    /**
     * 功能：读取物理线程 TPS，供服务器信息屏显示；若 Simulated-Project 不可用则返回 0。
     */
    public static int getPhysicsTps(@Nullable MinecraftServer server) {
        if (server == null) {
            return 0;
        }
        // 功能：迁移占位实现；后续替换为 Simulated-Project 的真实物理管线 TPS 查询。
        return 0;
    }
}
