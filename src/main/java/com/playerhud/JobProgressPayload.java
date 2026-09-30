package com.playerhud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record JobProgressPayload(int jobId, int xp, int gainedXp)
        implements CustomPacketPayload {

    public static final Type<JobProgressPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("playerhud", "job_progress"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JobProgressPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, JobProgressPayload::jobId,
                    ByteBufCodecs.VAR_INT, JobProgressPayload::xp,
                    ByteBufCodecs.VAR_INT, JobProgressPayload::gainedXp,
                    JobProgressPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
