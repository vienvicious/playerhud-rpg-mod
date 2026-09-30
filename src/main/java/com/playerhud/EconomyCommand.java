package com.playerhud;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "playerhud")
public class EconomyCommand {

    @SubscribeEvent
    public static void registerCommands(
            RegisterCommandsEvent event
    ) {

        event.getDispatcher().register(
                Commands.literal("pmoney")

                        .requires(
                                source -> source.hasPermission(2)
                        )

                        /*
                         * /pmoney
                         */

                        .executes(context -> {

                            ServerPlayer player =
                                    context.getSource()
                                            .getPlayerOrException();

                            long money =
                                    EconomySystem.getMoney(
                                            player
                                    );

                            player.sendSystemMessage(
                                    Component.literal(
                                            "현재 보유금액: "
                                                    + money
                                                    + "원"
                                    )
                            );

                            JobNetwork.sendMoney(
                                    player,
                                    money
                            );

                            return 1;
                        })

                        /*
                         * /pmoney add <금액>
                         */

                        .then(
                                Commands.literal("add")
                                        .then(
                                                Commands.argument(
                                                        "amount",
                                                        LongArgumentType.longArg(1)
                                                )
                                                .executes(context -> {

                                                    ServerPlayer player =
                                                            context.getSource()
                                                                    .getPlayerOrException();

                                                    long amount =
                                                            LongArgumentType.getLong(
                                                                    context,
                                                                    "amount"
                                                            );

                                                    EconomySystem.addMoney(
                                                            player,
                                                            amount
                                                    );

                                                    long money =
                                                            EconomySystem.getMoney(
                                                                    player
                                                            );

                                                    JobNetwork.sendMoney(
                                                            player,
                                                            money
                                                    );

                                                    player.sendSystemMessage(
                                                            Component.literal(
                                                                    "+"
                                                                            + amount
                                                                            + "원 지급됨"
                                                                            + " | 현재 "
                                                                            + money
                                                                            + "원"
                                                            )
                                                    );

                                                    return 1;
                                                })
                                        )
                        )

                        /*
                         * /pmoney remove <금액>
                         */

                        .then(
                                Commands.literal("remove")
                                        .then(
                                                Commands.argument(
                                                        "amount",
                                                        LongArgumentType.longArg(1)
                                                )
                                                .executes(context -> {

                                                    ServerPlayer player =
                                                            context.getSource()
                                                                    .getPlayerOrException();

                                                    long amount =
                                                            LongArgumentType.getLong(
                                                                    context,
                                                                    "amount"
                                                            );

                                                    boolean success =
                                                            EconomySystem.removeMoney(
                                                                    player,
                                                                    amount
                                                            );

                                                    if (!success) {

                                                        player.sendSystemMessage(
                                                                Component.literal(
                                                                        "돈이 부족합니다."
                                                                )
                                                        );

                                                        return 0;
                                                    }

                                                    long money =
                                                            EconomySystem.getMoney(
                                                                    player
                                                            );

                                                    JobNetwork.sendMoney(
                                                            player,
                                                            money
                                                    );

                                                    player.sendSystemMessage(
                                                            Component.literal(
                                                                    "-"
                                                                            + amount
                                                                            + "원 차감됨"
                                                                            + " | 현재 "
                                                                            + money
                                                                            + "원"
                                                            )
                                                    );

                                                    return 1;
                                                })
                                        )
                        )

                        /*
                         * /pmoney set <금액>
                         */

                        .then(
                                Commands.literal("set")
                                        .then(
                                                Commands.argument(
                                                        "amount",
                                                        LongArgumentType.longArg(0)
                                                )
                                                .executes(context -> {

                                                    ServerPlayer player =
                                                            context.getSource()
                                                                    .getPlayerOrException();

                                                    long amount =
                                                            LongArgumentType.getLong(
                                                                    context,
                                                                    "amount"
                                                            );

                                                    EconomySystem.setMoney(
                                                            player,
                                                            amount
                                                    );

                                                    JobNetwork.sendMoney(
                                                            player,
                                                            amount
                                                    );

                                                    player.sendSystemMessage(
                                                            Component.literal(
                                                                    "보유금액이 "
                                                                            + amount
                                                                            + "원으로 설정되었습니다."
                                                            )
                                                    );

                                                    return 1;
                                                })
                                        )
                        )
        );
    }
}