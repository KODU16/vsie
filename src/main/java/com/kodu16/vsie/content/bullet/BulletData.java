package com.kodu16.vsie.content.bullet;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.kodu16.vsie.utility.FxData;
import lombok.Getter;

@Getter
public class BulletData {
    // 功能：保存子弹的特效配置；兼容数据文件中使用 "fx" 或 "fxData" 两种键名。
    @SerializedName(value = "fx", alternate = {"fxData"})
    private FxData fxData;

    // 功能：构造一个默认粒子炮子弹数据，确保 tickCount==1 时可读取到 awake 的 FX。
    public static BulletData createParticleCannonDefault() {
        return new Gson().fromJson("""
                {
                  \"fx\": {
                    \"awake\": {
                      \"id\": \"vsie:particle_cannon_fire\"
                    }
                  }
                }
                """, BulletData.class);
    }
}
