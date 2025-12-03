package com.kodu16.vsie.registries;

import com.kodu16.vsie.vsie;
//import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeItem;
//import com.deltasf.createpropulsion.utility.BurnableItem;
//import com.deltasf.createpropulsion.design_goggles.DesignGogglesItem;
import com.kodu16.vsie.content.testItem.testItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

public class vsieItems {
    public static final CreateRegistrate REGISTRATE = vsie.registrate();
    public static void register() {} //Loads this class

    //public static final ItemEntry<BurnableItem> PINE_RESIN = REGISTRATE.item("pine_resin", p -> new BurnableItem(p, 1200)).register();
    //Lenses
    //public static final ItemEntry<OpticalLensItem> OPTICAL_LENS = REGISTRATE.item("optical_lens", OpticalLensItem::new).register();
    public static final ItemEntry<testItem> TEST_ITEM = REGISTRATE.item("test_item", testItem::new).register();
    //public static final ItemEntry<Item> FOCUS_LENS = REGISTRATE.item("focus_lens", Item::new).register();
    //public static final ItemEntry<Item> INVISIBILITY_LENS = REGISTRATE.item("invisibility_lens", Item::new).register();
    //public static final ItemEntry<Item> UNFINISHED_LENS = REGISTRATE.item("unfinished_lens", Item::new).register();

    //public static final ItemEntry<DesignGogglesItem> DESIGN_GOGGLES = REGISTRATE.item("design_goggles", DesignGogglesItem::new).register();

    //public static final TagKey<Item> OPTICAL_LENS_TAG = makeTag("optical_lens");

    /*public static TagKey<Item> makeTag(String key) {
        ResourceLocation resource = new ResourceLocation(CreatePropulsion.ID, key);
        TagKey<Item> tag = TagKey.create(Registries.ITEM, resource);
        //No datagen :(
        return tag;
    }*/
}
