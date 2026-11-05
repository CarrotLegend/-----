package com.carrot123.yixiangshouronglu.shop;

import net.minecraft.nbt.CompoundTag;

public final class PlayerShopData {
    public record State(long coins, boolean welcomed, long revision) {}
    private long coins;
    private boolean welcomed;
    private long revision;

    public long coins() { return coins; }
    public boolean welcomed() { return welcomed; }
    public long revision() { return revision; }
    public boolean canPay(long amount) { return amount > 0 && coins >= amount && revision < Long.MAX_VALUE; }
    public boolean canAdd(long amount) { return amount > 0 && amount <= Long.MAX_VALUE - coins && revision < Long.MAX_VALUE; }
    public boolean add(long amount) {
        if (!canAdd(amount)) { return false; }
        coins += amount; revision++; return true;
    }
    public boolean pay(long amount) {
        if (!canPay(amount)) { return false; }
        coins -= amount; revision++; return true;
    }
    public boolean welcome() {
        if (welcomed) { return false; }
        welcomed = true;
        if (revision < Long.MAX_VALUE) { revision++; }
        return true;
    }
    public State state() { return new State(coins, welcomed, revision); }
    void restore(State state) { coins = state.coins(); welcomed = state.welcomed(); revision = state.revision(); }
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", 1); tag.putLong("Coins", coins); tag.putBoolean("Welcomed", welcomed); tag.putLong("Revision", revision);
        return tag;
    }
    public void load(CompoundTag tag) {
        coins = Math.max(0, tag.getLong("Coins")); welcomed = tag.getBoolean("Welcomed"); revision = Math.max(0, tag.getLong("Revision"));
    }
}
