package com.kodu16.vsie.content.bullet;

// NeoForge 1.21.1 迁移：ResourceLocation 构造器已不可用，这里统一改用静态工厂方法创建资源ID。

import com.google.gson.annotations.SerializedName;
import com.kodu16.vsie.utility.FxData;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public class BulletData {
    private static final ResourceLocation PARTICLE_BULLET_FX = ResourceLocation.fromNamespaceAndPath("vsie", "particle_bullet");
    private static final ResourceLocation CENIX_PLASMA_BULLET_FX = ResourceLocation.fromNamespaceAndPath("vsie", "cenix_plasma_bullet");
    private static final ResourceLocation HEAVY_ELECTROMAGNETIC_BULLET_FX = ResourceLocation.fromNamespaceAndPath("vsie", "heavy_electromagnetic_bullet");

    // 功能：保存子弹的特效配置；兼容数据文件中使用 "fx" 或 "fxData" 两种键名。
    @SerializedName(value = "fx", alternate = {"fxData"})
    private FxData fxData;

    // 功能：保留无参构造，确保 Gson 在读取外部 JSON 时可以正常实例化。
    public BulletData() {
    }

    // 功能：用于代码内直接构造子弹数据，避免 Gson 反射解析 Minecraft 类型导致崩溃。
    private BulletData(FxData fxData) {
        this.fxData = fxData;
    }

    // Keep particle bullets responsible for their own lifetime FX, not the cannon fire event.
    public static BulletData createParticleBulletDefault() {
        return new BulletData(FxData.createWithAwake(PARTICLE_BULLET_FX));
    }

    // Function: Cenix bullets use the dedicated plasma bullet FX for both flight and impact playback.
    public static BulletData createCenixPlasmaBulletDefault() {
        return new BulletData(FxData.createWithAwake(CENIX_PLASMA_BULLET_FX));
    }

    // Function: heavy electromagnetic bullets currently copy particle bullet behavior with a dedicated trail FX.
    public static BulletData createHeavyElectroMagnetBulletDefault() {
        return new BulletData(FxData.createWithAwake(HEAVY_ELECTROMAGNETIC_BULLET_FX));
    }

    public FxData getFxData() {
        return fxData;
    }
}
