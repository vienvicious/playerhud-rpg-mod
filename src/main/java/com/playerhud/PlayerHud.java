package com.playerhud;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(
        modid = "playerhud",
        value = Dist.CLIENT
)
public class PlayerHud {

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(
                        "playerhud",
                        "main_hud"
                ),
                HudLayer::render
        );
    }
}