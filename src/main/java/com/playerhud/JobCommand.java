package com.playerhud;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = "playerhud")
public class JobCommand {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("pjob")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                            JobSystem.resetJob(target);
                                            target.sendSystemMessage(Component.literal(
                                                    "관리자가 직업 선택을 초기화했습니다. 새 직업을 선택해주세요."
                                            ));
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            target.getGameProfile().getName()
                                                                    + "님의 직업 선택을 초기화했습니다."
                                                    ),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("level")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("job", StringArgumentType.word())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        new String[]{"miner", "farmer", "fisher", "광부", "농부", "어부"}, builder))
                                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                                        .executes(context -> {
                                                            ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                            String value = StringArgumentType.getString(context, "job");
                                                            JobType job = parseJob(value);
                                                            if (job == null) {
                                                                context.getSource().sendFailure(Component.literal(
                                                                        "직업은 miner, farmer, fisher 중 하나여야 합니다."));
                                                                return 0;
                                                            }
                                                            int level = IntegerArgumentType.getInteger(context, "level");
                                                            JobSystem.setJobLevel(target, job, level);
                                                            target.sendSystemMessage(Component.literal(
                                                                    "관리자가 직업을 " + job.getDisplayName()
                                                                            + " Lv." + level + "로 설정했습니다."));
                                                            context.getSource().sendSuccess(() -> Component.literal(
                                                                    target.getGameProfile().getName() + "님의 "
                                                                            + job.getDisplayName() + " 레벨을 "
                                                                            + level + "로 설정했습니다."), true);
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("combat")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 100))
                                                .executes(context -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                                    int level = IntegerArgumentType.getInteger(context, "level");
                                                    JobSystem.setCombatLevel(target, level);
                                                    target.sendSystemMessage(Component.literal(
                                                            "관리자가 전투 레벨을 Lv." + level + "로 설정했습니다."));
                                                    context.getSource().sendSuccess(() -> Component.literal(
                                                            target.getGameProfile().getName()
                                                                    + "님의 전투 레벨을 Lv." + level
                                                                    + "로 설정했습니다."), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }

    private static JobType parseJob(String value) {
        return switch (value.toLowerCase(java.util.Locale.ROOT)) {
            case "miner", "광부" -> JobType.MINER;
            case "farmer", "농부" -> JobType.FARMER;
            case "fisher", "어부" -> JobType.FISHER;
            default -> null;
        };
    }
}
