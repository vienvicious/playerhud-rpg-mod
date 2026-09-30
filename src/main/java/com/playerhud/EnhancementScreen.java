package com.playerhud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class EnhancementScreen extends AbstractContainerScreen<EnhancementMenu> {
    public EnhancementScreen(EnhancementMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 232;
        titleLabelX = 12;
        titleLabelY = 8;
        inventoryLabelX = 12;
        inventoryLabelY = 112;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("강화 시도"), button ->
                        PacketDistributor.sendToServer(new EnhancementAttemptPayload()))
                .bounds(leftPos + 205, topPos + 34, 82, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF012121A);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 1, 0xFF806B9A);
        graphics.fill(leftPos, topPos + imageHeight - 1,
                leftPos + imageWidth, topPos + imageHeight, 0xFF806B9A);
        graphics.fill(leftPos, topPos, leftPos + 1, topPos + imageHeight, 0xFF806B9A);
        graphics.fill(leftPos + imageWidth - 1, topPos,
                leftPos + imageWidth, topPos + imageHeight, 0xFF806B9A);
        graphics.fill(leftPos + 12, topPos + 30, leftPos + 186, topPos + 53, 0xFF282433);
        graphics.fill(leftPos + 196, topPos + 30, leftPos + 290, topPos + 112, 0xFF201D28);

        graphics.drawString(font, "장비", leftPos + 26, topPos + 20, 0xFFE8D9FF, false);
        graphics.drawString(font, EnhancementSystem.getRequiredStoneName(menu.getStage()),
                leftPos + 67, topPos + 20, 0xFFC4A7FF, false);
        graphics.drawString(font, "하락 보호권", leftPos + 137, topPos + 20, 0xFFE8D9FF, false);

        String stage = String.format(java.util.Locale.ROOT, "+%02d강 / 상한 +%02d강",
                menu.getStage(), menu.getEnhancementCap());
        graphics.drawString(font, stage, leftPos + 204, topPos + 60, 0xFFFFD66B, false);
        graphics.drawString(font, "성공 " + menu.getSuccessChance() + "%", leftPos + 204, topPos + 75,
                0xFF70E090, false);
        graphics.drawString(font, "실패 " + menu.getFailureChance() + "%", leftPos + 204, topPos + 88,
                0xFFFFD66B, false);
        graphics.drawString(font, "실패 시 하락 " + menu.getDowngradeChance() + "%", leftPos + 204,
                topPos + 101, 0xFFFF7777, false);

        graphics.drawString(font, "장인의 기운 " + menu.getArtisanSpirit() + "%",
                leftPos + 12, topPos + 62, 0xFFC99AFF, false);
        int gaugeWidth = 174;
        graphics.fill(leftPos + 12, topPos + 76, leftPos + 12 + gaugeWidth, topPos + 82, 0xFF453A51);
        int filled = gaugeWidth * menu.getArtisanSpirit() / 100;
        if (filled > 0) {
            graphics.fill(leftPos + 12, topPos + 76, leftPos + 12 + filled, topPos + 82, 0xFFB45BFF);
        }
        graphics.drawString(font, menu.getResultText(), leftPos + 12, topPos + 91, 0xFFE0D9E8, false);
        String hint = "성공하면 능력치가 오르고, 실패하면 기운이 쌓입니다.";
        graphics.drawString(font, hint, leftPos + (imageWidth - font.width(hint)) / 2,
                topPos + 213, 0xFFAAA3B2, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFFFF, false);
        graphics.drawString(font, "인벤토리", inventoryLabelX, inventoryLabelY, 0xFFFFFFFF, false);
    }
}
