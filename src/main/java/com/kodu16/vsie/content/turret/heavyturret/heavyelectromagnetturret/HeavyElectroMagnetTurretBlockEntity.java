package com.kodu16.vsie.content.turret.heavyturret.heavyelectromagnetturret;

import com.kodu16.vsie.content.turret.TurretData;
import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import software.bernie.geckolib.core.animation.AnimatableManager;

import java.util.List;

public class HeavyElectroMagnetTurretBlockEntity extends AbstractHeavyTurretBlockEntity {
    public HeavyElectroMagnetTurretBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        // 初始化 turretData
        this.turretData = new TurretData();
    }

    @Override
    public Vector3d getShootLocation(Vector3d vec, List<Vector3d> preV, Level lv, Vector3d pos) {
        return null;
    }

    @Override
    public String getturrettype() {
        return "heavy_electromagnet";
    }

    @Override
    public double getYAxisOffset() {
        return 3;
    }

    @Override
    public float getMaxSpinSpeed() {
        return Mth.PI/256;
    }

    @Override
    public int getCoolDown() {
        return 60;
    }

    @Override
    public int getenergypertick() {
        return 1000;
    }

    @Override
    public void shootentity() {

    }

    @Override
    public void shootship() {

    }

    @Override
    public int getmaxpitchdowndegrees() {
        return 20;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }
}
