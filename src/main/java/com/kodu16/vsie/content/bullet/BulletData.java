package com.kodu16.vsie.content.bullet;

import com.google.gson.annotations.SerializedName;
import com.kodu16.vsie.utility.FxData;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;

@Getter
public class BulletData {
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

    // 功能：构造一个默认粒子炮子弹数据，确保 tickCount==1 时可读取到 awake 的 FX，且不触发 Gson 反射异常。
    public static BulletData createParticleCannonDefault() {
        return new BulletData(FxData.createWithAwake(new ResourceLocation("vsie", "particle_cannon_fire")));
    }
}
