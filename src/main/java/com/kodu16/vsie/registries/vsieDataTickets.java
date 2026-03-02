package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.missile.AbstractMissileEntity;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.vectorthruster.AbstractVectorThrusterBlockEntity;
import com.kodu16.vsie.vsie;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.network.SerializableDataTicket;
import software.bernie.geckolib.util.GeckoLibUtil;


@SuppressWarnings({"removal"})
public class vsieDataTickets {
    public static void registerDataTickets() {

        //turret
        AbstractTurretBlockEntity.HAS_TARGET = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofBoolean(new ResourceLocation(vsie.ID, "has_target")));
        AbstractTurretBlockEntity.XROT = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofFloat(new ResourceLocation(vsie.ID, "rot_x")));
        AbstractTurretBlockEntity.YROT = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofFloat(new ResourceLocation(vsie.ID, "rot_y")));

        //vector thruster
        AbstractVectorThrusterBlockEntity.FINAL_SPIN = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, "final_spin")));
        AbstractVectorThrusterBlockEntity.FINAL_PITCH = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, "final_pitch")));
        AbstractVectorThrusterBlockEntity.IS_SPINNING = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofBoolean(new ResourceLocation(vsie.ID, "is_spinning")));

        //missile
        AbstractMissileEntity.MOMENT_X = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, "missile_momentum_x")));
        AbstractMissileEntity.MOMENT_Y = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, "missile_momentum_y")));
        AbstractMissileEntity.MOMENT_Z = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, "missile_momentum_z")));

        //screen
        AbstractScreenBlockEntity.SPINX = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofInt(new ResourceLocation(vsie.ID, "screen_spinx")));
        AbstractScreenBlockEntity.SPINY = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofInt(new ResourceLocation(vsie.ID, "screen_spiny")));
        AbstractScreenBlockEntity.OFFSETX = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofInt(new ResourceLocation(vsie.ID, "screen_offsetx")));
        AbstractScreenBlockEntity.OFFSETY = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofInt(new ResourceLocation(vsie.ID, "screen_offsety")));
        AbstractScreenBlockEntity.OFFSETZ = GeckoLibUtil.addDataTicket(SerializableDataTicket.ofInt(new ResourceLocation(vsie.ID, "screen_offsetz")));
    }
}
