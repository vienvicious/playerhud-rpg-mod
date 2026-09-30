package com.playerhud;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PlayerHudMod.MOD_ID)
public final class RidingCommand {
    private RidingCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("priding")
                .then(Commands.literal("give").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("item", StringArgumentType.word())
                                        .executes(context -> give(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                StringArgumentType.getString(context, "item"), 1))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> give(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        StringArgumentType.getString(context, "item"),
                                                        IntegerArgumentType.getInteger(context, "count"))))))));
    }

    private static int give(net.minecraft.commands.CommandSourceStack source, ServerPlayer player,
                            String id, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ResourceLocation key = ResourceLocation.tryParse(PlayerHudMod.MOD_ID + ":" + id);
        Item item = key == null ? null : BuiltInRegistries.ITEM.get(key);
        if (item == null || !BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(PlayerHudMod.MOD_ID)) {
            source.sendFailure(Component.literal("알 수 없는 플레이어HUD 아이템입니다: " + id));
            return 0;
        }
        int remaining = count;
        int maxStack = Math.max(1, item.getDefaultMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(item, amount);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            remaining -= amount;
        }
        source.sendSuccess(() -> Component.literal(player.getName().getString() + "에게 " + id + " " + count + "개 지급"), true);
        return count;
    }
}
