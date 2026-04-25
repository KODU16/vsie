// ModMenuTypes.java
package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.content.turret.heavyturret.HeavyTurretContainerMenu;
import com.kodu16.vsie.content.item.IFF.IFFContainerMenu;
import com.kodu16.vsie.content.item.shieldtool.ShieldToolContainerMenu;
import com.kodu16.vsie.content.controlseat.block.ControlSeatBlockEntity;
import com.kodu16.vsie.content.controlseat.gui.ControlSeatWarpContainerMenu;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.core.ElectroMagnetRailCoreContainerMenu;
import com.kodu16.vsie.content.screen.AbstractScreenBlockEntity;
import com.kodu16.vsie.content.screen.server.ScreenContainerMenu;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxBlockEntity;
import com.kodu16.vsie.content.storage.ammobox.AmmoBoxContainerMenu;
import com.kodu16.vsie.content.turret.AbstractTurretBlockEntity;
import com.kodu16.vsie.content.turret.TurretContainerMenu;
import com.kodu16.vsie.content.weapon.AbstractWeaponBlockEntity;
import com.kodu16.vsie.content.weapon.server.WeaponContainerMenu;
import com.kodu16.vsie.vsie;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    // 功能：在 NeoForge 1.21.1 中使用 Registries.MENU + DeferredHolder 注册菜单类型。
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, vsie.ID);

    // 功能：使用 IMenuTypeExtension#create 注册带额外同步数据（BlockPos）的菜单工厂。
    public static final DeferredHolder<MenuType<?>, MenuType<TurretContainerMenu>> TURRET_MENU = MENUS.register("turret_menu",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AbstractTurretBlockEntity turret = (AbstractTurretBlockEntity) inv.player.level().getBlockEntity(pos);
                return new TurretContainerMenu(windowId, inv, turret);
            }));
    public static final DeferredHolder<MenuType<?>, MenuType<WeaponContainerMenu>> WEAPON_MENU = MENUS.register("weapon_menu",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AbstractWeaponBlockEntity weapon = (AbstractWeaponBlockEntity) inv.player.level().getBlockEntity(pos);
                return new WeaponContainerMenu(windowId, inv, weapon);
            }));
    public static final DeferredHolder<MenuType<?>, MenuType<ScreenContainerMenu>> SCREEN_MENU = MENUS.register("screen_menu",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AbstractScreenBlockEntity screen = (AbstractScreenBlockEntity) inv.player.level().getBlockEntity(pos);
                return new ScreenContainerMenu(windowId, inv, screen);
            }));
    public static final DeferredHolder<MenuType<?>, MenuType<AmmoBoxContainerMenu>> AMMO_BOX_MENU = MENUS.register("ammo_box_menu",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AmmoBoxBlockEntity screen = (AmmoBoxBlockEntity) inv.player.level().getBlockEntity(pos);
                return new AmmoBoxContainerMenu(windowId, inv, screen);
            }));
    public static final DeferredHolder<MenuType<?>, MenuType<ControlSeatWarpContainerMenu>> CONTROL_SEAT_WARP_MENU = MENUS.register("control_seat_warp_menu",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                // 功能：读取控制椅坐标，为 Shift+右键打开的 warp data chip GUI 构建菜单。
                BlockPos pos = data.readBlockPos();
                ControlSeatBlockEntity controlSeat = (ControlSeatBlockEntity) inv.player.level().getBlockEntity(pos);
                return new ControlSeatWarpContainerMenu(windowId, inv, controlSeat);
            }));
    public static final DeferredHolder<MenuType<?>, MenuType<HeavyTurretContainerMenu>> HEAVY_TURRET_MENU = MENUS.register("heavy_turret_menu",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AbstractHeavyTurretBlockEntity turret = (AbstractHeavyTurretBlockEntity) inv.player.level().getBlockEntity(pos);
                return new HeavyTurretContainerMenu(windowId, inv, turret);
            }));

    public static final DeferredHolder<MenuType<?>, MenuType<IFFContainerMenu>> IFF_MENU = MENUS.register("iff_menu",
            () -> IMenuTypeExtension.create((id, inv, data) ->
                    new IFFContainerMenu(id, inv, inv.player.getMainHandItem())));
    public static final DeferredHolder<MenuType<?>, MenuType<ShieldToolContainerMenu>> SHIELD_TOOL_MENU = MENUS.register("shield_tool_menu",
            () -> IMenuTypeExtension.create((id, inv, data) ->
                    new ShieldToolContainerMenu(id, inv, inv.player.getMainHandItem())));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectroMagnetRailCoreContainerMenu>> ELECTRO_MAGNET_RAIL_CORE_MENU = MENUS.register("electro_magnet_rail_core_menu",
            () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                // 读取方块坐标并构建电磁轨核心容器。
                BlockPos pos = data.readBlockPos();
                ElectroMagnetRailCoreBlockEntity core = (ElectroMagnetRailCoreBlockEntity) inv.player.level().getBlockEntity(pos);
                return new ElectroMagnetRailCoreContainerMenu(windowId, inv, core);
            }));
}
