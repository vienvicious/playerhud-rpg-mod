package com.playerhud;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = PlayerHudMod.MOD_ID)
public final class RidingMountEvents {
    private static final Map<UUID, UUID> PENDING_DISMISSALS = new ConcurrentHashMap<>();

    private RidingMountEvents() {}

    @SubscribeEvent
    public static void onMountChanged(EntityMountEvent event) {
        if (event.isMounting()
                || !(event.getEntityMounting() instanceof ServerPlayer player)) return;
        Entity mount = event.getEntityBeingMounted();
        if (mount == null) return;
        if (!mount.getPersistentData().getBoolean(RidingMountItem.MOUNT_MARKER)
                || !mount.getPersistentData().hasUUID(RidingMountItem.MOUNT_OWNER)
                || !player.getUUID().equals(mount.getPersistentData().getUUID(RidingMountItem.MOUNT_OWNER))) return;
        if (!player.getPersistentData().hasUUID(RidingMountItem.PLAYER_MOUNT_ID)
                || !mount.getUUID().equals(player.getPersistentData().getUUID(RidingMountItem.PLAYER_MOUNT_ID))) return;
        // Dismounting a horse with Shift does not set the player's crouching state.
        // Queue the exact entity from the mount event and remove it on the server tick.
        PENDING_DISMISSALS.put(player.getUUID(), mount.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID mountId = PENDING_DISMISSALS.remove(player.getUUID());
        if (mountId == null) return;
        Entity mount = player.serverLevel().getEntity(mountId);
        if (mount != null && mount.getPersistentData().getBoolean(RidingMountItem.MOUNT_MARKER)
                && mount.getPersistentData().hasUUID(RidingMountItem.MOUNT_OWNER)
                && player.getUUID().equals(mount.getPersistentData().getUUID(RidingMountItem.MOUNT_OWNER))) {
            // KILLED marks it as dead (unlike discard), without dropping the virtual saddle or loot.
            mount.remove(Entity.RemovalReason.KILLED);
        }

        if (player.getPersistentData().hasUUID(RidingMountItem.PLAYER_MOUNT_ID)
                && mountId.equals(player.getPersistentData().getUUID(RidingMountItem.PLAYER_MOUNT_ID))) {
            player.getPersistentData().remove(RidingMountItem.PLAYER_MOUNT_ID);
        }
    }

    @SubscribeEvent
    public static void onMountDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.getPersistentData().getBoolean(RidingMountItem.MOUNT_MARKER)) event.setCanceled(true);
    }
}
