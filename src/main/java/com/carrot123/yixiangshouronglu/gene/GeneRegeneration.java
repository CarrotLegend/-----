package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AnomalousContainmentChronicles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GeneRegeneration {
    private static final ThreadLocal<UUID> INTERNAL = new ThreadLocal<>();
    private GeneRegeneration() {}
    public static int renewedDuration(int duration) { return duration > 0 && duration <= 20 ? duration + 25 : 0; }
    private static boolean eligible(MobEffectInstance effect) { return effect != null && effect.getAmplifier() == 1 && effect.getDuration() > 0 && effect.getDuration() <= 45; }
    private static void clear(PlayerGenes data) { data.regenerationInstance = null; data.effectRuntime().remove("UndeadRegeneration"); }
    private static boolean owned(PlayerGenes data, MobEffectInstance current) {
        if (!eligible(current)) { clear(data); return false; }
        CompoundTag lease = data.effectRuntime().getCompound("UndeadRegeneration");
        if (!lease.getBoolean("Owned")) { return false; }
        if (data.regenerationInstance == current) { return true; }
        if (data.regenerationInstance == null && lease.getCompound("Expected").equals(current.save(new CompoundTag()))) { data.regenerationInstance = current; return true; }
        clear(data); return false;
    }
    private static void stamp(PlayerGenes data, MobEffectInstance effect) {
        data.regenerationInstance = effect; CompoundTag lease = new CompoundTag(); lease.putBoolean("Owned", true); lease.put("Expected", effect.save(new CompoundTag())); data.effectRuntime().put("UndeadRegeneration", lease);
    }
    private static void internal(ServerPlayer player, Runnable operation) {
        UUID previous = INTERNAL.get(); INTERNAL.set(player.getUUID());
        try { operation.run(); } finally { if (previous == null) { INTERNAL.remove(); } else { INTERNAL.set(previous); } }
    }
    public static void maintain(ServerPlayer player, boolean active) {
        PlayerGenes data = GeneCapability.get(player); MobEffectInstance current = player.getEffect(MobEffects.REGENERATION);
        boolean owned = owned(data, current);
        if (!active) {
            if (owned) {
                CompoundTag hidden = current.save(new CompoundTag()).getCompound("HiddenEffect"); clear(data);
                internal(player, () -> {
                    if (player.removeEffect(MobEffects.REGENERATION)) {
                        CompoundTag remaining = hidden;
                        while (!remaining.isEmpty()) {
                            MobEffectInstance fallback = MobEffectInstance.load(remaining);
                            if (fallback != null && (fallback.isInfiniteDuration() || fallback.getDuration() > 0)) { player.addEffect(fallback); break; }
                            remaining = remaining.getCompound("HiddenEffect");
                        }
                    }
                });
            } else { clear(data); }
            return;
        }
        if (current == null) {
            MobEffectInstance effect = new MobEffectInstance(MobEffects.REGENERATION, 45, 1, false, true, true);
            internal(player, () -> player.addEffect(effect));
            if (player.getEffect(MobEffects.REGENERATION) == effect) { stamp(data, effect); }
        } else if (!current.isInfiniteDuration() && current.getAmplifier() < 1) {
            CompoundTag compound = new MobEffectInstance(MobEffects.REGENERATION, 45, 1, false, true, true).save(new CompoundTag());
            compound.put("HiddenEffect", current.save(new CompoundTag())); MobEffectInstance effect = MobEffectInstance.load(compound);
            if (effect != null) { internal(player, () -> player.forceAddEffect(effect, player)); if (player.getEffect(MobEffects.REGENERATION) == effect) { stamp(data, effect); } }
        } else if (!owned && !current.isInfiniteDuration() && current.getAmplifier() == 1 && renewedDuration(current.getDuration()) > 0) {
            CompoundTag compound = new MobEffectInstance(MobEffects.REGENERATION, renewedDuration(current.getDuration()), 1, false, true, true).save(new CompoundTag());
            compound.put("HiddenEffect", current.save(new CompoundTag())); MobEffectInstance effect = MobEffectInstance.load(compound);
            if (effect != null) { internal(player, () -> player.forceAddEffect(effect, player)); if (player.getEffect(MobEffects.REGENERATION) == effect) { stamp(data, effect); } }
        } else if (owned) {
            int duration = renewedDuration(current.getDuration());
            if (duration > 0) { internal(player, () -> player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1, false, true, true))); }
            if (player.getEffect(MobEffects.REGENERATION) == current) { stamp(data, current); }
        }
    }
    @SubscribeEvent public static void added(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance().getEffect() == MobEffects.REGENERATION && !player.getUUID().equals(INTERNAL.get())) { clear(GeneCapability.get(player)); }
    }
    @SubscribeEvent public static void removed(MobEffectEvent.Remove event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffect() == MobEffects.REGENERATION && !player.getUUID().equals(INTERNAL.get())) { clear(GeneCapability.get(player)); }
    }
    @SubscribeEvent public static void expired(MobEffectEvent.Expired event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance().getEffect() == MobEffects.REGENERATION) { clear(GeneCapability.get(player)); }
    }
}
