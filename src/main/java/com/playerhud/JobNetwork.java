package com.playerhud;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(
        modid = "playerhud",
        bus = EventBusSubscriber.Bus.MOD
)
public class JobNetwork {

    @SubscribeEvent
    public static void register(
            RegisterPayloadHandlersEvent event
    ) {

        PayloadRegistrar registrar =
                event.registrar("1");

        /*
         * 광부 XP
         */

        registrar.playToClient(
                MinerXpPayload.TYPE,
                MinerXpPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        HudLayer.setMinerXp(
                                payload.xp()
                        );

                    });
                }
        );

        registrar.playToClient(
                JobProgressPayload.TYPE,
                JobProgressPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        HudLayer.setJobProgress(
                                payload.jobId(),
                                payload.xp(),
                                payload.gainedXp()
                        )
                )
        );

        registrar.playToClient(
                CombatProgressPayload.TYPE,
                CombatProgressPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        HudLayer.setCombatProgress(payload.xp(), payload.gainedXp()))
        );

        registrar.playToServer(
                EnhancementAttemptPayload.TYPE,
                EnhancementAttemptPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player
                            && player.containerMenu instanceof EnhancementMenu menu) {
                        menu.attemptEnhancement(player);
                    }
                })
        );

        /*
         * 돈
         */

        registrar.playToClient(
                MoneyPayload.TYPE,
                MoneyPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        HudLayer.setMoney(
                                payload.money()
                        );

                    });
                }
        );

        registrar.playToClient(
                BossOutcomePayload.TYPE,
                BossOutcomePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        HudLayer.showBossOutcome(payload.outcome()))
        );

        registrar.playToClient(
                RidingResultPayload.TYPE,
                RidingResultPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        HudLayer.showRidingResult(payload.itemId(), payload.rarity()))
        );

        /*
         * 직업 선택창 열기
         */

        registrar.playToClient(
                OpenJobSelectionPayload.TYPE,
                OpenJobSelectionPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        if (payload.open()) {

                            net.minecraft.client.Minecraft
                                    .getInstance()
                                    .setScreen(
                                            new JobSelectionScreen()
                                    );
                        }
                    });
                }
        );

        /*
         * 직업 선택 → 서버
         */

        registrar.playToServer(
                JobSelectionPayload.TYPE,
                JobSelectionPayload.STREAM_CODEC,
                (payload, context) -> {

                    context.enqueueWork(() -> {

                        if (!(context.player()
                                instanceof ServerPlayer player)) {
                            return;
                        }

                        JobSystem.setJob(
                                player,
                                payload.jobId()
                        );
                    });
                }
        );
    }

    public static void sendMinerXp(
            ServerPlayer player,
            int xp
    ) {

        PacketDistributor.sendToPlayer(
                player,
                new MinerXpPayload(xp)
        );
    }

    public static void sendJobProgress(ServerPlayer player, int jobId, int xp, int gainedXp) {
        PacketDistributor.sendToPlayer(
                player,
                new JobProgressPayload(jobId, xp, gainedXp)
        );
    }

    public static void sendCombatProgress(ServerPlayer player, int xp, int gainedXp) {
        PacketDistributor.sendToPlayer(player, new CombatProgressPayload(xp, gainedXp));
    }

    public static void sendMoney(
            ServerPlayer player,
            long money
    ) {

        PacketDistributor.sendToPlayer(
                player,
                new MoneyPayload(money)
        );
    }

    public static void sendBossOutcome(ServerPlayer player, int outcome) {
        PacketDistributor.sendToPlayer(player, new BossOutcomePayload(outcome));
    }

    public static void sendRidingResult(ServerPlayer player, String itemId, int rarity) {
        PacketDistributor.sendToPlayer(player, new RidingResultPayload(itemId, rarity));
    }

    public static void openJobSelection(
            ServerPlayer player
    ) {

        PacketDistributor.sendToPlayer(
                player,
                new OpenJobSelectionPayload(true)
        );
    }
}
