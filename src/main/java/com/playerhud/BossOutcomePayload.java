package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BossOutcomePayload(int outcome) implements CustomPacketPayload {
    public static final Type<BossOutcomePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("playerhud", "boss_outcome"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BossOutcomePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, BossOutcomePayload::outcome, BossOutcomePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
