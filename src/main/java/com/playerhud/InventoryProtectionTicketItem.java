package com.playerhud;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class InventoryProtectionTicketItem extends Item {
    public InventoryProtectionTicketItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltipComponents,
                                net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable(
                "item.playerhud.inventory_protection_ticket.tooltip"));
    }
}
