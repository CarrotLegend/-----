package com.carrot123.yixiangshouronglu.entity;

import com.carrot123.yixiangshouronglu.gene.GeneEffects;
import com.carrot123.yixiangshouronglu.gene.HighTierBossGenes;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class GeneDragonBreathBeam extends Entity {
    public enum Kind {
        FIRE(HighTierBossGenes.FIRE_DRAGON_GLAND, "flame_dragon_breath", "fire_dragon_blood", "firedragon_breath", DamageTypes.IN_FIRE),
        ICE(HighTierBossGenes.ICE_DRAGON_GLAND, "frost_dragon_breath", "ice_dragon_blood", "icedragon_breath", DamageTypes.FREEZE),
        LIGHTNING(HighTierBossGenes.LIGHTNING_DRAGON_GLAND, "lightning_dragon_breath", "lightning_dragon_blood", "lightningdragon_breath", DamageTypes.LIGHTNING_BOLT);

        private final ResourceLocation gene;
        private final ResourceLocation skill;
        private final ResourceLocation blood;
        private final ResourceLocation sound;
        private final ResourceKey<DamageType> damage;

        Kind(ResourceLocation gene, String skill, String blood, String sound, ResourceKey<DamageType> damage) {
            this.gene = gene;
            this.skill = new ResourceLocation("yixiangshouronglu", skill);
            this.blood = new ResourceLocation("iceandfire", blood);
            this.sound = new ResourceLocation("iceandfire", sound);
            this.damage = damage;
        }

        public ResourceLocation gene() { return gene; }
        public ResourceLocation skill() { return skill; }
        public String skillNameKey() { return "gene.yixiangshouronglu.skill." + skill.getPath(); }
        public String bloodNameKey() { return "gene.yixiangshouronglu.dragon_blood." + name().toLowerCase(java.util.Locale.ROOT); }
    }

    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(GeneDragonBreathBeam.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(GeneDragonBreathBeam.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(GeneDragonBreathBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(GeneDragonBreathBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(GeneDragonBreathBeam.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<CompoundTag> CHAIN = SynchedEntityData.defineId(GeneDragonBreathBeam.class, EntityDataSerializers.COMPOUND_TAG);
    private static final Map<UUID, GeneDragonBreathBeam> ACTIVE = new HashMap<>();
    private UUID ownerId;
    private long started;
    private int nextPayment = 20;
    private int nextPulse = 10;

    public GeneDragonBreathBeam(EntityType<? extends GeneDragonBreathBeam> type, Level level) {
        super(type, level);
        noCulling = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(OWNER, -1);
        entityData.define(KIND, 0);
        entityData.define(END_X, 0F);
        entityData.define(END_Y, 0F);
        entityData.define(END_Z, 0F);
        entityData.define(CHAIN, new CompoundTag());
    }

    public Kind kind() { return Kind.values()[entityData.get(KIND)]; }
    public Entity owner() { return level().getEntity(entityData.get(OWNER)); }
    public Vec3 direction() { return new Vec3(entityData.get(END_X), entityData.get(END_Y), entityData.get(END_Z)); }
    public int[] chain() { return entityData.get(CHAIN).getIntArray("Targets"); }

    public static String start(ServerPlayer player, Kind kind) {
        validate(player);
        if (ACTIVE.containsKey(player.getUUID())) { return "breath_already_active"; }
        if (!ForgeRegistries.ITEMS.containsKey(kind.blood)) { return "dragon_blood_unavailable"; }
        int slot = bloodSlot(player, kind);
        if (slot < 0) { return "dragon_blood_missing"; }
        if (!GeneEffects.installed(player).containsKey(kind.gene)) { return "not_installed"; }
        GeneDragonBreathBeam beam = new GeneDragonBreathBeam(ModEntities.GENE_DRAGON_BREATH_BEAM.get(), player.level());
        beam.ownerId = player.getUUID();
        beam.entityData.set(OWNER, player.getId());
        beam.entityData.set(KIND, kind.ordinal());
        beam.started = player.server.overworld().getGameTime();
        beam.updateLine(player);
        ItemStack before = player.getInventory().getItem(slot).copy();
        if (!player.serverLevel().addFreshEntity(beam)) { return "breath_spawn_failed"; }
        if (!beam.valid(player) || !ItemStack.matches(before, player.getInventory().getItem(slot)) || ACTIVE.containsKey(player.getUUID())) {
            beam.discard();
            return "invalid_state";
        }
        consume(player, slot);
        ACTIVE.put(player.getUUID(), beam);
        beam.playBreathSound();
        beam.pulse(player);
        return "ok";
    }

    private static int bloodSlot(ServerPlayer player, Kind kind) {
        if (!ForgeRegistries.ITEMS.containsKey(kind.blood)) { return -1; }
        var blood = ForgeRegistries.ITEMS.getValue(kind.blood);
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == blood) { return slot; }
        }
        return -1;
    }

    private static void consume(ServerPlayer player, int slot) {
        player.getInventory().getItem(slot).shrink(1);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != player.inventoryMenu) { player.containerMenu.broadcastChanges(); }
    }

    private boolean valid(ServerPlayer player) {
        return player != null && player.getUUID().equals(ownerId) && GeneDragonFireball.validOwner(player, level())
                && !player.isChangingDimension() && GeneEffects.installed(player).containsKey(kind().gene);
    }

    public static void validate(ServerPlayer player) {
        GeneDragonBreathBeam beam = ACTIVE.get(player.getUUID());
        if (beam != null && (beam.isRemoved() || !beam.valid(player))) { beam.discard(); }
    }

    public static void stop(ServerPlayer player) {
        GeneDragonBreathBeam beam = ACTIVE.get(player.getUUID());
        if (beam != null) { beam.discard(); }
    }

    public static void stop(ServerPlayer player, ResourceLocation gene) {
        GeneDragonBreathBeam beam = ACTIVE.get(player.getUUID());
        if (beam != null && beam.kind().gene.equals(gene)) { beam.discard(); }
    }

    public static void stopAll() {
        List.copyOf(ACTIVE.values()).forEach(Entity::discard);
        ACTIVE.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (!level().isClientSide && ownerId != null) { ACTIVE.remove(ownerId, this); }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) { return; }
        if (!(owner() instanceof ServerPlayer player) || !valid(player)) { discard(); return; }
        long elapsed = player.server.overworld().getGameTime() - started;
        if (elapsed < 0 || elapsed >= 80) { discard(); return; }
        if (elapsed >= nextPayment) {
            int slot = bloodSlot(player, kind());
            if (slot < 0) { discard(); return; }
            consume(player, slot);
            nextPayment += 20;
            playBreathSound();
        }
        updateLine(player);
        if (elapsed >= nextPulse) {
            pulse(player);
            nextPulse += 10;
        } else if (elapsed % 10 == 4) {
            entityData.set(CHAIN, new CompoundTag());
        }
    }

    private void updateLine(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        setPos(start);
        Vec3 end = HighTierBossGenes.clippedEnd(player, start, start.add(player.getLookAngle().scale(12)));
        Vec3 direction = end.subtract(start);
        entityData.set(END_X, (float) direction.x);
        entityData.set(END_Y, (float) direction.y);
        entityData.set(END_Z, (float) direction.z);
    }

    private boolean hit(ServerPlayer player, LivingEntity target, float damage) {
        if (!HighTierBossGenes.canTarget(player, target)
                || !target.hurt(HighTierBossGenes.source(player, this, kind().damage), damage)) { return false; }
        if (kind() == Kind.FIRE) { target.setSecondsOnFire(5); }
        if (kind() == Kind.ICE) { target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), player); }
        return true;
    }

    private void pulse(ServerPlayer player) {
        ItemStack module = GeneEffects.installed(player).get(kind().gene);
        if (module == null) { discard(); return; }
        float damage = 4 + 2 * GeneModuleItem.read(module).quality().ordinal();
        List<LivingEntity> targets = HighTierBossGenes.targets(player, position().add(direction()), 0.5, true);
        Set<Integer> visited = new HashSet<>();
        LivingEntity first = null;
        for (LivingEntity target : targets) {
            visited.add(target.getId());
            if (hit(player, target, damage) && first == null) { first = target; }
        }
        List<Integer> links = new ArrayList<>();
        if (kind() == Kind.LIGHTNING && first != null) {
            LivingEntity previous = first;
            links.add(first.getId());
            for (int hop = 0; hop < 3; hop++) {
                Vec3 start = previous.getBoundingBox().getCenter();
                LivingEntity next = level().getEntitiesOfClass(LivingEntity.class, previous.getBoundingBox().inflate(4),
                        target -> !visited.contains(target.getId()) && HighTierBossGenes.canTarget(player, target)
                                && target.getBoundingBox().getCenter().distanceToSqr(start) <= 16
                                && HighTierBossGenes.unobstructed(player, start, target.getBoundingBox().getCenter()))
                        .stream().min(Comparator.comparingDouble((LivingEntity target) -> target.getBoundingBox().getCenter().distanceToSqr(start))
                                .thenComparingInt(Entity::getId)).orElse(null);
                if (next == null) { break; }
                visited.add(next.getId());
                if (!hit(player, next, damage)) { break; }
                links.add(next.getId());
                previous = next;
            }
        }
        CompoundTag chain = new CompoundTag();
        chain.putIntArray("Targets", links);
        entityData.set(CHAIN, chain);
    }

    private void playBreathSound() {
        if (!ForgeRegistries.SOUND_EVENTS.containsKey(kind().sound)) { return; }
        var sound = ForgeRegistries.SOUND_EVENTS.getValue(kind().sound);
        if (sound != null) { level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.PLAYERS, 1, 1); }
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
