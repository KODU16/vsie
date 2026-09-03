package com.kodu16.vsie.content.turret;

/** Pure-math mirror of the six-face renderer rotations used by targeting tests. */
public final class TurretMountAxes {
    private TurretMountAxes() {
    }

    public record Mount(float translateX, float translateY, float translateZ,
                        float rotateXDegrees, float rotateZDegrees) {
    }

    public static Mount mount(String facing) {
        // Function: one source of truth feeds both the client PoseStack and pure targeting tests.
        return switch (facing) {
            case "south" -> new Mount(0.0F, 0.5F, 0.5F, 270.0F, 0.0F);
            case "west" -> new Mount(-0.5F, 0.5F, 0.0F, 0.0F, -90.0F);
            case "north" -> new Mount(0.0F, 0.5F, -0.5F, 90.0F, 0.0F);
            case "east" -> new Mount(0.5F, 0.5F, 0.0F, 0.0F, 90.0F);
            case "up" -> new Mount(0.0F, 1.0F, 0.0F, 0.0F, 180.0F);
            case "down" -> new Mount(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            default -> throw new IllegalArgumentException("Unknown turret facing: " + facing);
        };
    }

    public static float[] transform(String facing, float x, float y, float z) {
        Mount mount = mount(facing);
        double xAngle = Math.toRadians(mount.rotateXDegrees());
        float afterXy = (float) (Math.cos(xAngle) * y - Math.sin(xAngle) * z);
        float afterXz = (float) (Math.sin(xAngle) * y + Math.cos(xAngle) * z);
        double zAngle = Math.toRadians(mount.rotateZDegrees());
        // Function: rotate directions exactly like TurretMountTransform, without client-only Minecraft classes.
        return new float[]{
                (float) (Math.cos(zAngle) * x - Math.sin(zAngle) * afterXy),
                (float) (Math.sin(zAngle) * x + Math.cos(zAngle) * afterXy),
                afterXz
        };
    }
}
