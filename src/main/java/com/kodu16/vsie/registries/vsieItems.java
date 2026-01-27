package com.kodu16.vsie.registries;

import com.kodu16.vsie.content.item.HUD.targetframe;
import com.kodu16.vsie.content.item.linker.linker;
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
    public static final ItemEntry<targetframe> TARGET_FRAME = REGISTRATE.item("target_frame", targetframe::new).register();
}
