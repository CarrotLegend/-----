package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.menu.CarrotKingShopMenu;
import com.carrot123.yixiangshouronglu.shop.ShopCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class LegendaryCarrotKingEntity extends PathfinderMob {
    public LegendaryCarrotKingEntity(EntityType<? extends LegendaryCarrotKingEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 8);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(Items.NAME_TAG)) { return player.getItemInHand(hand).interactLivingEntity(player, this, hand); }
        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !isAlive()) { return InteractionResult.PASS; }
        if (player instanceof ServerPlayer serverPlayer) {
            boolean welcome = ShopCapability.get(player).welcome();
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, owner) -> new CarrotKingShopMenu(id, inventory, getId(), getUUID(), welcome),
                    Component.translatable("shop.yixiangshouronglu.title")),
                    buffer -> { buffer.writeVarInt(getId()); buffer.writeUUID(getUUID()); buffer.writeBoolean(welcome); });
            if (serverPlayer.containerMenu instanceof CarrotKingShopMenu menu) { menu.syncState("ok"); }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
