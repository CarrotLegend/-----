package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.menu.CarrotKingShopMenu;
import com.carrot123.yixiangshouronglu.shop.ShopCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class LegendaryCarrotKingEntity extends PathfinderMob {

    public LegendaryCarrotKingEntity(
            EntityType<? extends LegendaryCarrotKingEntity> type,
            Level level
    ) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 8.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof LegendaryCarrotKingEntity) {
            event.setNewTarget(null);
            event.setCanceled(true);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).is(Items.NAME_TAG)) {
            return player.getItemInHand(hand).interactLivingEntity(player, this, hand);
        }

        if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || !isAlive()) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            boolean welcome = ShopCapability.get(player).welcome();

            NetworkHooks.openScreen(
                    serverPlayer,
                    new SimpleMenuProvider(
                            (id, inventory, owner) -> new CarrotKingShopMenu(
                                    id,
                                    inventory,
                                    getId(),
                                    getUUID(),
                                    welcome
                            ),
                            Component.translatable("shop.yixiangshouronglu.title")
                    ),
                    buffer -> {
                        buffer.writeVarInt(getId());
                        buffer.writeUUID(getUUID());
                        buffer.writeBoolean(welcome);
                    }
            );

            if (serverPlayer.containerMenu instanceof CarrotKingShopMenu menu) {
                menu.syncState("ok");
            }
        }

        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
