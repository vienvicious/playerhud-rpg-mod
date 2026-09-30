package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JobSelectionPayload(int jobId)
        implements CustomPacketPayload {

    public static final Type<JobSelectionPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "playerhud",
                            "job_selection"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            JobSelectionPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    JobSelectionPayload::jobId,
                    JobSelectionPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}