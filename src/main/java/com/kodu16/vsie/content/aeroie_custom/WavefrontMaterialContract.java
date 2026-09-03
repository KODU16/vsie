package com.kodu16.vsie.content.aeroie_custom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /** Returns every MTL file referenced by mtllib directives, preserving first-use order. */
    public static List<String> materialLibraries(String objSource) {
        List<String> libraries = new ArrayList<>();
        if (objSource == null || objSource.isBlank()) {
            return libraries;
        }
        for (String rawLine : objSource.split("\\R")) {
            String line = rawLine.strip();
            if (!isDirective(line, "mtllib")) {
                continue;
            }
            for (String library : valueAfterDirective(line, "mtllib").split("\\s+")) {
                if (!library.isBlank() && !libraries.contains(library)) {
                    libraries.add(library);
                }
            }
        }
        return libraries;
    }

    /** Returns each newmtl section's first diffuse texture link, keyed by material name. */
    public static Map<String, String> diffuseTexturesByMaterial(String mtlSource) {
        Map<String, String> textures = new LinkedHashMap<>();
        if (mtlSource == null || mtlSource.isBlank()) {
            return textures;
        }
        String currentMaterial = "";
        for (String rawLine : mtlSource.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (isDirective(line, "newmtl")) {
                currentMaterial = valueAfterDirective(line, "newmtl");
            } else if (!currentMaterial.isEmpty() && isDirective(line, "map_Kd")) {
                String texture = valueAfterDirective(line, "map_Kd");
                if (!texture.isBlank()) {
                    textures.putIfAbsent(currentMaterial, texture);
                }
            }
        }
        return textures;
    }

    public static Set<String> emissiveMaterials(String mtlSource) {
        Set<String> materials = new LinkedHashSet<>();
        if (mtlSource == null || mtlSource.isBlank()) {
            return materials;
        }
        String currentMaterial = "";
        for (String rawLine : mtlSource.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }
            if (isDirective(line, "newmtl")) {
                currentMaterial = valueAfterDirective(line, "newmtl");
            } else if (!currentMaterial.isEmpty()
                    && (line.equalsIgnoreCase("# aeroie_emissive true") || hasPositiveEmission(line))) {
                materials.add(currentMaterial);
            }
        }
        return materials;
    }

    private static String directiveValue(String source, String directive) {
        if (source == null || source.isBlank()) {
            return "";
        }
        for (String rawLine : source.split("\\R")) {
            String line = rawLine.strip();
            if (isDirective(line, directive)) {
                return valueAfterDirective(line, directive);
            }
        }
        return "";
    }

    private static boolean isDirective(String line, String directive) {
        return line.length() > directive.length()
                && line.regionMatches(true, 0, directive, 0, directive.length())
                && Character.isWhitespace(line.charAt(directive.length()));
    }

    private static String valueAfterDirective(String line, String directive) {
        String value = line.substring(directive.length()).strip();
        int comment = value.indexOf('#');
        return (comment >= 0 ? value.substring(0, comment) : value).strip();
    }

    private static boolean hasPositiveEmission(String line) {
        if (!isDirective(line, "Ke")) {
            return false;
        }
        String[] parts = valueAfterDirective(line, "Ke").split("\\s+");
        for (String part : parts) {
            try {
                if (!part.isEmpty() && Float.parseFloat(part) > 0.0F) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // Function: keep third-party MTL variants from breaking OBJ loading.
            }
        }
        return false;
    }
}
