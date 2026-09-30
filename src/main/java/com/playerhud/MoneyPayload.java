package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MoneyPayload(long money)
        implements CustomPacketPayload {

    public static final Type<MoneyPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "playerhud",
                            "money"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            MoneyPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    MoneyPayload::money,
                    MoneyPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}