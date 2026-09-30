package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MinerXpPayload(int xp)
        implements CustomPacketPayload {

    public static final Type<MinerXpPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "playerhud",
                            "miner_xp"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            MinerXpPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    MinerXpPayload::xp,
                    MinerXpPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}