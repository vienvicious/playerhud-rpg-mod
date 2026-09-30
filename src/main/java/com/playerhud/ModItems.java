package com.playerhud;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlayerHudMod.MOD_ID);
    public static final DeferredItem<Item> BOSS_TICKET = ITEMS.register("boss_ticket",
            () -> new BossTicketItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> INVENTORY_PROTECTION_TICKET = ITEMS.register(
            "inventory_protection_ticket",
            () -> new InventoryProtectionTicketItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> LESSER_ENHANCEMENT_STONE = ITEMS.register(
            "enhancement_stone", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> INTERMEDIATE_ENHANCEMENT_STONE = ITEMS.register(
            "intermediate_enhancement_stone", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> GREATER_ENHANCEMENT_STONE = ITEMS.register(
            "greater_enhancement_stone", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<Item> DOWNGRADE_PROTECTION_SCROLL = ITEMS.register(
            "downgrade_protection_scroll", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<BlockItem> ENHANCEMENT_ALTAR = ITEMS.register("enhancement_altar",
            () -> new BlockItem(ModBlocks.ENHANCEMENT_ALTAR.get(), new Item.Properties()));

    public static final DeferredItem<Item> RIDING_TICKET = ITEMS.register("riding_ticket",
            () -> new RidingTicketItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> RIDING_BROWN_HORSE = ridingMount("riding_brown_horse", RidingMountItem.Mount.BROWN_HORSE);
    public static final DeferredItem<Item> RIDING_CHESTNUT_HORSE = ridingMount("riding_chestnut_horse", RidingMountItem.Mount.CHESTNUT_HORSE);
    public static final DeferredItem<Item> RIDING_CREAMY_HORSE = ridingMount("riding_creamy_horse", RidingMountItem.Mount.CREAMY_HORSE);
    public static final DeferredItem<Item> RIDING_DARK_BROWN_HORSE = ridingMount("riding_dark_brown_horse", RidingMountItem.Mount.DARK_BROWN_HORSE);
    public static final DeferredItem<Item> RIDING_CAMEL = ridingMount("riding_camel", RidingMountItem.Mount.CAMEL);
    public static final DeferredItem<Item> RIDING_STRIDER = ridingMount("riding_strider", RidingMountItem.Mount.STRIDER);
    public static final DeferredItem<Item> RIDING_WHITE_HORSE = ridingMount("riding_white_horse", RidingMountItem.Mount.WHITE_HORSE);
    public static final DeferredItem<Item> RIDING_BLACK_HORSE = ridingMount("riding_black_horse", RidingMountItem.Mount.BLACK_HORSE);
    public static final DeferredItem<Item> RIDING_SKELETON_HORSE = ridingMount("riding_skeleton_horse", RidingMountItem.Mount.SKELETON_HORSE);
    public static final DeferredItem<Item> RIDING_UNDEAD_HORSE = ridingMount("riding_undead_horse", RidingMountItem.Mount.UNDEAD_HORSE);

    private static DeferredItem<Item> ridingMount(String id, RidingMountItem.Mount mount) {
        return ITEMS.register(id, () -> new RidingMountItem(mount, new Item.Properties().stacksTo(1)));
    }

    private ModItems() {}
}
