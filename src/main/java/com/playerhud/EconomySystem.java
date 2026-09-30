package com.playerhud;

import net.minecraft.world.entity.player.Player;

public class EconomySystem {

    private static final String MONEY_KEY =
            "playerhud_money";

    /*
     * =========================
     * 현재 보유 금액
     * =========================
     */

    public static long getMoney(
            Player player
    ) {

        if (player == null) {
            return 0L;
        }

        return player
                .getPersistentData()
                .getLong(MONEY_KEY);
    }

    /*
     * =========================
     * 돈 설정
     * =========================
     */

    public static void setMoney(
            Player player,
            long amount
    ) {

        if (player == null) {
            return;
        }

        if (amount < 0L) {
            amount = 0L;
        }

        player
                .getPersistentData()
                .putLong(
                        MONEY_KEY,
                        amount
                );
    }

    /*
     * =========================
     * 돈 지급
     * =========================
     */

    public static void addMoney(
            Player player,
            long amount
    ) {

        if (player == null) {
            return;
        }

        if (amount <= 0L) {
            return;
        }

        long currentMoney =
                getMoney(player);

        long newMoney;

        if (Long.MAX_VALUE - currentMoney < amount) {
            newMoney = Long.MAX_VALUE;
        } else {
            newMoney =
                    currentMoney + amount;
        }

        setMoney(
                player,
                newMoney
        );
    }

    /*
     * =========================
     * 돈 차감
     * =========================
     */

    public static boolean removeMoney(
            Player player,
            long amount
    ) {

        if (player == null) {
            return false;
        }

        if (amount <= 0L) {
            return false;
        }

        long currentMoney =
                getMoney(player);

        if (currentMoney < amount) {
            return false;
        }

        setMoney(
                player,
                currentMoney - amount
        );

        return true;
    }

    /*
     * =========================
     * 돈이 충분한지 확인
     * =========================
     */

    public static boolean hasMoney(
            Player player,
            long amount
    ) {

        if (player == null) {
            return false;
        }

        if (amount < 0L) {
            return false;
        }

        return getMoney(player) >= amount;
    }
}