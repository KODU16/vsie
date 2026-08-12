package com.kodu16.vsie.content.custom_turret;

/** Parses the resource links used by the OBJ/MTL files produced by the region exporter. */
public final class WavefrontMaterialContract {
    private WavefrontMaterialContract() {
    }

    public static String materialLibrary(String objSource) {
        return directiveValue(objSource, "mtllib");
    }

    public static String diffuseTexture(String mtlSource) {
        return directiveValue(mtlSource, "map_Kd");
    }

    private static String directiveValue(String source, String directive) {
        if (source == null || source.isBlank()) {
            return "";
        }
        for (String rawLine : source.split("\\R")) {
            String line = rawLine.strip();
            if (line.length() > directive.length()
                    && line.regionMatches(true, 0, directive, 0, directive.length())
                    && Character.isWhitespace(line.charAt(directive.length()))) {
                String value = line.substring(directive.length()).strip();
                int comment = value.indexOf('#');
                return (comment >= 0 ? value.substring(0, comment) : value).strip();
            }
        }
        return "";
    }
}
