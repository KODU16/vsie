package com.kodu16.vsie.content.aeroie_custom;

/** Computes the model translation that places the root pivot at the placed turret block. */
public final class CustomDeviceAnchor {
    private CustomDeviceAnchor() {
    }

    public static float[] rootPivotOffset(CustomDeviceDefinition definition) {
        float[] rootAnchor = rootAnchor(definition);
        // Function: X/Z use the block center while Y uses the block base, avoiding the former extra half-block lift.
        return new float[]{
                0.5F - rootAnchor[0],
                -rootAnchor[1],
                0.5F - rootAnchor[2]
        };
    }

    public static float[] rootPivotWorldPosition(int blockX, int blockY, int blockZ,
                                                  CustomDeviceDefinition definition) {
        float[] renderOffset = rootPivotOffset(definition);
        float[] rootAnchor = rootAnchor(definition);
        // Function: mirror renderer + root transforms; placement never moves the block by the authored pivot.
        return new float[]{
                blockX + renderOffset[0] + rootAnchor[0],
                blockY + renderOffset[1] + rootAnchor[1],
                blockZ + renderOffset[2] + rootAnchor[2]
        };
    }

    public static int[] placementTarget(int clickedX, int clickedY, int clickedZ,
                                        int faceStepX, int faceStepY, int faceStepZ) {
        // Function: resolve only the clicked face; authored root coordinates must never move the physical block.
        return new int[]{clickedX + faceStepX, clickedY + faceStepY, clickedZ + faceStepZ};
    }

    private static float[] rootAnchor(CustomDeviceDefinition definition) {
        if ("decoration".equals(definition.deviceType)) {
            // Function: decorations have no semantic root group, even when an ordinary bone happens to use that id.
            return new float[]{0.0F, 0.0F, 0.0F};
        }
        CustomDeviceDefinition.Bone root = definition.findBone("root");
        if (root == null) {
            return new float[]{0.0F, 0.0F, 0.0F};
        }
        return new float[]{
                root.position[0] + root.pivot[0],
                root.position[1] + root.pivot[1],
                root.position[2] + root.pivot[2]
        };
    }
}
