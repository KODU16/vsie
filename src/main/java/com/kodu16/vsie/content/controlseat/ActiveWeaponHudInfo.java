package com.kodu16.vsie.content.controlseat;

// 功能：封装 HUD 展示所需的单个激活武器信息（名称、当前冷却 tick、最大冷却 tick）。
public class ActiveWeaponHudInfo {
    public final String displayName;
    public final int currentTick;
    public final int maxCooldown;

    // 功能：构造单个武器 HUD 数据，便于在服务端收集后同步到客户端绘制。
    public ActiveWeaponHudInfo(String displayName, int currentTick, int maxCooldown) {
        this.displayName = displayName;
        this.currentTick = currentTick;
        this.maxCooldown = maxCooldown;
    }
}
