package com.playerhud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = PlayerHudMod.MOD_ID)
public final class BossArenaManager {
    private static final String BOSS_MARKER = "playerhud_ticket_boss";
    private static final String ORIGIN_TAG = "playerhud_boss_origin";
    private static final ResourceKey<net.minecraft.world.level.Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    private static final BlockPos ARENA_CENTER = new BlockPos(1_000_000, 180, 1_000_000);
    private static final Set<UUID> ACTIVE_PARTY = new HashSet<>();
    private static final Map<UUID, PendingReturn> PENDING_RETURNS = new HashMap<>();
    private static final ServerBossEvent BOSS_BAR = new ServerBossEvent(
            Component.literal("고대의 수호 골렘"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private static boolean activeEncounter;
    private static final int CLEAR = 1;
    private static final int DEFEAT = 2;
    private static final int RETURN_DELAY_TICKS = 100;

    private record PendingReturn(int ticks, int outcome) {}

    private BossArenaManager() {}

    public static boolean startEncounter(ServerPlayer leader) {
        MinecraftServer server = leader.getServer();
        if (server == null || activeEncounter || hasLivingBoss(server)) {
            leader.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "이미 진행 중인 보스전이 있습니다."));
            return false;
        }

        ServerLevel arena = server.getLevel(OVERWORLD);
        if (arena == null) {
            leader.sendSystemMessage(net.minecraft.network.chat.Component.literal("아레나 월드를 찾을 수 없습니다."));
            return false;
        }

        List<ServerPlayer> party = new ArrayList<>();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate.serverLevel() == leader.serverLevel()
                    && candidate.distanceToSqr(leader) <= 64.0D
                    && !candidate.getPersistentData().contains(ORIGIN_TAG)) {
                party.add(candidate);
            }
        }
        if (party.isEmpty()) party.add(leader);

        buildArena(arena);
        ACTIVE_PARTY.clear();
        for (int i = 0; i < party.size(); i++) {
            ServerPlayer member = party.get(i);
            saveOrigin(member);
            ACTIVE_PARTY.add(member.getUUID());
            member.teleportTo(arena, ARENA_CENTER.getX() + (i % 3) - 1,
                    ARENA_CENTER.getY() + 1, ARENA_CENTER.getZ() + (i / 3) - 3,
                    Set.of(), member.getYRot(), member.getXRot());
            member.sendSystemMessage(net.minecraft.network.chat.Component.literal("보스전이 시작됩니다!"));
        }

        IronGolem boss = EntityType.IRON_GOLEM.create(arena);
        if (boss == null) {
            for (ServerPlayer member : party) returnPlayer(member);
            ACTIVE_PARTY.clear();
            return false;
        }
        boss.moveTo(ARENA_CENTER.getX() + 0.5, ARENA_CENTER.getY() + 1,
                ARENA_CENTER.getZ() + 0.5, 0.0F, 0.0F);
        boss.setCustomName(net.minecraft.network.chat.Component.literal("고대의 수호 골렘"));
        boss.setCustomNameVisible(true);
        boss.setPersistenceRequired();
        boss.getPersistentData().putBoolean(BOSS_MARKER, true);
        boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0D);
        boss.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(28.0D);
        boss.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
        boss.getAttribute(Attributes.ARMOR).setBaseValue(12.0D);
        boss.setHealth(boss.getMaxHealth());
        if (!arena.addFreshEntity(boss)) {
            for (ServerPlayer member : party) returnPlayer(member);
            ACTIVE_PARTY.clear();
            return false;
        }
        BOSS_BAR.removeAllPlayers();
        BOSS_BAR.setProgress(1.0F);
        BOSS_BAR.setVisible(true);
        for (ServerPlayer member : party) BOSS_BAR.addPlayer(member);
        activeEncounter = true;
        return true;
    }

    public static boolean endEncounter(MinecraftServer server) {
        boolean existed = false;
        BOSS_BAR.removeAllPlayers();
        ServerLevel arena = server.getLevel(OVERWORLD);
        if (arena != null) {
            arena.getChunkAt(ARENA_CENTER);
            for (IronGolem golem : arena.getEntitiesOfClass(IronGolem.class,
                    new net.minecraft.world.phys.AABB(ARENA_CENTER).inflate(64),
                    entity -> entity.getPersistentData().getBoolean(BOSS_MARKER))) {
                golem.discard();
                existed = true;
            }
        }
        for (UUID id : Set.copyOf(ACTIVE_PARTY)) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.setInvulnerable(false);
                JobNetwork.sendBossOutcome(player, 0);
                returnPlayer(player);
            }
        }
        for (UUID id : Set.copyOf(PENDING_RETURNS.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                player.setInvulnerable(false);
                JobNetwork.sendBossOutcome(player, 0);
                returnPlayer(player);
            }
        }
        PENDING_RETURNS.clear();
        ACTIVE_PARTY.clear();
        activeEncounter = false;
        return existed;
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof IronGolem boss)
                || !boss.getPersistentData().getBoolean(BOSS_MARKER)
                || !(boss.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        server.execute(() -> {
            for (UUID id : Set.copyOf(ACTIVE_PARTY)) {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("보스 처치! 아레나 보상을 획득했습니다."));
                    net.minecraft.world.item.ItemStack reward = new net.minecraft.world.item.ItemStack(
                            Blocks.GOLD_BLOCK.asItem(), 4);
                    if (!player.getInventory().add(reward)) player.drop(reward, false);
                    queueOutcome(player, CLEAR);
                }
            }
            BOSS_BAR.removeAllPlayers();
            ACTIVE_PARTY.clear();
            if (PENDING_RETURNS.isEmpty()) activeEncounter = false;
        });
    }

    @SubscribeEvent
    public static void onPartyMemberDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !ACTIVE_PARTY.contains(player.getUUID())) return;

        event.setCanceled(true);
        player.setHealth(1.0F);
        queueOutcome(player, DEFEAT);
        if (ACTIVE_PARTY.isEmpty()) {
            discardActiveBoss(player.getServer());
            BOSS_BAR.removeAllPlayers();
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getServer().execute(() -> returnPlayer(player));
        }
    }

    @SubscribeEvent
    public static void onBossTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof IronGolem boss)
                || !boss.getPersistentData().getBoolean(BOSS_MARKER)
                || !(boss.level() instanceof ServerLevel)) return;

        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (UUID id : ACTIVE_PARTY) {
            ServerPlayer candidate = boss.getServer().getPlayerList().getPlayer(id);
            if (candidate == null || candidate.serverLevel() != boss.level()) continue;
            double distance = boss.distanceToSqr(candidate);
            if (distance < nearestDistance) {
                nearest = candidate;
                nearestDistance = distance;
            }
        }
        if (nearest == null) return;

        BOSS_BAR.setProgress(Math.max(0.0F, Math.min(1.0F, boss.getHealth() / boss.getMaxHealth())));
        boss.setTarget(nearest);
        if (boss.tickCount % 10 == 0) {
            boss.getNavigation().moveTo(nearest, 1.15D);
        }
        if (nearestDistance <= 9.0D && boss.tickCount % 20 == 0) {
            nearest.hurt(boss.damageSources().mobAttack(boss), 28.0F);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        for (Map.Entry<UUID, PendingReturn> entry : Map.copyOf(PENDING_RETURNS).entrySet()) {
            PendingReturn pending = entry.getValue();
            int remaining = pending.ticks() - 1;
            if (remaining > 0) {
                PENDING_RETURNS.put(entry.getKey(), new PendingReturn(remaining, pending.outcome()));
                continue;
            }

            PENDING_RETURNS.remove(entry.getKey());
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                if (pending.outcome() == DEFEAT) player.setHealth(player.getMaxHealth());
                player.setInvulnerable(false);
                returnPlayer(player);
            }
        }
        if (activeEncounter && ACTIVE_PARTY.isEmpty() && PENDING_RETURNS.isEmpty()) {
            activeEncounter = false;
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            returnPlayer(player);
            ACTIVE_PARTY.remove(player.getUUID());
            PENDING_RETURNS.remove(player.getUUID());
            player.setInvulnerable(false);
            if (ACTIVE_PARTY.isEmpty() && activeEncounter && player.getServer() != null) {
                endEncounter(player.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BOSS_BAR.removePlayer(player);
        }
    }

    private static boolean hasLivingBoss(MinecraftServer server) {
        ServerLevel arena = server.getLevel(OVERWORLD);
        if (arena == null) return false;
        arena.getChunkAt(ARENA_CENTER);
        return !arena.getEntitiesOfClass(IronGolem.class,
                new net.minecraft.world.phys.AABB(ARENA_CENTER).inflate(64),
                entity -> entity.getPersistentData().getBoolean(BOSS_MARKER)).isEmpty();
    }

    private static void queueOutcome(ServerPlayer player, int outcome) {
        ACTIVE_PARTY.remove(player.getUUID());
        BOSS_BAR.removePlayer(player);
        player.setInvulnerable(true);
        PENDING_RETURNS.put(player.getUUID(), new PendingReturn(RETURN_DELAY_TICKS, outcome));
        JobNetwork.sendBossOutcome(player, outcome);
    }

    private static void discardActiveBoss(MinecraftServer server) {
        if (server == null) return;
        ServerLevel arena = server.getLevel(OVERWORLD);
        if (arena == null) return;
        arena.getChunkAt(ARENA_CENTER);
        for (IronGolem golem : arena.getEntitiesOfClass(IronGolem.class,
                new net.minecraft.world.phys.AABB(ARENA_CENTER).inflate(64),
                entity -> entity.getPersistentData().getBoolean(BOSS_MARKER))) {
            golem.discard();
        }
    }

    private static void buildArena(ServerLevel level) {
        int cx = ARENA_CENTER.getX();
        int cy = ARENA_CENTER.getY();
        int cz = ARENA_CENTER.getZ();
        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                level.setBlock(new BlockPos(cx + x, cy, cz + z), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
                if (Math.abs(x) == 16 || Math.abs(z) == 16) {
                    for (int y = 1; y <= 5; y++) {
                        level.setBlock(new BlockPos(cx + x, cy + y, cz + z), Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
                    }
                }
            }
        }
        for (int x = -14; x <= 14; x += 14) {
            for (int z = -14; z <= 14; z += 14) {
                level.setBlock(new BlockPos(cx + x, cy + 1, cz + z), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
            }
        }
    }

    private static void saveOrigin(ServerPlayer player) {
        CompoundTag origin = new CompoundTag();
        origin.putString("dimension", player.level().dimension().location().toString());
        origin.putDouble("x", player.getX());
        origin.putDouble("y", player.getY());
        origin.putDouble("z", player.getZ());
        origin.putFloat("yaw", player.getYRot());
        origin.putFloat("pitch", player.getXRot());
        player.getPersistentData().put(ORIGIN_TAG, origin);
    }

    private static void returnPlayer(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ORIGIN_TAG)) return;
        CompoundTag origin = data.getCompound(ORIGIN_TAG);
        ResourceKey<net.minecraft.world.level.Level> dimension = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(origin.getString("dimension")));
        ServerLevel destination = player.getServer().getLevel(dimension);
        if (destination != null) {
            player.teleportTo(destination, origin.getDouble("x"), origin.getDouble("y"),
                    origin.getDouble("z"), Set.of(), origin.getFloat("yaw"), origin.getFloat("pitch"));
        }
        data.remove(ORIGIN_TAG);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("원래 위치로 돌아왔습니다."));
    }
}
