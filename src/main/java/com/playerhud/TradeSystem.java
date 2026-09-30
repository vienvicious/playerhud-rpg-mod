package com.playerhud;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Curated items and conservative base prices for the job market. */
public final class TradeSystem {

    public record TradeEntry(String itemId, JobType job, long basePrice) {}

    private static final Map<String, TradeEntry> ENTRIES;

    static {
        Map<String, TradeEntry> entries = new LinkedHashMap<>();

        // Miner: raw ores and the matching processed drops. Auto-smelt keeps the same value.
        add(entries, JobType.MINER, 3,
                "minecraft:coal", "minecraft:raw_copper", "minecraft:copper_ingot");
        add(entries, JobType.MINER, 6,
                "minecraft:raw_iron", "minecraft:iron_ingot");
        add(entries, JobType.MINER, 8,
                "minecraft:raw_gold", "minecraft:gold_ingot");
        add(entries, JobType.MINER, 4,
                "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:quartz");
        add(entries, JobType.MINER, 5, "minecraft:amethyst_shard");
        add(entries, JobType.MINER, 22, "minecraft:emerald");
        add(entries, JobType.MINER, 35, "minecraft:diamond");
        add(entries, JobType.MINER, 50, "minecraft:ancient_debris");
        add(entries, JobType.MINER, 60, "minecraft:netherite_scrap");

        // Farmer: harvested staples, Farmer's Delight produce, and a few prepared foods.
        add(entries, JobType.FARMER, 1,
                "minecraft:wheat", "minecraft:carrot", "minecraft:potato",
                "minecraft:beetroot", "minecraft:melon_slice", "minecraft:sweet_berries",
                "minecraft:sugar_cane");
        add(entries, JobType.FARMER, 2,
                "minecraft:cocoa_beans",
                "farmersdelight:tomato", "farmersdelight:onion", "farmersdelight:cabbage_leaf");
        add(entries, JobType.FARMER, 3,
                "minecraft:pumpkin", "minecraft:nether_wart", "farmersdelight:cabbage",
                "farmersdelight:rice");
        add(entries, JobType.FARMER, 1,
                "farmersdelight:rice_panicle", "farmersdelight:straw");
        add(entries, JobType.FARMER, 5, "farmersdelight:tomato_sauce");
        add(entries, JobType.FARMER, 7,
                "farmersdelight:mixed_salad", "farmersdelight:fruit_salad");
        add(entries, JobType.FARMER, 8, "farmersdelight:vegetable_soup");

        // Fisher: fish only. Nets, bait, rods, armor, hooks, and treasure are excluded.
        add(entries, JobType.FISHER, 3,
                "minecraft:cod", "aquaculture:blackfish", "aquaculture:bluegill",
                "aquaculture:carp", "aquaculture:perch", "aquaculture:atlantic_herring");
        add(entries, JobType.FISHER, 5,
                "minecraft:salmon", "minecraft:tropical_fish", "aquaculture:brown_trout",
                "aquaculture:pollock", "aquaculture:pink_salmon", "aquaculture:rainbow_trout",
                "aquaculture:largemouth_bass", "aquaculture:smallmouth_bass",
                "aquaculture:bayad", "aquaculture:boulti", "aquaculture:synodontis");
        add(entries, JobType.FISHER, 7,
                "minecraft:pufferfish", "aquaculture:atlantic_cod", "aquaculture:atlantic_halibut",
                "aquaculture:gar", "aquaculture:catfish", "aquaculture:muskellunge",
                "aquaculture:jellyfish", "aquaculture:red_grouper", "aquaculture:brown_shrooma",
                "aquaculture:red_shrooma");
        add(entries, JobType.FISHER, 12,
                "aquaculture:pacific_halibut", "aquaculture:arapaima", "aquaculture:piranha",
                "aquaculture:tambaqui", "aquaculture:tuna", "aquaculture:capitaine");
        add(entries, JobType.FISHER, 2, "aquaculture:fish_fillet_raw");
        add(entries, JobType.FISHER, 4, "aquaculture:fish_fillet_cooked");
        add(entries, JobType.FISHER, 8, "aquaculture:sushi");
        ENTRIES = Collections.unmodifiableMap(entries);
    }

    private static void add(Map<String, TradeEntry> entries, JobType job, long price, String... ids) {
        for (String id : ids) {
            entries.put(id, new TradeEntry(id, job, price));
        }
    }

    public static TradeEntry getEntry(Item item) {
        if (item == null) return null;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? null : ENTRIES.get(id.toString());
    }

    public static TradeEntry getEntry(String itemId) {
        return ENTRIES.get(itemId);
    }

    public static List<TradeEntry> getAvailableEntries(JobType job) {
        List<TradeEntry> available = new ArrayList<>();
        for (TradeEntry entry : ENTRIES.values()) {
            if (entry.job() == job && BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(entry.itemId())).isPresent()) {
                available.add(entry);
            }
        }
        return available;
    }

    public static long getSaleValue(net.minecraft.world.entity.player.Player player, ItemStack stack, int count) {
        TradeEntry entry = getEntry(stack.getItem());
        if (entry == null) return 0L;
        double multiplier = JobSystem.getSellPriceMultiplier(player, stack);
        return Math.max(1L, Math.round(entry.basePrice() * (double) count * multiplier));
    }

    private TradeSystem() {}
}

