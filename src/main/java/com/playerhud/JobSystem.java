package com.playerhud;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = "playerhud")
public class JobSystem {

    private static final int MAX_LEVEL = 100;
    private static final Map<UUID, List<PendingReplant>> PENDING_REPLANTS = new HashMap<>();
    private static final Set<UUID> VEIN_MINING = new HashSet<>();
    private record PendingReplant(ServerLevel level, net.minecraft.core.BlockPos pos,
                                  BlockState state, UUID owner, int farmerLevel) {}

    private static final String JOB =
            "playerhud_job";

    private static final String MINER_XP =
            "playerhud_miner_xp";

    private static final String FARMER_XP =
            "playerhud_farmer_xp";

    private static final String FISHER_XP =
            "playerhud_fisher_xp";

    private static final String COMBAT_XP =
            "playerhud_combat_xp";

    private static final ResourceLocation COMBAT_HEALTH_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath("playerhud", "combat_level_health");
    private static final String TICKET_BOSS_MARKER = "playerhud_ticket_boss";

    private static final String FISHING_SPEED_REMAINDER =
            "playerhud_fishing_speed_remainder";

    private static String xpKey(JobType job) {
        return switch (job) {
            case MINER -> MINER_XP;
            case FARMER -> FARMER_XP;
            case FISHER -> FISHER_XP;
        };
    }

    public static int getJobXp(Player player, JobType job) {
        return Math.max(0, player.getPersistentData().getInt(xpKey(job)));
    }

    public static void setJobLevel(ServerPlayer player, JobType job, int requestedLevel) {
        int level = Math.max(1, Math.min(MAX_LEVEL, requestedLevel));
        int xp = getXpForLevel(level);
        player.getPersistentData().putInt(JOB, job.ordinal());
        player.getPersistentData().putInt(xpKey(job), xp);
        if (job == JobType.FARMER) {
            FarmerCropData.get(player.getServer()).updateFarmerLevel(player.getUUID(), level);
        }
        JobNetwork.sendJobProgress(player, job.ordinal(), xp, 0);
    }

    private static int getJobLevel(Player player, JobType job) {
        return getLevelFromXp(getJobXp(player, job));
    }

    public static double getSellPriceMultiplier(Player player, ItemStack item) {
        JobType job = getJob(player);
        if (job == null || item.isEmpty()) {
            return 1.0;
        }

        boolean supportedItem = switch (job) {
            case MINER -> isOreItem(item);
            case FARMER -> isFarmProduce(item);
            case FISHER -> isFishItem(item);
        };
        if (!supportedItem) {
            return 1.0;
        }

        int level = getJobLevel(player, job);
        // Lv.20 기본 판매 보너스 5%, Lv.80 추가 10%.
        return 1.0 + (level >= 20 ? 0.05 : 0.0) + (level >= 80 ? 0.10 : 0.0);
    }

    private static boolean isOreItem(ItemStack item) {
        String path = item.getItem().builtInRegistryHolder().key().location().getPath();
        return path.contains("ore") || path.equals("ancient_debris");
    }

    private static boolean isFishItem(ItemStack item) {
        var id = item.getItem().builtInRegistryHolder().key().location();
        return item.is(ItemTags.FISHES) || id.getNamespace().equals("aquaculture");
    }

    private static boolean isFarmProduce(ItemStack item) {
        var itemId = item.getItem().builtInRegistryHolder().key().location();
        String id = itemId.getPath();
        if (itemId.getNamespace().equals("farmersdelight")) {
            return switch (id) {
                case "cabbage", "tomato", "onion", "rice", "cabbage_leaf",
                        "tomato_seeds", "rice_panicle" -> true;
                default -> false;
            };
        }
        return id.equals("wheat") || id.equals("carrot") || id.equals("potato")
                || id.equals("beetroot") || id.equals("melon") || id.equals("pumpkin")
                || id.equals("nether_wart") || id.equals("cocoa_beans")
                || id.equals("sweet_berries") || id.equals("sugar_cane")
                || id.equals("beetroot_seeds") || id.equals("wheat_seeds");
    }

    private static void addJobXp(ServerPlayer player, JobType job, int gainedXp) {
        if (gainedXp <= 0) {
            return;
        }

        int level = getJobLevel(player, job);
        // Lv.30부터 XP +10%, Lv.60에서 추가 +10%.
        int adjustedXp = gainedXp;
        if (level >= 30) adjustedXp += Math.max(1, gainedXp / 10);
        if (level >= 60) adjustedXp += Math.max(1, gainedXp / 10);
        int newXp = Math.min(getXpForMaxLevel(), getJobXp(player, job) + adjustedXp);
        if (newXp == getJobXp(player, job)) {
            return;
        }
        player.getPersistentData().putInt(xpKey(job), newXp);
        if (job == JobType.FARMER) {
            FarmerCropData.get(player.getServer()).updateFarmerLevel(
                    player.getUUID(),
                    getLevelFromXp(newXp)
            );
        }
        JobNetwork.sendJobProgress(player, job.ordinal(), newXp, adjustedXp);
    }

    /*
     * =========================
     * 직업
     * =========================
     */

    public static boolean hasJob(
            Player player
    ) {
        if (!player.getPersistentData().contains(JOB)) {
            return false;
        }

        int id = player.getPersistentData().getInt(JOB);
        return id >= 0 && id < JobType.values().length;
    }

    public static JobType getJob(
            Player player
    ) {

        if (!hasJob(player)) {
            return null;
        }

        int id =
                player
                        .getPersistentData()
                        .getInt(JOB);

        if (id < 0 || id >= JobType.values().length) {
            return null;
        }

        return JobType.values()[id];
    }

    public static void setJob(
            ServerPlayer player,
            int jobId
    ) {

        if (hasJob(player)) {
            return;
        }

        if (
                jobId < 0
                        || jobId >= JobType.values().length
        ) {
            return;
        }

        player
                .getPersistentData()
                .putInt(
                        JOB,
                        jobId
                );

        JobType job =
                JobType.values()[jobId];

        player.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                        "직업이 "
                                + job.getDisplayName()
                                + "으로 결정되었습니다!"
                )
        );

        JobNetwork.sendJobProgress(
                player,
                jobId,
                getJobXp(player, job),
                0
        );
        FarmerCropData.get(player.getServer()).updateFarmerLevel(
                player.getUUID(),
                job == JobType.FARMER ? getJobLevel(player, job) : 0
        );

        /*
         * 직업 선택이 완료됐으므로
         * 혹시 화면이 다시 열리지 않게 함
         */
    }

    public static void resetJob(ServerPlayer player) {
        player.getPersistentData().remove(JOB);
        FarmerCropData.get(player.getServer()).updateFarmerLevel(player.getUUID(), 0);
        JobNetwork.openJobSelection(player);
    }

    public static int getCombatXp(Player player) {
        return Math.max(0, player.getPersistentData().getInt(COMBAT_XP));
    }

    public static int getCombatLevel(Player player) {
        return getCombatLevelFromXp(getCombatXp(player));
    }

    public static void setCombatLevel(ServerPlayer player, int requestedLevel) {
        int level = Math.max(1, Math.min(MAX_LEVEL, requestedLevel));
        int xp = getCombatXpForLevel(level);
        player.getPersistentData().putInt(COMBAT_XP, xp);
        applyCombatHealthBonus(player);
        JobNetwork.sendCombatProgress(player, xp, 0);
    }

    private static boolean isCombatTarget(LivingEntity entity) {
        return entity.getType().getCategory() == MobCategory.MONSTER
                || entity.getPersistentData().getBoolean(TICKET_BOSS_MARKER);
    }

    private static void applyCombatHealthBonus(ServerPlayer player) {
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) return;

        maxHealth.removeModifier(COMBAT_HEALTH_BONUS_ID);
        int bonusHearts = getCombatLevel(player) / 20;
        if (bonusHearts > 0) {
            maxHealth.addOrReplacePermanentModifier(new AttributeModifier(
                    COMBAT_HEALTH_BONUS_ID, bonusHearts * 2.0D, Operation.ADD_VALUE));
        }
        player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
    }

    /*
     * =========================
     * 접속
     * =========================
     */

    @SubscribeEvent
    public static void onPlayerLogin(
            PlayerEvent.PlayerLoggedInEvent event
    ) {

        Player player =
                event.getEntity();

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        /*
         * 직업이 없으면 선택창
         */

        if (!hasJob(player)) {

            JobNetwork.openJobSelection(
                    serverPlayer
            );

        } else {

            JobType job = getJob(player);
            JobNetwork.sendJobProgress(
                    serverPlayer,
                    job.ordinal(),
                    getJobXp(player, job),
                    0
            );
            FarmerCropData.get(serverPlayer.getServer()).updateFarmerLevel(
                    serverPlayer.getUUID(),
                    job == JobType.FARMER ? getJobLevel(player, job) : 0
            );
        }

        applyCombatHealthBonus(serverPlayer);

        /*
         * 돈
         */

        long money =
                EconomySystem.getMoney(
                        player
                );

        JobNetwork.sendMoney(
                serverPlayer,
                money
        );
        JobNetwork.sendCombatProgress(serverPlayer, getCombatXp(serverPlayer), 0);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var oldData = event.getOriginal().getPersistentData();
        if (oldData.contains(COMBAT_XP)) {
            player.getPersistentData().putInt(COMBAT_XP, oldData.getInt(COMBAT_XP));
        }
        applyCombatHealthBonus(player);
        JobNetwork.sendCombatProgress(player, getCombatXp(player), 0);
    }

    /*
     * =========================
     * 광물 XP
     * =========================
     */

    private static int getOreXp(
            String blockId
    ) {

        if (blockId.contains("coal_ore")) {
            return 3;
        }

        if (blockId.contains("copper_ore")) {
            return 4;
        }

        if (blockId.contains("iron_ore")) {
            return 7;
        }

        if (blockId.contains("gold_ore")) {
            return 12;
        }

        if (blockId.contains("lapis_ore")) {
            return 10;
        }

        if (blockId.contains("redstone_ore")) {
            return 10;
        }

        if (blockId.contains("diamond_ore")) {
            return 30;
        }

        if (blockId.contains("emerald_ore")) {
            return 40;
        }

        if (blockId.contains("ancient_debris")) {
            return 100;
        }

        if (blockId.contains("ore")) {
            return 5;
        }

        return 0;
    }

    private static int getFarmerXp(BlockState state, String blockId) {
        if (blockId.equals("melon") || blockId.equals("pumpkin")) {
            return 5;
        }

        if (!isMatureCrop(state)) {
            return 0;
        }

        if (blockId.contains("nether_wart")) return 10;
        if (blockId.contains("wheat") || blockId.contains("beetroots")
                || blockId.contains("cabbage") || blockId.contains("rice")) return 8;
        if (blockId.contains("carrots") || blockId.contains("potatoes")
                || blockId.contains("tomato") || blockId.contains("onion")) return 6;
        if (blockId.contains("cocoa")) return 6;
        if (blockId.contains("sweet_berry_bush")) return 5;
        return 0;
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }

        JobType job = getJob(player);
        int level = job == null ? 0 : getJobLevel(player, job);
        BlockState state = event.getState();
        String blockPath = state.getBlock().builtInRegistryHolder().key().location().getPath();
        boolean ore = blockPath.contains("ore") || blockPath.equals("ancient_debris");
        boolean crop = job == JobType.FARMER && isHarvestableFarmerCrop(state);
        boolean earnsDouble = (job == JobType.MINER && ore || crop) && level >= 50;

        if (earnsDouble) {
            List<ItemEntity> originals = List.copyOf(event.getDrops());
            for (ItemEntity original : originals) {
                ItemStack source = original.getItem();
                int remaining = source.getCount();
                while (remaining > 0) {
                    int amount = Math.min(remaining, source.getMaxStackSize());
                    ItemEntity extra = new ItemEntity(
                            event.getLevel(), original.getX(), original.getY(), original.getZ(),
                            source.copyWithCount(amount)
                    );
                    extra.setDefaultPickUpDelay();
                    event.getDrops().add(extra);
                    remaining -= amount;
                }
            }
        }

        if (job == JobType.MINER && level >= 70 && ore && player.getRandom().nextFloat() < 0.05F) {
            List<ItemEntity> originals = List.copyOf(event.getDrops());
            for (ItemEntity original : originals) {
                ItemStack stack = original.getItem();
                ItemEntity bonus = new ItemEntity(event.getLevel(), original.getX(), original.getY(),
                        original.getZ(), stack.copy());
                bonus.setDefaultPickUpDelay();
                event.getDrops().add(bonus);
            }
        }

        Item replantItem = crop ? getReplantItem(state) : Items.AIR;
        if (crop && level >= 70 && replantItem != Items.AIR
                && player.getRandom().nextFloat() < (level >= 90 ? 0.30F : 0.15F)) {
            ItemStack seed = new ItemStack(replantItem);
            if (!seed.isEmpty()) {
                ItemEntity bonusSeed = new ItemEntity(event.getLevel(), player.getX(), player.getY(),
                        player.getZ(), seed);
                bonusSeed.setDefaultPickUpDelay();
                event.getDrops().add(bonusSeed);
            }
        }

        // Lv.90 광부 자동 제련. 실크 터치 원석은 드롭 아이템이 달라 그대로 유지된다.
        if (job == JobType.MINER && level >= 90 && ore) {
            for (ItemEntity drop : event.getDrops()) {
                ItemStack stack = drop.getItem();
                if (stack.is(Items.RAW_IRON)) stack = new ItemStack(Items.IRON_INGOT, stack.getCount());
                else if (stack.is(Items.RAW_GOLD)) stack = new ItemStack(Items.GOLD_INGOT, stack.getCount());
                else if (stack.is(Items.RAW_COPPER)) stack = new ItemStack(Items.COPPER_INGOT, stack.getCount());
                drop.setItem(stack);
            }
        }

        // Lv.90 자동 재심기: 우선 수확물에서 심기 아이템을 쓰고,
        // 수확물에 씨앗이 없으면 인벤토리의 같은 아이템을 사용한다.
        if (crop && level >= 90 && replantItem != Items.AIR
                && (consumeOneDrop(event.getDrops(), replantItem)
                || consumeOneInventoryItem(player, replantItem))) {
            BlockState replanted = withAgeZero(state);
            if (replanted != null) {
                PENDING_REPLANTS.computeIfAbsent(player.getUUID(), ignored -> new java.util.ArrayList<>())
                        .add(new PendingReplant(event.getLevel(), event.getPos().immutable(), replanted,
                                player.getUUID(), level));
            }
        }
    }

    private static Item getReplantItem(BlockState state) {
        String blockId = state.getBlock().builtInRegistryHolder().key().location().toString();
        String itemId = switch (blockId) {
            case "minecraft:wheat" -> "minecraft:wheat_seeds";
            case "minecraft:carrots" -> "minecraft:carrot";
            case "minecraft:potatoes" -> "minecraft:potato";
            case "minecraft:beetroots" -> "minecraft:beetroot_seeds";
            case "minecraft:nether_wart" -> "minecraft:nether_wart";
            case "minecraft:cocoa" -> "minecraft:cocoa_beans";
            case "minecraft:sweet_berry_bush" -> "minecraft:sweet_berries";
            case "farmersdelight:cabbages" -> "farmersdelight:cabbage_seeds";
            case "farmersdelight:tomatoes" -> "farmersdelight:tomato_seeds";
            case "farmersdelight:onions" -> "farmersdelight:onion";
            case "farmersdelight:rice" -> "farmersdelight:rice";
            default -> null;
        };
        return itemId == null ? Items.AIR
                : BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
    }

    private static boolean consumeOneDrop(List<ItemEntity> drops, Item item) {
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem();
            if (stack.is(item) && !stack.isEmpty()) {
                stack.shrink(1);
                if (stack.isEmpty()) drop.discard();
                return true;
            }
        }
        return false;
    }

    private static boolean consumeOneInventoryItem(ServerPlayer player, Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item) && !stack.isEmpty()) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static boolean isMatureCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                int current = state.getValue(age);
                int max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(current);
                return current >= max;
            }
        }
        return false;
    }

    private static boolean isHarvestableFarmerCrop(BlockState state) {
        String path = state.getBlock().builtInRegistryHolder().key().location().getPath();
        return isMatureCrop(state) || path.equals("melon") || path.equals("pumpkin");
    }

    @SubscribeEvent
    public static void onFarmerUltimate(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || getJob(player) != JobType.FARMER
                || getJobLevel(player, JobType.FARMER) < 40
                || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
                || !player.isCrouching()
                || !event.getItemStack().is(ItemTags.HOES)) {
            return;
        }
        int level = getJobLevel(player, JobType.FARMER);
        int radius = level >= 100 ? 4 : 1;
        FarmerCropData data = FarmerCropData.get(player.getServer());
        BlockPos center = event.getPos();
        BlockState centerState = player.serverLevel().getBlockState(center);
        if (!data.isCropOwnedBy(player.serverLevel(), center, player.getUUID())
                || !isHarvestableFarmerCrop(centerState)
                || (level < 100 && !isMatureCrop(centerState))) {
            return;
        }
        int harvested = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = center.offset(x, 0, z);
                BlockState state = player.serverLevel().getBlockState(pos);
                if (data.isCropOwnedBy(player.serverLevel(), pos, player.getUUID())
                        && isHarvestableFarmerCrop(state)
                        && (level >= 100 || isMatureCrop(state))
                        && player.gameMode.destroyBlock(pos)) {
                    harvested++;
                }
            }
        }
        if (level >= 100) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "🌾 풍요의 계절! 9×9 범위에서 다 자란 내 작물 " + harvested
                            + "개를 수확했습니다. 자동 재심기가 작동합니다."));
        } else {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "🌾 풍요 수확! 3×3 범위에서 다 자란 내 작물 " + harvested + "개를 수확했습니다."));
        }
        // Prevent the same crouch-click from also starting normal block breaking.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMinerUltimate(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || getJob(player) != JobType.MINER
                || getJobLevel(player, JobType.MINER) < 100
                || !event.getItemStack().is(ItemTags.PICKAXES)) return;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DIG_SPEED, 600, 1, true, false, true));
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "⛏ 심층 탐사! 30초 동안 채굴 속도가 크게 증가합니다."));
    }

    private static BlockState withAgeZero(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty age && age.getName().equals("age")) {
                int min = age.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(0);
                return state.setValue(age, min);
            }
        }
        return null;
    }

    private static void processPendingReplants(ServerPlayer player) {
        List<PendingReplant> pending = PENDING_REPLANTS.remove(player.getUUID());
        if (pending == null) {
            return;
        }
        for (PendingReplant replant : pending) {
            BlockState planted = replant.state();
            if (replant.farmerLevel() >= 100) {
                BlockState advanced = getNextAgeState(planted);
                if (advanced != null) planted = advanced;
            }
            if (replant.level().getBlockState(replant.pos()).isAir()
                    && planted.canSurvive(replant.level(), replant.pos())) {
                replant.level().setBlock(replant.pos(), planted, 3);
                FarmerCropData.get(player.getServer()).markCrop(
                        replant.level(), replant.pos(), replant.owner(), replant.farmerLevel()
                );
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(
            BlockEvent.BreakEvent event
    ) {

        if (event.isCanceled()) {
            return;
        }

        Player player =
                event.getPlayer();

        if (player == null) {
            return;
        }

        BlockState state =
                event.getState();

        if (!event.isCanceled() && player instanceof ServerPlayer serverPlayer
                && isGrowthCrop(state)) {
            FarmerCropData.get(serverPlayer.getServer()).removeCrop(
                    serverPlayer.serverLevel(),
                    event.getPos()
            );
        }

        JobType job = getJob(player);
        if (job != JobType.MINER && job != JobType.FARMER) {
            return;
        }

        String blockId =
                state
                        .getBlock()
                        .builtInRegistryHolder()
                        .key()
                        .location()
                        .getPath();

        int gainedXp = job == JobType.MINER
                ? getOreXp(blockId)
                : getFarmerXp(state, blockId);

        if (gainedXp <= 0) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            addJobXp(serverPlayer, job, gainedXp);
            if (job == JobType.MINER && getJobLevel(serverPlayer, job) >= 40
                    && !VEIN_MINING.contains(serverPlayer.getUUID())
                    && serverPlayer.getMainHandItem().is(ItemTags.PICKAXES)
                    && (blockId.contains("ore") || blockId.equals("ancient_debris"))) {
                VEIN_MINING.add(serverPlayer.getUUID());
                try {
                    BlockPos origin = event.getPos();
                    int mined = 0;
                    for (int dx = -1; dx <= 1 && mined < 3; dx++) {
                        for (int dy = -1; dy <= 1 && mined < 3; dy++) {
                            for (int dz = -1; dz <= 1 && mined < 3; dz++) {
                                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) continue;
                                BlockPos neighbor = origin.offset(dx, dy, dz);
                                if (serverPlayer.serverLevel().getBlockState(neighbor).is(state.getBlock())
                                        && serverPlayer.gameMode.destroyBlock(neighbor)) mined++;
                            }
                        }
                    }
                } finally {
                    VEIN_MINING.remove(serverPlayer.getUUID());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCropGrow(CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        FarmerCropData cropData = FarmerCropData.get(level.getServer());
        int farmerLevel = cropData.getCropOwnerLevel(level, event.getPos());
        if (farmerLevel <= 0) {
            return;
        }

        BlockState nextAge = getNextAgeState(event.getState());
        if (nextAge == null) {
            return;
        }

        float extraGrowthChance = Math.min(0.5F, farmerLevel * 0.005F);
        if (level.random.nextFloat() < extraGrowthChance) {
            level.setBlock(event.getPos(), nextAge, 2);
        }
    }

    private static boolean isGrowthCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock) {
            return true;
        }
        String path = state.getBlock().builtInRegistryHolder().key().location().getPath();
        boolean likelyCrop = path.contains("crop") || path.contains("cabbage")
                || path.contains("tomato") || path.contains("onion")
                || path.contains("rice") || path.contains("nether_wart")
                || path.contains("sweet_berry") || path.contains("cocoa");
        return likelyCrop && state.getProperties().stream().anyMatch(property ->
                property instanceof IntegerProperty integerProperty
                        && integerProperty.getName().equals("age")
        );

    }

    private static BlockState getNextAgeState(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty ageProperty
                    && ageProperty.getName().equals("age")) {
                int age = state.getValue(ageProperty);
                int maxAge = ageProperty.getPossibleValues().stream()
                        .mapToInt(Integer::intValue).max().orElse(age);
                if (age < maxAge) {
                    return state.setValue(ageProperty, age + 1);
                }
                return null;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onCropPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        FarmerCropData data = FarmerCropData.get(level.getServer());
        if (isGrowthCrop(event.getPlacedBlock())
                && getJob(player) == JobType.FARMER) {
            data.markCrop(
                    level,
                    event.getPos(),
                    player.getUUID(),
                    getJobLevel(player, JobType.FARMER)
            );
        } else {
            data.removeCrop(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onFishCaught(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || getJob(player) != JobType.FISHER) {
            return;
        }

        int fisherLevel = getJobLevel(player, JobType.FISHER);
        float junkRemovalChance = fisherLevel >= 90 ? 0.50F : fisherLevel >= 40 ? 0.25F : 0.0F;
        if (junkRemovalChance > 0 && player.getRandom().nextFloat() < junkRemovalChance) {
            event.getDrops().removeIf(JobSystem::isFishingJunk);
        }
        float bonusChance = Math.min(0.20F, fisherLevel * 0.002F);
        boolean caughtFish = event.getDrops().stream().anyMatch(JobSystem::isFishItem);
        if (!caughtFish) {
            return;
        }
        addJobXp(player, JobType.FISHER, 10);

        // Level 50 reward: receive a second copy of every fish caught.
        if (fisherLevel >= 50) {
            event.getDrops().stream()
                    .filter(JobSystem::isFishItem)
                    .map(ItemStack::copy)
                    .forEach(stack -> {
                        if (!player.addItem(stack)) {
                            player.drop(stack, false);
                        }
                    });
        }
        if (fisherLevel >= 100) {
            ItemStack rare = switch (player.getRandom().nextInt(100)) {
                case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 -> createFishingEnchantedBook(player);
                case 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 -> new ItemStack(Items.SADDLE);
                case 20, 21, 22, 23, 24, 25, 26, 27, 28, 29 -> new ItemStack(Items.NAME_TAG);
                default -> new ItemStack(Items.NAUTILUS_SHELL);
            };
            if (!player.addItem(rare)) player.drop(rare, false);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "🎣 전설 어획! 희귀 보상을 추가로 얻었습니다."));
        }
        if (caughtFish && player.getRandom().nextFloat() < bonusChance) {
            ItemStack bonus = new ItemStack(Items.NAUTILUS_SHELL);
            if (!player.addItem(bonus)) {
                player.drop(bonus, false);
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "🎣 희귀 어획! 앵무조개 껍데기를 추가로 얻었습니다."
            ));
        }
    }

    private static boolean isFishingJunk(ItemStack stack) {
        return stack.is(Items.LEATHER_BOOTS) || stack.is(Items.ROTTEN_FLESH)
                || stack.is(Items.STICK) || stack.is(Items.STRING) || stack.is(Items.BOWL)
                || stack.is(Items.INK_SAC) || stack.is(Items.TRIPWIRE_HOOK)
                || stack.is(Items.FISHING_ROD);
    }

    private static ItemStack createFishingEnchantedBook(ServerPlayer player) {
        var enchantments = player.serverLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        // Curated useful enchantments: always store a real enchantment on the book.
        return switch (player.getRandom().nextInt(100)) {
            case 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                    15, 16, 17, 18, 19, 20, 21, 22, 23, 24 -> enchantedBook(enchantments, Enchantments.LUCK_OF_THE_SEA, 3);
            case 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
                    40, 41, 42, 43, 44, 45, 46, 47, 48, 49 -> enchantedBook(enchantments, Enchantments.LURE, 3);
            case 50, 51, 52, 53, 54, 55, 56, 57, 58, 59 -> enchantedBook(enchantments, Enchantments.MENDING, 1);
            case 60, 61, 62, 63, 64, 65, 66, 67, 68, 69 -> enchantedBook(enchantments, Enchantments.UNBREAKING, 3);
            case 70, 71, 72, 73, 74 -> enchantedBook(enchantments, Enchantments.EFFICIENCY, 5);
            case 75, 76, 77, 78, 79 -> enchantedBook(enchantments, Enchantments.FORTUNE, 3);
            case 80, 81, 82, 83, 84 -> enchantedBook(enchantments, Enchantments.SILK_TOUCH, 1);
            case 85, 86, 87, 88, 89 -> enchantedBook(enchantments, Enchantments.LOOTING, 3);
            case 90, 91, 92, 93, 94 -> enchantedBook(enchantments, Enchantments.SHARPNESS, 5);
            case 95, 96, 97 -> enchantedBook(enchantments, Enchantments.PROTECTION, 4);
            default -> enchantedBook(enchantments, Enchantments.FEATHER_FALLING, 4);
        };
    }

    private static ItemStack enchantedBook(
            net.minecraft.core.HolderLookup.RegistryLookup<net.minecraft.world.item.enchantment.Enchantment> enchantments,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment,
            int level) {
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(
                enchantments.getOrThrow(enchantment), level));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        processPendingReplants(player);
        if (getJob(player) == JobType.MINER && player.getMainHandItem().is(ItemTags.PICKAXES)) {
            int minerLevel = getJobLevel(player, JobType.MINER);
            if (minerLevel >= 10) {
                int amplifier = minerLevel >= 60 ? 1 : 0;
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DIG_SPEED, 40, amplifier, true, false, false));
            }
        }
        if (getJob(player) != JobType.FISHER || player.fishing == null) return;

        FishingHook hook = player.fishing;
        int actualLevel = getJobLevel(player, JobType.FISHER);
        int fisherLevel;
        if (actualLevel <= 30) {
            fisherLevel = actualLevel * 40 / 30;
        } else if (actualLevel <= 60) {
            fisherLevel = 40 + (actualLevel - 30) * 40 / 30;
        } else {
            fisherLevel = 80 + (actualLevel - 60);
        }
        if (fisherLevel <= 0) {
            return;
        }

        int remainder = hook.getPersistentData().getInt(FISHING_SPEED_REMAINDER) + fisherLevel;
        int extraTicks = remainder / 200;
        hook.getPersistentData().putInt(FISHING_SPEED_REMAINDER, remainder % 200);
        if (extraTicks > 0) {
            hook.timeUntilLured = Math.max(0, hook.timeUntilLured - extraTicks);
            // Aquaculture uses its own bobber/retrieve implementation. Leave its bite
            // window timer alone so the visible bite and the server-side catch window stay in sync.
            boolean aquacultureBobber = hook.getType().builtInRegistryHolder().key()
                    .location().getNamespace().equals("aquaculture");
            if (!aquacultureBobber) {
                hook.timeUntilHooked = Math.max(0, hook.timeUntilHooked - extraTicks);
            }
        }
    }

    /*
     * =========================
     * 공용 전투 XP
     * =========================
     */

    @SubscribeEvent
    public static void onMonsterKilled(
            LivingDeathEvent event
    ) {

        if (!(event.getEntity() instanceof LivingEntity victim)
                || !isCombatTarget(victim)
                || victim.getPersistentData().getBoolean(TICKET_BOSS_MARKER)
                || victim.getType() == net.minecraft.world.entity.EntityType.WITHER
                || victim.getType() == net.minecraft.world.entity.EntityType.ENDER_DRAGON) {
            return;
        }

        if (!(event.getSource().getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        int gainedXp = getCombatXpForMob(victim);
        if (gainedXp <= 0) return;

        int oldXp = getCombatXp(player);
        int oldLevel = getCombatLevel(player);
        int newXp = Math.min(getCombatXpForMaxLevel(), oldXp + gainedXp);
        int actualGainedXp = newXp - oldXp;
        player.getPersistentData().putInt(COMBAT_XP, newXp);
        int newLevel = getCombatLevelFromXp(newXp);
        JobNetwork.sendCombatProgress(player, newXp, actualGainedXp);

        if (newLevel > oldLevel) {
            applyCombatHealthBonus(player);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "⚔ 전투 레벨 상승! Lv." + newLevel
                            + " · 공격력 +" + getCombatAttackBonusPercent(newLevel) + "%"
                            + " · 피해 감소 " + getCombatDamageReductionPercent(newLevel) + "%"
                            + " · 최대 체력 +" + (newLevel / 20) + "하트"));
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "⚔ 전투 XP +" + actualGainedXp + " | 전투 Lv." + newLevel));
    }

    private static int getCombatXpForMob(LivingEntity mob) {
        String id = mob.getType().builtInRegistryHolder().key().location().toString();
        return switch (id) {
            case "minecraft:silverfish", "minecraft:endermite" -> 5;
            case "minecraft:zombie", "minecraft:skeleton", "minecraft:spider",
                    "minecraft:creeper", "minecraft:drowned", "minecraft:husk",
                    "minecraft:stray", "minecraft:cave_spider", "minecraft:slime",
                    "minecraft:phantom", "minecraft:pillager" -> 10;
            case "minecraft:witch", "minecraft:blaze", "minecraft:ghast",
                    "minecraft:guardian", "minecraft:piglin", "minecraft:zombified_piglin",
                    "minecraft:hoglin", "minecraft:zoglin" -> 15;
            case "minecraft:enderman", "minecraft:ravager", "minecraft:evoker",
                    "minecraft:piglin_brute", "minecraft:wither_skeleton",
                    "minecraft:shulker", "minecraft:elder_guardian" -> 25;
            default -> mob.getType().getCategory() == MobCategory.MONSTER ? 10 : 0;
        };
    }

    private static int getCombatAttackBonusPercent(int level) {
        return Math.min(10, Math.max(0, level / 10)) * 3;
    }

    private static int getCombatDamageReductionPercent(int level) {
        return getCombatAttackBonusPercent(level);
    }

    private static int getCombatXpForLevel(int level) {
        int boundedLevel = Math.max(1, Math.min(MAX_LEVEL, level));
        return (int) Math.round(getXpForLevel(boundedLevel) * 0.70D);
    }

    private static int getCombatXpForMaxLevel() {
        return getCombatXpForLevel(MAX_LEVEL);
    }

    public static int getCombatLevelFromXp(int totalXp) {
        int xp = Math.max(0, totalXp);
        int level = 1;
        while (level < MAX_LEVEL && xp >= getCombatXpForLevel(level + 1)) {
            level++;
        }
        return level;
    }

    public static int getCurrentCombatLevelXp(int totalXp) {
        return Math.max(0, totalXp - getCombatXpForLevel(getCombatLevelFromXp(totalXp)));
    }

    public static int getCurrentCombatLevelMaxXp(int totalXp) {
        int level = getCombatLevelFromXp(totalXp);
        return level >= MAX_LEVEL
                ? 0
                : getCombatXpForLevel(level + 1) - getCombatXpForLevel(level);
    }

    @SubscribeEvent
    public static void onCombatDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        var attacker = event.getSource().getEntity();

        if (attacker instanceof ServerPlayer player && isCombatTarget(victim)) {
            int bonus = getCombatAttackBonusPercent(getCombatLevel(player));
            if (bonus > 0) event.setNewDamage(event.getNewDamage() * (1.0F + bonus / 100.0F));
        }

        if (victim instanceof ServerPlayer player && attacker instanceof LivingEntity source
                && isCombatTarget(source)) {
            int reduction = getCombatDamageReductionPercent(getCombatLevel(player));
            if (reduction > 0) event.setNewDamage(event.getNewDamage() * (1.0F - reduction / 100.0F));
        }
    }

    /*
     * =========================
     * 레벨 계산
     * =========================
     */

    public static int getXpRequiredForLevel(
            int level
    ) {

        return Math.max(1, 100 + ((level - 1) * 20));
    }

    private static int getXpForMaxLevel() {
        return getXpForLevel(MAX_LEVEL);
    }

    public static int getXpForLevel(int requestedLevel) {
        int level = Math.max(1, Math.min(MAX_LEVEL, requestedLevel));
        int total = 0;
        for (int current = 1; current < level; current++) {
            total += getXpRequiredForLevel(current);
        }
        return total;
    }

    public static int getLevelFromXp(
            int totalXp
    ) {

        int level = 1;

        int remainingXp = Math.max(0, totalXp);

        while (level < MAX_LEVEL &&
                remainingXp
                        >= getXpRequiredForLevel(level)
        ) {

            remainingXp -=
                    getXpRequiredForLevel(level);

            level++;
        }

        return Math.min(MAX_LEVEL, level);
    }

    public static int getCurrentLevelXp(
            int totalXp
    ) {

        int level =
                getLevelFromXp(totalXp);

        int xpBeforeLevel = 0;

        for (int i = 1; i < level; i++) {

            xpBeforeLevel +=
                    getXpRequiredForLevel(i);
        }

        return
                totalXp
                        - xpBeforeLevel;
    }

    public static int getCurrentLevelMaxXp(
            int totalXp
    ) {

        int level =
                getLevelFromXp(totalXp);

        return level >= MAX_LEVEL ? 0 : getXpRequiredForLevel(level);
    }

    /*
     * =========================
     * 광부 해금
     * =========================
     */

    public static boolean hasMinerUnlock(
            Player player,
            int requiredLevel
    ) {

        int xp =
                player
                        .getPersistentData()
                        .getInt(MINER_XP);

        int level =
                getLevelFromXp(xp);

        return level >= requiredLevel;
    }

    public static boolean hasMinerLevel5(
            Player player
    ) {

        return hasMinerUnlock(
                player,
                5
        );
    }

    public static boolean hasMinerLevel10(
            Player player
    ) {

        return hasMinerUnlock(
                player,
                10
        );
    }

    public static boolean hasMinerLevel20(
            Player player
    ) {

        return hasMinerUnlock(
                player,
                20
        );
    }

    public static boolean hasMinerLevel30(
            Player player
    ) {

        return hasMinerUnlock(
                player,
                30
        );
    }
}

