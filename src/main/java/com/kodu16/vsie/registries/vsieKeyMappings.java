package com.kodu16.vsie.registries;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

// Function: client-only key mapping registration for NeoForge 1.21.1.
@SuppressWarnings("removal")
@EventBusSubscriber(modid = "vsie", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class vsieKeyMappings {
    public static final KeyMapping KEY_TOGGLE_LOCK = new KeyMapping(
            "key.vsie.toggle_mouse_lock",
            GLFW.GLFW_KEY_LEFT_ALT,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_FORCE_ASSIST = new KeyMapping(
            "key.vsie.toggle_force_assist",
            GLFW.GLFW_KEY_B,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_TORQUE_ASSIST = new KeyMapping(
            "key.vsie.toggle_torque_assist",
            GLFW.GLFW_KEY_J,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_DEEPSPACE_HUD = new KeyMapping(
            "key.vsie.toggle_deepspace_hud",
            GLFW.GLFW_KEY_H,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_ANTI_GRAVITY = new KeyMapping(
            "key.vsie.toggle_anti_gravity",
            GLFW.GLFW_KEY_G,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_AUTO_LEVEL = new KeyMapping(
            "key.vsie.toggle_auto_level",
            GLFW.GLFW_KEY_L,
            "category.vsie"
    );
    public static final KeyMapping KEY_THROTTLE = new KeyMapping(
            "key.vsie.throttle",
            GLFW.GLFW_KEY_TAB,
            "category.vsie"
    );
    public static final KeyMapping KEY_BRAKE = new KeyMapping(
            "key.vsie.brake",
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "category.vsie"
    );
    public static final KeyMapping KEY_ROLL_L = new KeyMapping(
            "key.vsie.roll_left",
            GLFW.GLFW_KEY_A,
            "category.vsie"
    );
    public static final KeyMapping KEY_ROLL_R = new KeyMapping(
            "key.vsie.roll_right",
            GLFW.GLFW_KEY_D,
            "category.vsie"
    );
    public static final KeyMapping KEY_CONTROL_LEFT = new KeyMapping(
            "key.vsie.control_left",
            GLFW.GLFW_KEY_Z,
            "category.vsie"
    );
    public static final KeyMapping KEY_CONTROL_RIGHT = new KeyMapping(
            "key.vsie.control_right",
            GLFW.GLFW_KEY_C,
            "category.vsie"
    );
    public static final KeyMapping KEY_SWITCH_ENEMY = new KeyMapping(
            "key.vsie.switch_enemy",
            GLFW.GLFW_KEY_COMMA,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_SHIELD = new KeyMapping(
            "key.vsie.toggle_shield",
            GLFW.GLFW_KEY_V,
            "category.vsie"
    );
    public static final KeyMapping KEY_START_WARP = new KeyMapping(
            "key.vsie.warp",
            GLFW.GLFW_KEY_P,
            "category.vsie"
    );
    public static final KeyMapping KEY_ACTIVATE_HYPER_RELAY = new KeyMapping(
            "key.vsie.activate_hyper_relay",
            GLFW.GLFW_KEY_K,
            "category.vsie"
    );

    public static void register(IEventBus modBus) {
        // Function: register all client key mappings on the mod event bus.
        modBus.addListener(vsieKeyMappings::registerKeyMappings);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_TOGGLE_LOCK);
        event.register(KEY_THROTTLE);
        event.register(KEY_BRAKE);
        event.register(KEY_ROLL_L);
        event.register(KEY_ROLL_R);
        event.register(KEY_CONTROL_LEFT);
        event.register(KEY_CONTROL_RIGHT);
        event.register(KEY_SWITCH_ENEMY);
        event.register(KEY_TOGGLE_SHIELD);
        event.register(KEY_START_WARP);
        event.register(KEY_TOGGLE_FORCE_ASSIST);
        event.register(KEY_TOGGLE_TORQUE_ASSIST);
        event.register(KEY_TOGGLE_ANTI_GRAVITY);
        event.register(KEY_TOGGLE_AUTO_LEVEL);
        event.register(KEY_TOGGLE_DEEPSPACE_HUD);
        event.register(KEY_ACTIVATE_HYPER_RELAY);
    }
}
