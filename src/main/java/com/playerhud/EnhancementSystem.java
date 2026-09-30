package com.playerhud;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = PlayerHudMod.MOD_ID)
public final class EnhancementSystem {
    public static final int MAX_LEVEL = 50;
    private static final String LEVEL_KEY = "playerhudEnhancementLevel";
    private static final String BASE_NAME_KEY = "playerhudEnhancementBaseName";
    private static final String HAD_CUSTOM_NAME_KEY = "playerhudEnhancementHadCustomName";
    private static final String SPIRIT_KEY = "playerhudArtisanSpirit";
    private static final String PROJECTILE_LEVEL_KEY = "playerhudEnhancementProjectileLevel";
    private static final net.minecraft.resources.ResourceLocation[] ARMOR_HEALTH_MODIFIERS = {
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PlayerHudMod.MOD_ID, "enhancement_head_health"),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PlayerHudMod.MOD_ID, "enhancement_chest_health"),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PlayerHudMod.MOD_ID, "enhancement_legs_health"),
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(PlayerHudMod.MOD_ID, "enhancement_feet_health")
    };

    private EnhancementSystem() {}

    public static boolean isEnhanceable(ItemStack stack) {
        return !stack.isEmpty() && (isWeapon(stack) || isArmor(stack));
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES) || stack.is(ItemTags.TRIDENT_ENCHANTABLE)
                || stack.is(ItemTags.BOW_ENCHANTABLE) || stack.is(ItemTags.CROSSBOW_ENCHANTABLE)
                || stack.is(ItemTags.MACE_ENCHANTABLE);
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR)
                || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR);
    }

    public static int getLevel(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.max(0, Math.min(MAX_LEVEL, data.copyTag().getInt(LEVEL_KEY)));
    }

    public static int getEnhancementCap(Player player) {
        return Math.min(MAX_LEVEL, (JobSystem.getCombatLevel(player) / 10) * 5);
    }

    public static int getArtisanSpirit(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.max(0, Math.min(100, data.copyTag().getInt(SPIRIT_KEY)));
    }

    public static void setArtisanSpirit(ItemStack stack, int spirit) {
        int boundedSpirit = Math.max(0, Math.min(100, spirit));
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (boundedSpirit == 0) tag.remove(SPIRIT_KEY);
            else tag.putInt(SPIRIT_KEY, boundedSpirit);
        });

    }

    private static void updateEnhancementLore(ItemStack stack, int level) {
        ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        java.util.List<Component> lines = new java.util.ArrayList<>();
        for (Component line : lore.lines()) {
            if (line.getContents() instanceof TranslatableContents translated
                    && translated.getKey().startsWith("item.playerhud.enhancement_lore.")) continue;
            lines.add(line);
        }
        if (level > 0 && isWeapon(stack)) {
            lines.add(Component.translatable("item.playerhud.enhancement_lore.weapon", level));
        }
        if (level > 0 && isArmor(stack)) {
            lines.add(Component.translatable("item.playerhud.enhancement_lore.armor_reduction", level * 0.15D));
            int bonusHearts = level / 5;
            if (bonusHearts > 0) {
                lines.add(Component.translatable("item.playerhud.enhancement_lore.armor_health", bonusHearts));
            }
        }
        if (lines.isEmpty()) stack.remove(DataComponents.LORE);
        else stack.set(DataComponents.LORE, new ItemLore(lines));
    }

    public static int getSuccessChance(int currentLevel) {
        return Math.max(20, (int) Math.round(95.0D - currentLevel * 1.5D));
    }

    public static int getDowngradeChance(int currentLevel) {
        return Math.max(0, Math.min(50, (currentLevel - 9) * 2));
    }

    public static net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.Item> getRequiredStone(int currentLevel) {
        if (currentLevel < 20) return ModItems.LESSER_ENHANCEMENT_STONE;
        if (currentLevel < 40) return ModItems.INTERMEDIATE_ENHANCEMENT_STONE;
        return ModItems.GREATER_ENHANCEMENT_STONE;
    }

    public static String getRequiredStoneName(int currentLevel) {
        if (currentLevel < 20) return "하급 강화석";
        if (currentLevel < 40) return "중급 강화석";
        return "상급 강화석";
    }

    public static void updateArmorHealth(Player player) {
        if (player.level().isClientSide) return;
        var attribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < slots.length; i++) {
            attribute.removeModifier(ARMOR_HEALTH_MODIFIERS[i]);
            ItemStack piece = player.getItemBySlot(slots[i]);
            int bonusHearts = isArmor(piece) ? getLevel(piece) / 5 : 0;
            if (bonusHearts > 0) attribute.addOrReplacePermanentModifier(new AttributeModifier(
                    ARMOR_HEALTH_MODIFIERS[i], bonusHearts * 2.0D, Operation.ADD_VALUE));
        }
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    @SubscribeEvent
    public static void onEquipmentChanged(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) updateArmorHealth(player);
    }

    public static void setLevel(ItemStack stack, int level, HolderLookup.Provider registries) {
        int currentLevel = getLevel(stack);
        int boundedLevel = Math.max(0, Math.min(MAX_LEVEL, level));
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        var oldTag = existing == null ? new net.minecraft.nbt.CompoundTag() : existing.copyTag();
        String baseNameJson = oldTag.getString(BASE_NAME_KEY);
        boolean hadCustomName = oldTag.getBoolean(HAD_CUSTOM_NAME_KEY);

        if (currentLevel == 0 && boundedLevel > 0) {
            hadCustomName = stack.has(DataComponents.CUSTOM_NAME);
            baseNameJson = Component.Serializer.toJson(stack.getHoverName(), registries);
        }

        final String savedBaseName = baseNameJson;
        final boolean savedHadCustomName = hadCustomName;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (boundedLevel > 0) {
                tag.putInt(LEVEL_KEY, boundedLevel);
                tag.putString(BASE_NAME_KEY, savedBaseName);
                tag.putBoolean(HAD_CUSTOM_NAME_KEY, savedHadCustomName);
            } else {
                tag.remove(LEVEL_KEY);
                tag.remove(BASE_NAME_KEY);
                tag.remove(HAD_CUSTOM_NAME_KEY);
            }
        });

        updateEnhancementLore(stack, boundedLevel);

        if (boundedLevel > 0) {
            Component baseName = Component.Serializer.fromJson(savedBaseName, registries);
            if (baseName == null) baseName = Component.literal(stack.getItem().toString());
            stack.set(DataComponents.CUSTOM_NAME,
                    Component.literal(String.format(java.util.Locale.ROOT, "+%02d강 ", boundedLevel))
                            .withStyle(ChatFormatting.GOLD).append(baseName));
        } else if (currentLevel > 0) {
            if (hadCustomName) {
                Component baseName = Component.Serializer.fromJson(savedBaseName, registries);
                if (baseName != null) stack.set(DataComponents.CUSTOM_NAME, baseName);
            } else {
                stack.remove(DataComponents.CUSTOM_NAME);
            }
        }
    }

    @SubscribeEvent
    public static void onEquipmentDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getMainHandItem();
            int weaponLevel = isWeapon(weapon) ? getLevel(weapon) : 0;
            if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow) {
                weaponLevel = Math.max(weaponLevel,
                        arrow.getPersistentData().getInt(PROJECTILE_LEVEL_KEY));
            }
            if (weaponLevel > 0) {
                event.setNewDamage(event.getNewDamage() * (1.0F + weaponLevel * 0.01F));
            }
        }

        if (event.getEntity() instanceof Player player && event.getSource().getEntity() != null) {
            int armorLevelSum = 0;
            for (ItemStack armorPiece : player.getArmorSlots()) {
                if (isArmor(armorPiece)) armorLevelSum += getLevel(armorPiece);
            }
            if (armorLevelSum > 0) {
                float reduction = Math.min(0.30F, armorLevelSum * 0.0015F);
                event.setNewDamage(event.getNewDamage() * (1.0F - reduction));
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileCreated(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof Player player)) return;
        ItemStack weapon = player.getMainHandItem();
        if (isWeapon(weapon)) {
            int level = getLevel(weapon);
            if (level > 0) arrow.getPersistentData().putInt(PROJECTILE_LEVEL_KEY, level);
        }
    }
}
