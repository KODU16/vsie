package com.kodu16.vsie.content.aeroie_custom;

/** Shared definition access for custom turret, weapon and thruster renderers. */
public interface CustomDeviceBlockEntity {
    CustomDeviceDefinition getDefinition();

    void setDefinitionJson(String json);
}
