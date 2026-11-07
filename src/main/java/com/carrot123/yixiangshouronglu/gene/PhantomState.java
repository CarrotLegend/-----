package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID)
public final class PhantomState {
    public static final ResourceLocation SKILL = new ResourceLocation("yixiangshouronglu", "phantom_state");
    public static final ResourceKey<DamageType> FOLLOW_UP = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("yixiangshouronglu", "phantom_follow_up"));
    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static long session;

    public record State(ServerPlayer player, Level level, long started, long session) {}

    public static final class FollowUpDamage extends DamageSource {
        private FollowUpDamage(ServerPlayer player) {
            super(player.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FOLLOW_UP), null, player);
        }
    }

    private PhantomState() {}

    public static boolean isFollowUp(DamageSource source) { return source instanceof FollowUpDamage || source.is(FOLLOW_UP); }
    public static DamageSource followUpSource(ServerPlayer player) { return new FollowUpDamage(player); }

    public static State current(ServerPlayer player) {
        State state = ACTIVE.get(player.getUUID());
        if (state == null) { return null; }
        if (state.player() != player || !AscendantGenes.valid(player, state.level()) || AscendantGenes.now(player) >= state.started() + 600
                || !GeneEffects.installed(player).containsKey(AscendantGenes.PHANTOM_NERVES)) {
            stop(player);
            return null;
        }
        return state;
    }

    public static String start(ServerPlayer player) {
        if (current(player) != null) { return "phantom_already_active"; }
        if (!AscendantGenes.valid(player, player.level())) { return "invalid_state"; }
        if (!GeneEffects.installed(player).containsKey(AscendantGenes.PHANTOM_NERVES)) { return "not_installed"; }
        State state = new State(player, player.level(), AscendantGenes.now(player), ++session);
        ACTIVE.put(player.getUUID(), state);
        GeneNetwork.phantomState(player, state.session(), 600);
        return "ok";
    }

    public static void stop(ServerPlayer player) {
        State state = ACTIVE.remove(player.getUUID());
        if (state != null && !player.hasDisconnected()) { GeneNetwork.phantomState(player, state.session(), 0); }
    }

    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (State state : List.copyOf(ACTIVE.values())) { current(state.player()); }
        }
    }

    static void clear() { ACTIVE.clear(); session = 0; }
}
