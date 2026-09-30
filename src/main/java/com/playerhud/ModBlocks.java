package com.playerhud;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PlayerHudMod.MOD_ID);
    public static final DeferredBlock<Block> ENHANCEMENT_ALTAR = BLOCKS.register("enhancement_altar",
            () -> new EnhancementAltarBlock(BlockBehaviour.Properties.of()
                    .strength(4.0F).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)));

    private ModBlocks() {}
}
