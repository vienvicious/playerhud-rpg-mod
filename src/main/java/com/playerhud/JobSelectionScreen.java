package com.playerhud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public class JobSelectionScreen extends Screen {

    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_HEIGHT = 440;

    private static final int CARD_WIDTH = 210;
    private static final int CARD_HEIGHT = 250;

    private static final int CARD_GAP = 18;

    private static final int BACKGROUND =
            0xDD080808;

    private static final int PANEL_COLOR =
            0xFF151515;

    private static final int CARD_COLOR =
            0xFF202020;

    private static final int CARD_HOVER_COLOR =
            0xFF292929;

    private static final int BORDER_COLOR =
            0xFF555555;

    private static final int BORDER_HOVER_COLOR =
            0xFFFFD966;

    private static final int TITLE_COLOR =
            0xFFFFFFFF;

    private static final int SUBTITLE_COLOR =
            0xFFAAAAAA;

    private static final int DESCRIPTION_COLOR =
            0xFFBBBBBB;

    private static final int LEVEL_COLOR =
            0xFF80FF80;

    public JobSelectionScreen() {

        super(
                Component.literal(
                        "직업 선택"
                )
        );
    }

    @Override
    protected void init() {

        int panelX =
                (this.width - PANEL_WIDTH) / 2;

        int panelY =
                (this.height - PANEL_HEIGHT) / 2;

        int totalCardsWidth =
                CARD_WIDTH * 3
                        + CARD_GAP * 2;

        int startX =
                panelX
                        + (PANEL_WIDTH - totalCardsWidth) / 2;

        int buttonY =
                panelY + 350;

        addJobButton(
                JobType.MINER,
                startX,
                buttonY
        );

        addJobButton(
                JobType.FARMER,
                startX
                        + CARD_WIDTH
                        + CARD_GAP,
                buttonY
        );

        addJobButton(
                JobType.FISHER,
                startX
                        + (CARD_WIDTH + CARD_GAP) * 2,
                buttonY
        );
    }

    /*
     * 마인크래프트 기본 버튼
     */
    private void addJobButton(
            JobType jobType,
            int x,
            int y
    ) {

        Button button =
                Button.builder(
                        Component.literal(
                                "선택하기"
                        ),
                        pressed ->
                                selectJob(
                                        jobType
                                )
                )
                .bounds(
                        x,
                        y,
                        CARD_WIDTH,
                        32
                )
                .build();

        addRenderableWidget(
                button
        );
    }

    private void selectJob(
            JobType jobType
    ) {

        PacketDistributor.sendToServer(
                new JobSelectionPayload(
                        jobType.ordinal()
                )
        );

        Minecraft.getInstance()
                .setScreen(null);
    }

    @Override
    public void renderBackground(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        // 이 화면은 자체 배경을 그리므로 기본 메뉴의 흐림 배경을 적용하지 않습니다.
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {

        /*
         * 전체 배경
         */
        guiGraphics.fill(
                0,
                0,
                this.width,
                this.height,
                BACKGROUND
        );

        int panelX =
                (this.width - PANEL_WIDTH) / 2;

        int panelY =
                (this.height - PANEL_HEIGHT) / 2;

        drawPanel(
                guiGraphics,
                panelX,
                panelY
        );

        drawTitle(
                guiGraphics,
                panelX,
                panelY
        );

        int totalCardsWidth =
                CARD_WIDTH * 3
                        + CARD_GAP * 2;

        int startX =
                panelX
                        + (PANEL_WIDTH - totalCardsWidth) / 2;

        int cardY =
                panelY + 80;

        drawJobCard(
                guiGraphics,
                JobType.MINER,
                startX,
                cardY,
                mouseX,
                mouseY
        );

        drawJobCard(
                guiGraphics,
                JobType.FARMER,
                startX
                        + CARD_WIDTH
                        + CARD_GAP,
                cardY,
                mouseX,
                mouseY
        );

        drawJobCard(
                guiGraphics,
                JobType.FISHER,
                startX
                        + (CARD_WIDTH + CARD_GAP) * 2,
                cardY,
                mouseX,
                mouseY
        );

        // 직접 그린 UI를 먼저 확정한 뒤 기본 버튼을 렌더링합니다.
        guiGraphics.flush();

        // 배경과 카드 위에 기본 위젯(마인크래프트 버튼)을 마지막으로 그립니다.
        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void drawPanel(
            GuiGraphics guiGraphics,
            int x,
            int y
    ) {

        guiGraphics.fill(
                x,
                y,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                PANEL_COLOR
        );

        guiGraphics.fill(
                x,
                y,
                x + PANEL_WIDTH,
                y + 2,
                BORDER_COLOR
        );

        guiGraphics.fill(
                x,
                y + PANEL_HEIGHT - 2,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                BORDER_COLOR
        );

        guiGraphics.fill(
                x,
                y,
                x + 2,
                y + PANEL_HEIGHT,
                BORDER_COLOR
        );

        guiGraphics.fill(
                x + PANEL_WIDTH - 2,
                y,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                BORDER_COLOR
        );
    }

    private void drawTitle(
            GuiGraphics guiGraphics,
            int panelX,
            int panelY
    ) {

        String title =
                "직업을 선택하세요";

        int titleWidth =
                this.font.width(title);

        guiGraphics.drawString(
                this.font,
                title,
                panelX
                        + (PANEL_WIDTH - titleWidth) / 2,
                panelY + 25,
                TITLE_COLOR
        );

        String subtitle =
                "당신의 플레이 스타일에 맞는 직업을 선택하세요.";

        int subtitleWidth =
                this.font.width(subtitle);

        guiGraphics.drawString(
                this.font,
                subtitle,
                panelX
                        + (PANEL_WIDTH - subtitleWidth) / 2,
                panelY + 48,
                SUBTITLE_COLOR
        );
    }

    private void drawJobCard(
            GuiGraphics guiGraphics,
            JobType jobType,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {

        boolean hovered =
                mouseX >= x
                        && mouseX <= x + CARD_WIDTH
                        && mouseY >= y
                        && mouseY <= y + CARD_HEIGHT;

        int cardColor =
                hovered
                        ? CARD_HOVER_COLOR
                        : CARD_COLOR;

        guiGraphics.fill(
                x,
                y,
                x + CARD_WIDTH,
                y + CARD_HEIGHT,
                cardColor
        );

        int borderColor =
                hovered
                        ? BORDER_HOVER_COLOR
                        : BORDER_COLOR;

        guiGraphics.fill(
                x,
                y,
                x + CARD_WIDTH,
                y + 2,
                borderColor
        );

        guiGraphics.fill(
                x,
                y + CARD_HEIGHT - 2,
                x + CARD_WIDTH,
                y + CARD_HEIGHT,
                borderColor
        );

        guiGraphics.fill(
                x,
                y,
                x + 2,
                y + CARD_HEIGHT,
                borderColor
        );

        guiGraphics.fill(
                x + CARD_WIDTH - 2,
                y,
                x + CARD_WIDTH,
                y + CARD_HEIGHT,
                borderColor
        );

        /*
         * 아이콘
         */

        String icon =
                getJobIcon(
                        jobType
                );

        int iconWidth =
                this.font.width(icon);

        int iconColor =
                hovered
                        ? 0xFFFFE88A
                        : 0xFFFFD966;

        guiGraphics.drawString(
                this.font,
                icon,
                x
                        + (CARD_WIDTH - iconWidth) / 2,
                y + 25,
                iconColor
        );

        /*
         * 직업명
         */

        String name =
                jobType.getDisplayName();

        int nameWidth =
                this.font.width(name);

        guiGraphics.drawString(
                this.font,
                name,
                x
                        + (CARD_WIDTH - nameWidth) / 2,
                y + 70,
                0xFFFFFFFF
        );

        /*
         * 직업 설명
         */

        String description =
                jobType.getDescription();

        int descriptionWidth =
                this.font.width(description);

        if (descriptionWidth <= CARD_WIDTH - 20) {

            guiGraphics.drawString(
                    this.font,
                    description,
                    x
                            + (CARD_WIDTH - descriptionWidth) / 2,
                    y + 110,
                    DESCRIPTION_COLOR
            );

        } else {

            String[] words =
                    description.split(" ");

            String line1 = "";
            String line2 = "";

            for (String word : words) {

                String test =
                        line1.isEmpty()
                                ? word
                                : line1
                                        + " "
                                        + word;

                if (
                        this.font.width(test)
                                <= CARD_WIDTH - 30
                ) {

                    line1 = test;

                } else {

                    line2 =
                            line2.isEmpty()
                                    ? word
                                    : line2
                                            + " "
                                            + word;
                }
            }

            int line1Width =
                    this.font.width(line1);

            guiGraphics.drawString(
                    this.font,
                    line1,
                    x
                            + (CARD_WIDTH - line1Width) / 2,
                    y + 103,
                    DESCRIPTION_COLOR
            );

            if (!line2.isEmpty()) {

                int line2Width =
                        this.font.width(line2);

                guiGraphics.drawString(
                        this.font,
                        line2,
                        x
                                + (CARD_WIDTH - line2Width) / 2,
                        y + 120,
                        DESCRIPTION_COLOR
                );
            }
        }

        /*
         * =========================
         * 레벨업 효과
         * =========================
         */

        String levelTitle =
                "레벨업 효과";

        int levelTitleWidth =
                this.font.width(levelTitle);

        guiGraphics.drawString(
                this.font,
                levelTitle,
                x
                        + (CARD_WIDTH - levelTitleWidth) / 2,
                y + 155,
                LEVEL_COLOR
        );

        /*
         * 레벨업 효과 설명
         */

        String[] perks = jobType.getLevelDescription().split(" · ");
        for (int i = 0; i < perks.length; i++) {
            String perk = perks[i];
            guiGraphics.drawCenteredString(
                    this.font,
                    perk,
                    x + CARD_WIDTH / 2,
                    y + 175 + i * 16,
                    DESCRIPTION_COLOR
            );
        }

    }

    private String getJobIcon(
            JobType jobType
    ) {

        if (jobType == JobType.MINER) {

            return "⛏";

        } else if (jobType == JobType.FARMER) {

            return "🌾";

        } else {

            return "🎣";
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {

        return false;
    }
}
