package com.playerhud;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.InteractionResult;

public class EnhancementAltarBlock extends Block {
    public EnhancementAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) ->
                            new EnhancementMenu(containerId, inventory, pos),
                    Component.translatable("block.playerhud.enhancement_altar"));
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }
}
