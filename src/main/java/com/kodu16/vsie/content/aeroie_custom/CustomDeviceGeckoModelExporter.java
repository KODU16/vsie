package com.kodu16.vsie.content.aeroie_custom;

import java.util.Locale;

/** Emits a GeckoLib bone scaffold from the saved custom device definition. */
public final class CustomDeviceGeckoModelExporter {
    private CustomDeviceGeckoModelExporter() {
    }

    public static String toGeoJson(CustomDeviceDefinition definition) {
        CustomDeviceDefinition normalized = definition.copy();
        StringBuilder builder = new StringBuilder(4096);
        builder.append("{\n")
                .append("  \"format_version\": \"1.12.0\",\n")
                .append("  \"minecraft:geometry\": [\n")
                .append("    {\n")
                .append("      \"description\": {\n")
                .append("        \"identifier\": \"geometry.")
                .append(escape(normalized.id))
                .append("\",\n")
                .append("        \"texture_width\": 16,\n")
                .append("        \"texture_height\": 16,\n")
                .append("        \"visible_bounds_width\": 4,\n")
                .append("        \"visible_bounds_height\": 4,\n")
                .append("        \"visible_bounds_offset\": [0, 1, 0]\n")
                .append("      },\n")
                .append("      \"bones\": [\n");
        for (int index = 0; index < normalized.bones.size(); index++) {
            CustomDeviceDefinition.Bone bone = normalized.bones.get(index);
            builder.append("        {\"name\": \"").append(escape(bone.id)).append("\"");
            if (!bone.parent.isBlank()) {
                builder.append(", \"parent\": \"").append(escape(bone.parent)).append("\"");
            }
            builder.append(", \"pivot\": ").append(vector(bone.pivot));
            if (!isZero(bone.rotation)) {
                builder.append(", \"rotation\": ").append(vector(bone.rotation));
            }
            builder.append("}");
            if (index + 1 < normalized.bones.size()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append("      ]\n")
                .append("    }\n")
                .append("  ]\n")
                .append("}\n");
        return builder.toString();
    }

    private static String vector(float[] values) {
        return "[" + format(values[0]) + ", " + format(values[1]) + ", " + format(values[2]) + "]";
    }

    private static boolean isZero(float[] values) {
        return Math.abs(values[0]) < 0.0001F && Math.abs(values[1]) < 0.0001F && Math.abs(values[2]) < 0.0001F;
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.0001F) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
