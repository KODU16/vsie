package com.kodu16.vsie.utility;

import com.kodu16.vsie.network.fx.FxBlockS2CPacket;
import com.kodu16.vsie.network.fx.FxEntityS2CPacket;
import com.lowdragmc.photon.client.fx.BlockEffect;
import com.lowdragmc.photon.client.fx.EntityEffect;
import com.lowdragmc.photon.client.fx.FX;
import com.lowdragmc.photon.client.fx.FXHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public final class vsieFxHelper
{
    // 功能：从 FxData 中提取指定 FX；兼容扁平结构和顶层带 "fx" 包裹的结构。
    public static Optional<FxData.FxUnit> extractFxUnit(@Nullable FxData fxData, Function<FxData, FxData.FxUnit> mapper)
    {
        return Optional.ofNullable(fxData).map(data -> data.resolveUnit(mapper));
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientTriggerEntityFx(FxEntityS2CPacket triggerPacket)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if(level != null)
        {
            Entity entity = level.getEntity(triggerPacket.getEntityID());
            if(entity != null)
            {
                FX fx = FXHelper.getFX(triggerPacket.getFx());
                if(fx != null)
                {
                    var effect = new EntityEffect(fx, level,entity, EntityEffect.AutoRotate.NONE);
                    effect.setForcedDeath(triggerPacket.isForceDead());
                    effect.start();
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientTriggerBlockEffectFx(FxBlockS2CPacket triggerPacket)
    {
        ClientLevel level = Minecraft.getInstance().level;
        if(level != null)
        {
            BlockPos pos = triggerPacket.getBlockPos();
            FX fx = FXHelper.getFX(triggerPacket.getFx());
            if(fx != null)
            {
                var effect = new BlockEffect(fx,level,pos);
                effect.setForcedDeath(triggerPacket.isForceDead());
                effect.start();
            }
        }
    }
}
