package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record CombatProgressPayload(int xp, int gainedXp) implements CustomPacketPayload {

    public static final Type<CombatProgressPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("playerhud", "combat_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CombatProgressPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CombatProgressPayload::xp,
                    ByteBufCodecs.VAR_INT, CombatProgressPayload::gainedXp,
                    CombatProgressPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
