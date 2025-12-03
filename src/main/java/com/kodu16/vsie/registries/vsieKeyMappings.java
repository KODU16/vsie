package com.kodu16.vsie.registries;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
            "key.vsie.roll_left", // 键位描述的语言键
            GLFW.GLFW_KEY_D,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_ADJUST_L =  new KeyMapping(
            "key.vsie.adjust_left", // 键位描述的语言键
            GLFW.GLFW_KEY_A,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_ADJUST_R =  new KeyMapping(
            "key.vsie.adjust_right", // 键位描述的语言键
            GLFW.GLFW_KEY_D,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );
    public static final KeyMapping KEY_SCAN_PERIPHERAL =  new KeyMapping(
            "key.vsie.scan_peripheral", // 键位描述的语言键
            GLFW.GLFW_KEY_RIGHT_ALT,
            "category.vsie" // 键位分类（显示在设置菜单中的类别）
    );

    // 其他键位可以按照类似的方式进行定义
    // public static final KeyMapping KEY_ANOTHER_ACTION = new KeyMapping(...);

    /**
     * 注册所有键位的方法
     */
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
        event.register(KEY_SCAN_PERIPHERAL);
        // 注册其他键位
        // event.register(KEY_ANOTHER_ACTION);
    }
}