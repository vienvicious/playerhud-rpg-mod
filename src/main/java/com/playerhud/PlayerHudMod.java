package com.playerhud;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(PlayerHudMod.MOD_ID)
public class PlayerHudMod {
    public static final String MOD_ID = "playerhud";

    public PlayerHudMod(IEventBus modEventBus) {
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
    }
}
