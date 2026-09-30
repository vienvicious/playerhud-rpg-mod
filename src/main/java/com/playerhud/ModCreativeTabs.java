package com.playerhud;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PlayerHudMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.playerhud.main"))
                    .icon(() -> ModItems.LESSER_ENHANCEMENT_STONE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BOSS_TICKET.get());
                        output.accept(ModItems.INVENTORY_PROTECTION_TICKET.get());
                        output.accept(ModItems.LESSER_ENHANCEMENT_STONE.get());
                        output.accept(ModItems.INTERMEDIATE_ENHANCEMENT_STONE.get());
                        output.accept(ModItems.GREATER_ENHANCEMENT_STONE.get());
                        output.accept(ModItems.DOWNGRADE_PROTECTION_SCROLL.get());
                        output.accept(ModItems.ENHANCEMENT_ALTAR.get());
                        output.accept(ModItems.RIDING_TICKET.get());
                        output.accept(ModItems.RIDING_BROWN_HORSE.get());
                        output.accept(ModItems.RIDING_CHESTNUT_HORSE.get());
                        output.accept(ModItems.RIDING_CREAMY_HORSE.get());
                        output.accept(ModItems.RIDING_DARK_BROWN_HORSE.get());
                        output.accept(ModItems.RIDING_CAMEL.get());
                        output.accept(ModItems.RIDING_STRIDER.get());
                        output.accept(ModItems.RIDING_WHITE_HORSE.get());
                        output.accept(ModItems.RIDING_BLACK_HORSE.get());
                        output.accept(ModItems.RIDING_SKELETON_HORSE.get());
                        output.accept(ModItems.RIDING_UNDEAD_HORSE.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
