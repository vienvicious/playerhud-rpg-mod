package com.playerhud;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class RidingTicketItem extends Item {
    public RidingTicketItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.common").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.brown").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.chestnut").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.creamy").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.dark_brown").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.camel").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.strider").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.rare").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.white").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.black").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.unique").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.skeleton").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.odds.undead").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.playerhud.riding_ticket.hint").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
        int roll = level.random.nextInt(100);
        RidingMountItem.Mount reward;
        RidingMountItem.Mount[] mounts = RidingMountItem.Mount.values();
        reward = mounts[roll / 10];

        ItemStack prize = new ItemStack(switch (reward) {
            case BROWN_HORSE -> ModItems.RIDING_BROWN_HORSE.get();
            case CHESTNUT_HORSE -> ModItems.RIDING_CHESTNUT_HORSE.get();
            case CREAMY_HORSE -> ModItems.RIDING_CREAMY_HORSE.get();
            case DARK_BROWN_HORSE -> ModItems.RIDING_DARK_BROWN_HORSE.get();
            case CAMEL -> ModItems.RIDING_CAMEL.get();
            case STRIDER -> ModItems.RIDING_STRIDER.get();
            case WHITE_HORSE -> ModItems.RIDING_WHITE_HORSE.get();
            case BLACK_HORSE -> ModItems.RIDING_BLACK_HORSE.get();
            case SKELETON_HORSE -> ModItems.RIDING_SKELETON_HORSE.get();
            case UNDEAD_HORSE -> ModItems.RIDING_UNDEAD_HORSE.get();
        });
        if (!serverPlayer.getInventory().add(prize)) serverPlayer.drop(prize, false);
        if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
        serverPlayer.sendSystemMessage(Component.translatable("message.playerhud.riding_ticket.reward", prize.getHoverName()));
        int rarity = switch (reward) {
            case BROWN_HORSE, CHESTNUT_HORSE, CREAMY_HORSE, DARK_BROWN_HORSE, CAMEL, STRIDER -> 0;
            case WHITE_HORSE, BLACK_HORSE -> 1;
            case SKELETON_HORSE, UNDEAD_HORSE -> 2;
        };
        JobNetwork.sendRidingResult(serverPlayer, reward.itemId, rarity);
        if (rarity >= 1) {
            serverPlayer.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,
                    serverPlayer.getX(), serverPlayer.getY() + 1.0D, serverPlayer.getZ(),
                    rarity == 1 ? 24 : 40, 0.35D, 0.55D, 0.35D, 0.08D);
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(),
                    net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, rarity == 1 ? 1.0F : 0.75F);
        }
        return InteractionResultHolder.consume(stack);
    }
}
