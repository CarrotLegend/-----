package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class BlazingDomain {
    public static final int DURATION = 600;
    public static final int COOLDOWN = 2400;
    public static final int INTERVAL = 20;
    public static final double RANGE = 10;
    public static final double RANGE_Y = 5;
    public static final ResourceLocation HEAT_PARTICLE = new ResourceLocation("mutantmore", "heat");
    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ElementalGenes.BLAZING_DOMAIN);
    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final class State {
        final long started;
        final ResourceKey<Level> dimension;
        long next;
        State(long now, ResourceKey<Level> dimension) { started = now; next = now + INTERVAL; this.dimension = dimension; }
    }
    private BlazingDomain() {}
    public static boolean active(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
    public static String start(ServerPlayer player) {
        if (!player.isAlive() || player.hasDisconnected() || player.isSpectator() || player.isChangingDimension()) { return "invalid_state"; }
        if (active(player)) { return "already_active"; }
        if (!ForgeRegistries.PARTICLE_TYPES.containsKey(HEAT_PARTICLE)) { return "heat_unavailable"; }
        if (!GeneEffects.installed(player).containsKey(ElementalGenes.BLAZING_NERVES)) { return "not_installed"; }
        long now = player.server.overworld().getGameTime();
        State state = new State(now, player.level().dimension());
        ACTIVE.put(player.getUUID(), state);
        GeneNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet(player, state, DURATION));
        return "ok";
    }
    private static GeneNetwork.DomainState packet(ServerPlayer player, State state, int remaining) {
        return new GeneNetwork.DomainState(player.getUUID(), player.getId(), state.dimension.location(), state.started, remaining);
    }
    public static void stop(ServerPlayer player) {
        State state = ACTIVE.remove(player.getUUID());
        if (state != null) {
            GeneNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet(player, state, 0));
        }
    }
    public static boolean canTarget(ServerPlayer player, LivingEntity target) {
        if (target == player || !target.isAlive() || target.isRemoved() || target.isSpectator() || target instanceof ArmorStand || target.level() != player.level()) { return false; }
        return !(target instanceof ServerPlayer other) || player.server.isPvpAllowed() && player.canHarmPlayer(other);
    }
    public static DamageSource source(ServerPlayer player) {
        return new DamageSource(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE), player, player);
    }
    public static void advance(ServerPlayer player, long now) {
        State state = ACTIVE.get(player.getUUID());
        if (state == null) { return; }
        if (!player.isAlive() || player.hasDisconnected() || player.isSpectator() || !player.level().dimension().equals(state.dimension)
                || !GeneEffects.installed(player).containsKey(ElementalGenes.BLAZING_NERVES) || now > state.started + DURATION) { stop(player); return; }
        if (now >= state.next) {
            state.next = now + INTERVAL;
            DamageSource source = source(player);
            for (var target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RANGE, RANGE_Y, RANGE), target -> canTarget(player, target))) {
                target.hurt(source, 8);
            }
        }
        if (now >= state.started + DURATION) { stop(player); }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) { advance(player, player.server.overworld().getGameTime()); }
    }
    @SubscribeEvent public static void died(LivingDeathEvent event) { if (event.getEntity() instanceof ServerPlayer player) { stop(player); } }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { if (event.getEntity() instanceof ServerPlayer player) { stop(player); } }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer player) { stop(player); } }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player) { stop(player); } }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer owner) {
            State state = ACTIVE.get(owner.getUUID());
            if (state != null) {
                int remaining = (int) Math.max(0, state.started + DURATION - owner.server.overworld().getGameTime());
                GeneNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), packet(owner, state, remaining));
            }
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { ACTIVE.clear(); }
}
