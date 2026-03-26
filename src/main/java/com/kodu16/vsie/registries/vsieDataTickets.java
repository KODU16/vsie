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
        AbstractTurretBlockEntity.XROT = addFloat("turret_yaw");
        AbstractTurretBlockEntity.YROT = addFloat("turret_pitch");
        AbstractTurretBlockEntity.TURRET_HAS_TARGET = addBoolean("turret_has_target");

        //vector thruster
        AbstractVectorThrusterBlockEntity.VECTOR_THRUSTER_YAW =     addDouble("vector_thruster_yaw");
        AbstractVectorThrusterBlockEntity.VECTOR_THRUSTER_PITCH =   addDouble("vector_thruster_pitch");
        AbstractVectorThrusterBlockEntity.VECTOR_THRUSTER_IS_SPINNING = addBoolean("vector_thruster_is_spinning");

        //missile
        AbstractMissileEntity.MISSILE_MOMENTUM_X = addDouble("missile_momentum_x");
        AbstractMissileEntity.MISSILE_MOMENTUM_Y = addDouble("missile_momentum_y");
        AbstractMissileEntity.MISSILE_MOMENTUM_Z = addDouble("missile_momentum_z");

        //screen
        AbstractScreenBlockEntity.SCREEN_SPIN_X = addInt("screen_spin_x");
        AbstractScreenBlockEntity.SCREEN_SPIN_Y = addInt("screen_spin_x");
        AbstractScreenBlockEntity.SCREEN_OFFSET_X = addInt("screen_offset_x");
        AbstractScreenBlockEntity.SCREEN_OFFSET_Y = addInt("screen_offset_y");
        AbstractScreenBlockEntity.SCREEN_OFFSET_Z = addInt("screen_offset_z");
    }


    private static SerializableDataTicket<Double> addDouble(String pPath){
        return GeckoLibUtil.addDataTicket(SerializableDataTicket.ofDouble(new ResourceLocation(vsie.ID, pPath)));
    }
    private static SerializableDataTicket<Float> addFloat(String pPath){
        return GeckoLibUtil.addDataTicket(SerializableDataTicket.ofFloat(new ResourceLocation(vsie.ID, pPath)));
    }
    private static SerializableDataTicket<Boolean> addBoolean(String pPath){
        return GeckoLibUtil.addDataTicket(SerializableDataTicket.ofBoolean(new ResourceLocation(vsie.ID, pPath)));
    }
    private static SerializableDataTicket<Integer> addInt(String pPath){
        return GeckoLibUtil.addDataTicket(SerializableDataTicket.ofInt(new ResourceLocation(vsie.ID, pPath)));
    }
    private static SerializableDataTicket<String> addString(String pPath){
        return GeckoLibUtil.addDataTicket(SerializableDataTicket.ofString(new ResourceLocation(vsie.ID, pPath)));
    }

}
