package com.kodu16.vsie.utility;

import com.google.gson.annotations.SerializedName;
import com.kodu16.vsie.vsie;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

@Getter
public class FxData
{
    // 功能：兼容你当前的数据格式（顶层是 "fx" 节点），例如 { "fx": { "awake": ... } }。
    @Nullable
    @SerializedName("fx")
    private FxData nestedFx;

    @Nullable
    @SerializedName("awake")
    private FxUnit awakeFx;
    @Nullable
    @SerializedName("end")
    private FxUnit endFx;
    @Nullable
    @SerializedName("hit")
    private FxUnit hitFx;

    // 功能：创建仅包含 awake 特效的 FxData，供代码内默认配置使用。
    public static FxData createWithAwake(ResourceLocation awakeFxId)
    {
        FxData fxData = new FxData();
        fxData.awakeFx = FxUnit.create(awakeFxId);
        return fxData;
    }

    // 功能：统一提取 FX 单元；优先读当前对象，若为空则回退读取嵌套的 "fx" 节点。
    @Nullable
    public FxUnit resolveUnit(java.util.function.Function<FxData, FxUnit> mapper)
    {
        FxUnit directUnit = mapper.apply(this);
        if(directUnit != null)
        {
            return directUnit;
        }
        return nestedFx == null ? null : mapper.apply(nestedFx);
    }


    @Getter
    public static class FxUnit
    {
        @SerializedName("id")
        private ResourceLocation id = ResourceLocation.tryBuild(vsie.ID, "null");

        // 功能：通过特效资源位置构造 FxUnit，统一默认值保护。
        public static FxUnit create(@Nullable ResourceLocation fxId)
        {
            FxUnit fxUnit = new FxUnit();
            fxUnit.id = fxId == null ? ResourceLocation.tryBuild(vsie.ID, "null") : fxId;
            return fxUnit;
        }
    }
}
