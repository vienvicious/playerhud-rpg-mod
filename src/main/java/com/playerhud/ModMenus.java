package com.playerhud;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, PlayerHudMod.MOD_ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MenuType<?>, MenuType<EnhancementMenu>> ENHANCEMENT =
            MENUS.register("enhancement", () -> IMenuTypeExtension.create(EnhancementMenu::new));

    private ModMenus() {}
}
