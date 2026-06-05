package com.kodu16.vsie.content.controlseat.functions;

import com.kodu16.vsie.content.particle.ShieldParticleOptions;
import com.kodu16.vsie.content.particle.ShieldParticleType;
import com.kodu16.vsie.registries.ModParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

public class ShieldHandler {
    public static double[] getMinMaxDistance(List<Vec3> points) {
        if (points == null || points.size() < 2) {
            return new double[]{0, 0};
        }

        double minDist = Double.MAX_VALUE;
        double maxDist = 0;

        for (int i = 0; i < points.size(); i++) {
            for (int j = i + 1; j < points.size(); j++) {
                double d = points.get(i).distanceTo(points.get(j));
                if (d < minDist) minDist = d;
                if (d > maxDist) maxDist = d;
            }
        }

        return new double[]{minDist, maxDist};
    }

}
