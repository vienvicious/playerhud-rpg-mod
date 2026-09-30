package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EnhancementAttemptPayload() implements CustomPacketPayload {
    public static final Type<EnhancementAttemptPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("playerhud", "enhancement_attempt"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EnhancementAttemptPayload> STREAM_CODEC =
            StreamCodec.unit(new EnhancementAttemptPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
