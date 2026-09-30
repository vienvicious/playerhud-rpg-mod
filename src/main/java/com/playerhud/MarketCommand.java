package com.playerhud;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;

@EventBusSubscriber(modid = "playerhud")
public final class MarketCommand {

    private static final int MAX_SELL_COUNT = 2304;

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pmarket")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    JobType job = JobSystem.getJob(player);
                    player.sendSystemMessage(Component.literal(
                            "거래소: /pmarket list <miner|farmer|fisher> | /pmarket sell <아이템ID> [수량]"
                                    + " | 현재 직업: " + (job == null ? "없음" : job.getDisplayName())
                                    + " | 보유금액: " + EconomySystem.getMoney(player) + "원"));
                    return 1;
                })
                .then(Commands.literal("list")
                        .then(Commands.argument("job", StringArgumentType.word())
                                .executes(context -> listPrices(
                                        context.getSource().getPlayerOrException(),
                                        StringArgumentType.getString(context, "job")))))
                .then(Commands.literal("sell")
                        .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                .executes(context -> sell(
                                        context.getSource().getPlayerOrException(),
                                        ResourceLocationArgument.getId(context, "item_id").toString(), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_SELL_COUNT))
                                        .executes(context -> sell(
                                                context.getSource().getPlayerOrException(),
                                                ResourceLocationArgument.getId(context, "item_id").toString(),
                                                IntegerArgumentType.getInteger(context, "count")))))));
    }

    private static int listPrices(ServerPlayer player, String value) {
        JobType job = switch (value.toLowerCase()) {
            case "miner", "광부" -> JobType.MINER;
            case "farmer", "농부" -> JobType.FARMER;
            case "fisher", "어부" -> JobType.FISHER;
            default -> null;
        };
        if (job == null) {
            player.sendSystemMessage(Component.literal("직업은 miner, farmer, fisher 중 하나를 입력하세요."));
            return 0;
        }

        StringBuilder message = new StringBuilder("[" + job.getDisplayName() + " 거래 목록] (개당 기본가)\n");
        for (TradeSystem.TradeEntry entry : TradeSystem.getAvailableEntries(job)) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.itemId()));
            message.append(new ItemStack(item).getHoverName().getString())
                    .append(" (/").append(entry.itemId()).append("): ")
                    .append(entry.basePrice()).append("원\n");
        }
        message.append("해당 직업 레벨 20부터 판매가 +5%, 80부터 추가 +10%가 적용됩니다.");
        player.sendSystemMessage(Component.literal(message.toString()));
        return 1;
    }

    private static int sell(ServerPlayer player, String rawItemId, int count) {
        ResourceLocation itemId = ResourceLocation.tryParse(rawItemId);
        if (itemId == null) {
            player.sendSystemMessage(Component.literal("아이템 ID 형식이 올바르지 않습니다. /pmarket list <직업>에서 확인하세요."));
            return 0;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(Items.AIR);
        TradeSystem.TradeEntry entry = TradeSystem.getEntry(item);
        if (item == Items.AIR || entry == null) {
            player.sendSystemMessage(Component.literal("이 아이템은 거래소에서 매입하지 않습니다."));
            return 0;
        }

        int available = 0;
        int inventorySlots = Math.min(36, player.getInventory().getContainerSize());
        for (int slot = 0; slot < inventorySlots; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) available += stack.getCount();
        }
        if (available < count) {
            player.sendSystemMessage(Component.literal(
                    "수량이 부족합니다. 보유: " + available + "개 / 요청: " + count + "개"));
            return 0;
        }

        long totalPrice = TradeSystem.getSaleValue(player, new ItemStack(item), count);
        int remaining = count;
        for (int slot = 0; slot < inventorySlots && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int removed = Math.min(stack.getCount(), remaining);
            stack.shrink(removed);
            remaining -= removed;
        }

        EconomySystem.addMoney(player, totalPrice);
        JobNetwork.sendMoney(player, EconomySystem.getMoney(player));
        player.sendSystemMessage(Component.literal("판매 완료: " + new ItemStack(item).getHoverName().getString()
                + " " + count + "개 × 기본가 " + entry.basePrice() + "원 = " + totalPrice + "원"));
        return 1;
    }

    private MarketCommand() {}
}

