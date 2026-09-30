package com.playerhud;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.BlockPos;

public class FarmerCropData extends SavedData {
    private static final String DATA_NAME = "playerhud_farmer_crops";
    private static final String OWNERS_TAG = "owners";
    private static final String LEVELS_TAG = "farmer_levels";
    private static final Factory<FarmerCropData> FACTORY = new Factory<>(
            FarmerCropData::new,
            FarmerCropData::load,
            DataFixTypes.LEVEL
    );

    private final Map<String, String> cropOwners = new HashMap<>();
    private final Map<UUID, Integer> farmerLevels = new HashMap<>();

    public static FarmerCropData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public static FarmerCropData load(CompoundTag tag, HolderLookup.Provider registries) {
        FarmerCropData data = new FarmerCropData();
        CompoundTag owners = tag.getCompound(OWNERS_TAG);
        for (String key : owners.getAllKeys()) {
            if (owners.contains(key, 8)) {
                data.cropOwners.put(key, owners.getString(key));
            }
        }

        CompoundTag levels = tag.getCompound(LEVELS_TAG);
        for (String key : levels.getAllKeys()) {
            try {
                if (levels.contains(key, 99)) {
                    data.farmerLevels.put(UUID.fromString(key), levels.getInt(key));
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid UUIDs in old or damaged data.
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag owners = new CompoundTag();
        cropOwners.forEach(owners::putString);
        tag.put(OWNERS_TAG, owners);

        CompoundTag levels = new CompoundTag();
        farmerLevels.forEach((uuid, level) -> levels.putInt(uuid.toString(), level));
        tag.put(LEVELS_TAG, levels);
        return tag;
    }

    private static String cropKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.asLong();
    }

    public void markCrop(ServerLevel level, BlockPos pos, UUID owner, int farmerLevel) {
        cropOwners.put(cropKey(level, pos), owner.toString());
        farmerLevels.put(owner, farmerLevel);
        setDirty();
    }

    public void removeCrop(ServerLevel level, BlockPos pos) {
        if (cropOwners.remove(cropKey(level, pos)) != null) {
            setDirty();
        }
    }

    public void updateFarmerLevel(UUID owner, int level) {
        if (!Integer.valueOf(level).equals(farmerLevels.put(owner, level))) {
            setDirty();
        }
    }

    public int getCropOwnerLevel(ServerLevel level, BlockPos pos) {
        String ownerValue = cropOwners.get(cropKey(level, pos));
        if (ownerValue == null) {
            return 0;
        }
        try {
            return farmerLevels.getOrDefault(UUID.fromString(ownerValue), 0);
        } catch (IllegalArgumentException ignored) {
            cropOwners.remove(cropKey(level, pos));
            setDirty();
            return 0;
        }
    }

    public boolean isCropOwnedBy(ServerLevel level, BlockPos pos, UUID owner) {
        return owner.toString().equals(cropOwners.get(cropKey(level, pos)));
    }
}
