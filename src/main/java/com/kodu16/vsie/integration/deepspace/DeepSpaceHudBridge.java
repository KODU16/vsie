package com.kodu16.vsie.integration.deepspace;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Reads the optional DeepSpace client registry without making VSIE depend on its classes. */
public final class DeepSpaceHudBridge {
    private static final String REGISTRY_CLASS = "world.landfall.deepspace.planet.PlanetRegistry";
    private static volatile Reflection reflection;

    private DeepSpaceHudBridge() {
    }

    public static boolean available() {
        return ModList.get().isLoaded("deepspace") && reflection() != null;
    }

    public static List<Body> bodies(ResourceKey<Level> galaxy) {
        Reflection api = reflection();
        if (api == null) {
            return List.of();
        }
        try {
            Collection<?> planets = (Collection<?>) api.getAllPlanets.invoke(null);
            List<Body> bodies = new ArrayList<>();
            for (Object planet : planets) {
                if (!galaxy.equals(api.getGalaxy.invoke(planet))) {
                    continue;
                }
                boolean relay = (boolean) api.isHyperRelay.invoke(planet);
                @SuppressWarnings("unchecked")
                List<Object> biomeSamples = (List<Object>) api.getSampledBiomes.invoke(planet);
                @SuppressWarnings("unchecked")
                List<Object> fluidSamples = (List<Object>) api.getSampledFluids.invoke(planet);
                @SuppressWarnings("unchecked")
                List<Object> blockSamples = (List<Object>) api.getSampledBlocks.invoke(planet);
                bodies.add(new Body(
                        (String) api.getName.invoke(planet),
                        (Vec3) api.getCenter.invoke(planet),
                        (AABB) api.getModelBounds.invoke(planet),
                        relay,
                        (String) api.getDiscovererName.invoke(planet),
                        sampleIds(api, biomeSamples, 1),
                        sampleIds(api, fluidSamples, 5),
                        sampleIds(api, blockSamples, 5)
                ));
            }
            return bodies;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return List.of();
        }
    }

    private static List<String> sampleIds(Reflection api, List<Object> samples, int limit)
            throws ReflectiveOperationException {
        List<String> ids = new ArrayList<>();
        for (int index = 0; index < Math.min(limit, samples.size()); index++) {
            ids.add((String) api.sampleId.invoke(samples.get(index)));
        }
        return List.copyOf(ids);
    }

    private static Reflection reflection() {
        if (reflection != null) {
            return reflection;
        }
        if (!ModList.get().isLoaded("deepspace")) {
            return null;
        }
        try {
            Class<?> registry = Class.forName(REGISTRY_CLASS);
            Method getAllPlanets = registry.getMethod("getAllPlanets");
            Collection<?> planets = (Collection<?>) getAllPlanets.invoke(null);
            Class<?> planet = planets.isEmpty()
                    ? Class.forName("world.landfall.deepspace.planet.Planet")
                    : planets.iterator().next().getClass();
            Class<?> sample = Class.forName("world.landfall.deepspace.planet.Planet$SurfaceSample");
            reflection = new Reflection(
                    getAllPlanets,
                    planet.getMethod("getName"),
                    planet.getMethod("getCenter"),
                    planet.getMethod("getModelBounds"),
                    planet.getMethod("getGalaxy"),
                    planet.getMethod("isHyperRelay"),
                    planet.getMethod("getDiscovererName"),
                    planet.getMethod("getSampledBiomes"),
                    planet.getMethod("getSampledFluids"),
                    planet.getMethod("getSampledBlocks"),
                    sample.getMethod("id")
            );
            return reflection;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    public record Body(
            String name,
            Vec3 center,
            AABB bounds,
            boolean hyperRelay,
            String discoverer,
            List<String> biomes,
            List<String> fluids,
            List<String> blocks
    ) {
    }

    private record Reflection(
            Method getAllPlanets,
            Method getName,
            Method getCenter,
            Method getModelBounds,
            Method getGalaxy,
            Method isHyperRelay,
            Method getDiscovererName,
            Method getSampledBiomes,
            Method getSampledFluids,
            Method getSampledBlocks,
            Method sampleId
    ) {
    }
}
