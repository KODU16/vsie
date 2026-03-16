// ModMenuTypes.java
package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.turret.heavyturret.AbstractHeavyTurretBlockEntity;
import com.kodu16.vsie.content.turret.heavyturret.HeavyTurretContainerMenu;
import com.kodu16.vsie.content.item.IFF.IFFContainerMenu;
import com.kodu16.vsie.content.item.shieldtool.ShieldToolContainerMenu;
import com.kodu16.vsie.content.misc.electromagnet_rail.ElectroMagnetRailCoreBlockEntity;
import com.kodu16.vsie.content.misc.electromagnet_rail.ElectroMagnetRailCoreContainerMenu;
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
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, vsie.ID);

    // 正确写法：使用 IForgeMenuType 让容器工厂可以读取额外数据（BlockPos）
    public static final RegistryObject<MenuType<TurretContainerMenu>> TURRET_MENU = MENUS.register("turret_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AbstractTurretBlockEntity turret = (AbstractTurretBlockEntity) inv.player.level().getBlockEntity(pos);
                return new TurretContainerMenu(windowId, inv, turret);
            }));
    public static final RegistryObject<MenuType<WeaponContainerMenu>> WEAPON_MENU = MENUS.register("weapon_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AbstractWeaponBlockEntity weapon = (AbstractWeaponBlockEntity) inv.player.level().getBlockEntity(pos);
                return new WeaponContainerMenu(windowId, inv, weapon);
            }));
    public static final RegistryObject<MenuType<ScreenContainerMenu>> SCREEN_MENU = MENUS.register("screen_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AbstractScreenBlockEntity screen = (AbstractScreenBlockEntity) inv.player.level().getBlockEntity(pos);
                return new ScreenContainerMenu(windowId, inv, screen);
            }));
    public static final RegistryObject<MenuType<AmmoBoxContainerMenu>> AMMO_BOX_MENU = MENUS.register("ammo_box_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AmmoBoxBlockEntity screen = (AmmoBoxBlockEntity) inv.player.level().getBlockEntity(pos);
                return new AmmoBoxContainerMenu(windowId, inv, screen);
            }));
    public static final RegistryObject<MenuType<HeavyTurretContainerMenu>> HEAVY_TURRET_MENU = MENUS.register("heavy_turret_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                // data 里读出客户端传来的 BlockPos
                BlockPos pos = data.readBlockPos();
                AbstractHeavyTurretBlockEntity turret = (AbstractHeavyTurretBlockEntity) inv.player.level().getBlockEntity(pos);
                return new HeavyTurretContainerMenu(windowId, inv, turret);
            }));

    public static final RegistryObject<MenuType<IFFContainerMenu>> IFF_MENU = MENUS.register("iff_menu",
            () -> IForgeMenuType.create((id, inv, data) ->
                    new IFFContainerMenu(id, inv, inv.player.getMainHandItem())));
    public static final RegistryObject<MenuType<ShieldToolContainerMenu>> SHIELD_TOOL_MENU = MENUS.register("shield_tool_menu",
            () -> IForgeMenuType.create((id, inv, data) ->
                    new ShieldToolContainerMenu(id, inv, inv.player.getMainHandItem())));

    public static final RegistryObject<MenuType<ElectroMagnetRailCoreContainerMenu>> ELECTRO_MAGNET_RAIL_CORE_MENU = MENUS.register("electro_magnet_rail_core_menu",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                // 读取方块坐标并构建电磁轨核心容器。
                BlockPos pos = data.readBlockPos();
                ElectroMagnetRailCoreBlockEntity core = (ElectroMagnetRailCoreBlockEntity) inv.player.level().getBlockEntity(pos);
                return new ElectroMagnetRailCoreContainerMenu(windowId, inv, core);
            }));
}
