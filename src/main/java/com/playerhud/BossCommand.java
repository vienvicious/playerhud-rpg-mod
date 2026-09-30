package com.playerhud;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = PlayerHudMod.MOD_ID)
public final class BossCommand {
    private BossCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("pboss")
                .then(Commands.literal("ticket")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> giveTicket(EntityArgument.getPlayer(context, "player"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> giveTicket(EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "count"))))))
                .then(Commands.literal("end")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            boolean ended = BossArenaManager.endEncounter(context.getSource().getServer());
                            context.getSource().sendSuccess(() -> Component.literal(
                                    ended ? "보스전을 종료하고 참가자를 귀환시켰습니다." : "진행 중인 보스전이 없습니다."), true);
                            return ended ? 1 : 0;
                        })));
    }

    private static int giveTicket(ServerPlayer player, int count) {
        ItemStack stack = new ItemStack(ModItems.BOSS_TICKET.get(), count);
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        player.sendSystemMessage(Component.literal("보스전 입장권 " + count + "개를 받았습니다."));
        return count;
    }
}
