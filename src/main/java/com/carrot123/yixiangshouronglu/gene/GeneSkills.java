package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.network.GeneNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntSupplier;

public final class GeneSkills {
    public static final ResourceLocation ENDER_TELEPORT = new ResourceLocation("yixiangshouronglu", "ender_teleport");
    private static final Map<ResourceLocation, Skill> SKILLS = new LinkedHashMap<>();
    private GeneSkills() {}
    public record Skill(ResourceLocation id, ResourceLocation gene, String nameKey, IntSupplier cooldownTicks,
                        Function<ServerPlayer, String> execute, boolean usesCooldown) {
        public Skill(ResourceLocation id, ResourceLocation gene, String nameKey, IntSupplier cooldownTicks,
                     Function<ServerPlayer, String> execute) {
            this(id, gene, nameKey, cooldownTicks, execute, true);
        }
    }
    public record Result(ResourceLocation skill, String status, long readyAt) {}
    public static void register(Skill skill) { if (SKILLS.putIfAbsent(skill.id(), skill) != null) { throw new IllegalArgumentException("Duplicate gene skill " + skill.id()); } }
    public static List<Skill> available(PlayerGenes genes) {
        Set<ResourceLocation> installed = new HashSet<>();
        for (var region : GeneTypes.Region.values()) { for (int slot = 0; slot < region.slots; slot++) {
            var stack = genes.get(region, slot); var definition = GeneModuleItem.definition(stack);
            if (definition != null && definition.region() == region && stack.getCount() == 1) { installed.add(definition.id()); }
        } }
        return SKILLS.values().stream().filter(skill -> installed.contains(skill.gene())).toList();
    }
    public static long readyAt(PlayerGenes genes, ResourceLocation skill) { return Math.max(0, genes.effectRuntime().getCompound("SkillCooldowns").getLong(skill.toString())); }
    public static Result request(ServerPlayer player, ResourceLocation id) {
        PlayerGenes genes = GeneCapability.get(player); long now = player.server.overworld().getGameTime();
        CompoundTag runtime = genes.effectRuntime(); long ready = readyAt(genes, id);
        if (runtime.contains("SkillRequestAfter") && now < runtime.getLong("SkillRequestAfter")) { return new Result(id, "throttled", ready); }
        runtime.putLong("SkillRequestAfter", now + 4);
        Skill skill = SKILLS.get(id);
        if (skill == null || available(genes).stream().noneMatch(candidate -> candidate.id().equals(id))) { return new Result(id, "not_installed", ready); }
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() || player.isSleeping() || player.isPassenger() || player.isVehicle() || player.isFallFlying() || player.isChangingDimension() || player.containerMenu != player.inventoryMenu) { return new Result(id, "invalid_state", ready); }
        if (!skill.usesCooldown()) { ready = 0; }
        if (skill.usesCooldown() && now < ready) { return new Result(id, "cooldown", ready); }
        String status = skill.execute().apply(player);
        if (status.equals("ok") && skill.usesCooldown()) {
            ready = now + Math.max(1, skill.cooldownTicks().getAsInt());
            CompoundTag cooldowns = runtime.getCompound("SkillCooldowns"); cooldowns.putLong(id.toString(), ready); runtime.put("SkillCooldowns", cooldowns);
        }
        return new Result(id, status, ready);
    }
    public static void handle(ServerPlayer player, ResourceLocation id) {
        Result result = request(player, id);
        if (!result.status().equals("throttled")) { GeneNetwork.sync(player, "ok"); GeneNetwork.skillResult(player, result); }
    }
}
