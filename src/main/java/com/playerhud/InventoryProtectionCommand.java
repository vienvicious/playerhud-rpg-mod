package com.playerhud;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PlayerHudMod.MOD_ID)
public final class InventoryProtectionCommand {
    private InventoryProtectionCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pinvprotection")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> giveProtection(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "player"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> giveProtection(
                                                context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "count")))))));
    }

    private static int giveProtection(net.minecraft.commands.CommandSourceStack source,
                                      ServerPlayer player, int count) {
        ItemStack stack = new ItemStack(ModItems.INVENTORY_PROTECTION_TICKET.get(), count);
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false);
        }
        player.getInventory().setChanged();
        player.sendSystemMessage(Component.literal("인벤토리 보호권 " + count + "개를 받았습니다."));
        source.sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + "에게 인벤토리 보호권 " + count + "개를 지급했습니다."), true);
        return count;
    }
}
