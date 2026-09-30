package com.playerhud;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class RidingMountItem extends Item {
    static final String MOUNT_MARKER = "playerhud_riding_mount";
    static final String MOUNT_OWNER = "playerhud_riding_owner";
    static final String PLAYER_MOUNT_ID = "playerhud_riding_entity";

    public enum Mount {
        BROWN_HORSE(EntityType.HORSE, "riding_brown_horse", Variant.BROWN),
        CHESTNUT_HORSE(EntityType.HORSE, "riding_chestnut_horse", Variant.CHESTNUT),
        CREAMY_HORSE(EntityType.HORSE, "riding_creamy_horse", Variant.CREAMY),
        DARK_BROWN_HORSE(EntityType.HORSE, "riding_dark_brown_horse", Variant.DARK_BROWN),
        CAMEL(EntityType.CAMEL, "riding_camel", null),
        STRIDER(EntityType.STRIDER, "riding_strider", null),
        WHITE_HORSE(EntityType.HORSE, "riding_white_horse", Variant.WHITE),
        BLACK_HORSE(EntityType.HORSE, "riding_black_horse", Variant.BLACK),
        SKELETON_HORSE(EntityType.SKELETON_HORSE, "riding_skeleton_horse", null),
        UNDEAD_HORSE(EntityType.ZOMBIE_HORSE, "riding_undead_horse", null);

        final EntityType<? extends Mob> type;
        final String itemId;
        final Variant variant;

        Mount(EntityType<? extends Mob> type, String itemId, Variant variant) {
            this.type = type;
            this.itemId = itemId;
            this.variant = variant;
        }
    }

    private final Mount mount;

    public RidingMountItem(Mount mount, Properties properties) {
        super(properties);
        this.mount = mount;
    }

    public Mount mount() { return mount; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ChatFormatting color = switch (mount) {
            case BROWN_HORSE, CHESTNUT_HORSE, CREAMY_HORSE, DARK_BROWN_HORSE, CAMEL, STRIDER -> ChatFormatting.WHITE;
            case WHITE_HORSE, BLACK_HORSE -> ChatFormatting.BLUE;
            case SKELETON_HORSE, UNDEAD_HORSE -> ChatFormatting.DARK_PURPLE;
        };
        String rarity = switch (mount) {
            case BROWN_HORSE, CHESTNUT_HORSE, CREAMY_HORSE, DARK_BROWN_HORSE, CAMEL, STRIDER -> "Common";
            case WHITE_HORSE, BLACK_HORSE -> "Rare";
            case SKELETON_HORSE, UNDEAD_HORSE -> "Unique";
        };
        tooltip.add(Component.literal(rarity).withStyle(color));
        tooltip.add(Component.translatable("item.playerhud.riding_mount.hint").withStyle(ChatFormatting.GRAY));
        if (mount == Mount.STRIDER) {
            tooltip.add(Component.translatable("item.playerhud.riding_strider.hint").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer) || player.isPassenger()) {
            return InteractionResultHolder.fail(stack);
        }

        Entity entity = mount.type.create(level);
        if (entity == null) return InteractionResultHolder.fail(stack);
        Vec3 look = player.getLookAngle();
        entity.moveTo(player.getX() + look.x * 1.5, player.getY(), player.getZ() + look.z * 1.5,
                player.getYRot(), 0);
        entity.setInvulnerable(true);
        entity.getPersistentData().putBoolean(MOUNT_MARKER, true);
        entity.getPersistentData().putUUID(MOUNT_OWNER, player.getUUID());
        if (entity instanceof AbstractHorse horse) {
            horse.setTamed(true);
            horse.setOwnerUUID(player.getUUID());
            horse.equipSaddle(new ItemStack(Items.SADDLE), null);
            if (entity instanceof Horse normalHorse && mount.variant != null) {
                normalHorse.setVariant(mount.variant);
            }
        } else if (entity instanceof Saddleable saddleable) {
            saddleable.equipSaddle(new ItemStack(Items.SADDLE), null);
        }
        var maxHealth = ((Mob) entity).getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(40.0D);
            ((Mob) entity).setHealth(40.0F);
        }
        var movementSpeed = ((Mob) entity).getAttribute(Attributes.MOVEMENT_SPEED);
        if (movementSpeed != null) {
            switch (mount) {
                case WHITE_HORSE -> movementSpeed.setBaseValue(0.44D);
                case BLACK_HORSE -> movementSpeed.setBaseValue(0.48D);
                case SKELETON_HORSE -> movementSpeed.setBaseValue(0.72D);
                case UNDEAD_HORSE -> movementSpeed.setBaseValue(0.82D);
                default -> { /* Common rides keep their vanilla movement speed. */ }
            }
        }
        if (!level.addFreshEntity(entity)) return InteractionResultHolder.fail(stack);
        if (!serverPlayer.startRiding(entity, true)) {
            entity.discard();
            return InteractionResultHolder.fail(stack);
        }
        serverPlayer.getPersistentData().putUUID(PLAYER_MOUNT_ID, entity.getUUID());
        return InteractionResultHolder.consume(stack);
    }
}
