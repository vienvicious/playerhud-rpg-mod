package com.playerhud;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = PlayerHudMod.MOD_ID, value = Dist.CLIENT)
public final class EnhancementTooltipEvents {
    private EnhancementTooltipEvents() {}

    @SubscribeEvent
    public static void showEnhancementStats(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        int level = EnhancementSystem.getLevel(stack);
        if (level <= 0 || (!EnhancementSystem.isWeapon(stack) && !EnhancementSystem.isArmor(stack))) return;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && lore.lines().stream().anyMatch(line ->
                line.getContents() instanceof TranslatableContents translated
                        && translated.getKey().startsWith("item.playerhud.enhancement_lore."))) return;

        var tooltip = event.getToolTip();
        int insertAt = Math.min(1, tooltip.size());
        if (EnhancementSystem.isWeapon(stack)) {
            tooltip.add(insertAt++, Component.translatable("item.playerhud.enhancement_lore.weapon", level));
        }
        if (EnhancementSystem.isArmor(stack)) {
            tooltip.add(insertAt++, Component.translatable(
                    "item.playerhud.enhancement_lore.armor_reduction", level * 0.15D));
            int bonusHearts = level / 5;
            if (bonusHearts > 0) {
                tooltip.add(insertAt, Component.translatable(
                        "item.playerhud.enhancement_lore.armor_health", bonusHearts));
            }
        }
    }
}
