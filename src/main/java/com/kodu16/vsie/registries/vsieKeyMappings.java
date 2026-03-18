package com.kodu16.vsie.registries;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "vsie", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class vsieKeyMappings {
    // 定义所有的键位
    public static final KeyMapping KEY_TOGGLE_LOCK = new KeyMapping(
            "key.vsie.toggle_mouse_lock", // 键位描述的语言键
            GLFW.GLFW_KEY_LEFT_ALT, // 默认键位为 alt 键
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_TOGGLE_FLIGHT_ASSIST = new KeyMapping(
            "key.vsie.toggle_flight_assist", // 键位描述的语言键
            GLFW.GLFW_KEY_B, // 默认键位为 alt 键
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_TOGGLE_ANTI_GRAVITY = new KeyMapping(
            "key.vsie.toggle_anti_gravity", // 键位描述的语言键
            GLFW.GLFW_KEY_G, // 默认键位为 alt 键
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_THROTTLE = new KeyMapping(
            "key.vsie.throttle", // 键位描述的语言键
            GLFW.GLFW_KEY_TAB,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_BRAKE = new KeyMapping(
            "key.vsie.brake", // 键位描述的语言键
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_ROLL_L = new KeyMapping(
            "key.vsie.roll_left", // 键位描述的语言键
            GLFW.GLFW_KEY_A,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_ROLL_R = new KeyMapping(
            "key.vsie.roll_right", // 键位描述的语言键
            GLFW.GLFW_KEY_D,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_SWITCH_ENEMY = new KeyMapping(
            "key.vsie.switch_enemy", // 键位描述的语言键
            GLFW.GLFW_KEY_Z,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_TOGGLE_SHIELD = new KeyMapping(
            "key.vsie.toggle_shield", // 键位描述的语言键
            GLFW.GLFW_KEY_V,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_START_WARP = new KeyMapping(
            "key.vsie.warp", // 键位描述的语言键
            GLFW.GLFW_KEY_P,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_TOGGLE_WEAPON_CHANNEL1 = new KeyMapping(
            "key.vsie.toggle_weapon_channel_1",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,                 // ← 指定要按住 Ctrl
            InputConstants.Type.KEYSYM,          // 按鍵
            GLFW.GLFW_KEY_H,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_WEAPON_CHANNEL2 = new KeyMapping(
            "key.vsie.toggle_weapon_channel_2",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,                 // ← 指定要按住 Ctrl
            InputConstants.Type.KEYSYM,          // 按鍵
            GLFW.GLFW_KEY_J,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_WEAPON_CHANNEL3 = new KeyMapping(
            "key.vsie.toggle_weapon_channel_3",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,                 // ← 指定要按住 Ctrl
            InputConstants.Type.KEYSYM,          // 按鍵
            GLFW.GLFW_KEY_K,
            "category.vsie"
    );
    public static final KeyMapping KEY_TOGGLE_WEAPON_CHANNEL4 = new KeyMapping(
            "key.vsie.toggle_weapon_channel_4",
            KeyConflictContext.IN_GAME,
            KeyModifier.CONTROL,                 // ← 指定要按住 Ctrl
            InputConstants.Type.KEYSYM,          // 按鍵
            GLFW.GLFW_KEY_L,
            "category.vsie"
    );


    public static void register(IEventBus modBus) {
        // 注册事件监听器，确保键位注册仅发生在客户端
        modBus.addListener(vsieKeyMappings::registerKeyMappings);
    }

    // 在客户端注册所有的键位
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        // 注册键位
        event.register(KEY_TOGGLE_LOCK);
        event.register(KEY_THROTTLE);
        event.register(KEY_BRAKE);
        event.register(KEY_ROLL_L);
        event.register(KEY_ROLL_R);
        event.register(KEY_SWITCH_ENEMY);
        event.register(KEY_TOGGLE_SHIELD);
        event.register(KEY_START_WARP);
        event.register(KEY_TOGGLE_FLIGHT_ASSIST);
        event.register(KEY_TOGGLE_ANTI_GRAVITY);
        event.register(KEY_TOGGLE_WEAPON_CHANNEL1);
        event.register(KEY_TOGGLE_WEAPON_CHANNEL2);
        event.register(KEY_TOGGLE_WEAPON_CHANNEL3);
        event.register(KEY_TOGGLE_WEAPON_CHANNEL4);
        // 注册其他键位
        // event.register(KEY_ANOTHER_ACTION);
    }
}
