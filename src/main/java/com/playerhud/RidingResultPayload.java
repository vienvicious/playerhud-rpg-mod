package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record RidingResultPayload(String itemId, int rarity) implements CustomPacketPayload {
    public static final Type<RidingResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlayerHudMod.MOD_ID, "riding_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RidingResultPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RidingResultPayload::itemId,
                    ByteBufCodecs.VAR_INT, RidingResultPayload::rarity, RidingResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
