package com.playerhud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class HudLayer {

    private static final int WIDTH = 190;
    private static final int HEIGHT = 100;
    private static final int FACE_SCALE = 4;
    private static final int MARGIN = 10;

    private static int activeJobId = 0;
    private static int activeJobXp = 0;
    private static int lastGainedXp = 0;
    private static int activeCombatXp = 0;
    private static int lastCombatGainedXp = 0;
    private static boolean combatProgressActive = false;

    private static long money = 0L;
    private static int bossOutcome = 0;
    private static long bossOutcomeVisibleUntil = 0L;
    private static final long BOSS_OUTCOME_DISPLAY_TIME = 5000L;
    private static String ridingResultItemId = "";
    private static int ridingResultRarity = 0;
    private static long ridingResultVisibleUntil = 0L;

    private static final long JOB_BAR_DISPLAY_TIME = 3000L;
    private static final long JOB_BAR_FADE_TIME = 800L;

    private static final int VANILLA_XP_BAR_WIDTH = 182;
    private static final int VANILLA_XP_BAR_HEIGHT = 5;
    private static final ResourceLocation XP_BAR_BACKGROUND =
            ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation XP_BAR_PROGRESS =
            ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");

    private static long jobBarVisibleUntil = 0L;

    /*
     * =========================
     * 광부 XP 설정
     * =========================
     */

    public static void setMinerXp(int xp) {
        setJobProgress(0, xp, Math.max(0, xp - activeJobXp));
    }

    public static void setJobProgress(int jobId, int xp, int gainedXp) {
        if (jobId < 0 || jobId >= JobType.values().length) {
            return;
        }

        activeJobId = jobId;
        combatProgressActive = false;
        activeJobXp = Math.max(0, xp);
        lastGainedXp = Math.max(0, gainedXp);

        if (gainedXp > 0) {
            jobBarVisibleUntil = System.currentTimeMillis() + JOB_BAR_DISPLAY_TIME;
        }
    }

    public static void setCombatProgress(int xp, int gainedXp) {
        activeCombatXp = Math.max(0, xp);
        lastCombatGainedXp = Math.max(0, gainedXp);
        combatProgressActive = true;
        if (gainedXp > 0) {
            jobBarVisibleUntil = System.currentTimeMillis() + JOB_BAR_DISPLAY_TIME;
        }
    }

    /*
     * =========================
     * 돈 설정
     * =========================
     */

    public static void setMoney(
            long newMoney
    ) {

        money = newMoney;
    }

    public static void showBossOutcome(int outcome) {
        bossOutcome = outcome == 1 || outcome == 2 ? outcome : 0;
        bossOutcomeVisibleUntil = bossOutcome == 0
                ? 0L : System.currentTimeMillis() + BOSS_OUTCOME_DISPLAY_TIME;
    }

    public static void showRidingResult(String itemId, int rarity) {
        ridingResultItemId = itemId;
        ridingResultRarity = Math.max(0, Math.min(2, rarity));
        ridingResultVisibleUntil = System.currentTimeMillis() + BOSS_OUTCOME_DISPLAY_TIME;
    }

    /*
     * =========================
     * HUD 렌더링
     * =========================
     */

    public static void render(
            GuiGraphics guiGraphics,
            DeltaTracker deltaTracker
    ) {

        Minecraft mc =
                Minecraft.getInstance();

        if (mc.player == null) {
            return;
        }

        int screenWidth =
                guiGraphics.guiWidth();

        /*
         * =========================
         * 우측 상단 HUD 위치
         * =========================
         */

        int x =
                screenWidth
                        - WIDTH
                        - MARGIN;

        int y =
                MARGIN;

        /*
         * =========================
         * HUD 배경
         * =========================
         */

        guiGraphics.fill(
                x,
                y,
                x + WIDTH,
                y + HEIGHT,
                0xCC111111
        );

        int borderColor =
                0xFF555555;

        guiGraphics.fill(
                x,
                y,
                x + WIDTH,
                y + 1,
                borderColor
        );

        guiGraphics.fill(
                x,
                y + HEIGHT - 1,
                x + WIDTH,
                y + HEIGHT,
                borderColor
        );

        guiGraphics.fill(
                x,
                y,
                x + 1,
                y + HEIGHT,
                borderColor
        );

        guiGraphics.fill(
                x + WIDTH - 1,
                y,
                x + WIDTH,
                y + HEIGHT,
                borderColor
        );

        /*
         * =========================
         * 플레이어 얼굴
         * =========================
         */

        ResourceLocation skinTexture =
                mc.player.getSkin().texture();

        int faceX =
                x + 8;

        int faceY =
                y + 8;

        guiGraphics.pose().pushPose();

        guiGraphics.pose().translate(
                (double) faceX,
                (double) faceY,
                0.0
        );

        guiGraphics.pose().scale(
                FACE_SCALE,
                FACE_SCALE,
                1.0F
        );

        guiGraphics.blit(
                skinTexture,
                0,
                0,
                8,
                8,
                8,
                8,
                64,
                64
        );

        guiGraphics.blit(
                skinTexture,
                0,
                0,
                40,
                8,
                8,
                8,
                64,
                64
        );

        guiGraphics.pose().popPose();

        /*
         * =========================
         * 이름
         * =========================
         */

        int textX =
                x + 48;

        guiGraphics.drawString(
                mc.font,
                mc.player.getName().getString(),
                textX,
                y + 10,
                0xFFFFFFFF
        );

        /*
         * =========================
         * 온라인 인원
         * =========================
         */

        int online =
                mc.getConnection() != null
                        ? mc.getConnection()
                            .getOnlinePlayers()
                            .size()
                        : 1;

        guiGraphics.drawString(
                mc.font,
                "● ONLINE " + online + "명",
                textX,
                y + 29,
                0xFF55FF55
        );

        /*
         * =========================
         * 광부 레벨
         * =========================
         */

        JobType activeJob = JobType.values()[activeJobId];
        int activeJobLevel =
                JobSystem.getLevelFromXp(
                        activeJobXp
                );

        String activeJobText = getJobIcon(activeJob) + " " + activeJob.getDisplayName()
                + " Lv." + activeJobLevel
                + (activeJobLevel >= 100 ? " · " + activeJob.getMasterTitle() : "");
        guiGraphics.drawString(
                mc.font,
                activeJobText,
                textX,
                y + 47,
                0xFFFFD966
        );

        int combatLevel = JobSystem.getCombatLevelFromXp(activeCombatXp);
        guiGraphics.drawString(
                mc.font,
                "⚔ 전투 Lv." + combatLevel,
                textX,
                y + 65,
                0xFFFF8066
        );

        /*
         * =========================
         * 보유 금액
         * =========================
         */

        String moneyText =
                "💰 "
                        + String.format(
                                java.util.Locale.US,
                                "%,d",
                                money
                        )
                        + "원";

        guiGraphics.drawString(
                mc.font,
                moneyText,
                textX,
                y + 83,
                0xFFFFD966
        );

        /*
         * =========================
         * 광부 XP 팝업
         * =========================
         */

        long now =
                System.currentTimeMillis();

        if (now < jobBarVisibleUntil) {

            long remaining =
                    jobBarVisibleUntil - now;

            float alpha = 1.0F;

            if (remaining < JOB_BAR_FADE_TIME) {

                alpha =
                        remaining
                                / (float) JOB_BAR_FADE_TIME;
            }

            if (alpha < 0.0F) {
                alpha = 0.0F;
            }

            if (alpha > 1.0F) {
                alpha = 1.0F;
            }

            int totalXp = combatProgressActive ? activeCombatXp : activeJobXp;
            int gainedXp = combatProgressActive ? lastCombatGainedXp : lastGainedXp;
            int requiredXp = combatProgressActive
                    ? JobSystem.getCurrentCombatLevelMaxXp(totalXp)
                    : JobSystem.getCurrentLevelMaxXp(totalXp);
            int currentXp = combatProgressActive
                    ? JobSystem.getCurrentCombatLevelXp(totalXp)
                    : JobSystem.getCurrentLevelXp(totalXp);
            float progress = requiredXp <= 0 ? 1.0F : currentXp / (float) requiredXp;

            if (progress < 0.0F) {
                progress = 0.0F;
            }

            if (progress > 1.0F) {
                progress = 1.0F;
            }

            // Keep the text first, with the vanilla-style bar directly below it.
            int barX = (screenWidth - VANILLA_XP_BAR_WIDTH) / 2;
            int barY = 18;

            guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
            guiGraphics.blitSprite(XP_BAR_BACKGROUND, barX, barY,
                    VANILLA_XP_BAR_WIDTH, VANILLA_XP_BAR_HEIGHT);
            int filledWidth = Math.min(VANILLA_XP_BAR_WIDTH, Math.round(183.0F * progress));
            if (filledWidth > 0) {
                guiGraphics.blitSprite(XP_BAR_PROGRESS, 182, 5, 0, 0,
                        barX, barY, filledWidth, VANILLA_XP_BAR_HEIGHT);
            }
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            String jobLabel = combatProgressActive
                    ? String.format(java.util.Locale.ROOT,
                            "전투 Lv %02d (%04d/%04d) +%03d",
                            JobSystem.getCombatLevelFromXp(totalXp), currentXp, requiredXp, gainedXp)
                    : String.format(java.util.Locale.ROOT,
                            "%s Lv %02d (%04d/%04d) +%03d",
                            activeJob.getDisplayName(), activeJobLevel, currentXp, requiredXp, gainedXp);
            int labelY = 5;
            guiGraphics.drawCenteredString(mc.font, jobLabel,
                    screenWidth / 2, labelY, withAlpha(0xFF80FF40, alpha));
        }

        if (now < bossOutcomeVisibleUntil && bossOutcome != 0) {
            int centerX = screenWidth / 2;
            int centerY = guiGraphics.guiHeight() / 2;
            int color = bossOutcome == 1 ? 0xFF55FF55 : 0xFFFF5555;
            String result = bossOutcome == 1 ? "Clear" : "Defeat";
            float alpha = Math.min(1.0F, (bossOutcomeVisibleUntil - now) / 500.0F);

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY - 16, 0.0F);
            guiGraphics.pose().scale(3.0F, 3.0F, 1.0F);
            guiGraphics.drawCenteredString(mc.font, result, 0, 0, withAlpha(color, alpha));
            guiGraphics.pose().popPose();

            guiGraphics.drawCenteredString(mc.font, "5초 후 원래 위치로 돌아갑니다",
                    centerX, centerY + 15, withAlpha(0xFFFFFFFF, alpha));
        }

        if (now < ridingResultVisibleUntil && !ridingResultItemId.isEmpty()) {
            int centerX = screenWidth / 2;
            int centerY = guiGraphics.guiHeight() / 2;
            int color = switch (ridingResultRarity) {
                case 1 -> 0xFF55AAFF;
                case 2 -> 0xFFCC55FF;
                default -> 0xFFFFFFFF;
            };
            float alpha = Math.min(1.0F, (ridingResultVisibleUntil - now) / 500.0F);
            String itemName = net.minecraft.network.chat.Component
                    .translatable("item.playerhud." + ridingResultItemId).getString();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX, centerY - 23, 0.0F);
            guiGraphics.pose().scale(2.5F, 2.5F, 1.0F);
            guiGraphics.drawCenteredString(mc.font, "획득!", 0, 0, withAlpha(color, alpha));
            guiGraphics.pose().popPose();
            guiGraphics.drawCenteredString(mc.font, itemName,
                    centerX, centerY + 12, withAlpha(color, alpha));
        }
    }

    private static String getJobIcon(JobType job) {
        return switch (job) {
            case MINER -> "⛏";
            case FARMER -> "🌾";
            case FISHER -> "🎣";
        };
    }

    private static int withAlpha(int color, float alpha) {
        int originalAlpha = (color >>> 24) & 0xFF;
        int finalAlpha = Math.round(originalAlpha * alpha);
        return (finalAlpha << 24) | (color & 0x00FFFFFF);
    }

}
