package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.item.HUD.horizontal_mark;
import com.kodu16.vsie.content.item.HUD.screen_bg;
import com.kodu16.vsie.content.item.HUD.targetframe;
import com.kodu16.vsie.content.item.HUD.targetframe_ally;
import com.kodu16.vsie.content.item.HUD.targetframe_enemy;
import com.kodu16.vsie.content.item.HUD.targetframe_enemy_locked;
import com.kodu16.vsie.content.item.IFF.iff;
import com.kodu16.vsie.content.item.linker.linker;
import com.kodu16.vsie.content.item.particlecontainer.ParticleContainerItem;
import com.kodu16.vsie.content.item.shieldtool.shieldtool;
import com.kodu16.vsie.content.item.warpdatachip.warp_data_chip;
import com.kodu16.vsie.vsie;
//import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeItem;
//import com.deltasf.createpropulsion.utility.BurnableItem;
//import com.deltasf.createpropulsion.design_goggles.DesignGogglesItem;
import com.kodu16.vsie.content.item.testItem.testItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

public class vsieItems {
    public static final CreateRegistrate REGISTRATE = vsie.registrate();
    public static void register() {} //Loads this class

    //public static final ItemEntry<BurnableItem> PINE_RESIN = REGISTRATE.item("pine_resin", p -> new BurnableItem(p, 1200)).register();
    //Lenses
    //public static final ItemEntry<OpticalLensItem> OPTICAL_LENS = REGISTRATE.item("optical_lens", OpticalLensItem::new).register();
    public static final ItemEntry<testItem> TEST_ITEM = REGISTRATE.item("test_item", testItem::new).register();
    public static final ItemEntry<linker> LINKER = REGISTRATE.item("linker", linker::new).register();
    public static final ItemEntry<horizontal_mark> HORIZONTAL_MARK = REGISTRATE.item("horizontal_mark", horizontal_mark::new).register();
    public static final ItemEntry<targetframe> TARGET_FRAME = REGISTRATE.item("target_frame", targetframe::new).register();
    public static final ItemEntry<targetframe_enemy> TARGET_FRAME_ENEMY = REGISTRATE.item("target_frame_enemy", targetframe_enemy::new).register();
    public static final ItemEntry<targetframe_enemy_locked> TARGET_FRAME_ENEMY_LOCKED = REGISTRATE.item("target_frame_enemy_locked", targetframe_enemy_locked::new).register();
    public static final ItemEntry<targetframe_ally> TARGET_FRAME_ALLY = REGISTRATE.item("target_frame_ally", targetframe_ally::new).register();
    public static final ItemEntry<screen_bg> SCREEN_BG = REGISTRATE.item("screen_bg", screen_bg::new).register();
    // 功能：注册粒子炮弹药容器物品，供 Particle Turret 的专用 3x3 仓位与自动化输入使用。
    public static final ItemEntry<ParticleContainerItem> PARTICLE_CONTAINER = REGISTRATE.item("particle_container", ParticleContainerItem::new).register();
    public static final ItemEntry<iff> IFF = REGISTRATE.item("iff", iff::new).register();
    public static final ItemEntry<shieldtool> SHIELD_TOOL = REGISTRATE.item("shield_tool", shieldtool::new).register();
    public static final ItemEntry<warp_data_chip> WARP_DATA_CHIP = REGISTRATE.item("warp_data_chip", warp_data_chip::new).register();
}
