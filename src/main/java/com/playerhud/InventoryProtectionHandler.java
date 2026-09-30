package com.playerhud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = PlayerHudMod.MOD_ID)
public final class InventoryProtectionHandler {
    private static final Map<UUID, List<ItemStack>> PENDING_RESTORES = new HashMap<>();

    private InventoryProtectionHandler() {}

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        boolean hasProtectionTicket = event.getDrops().stream()
                .map(ItemEntity::getItem)
                .anyMatch(stack -> stack.is(ModItems.INVENTORY_PROTECTION_TICKET.get()));
        if (!hasProtectionTicket) return;

        boolean consumed = false;
        List<ItemStack> savedItems = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem().copy();
            if (!consumed && stack.is(ModItems.INVENTORY_PROTECTION_TICKET.get())) {
                stack.shrink(1);
                consumed = true;
            }
            if (!stack.isEmpty()) savedItems.add(stack);
            drop.discard();
        }

        if (!consumed) return;
        PENDING_RESTORES.put(player.getUUID(), savedItems);
        event.getDrops().clear();
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal(
                "인벤토리 보호권이 사용되어 사망 시 아이템을 보존했습니다."));
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) return;
        List<ItemStack> savedItems = PENDING_RESTORES.remove(player.getUUID());
        if (savedItems == null) return;

        for (ItemStack saved : savedItems) {
            ItemStack remaining = saved.copy();
            if (!player.getInventory().add(remaining) && !remaining.isEmpty()) {
                player.drop(remaining, false);
            }
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}
