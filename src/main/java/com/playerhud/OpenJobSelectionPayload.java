package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenJobSelectionPayload(boolean open)
        implements CustomPacketPayload {

    public static final Type<OpenJobSelectionPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "playerhud",
                            "open_job_selection"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            OpenJobSelectionPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    OpenJobSelectionPayload::open,
                    OpenJobSelectionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}