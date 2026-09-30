package com.playerhud;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnhancementMenu extends AbstractContainerMenu {
    private final Container inputs = new SimpleContainer(3);
    private final ContainerLevelAccess access;
    private final Player player;
    private int stage;
    private int enhancementCap;
    private int successChance = EnhancementSystem.getSuccessChance(0);
    private int downgradeChance;
    private int artisanSpirit;
    private int result;

    public EnhancementMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, data.readBlockPos());
    }

    public EnhancementMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ENHANCEMENT.get(), containerId);
        this.player = inventory.player;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        inputs.startOpen(inventory.player);

        addSlot(new Slot(inputs, 0, 34, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return EnhancementSystem.isEnhanceable(stack); }
        });
        addSlot(new Slot(inputs, 1, 96, 35) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.LESSER_ENHANCEMENT_STONE.get())
                        || stack.is(ModItems.INTERMEDIATE_ENHANCEMENT_STONE.get())
                        || stack.is(ModItems.GREATER_ENHANCEMENT_STONE.get());
            }
        });
        addSlot(new Slot(inputs, 2, 158, 35) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.DOWNGRADE_PROTECTION_SCROLL.get());
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        12 + column * 18, 124 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 12 + column * 18, 182));
        }

        addDataSlot(sync(() -> stage, value -> stage = value));
        addDataSlot(sync(() -> enhancementCap, value -> enhancementCap = value));
        addDataSlot(sync(() -> successChance, value -> successChance = value));
        addDataSlot(sync(() -> downgradeChance, value -> downgradeChance = value));
        addDataSlot(sync(() -> artisanSpirit, value -> artisanSpirit = value));
        addDataSlot(sync(() -> result, value -> result = value));
        refreshStats();
    }

    private DataSlot sync(java.util.function.IntSupplier getter,
                          java.util.function.IntConsumer setter) {
        return new DataSlot() {
            @Override public int get() { return getter.getAsInt(); }
            @Override public void set(int value) { setter.accept(value); }
        };
    }

    @Override
    public void broadcastChanges() {
        refreshStats();
        super.broadcastChanges();
    }

    private void refreshStats() {
        ItemStack equipment = inputs.getItem(0);
        stage = EnhancementSystem.getLevel(equipment);
        enhancementCap = EnhancementSystem.getEnhancementCap(player);
        artisanSpirit = EnhancementSystem.getArtisanSpirit(equipment);
        successChance = stage >= enhancementCap ? 0
                : artisanSpirit >= 100 ? 100 : EnhancementSystem.getSuccessChance(stage);
        downgradeChance = stage >= enhancementCap
                ? 0 : EnhancementSystem.getDowngradeChance(stage);
    }

    public void attemptEnhancement(ServerPlayer player) {
        if (player.containerMenu != this || !stillValid(player)) return;
        ItemStack equipment = inputs.getItem(0);
        ItemStack stone = inputs.getItem(1);
        ItemStack scroll = inputs.getItem(2);
        stage = EnhancementSystem.getLevel(equipment);
        artisanSpirit = EnhancementSystem.getArtisanSpirit(equipment);

        if (!EnhancementSystem.isEnhanceable(equipment)) {
            result = 7;
            return;
        }
        if (stage >= EnhancementSystem.MAX_LEVEL) {
            result = 5;
            return;
        }
        enhancementCap = EnhancementSystem.getEnhancementCap(player);
        if (stage >= enhancementCap) {
            result = 10;
            return;
        }
        if (stone.isEmpty()) {
            result = 6;
            return;
        }
        if (!stone.is(EnhancementSystem.getRequiredStone(stage).get())) {
            result = 9;
            return;
        }

        boolean protectionConsumed = !scroll.isEmpty();
        if (protectionConsumed) scroll.shrink(1);
        stone.shrink(1);
        boolean guaranteed = artisanSpirit >= 100;
        if (guaranteed || player.getRandom().nextInt(100) < successChance) {
            EnhancementSystem.setLevel(equipment, stage + 1, player.registryAccess());
            EnhancementSystem.setArtisanSpirit(equipment, 0);
            result = guaranteed ? 8 : 1;
            playEnhancementEffect(player, true);
        } else {
            int newSpirit = Math.min(100, artisanSpirit + 10);
            EnhancementSystem.setArtisanSpirit(equipment, newSpirit);
            if (player.getRandom().nextInt(100) < downgradeChance) {
                if (protectionConsumed) {
                    result = 4;
                } else {
                    EnhancementSystem.setLevel(equipment, stage - 1, player.registryAccess());
                    result = 3;
                }
            } else {
                result = 2;
            }
            playEnhancementEffect(player, false);
        }

        inputs.setChanged();
        refreshStats();
        broadcastChanges();
    }

    private static void playEnhancementEffect(ServerPlayer player, boolean success) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        if (success) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,
                    player.getX(), player.getY() + 1.0D, player.getZ(),
                    24, 0.35D, 0.55D, 0.35D, 0.08D);
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        } else {
            level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ANVIL_BREAK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.0F);
        }
    }

    public int getStage() { return stage; }
    public int getEnhancementCap() { return enhancementCap; }
    public int getSuccessChance() { return successChance; }
    public int getFailureChance() {
        return stage >= enhancementCap ? 0 : 100 - successChance;
    }
    public int getDowngradeChance() { return downgradeChance; }
    public int getArtisanSpirit() { return artisanSpirit; }
    public int getResult() { return result; }

    public String getResultText() {
        return switch (result) {
            case 1 -> "강화 성공!";
            case 2 -> "실패: 강화 단계 유지";
            case 3 -> "실패: 강화 단계 하락";
            case 4 -> "하락 보호권이 발동했습니다.";
            case 5 -> "이미 최대 단계입니다.";
            case 6 -> "강화석이 필요합니다.";
            case 7 -> "무기 또는 방어구를 올려주세요.";
            case 8 -> "장인의 기운 발동! 강화 성공!";
            case 9 -> "현재 단계에 맞는 " + EnhancementSystem.getRequiredStoneName(stage) + "이 필요합니다.";
            case 10 -> "전투 레벨을 올려야 강화 상한이 증가합니다.";
            default -> "강화석을 넣고 강화를 시도하세요.";
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return empty;
        ItemStack original = slot.getItem();
        ItemStack moved = original.copy();

        if (index < 3) {
            if (!moveItemStackTo(original, 3, 39, true)) return ItemStack.EMPTY;
        } else if (EnhancementSystem.isEnhanceable(original)) {
            if (!moveItemStackTo(original, 0, 1, false)) return ItemStack.EMPTY;
        } else if (original.is(ModItems.LESSER_ENHANCEMENT_STONE.get())
                || original.is(ModItems.INTERMEDIATE_ENHANCEMENT_STONE.get())
                || original.is(ModItems.GREATER_ENHANCEMENT_STONE.get())) {
            if (!moveItemStackTo(original, 1, 2, false)) return ItemStack.EMPTY;
        } else if (original.is(ModItems.DOWNGRADE_PROTECTION_SCROLL.get())) {
            if (!moveItemStackTo(original, 2, 3, false)) return ItemStack.EMPTY;
        } else if (index < 30) {
            if (!moveItemStackTo(original, 30, 39, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 3, 30, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (original.getCount() == moved.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, original);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ENHANCEMENT_ALTAR.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            inputs.stopOpen(player);
            for (int i = 0; i < inputs.getContainerSize(); i++) {
                player.getInventory().placeItemBackInInventory(inputs.removeItemNoUpdate(i));
            }
        }
    }
}
