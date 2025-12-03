package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;


@SuppressWarnings({"removal"})
public class vsieDataTickets {
    public static void registerDataTickets() {
        AbstractTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofBoolean(new ResourceLocation(vsie.ID, "has_target")));
        AbstractTurretBlockEntity.TARGET_POS_X = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, "target_pos_x")));
        AbstractTurretBlockEntity.TARGET_POS_Y = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, "target_pos_y")));
        AbstractTurretBlockEntity.TARGET_POS_Z = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, "target_pos_z")));
    }
}
