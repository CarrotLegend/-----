package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.block.entity.GeneSynthesizerBlockEntity;
import com.carrot123.yixiangshouronglu.entity.GeneDragonBreathCloud;
import com.carrot123.yixiangshouronglu.entity.GeneDragonFireball;
import com.carrot123.yixiangshouronglu.item.ContainmentExtractItem;
import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.registry.ModBlocks;
import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("yixiangshouronglu")
@PrefixGameTestTemplate(false)
public final class DragonGeneRuntimeTests {
    private static ServerPlayer player(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "dragon-test"));
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        player.server.getPlayerList().placeNewPlayer(connection, player);
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        player.moveTo(pos.getX() + 0.5, pos.getY() + 100, pos.getZ() + 0.5, 0, 0);
        player.setHealth(20);
        return player;
    }

    private static void install(ServerPlayer player, ResourceLocation gene, GeneTypes.Quality quality) {
        GeneCapability.get(player).set(GeneDefinitions.get(gene).region(), 0, GeneModuleItem.create(gene, quality));
    }

    @GameTest(template = "shop_empty")
    public static void attributesAndLevitation(GameTestHelper helper) {
        var player = player(helper);
        var genes = GeneCapability.get(player);
        var lifesteal = ForgeRegistries.ATTRIBUTES.getValue(DragonGenes.LIFE_STEAL);
        var overheal = ForgeRegistries.ATTRIBUTES.getValue(RegenerativeCells.OVERHEAL);
        double baseAttack = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double baseHealth = player.getMaxHealth();
        double baseArmor = player.getAttributeValue(Attributes.ARMOR);
        double baseLifeSteal = player.getAttributeValue(lifesteal);
        double baseOverheal = player.getAttributeValue(overheal);
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100));
        for (var quality : GeneTypes.Quality.values()) {
            int tier = quality.ordinal() + 1;
            install(player, DragonGenes.WITHER_CORE, quality);
            install(player, DragonGenes.ENDER_DRAGON_BRAIN, quality);
            install(player, DragonGenes.BERSERKER_TENDONS, quality);
            install(player, DragonGenes.VOID_CARAPACE, quality);
            GeneEffects.refresh(player);
            GeneEffects.refresh(player);
            helper.assertTrue(player.getMaxHealth() == baseHealth + 40 + 15 * tier, "Dragon health bonuses applied once");
            helper.assertTrue(player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack + 8 + 5 * tier, "Attack bonuses applied once");
            helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == baseArmor + 1 + 2 * tier, "Armor matches quality");
            helper.assertTrue(Math.abs(player.getAttributeValue(lifesteal) - baseLifeSteal - 0.10 * tier) < 1.0E-8, "Life steal additive fraction");
            helper.assertTrue(Math.abs(player.getAttributeValue(overheal) - baseOverheal - 0.05 * tier) < 1.0E-8, "Overheal additive fraction");
            helper.assertTrue(!player.hasEffect(MobEffects.LEVITATION) && !player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100)), "Levitation cleared and denied");
        }
        helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100)), "Unrelated effect accepted");
        for (var region : GeneTypes.Region.values()) { genes.set(region, 0, ItemStack.EMPTY); }
        GeneEffects.refresh(player);
        helper.assertTrue(player.getMaxHealth() == baseHealth && player.getAttributeValue(Attributes.ATTACK_DAMAGE) == baseAttack, "Last dragon gene removes bonuses");
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR) == baseArmor && player.getAttributeValue(lifesteal) == baseLifeSteal
                && player.getAttributeValue(overheal) == baseOverheal, "Uninstall clears modifiers");
        helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100)), "Levitation works after removal");
        helper.assertTrue(GeneCategories.active(Map.of(GeneCategories.DRAGON, 2, GeneCategories.MUTANT, 3,
                GeneCategories.UNDEAD, 3, GeneCategories.ELEMENTAL, 3)).equals(java.util.Set.of(GeneCategories.DRAGON)), "Only dragon active below four");
        helper.succeed();
    }

    @GameTest(template = "shop_empty")
    public static void berserkerUsesOneRollPerValidHit(GameTestHelper helper) {
        var player = player(helper);
        var tick = new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, player.server);
        for (var quality : GeneTypes.Quality.values()) {
            install(player, DragonGenes.BERSERKER_TENDONS, quality);
            player.getRandom().setSeed(440);
            var expected = RandomSource.create(440);
            for (int i = 0; i < 100; i++) {
                player.removeEffect(MobEffects.DAMAGE_BOOST);
                var event = new LivingDamageEvent(player, player.damageSources().fall(), 1);
                DragonGenes.damaged(event);
                DragonGenes.damaged(event);
                DragonGenes.tick(tick);
                var effect = player.getEffect(MobEffects.DAMAGE_BOOST);
                helper.assertTrue((effect != null) == (expected.nextInt(100) < new int[]{13, 36, 39}[quality.ordinal()]), "One quality-specific roll");
                if (effect != null) {
                    helper.assertTrue(effect.getAmplifier() == 1 && effect.getDuration() == 200, "Default Strength II duration");
                }
            }
        }
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        var canceled = new LivingDamageEvent(player, player.damageSources().fall(), 1);
        DragonGenes.damaged(canceled);
        canceled.setCanceled(true);
        DragonGenes.damaged(new LivingDamageEvent(player, player.damageSources().fall(), 0));
        DragonGenes.tick(tick);
        helper.assertTrue(!player.hasEffect(MobEffects.DAMAGE_BOOST), "Canceled and zero damage rejected");
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 2));
        for (int i = 0; i < 100; i++) { DragonGenes.damaged(new LivingDamageEvent(player, player.damageSources().fall(), 1)); }
        DragonGenes.tick(tick);
        helper.assertTrue(player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() == 2, "Stronger effect retained");
        helper.succeed();
    }

    @GameTest(template = "shop_empty")
    public static void breathCollisionPulsesAndCooldown(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = player(helper);
        player.setPos(player.position().add(0, 50, 0));
        install(player, DragonGenes.ENDER_DRAGON_BRAIN, GeneTypes.Quality.ALPHA);
        var crit = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("attributeslib", "crit_damage"));
        player.getAttribute(crit).setBaseValue(1);
        var area = new AABB(player.position(), player.position()).inflate(20);
        var target = EntityType.ZOMBIE.create(level);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        target.setPos(player.getX(), player.getY(), player.getZ() + 4);
        helper.assertTrue(level.addFreshEntity(target), "Target spawn accepted");
        long now = player.server.overworld().getGameTime();
        var result = GeneSkills.request(player, DragonGenes.DRAGON_BREATH);
        helper.assertTrue(result.status().equals("ok") && result.readyAt() == now + 100, "Successful spawn starts default cooldown");
        var projectile = level.getEntitiesOfClass(GeneDragonFireball.class, area, value -> value.getOwner() == player).get(0);
        helper.assertTrue(projectile.zPower > 0 && Math.abs(projectile.xPower) < 1.0E-6, "Look direction applied");
        for (int i = 0; i < 20 && !projectile.isRemoved(); i++) { level.tickNonPassenger(projectile); }
        helper.assertTrue(projectile.isRemoved(), "Projectile must hit target by tick 20, position " + projectile.position() + ", target " + target.position());
        var clouds = level.getEntitiesOfClass(GeneDragonBreathCloud.class, area, value -> value.getOwner() == player);
        helper.assertTrue(clouds.size() == 1, "One cloud per impact at " + projectile.position() + ", target at " + target.position()
                + ", chunk loaded " + level.hasChunkAt(projectile.blockPosition()));
        helper.assertTrue(target.getHealth() == 980, "Entity impact expected health 980, got " + target.getHealth()
                + ", impact cloud at " + clouds.get(0).position() + ", target at " + target.position());
        assertSource(helper, target, player, projectile);
        var cloud = clouds.get(0);
        target.setPos(cloud.position());
        player.setPos(cloud.position());
        float ownerHealth = player.getHealth();
        for (int age = 1; age <= 620; age++) {
            target.invulnerableTime = 0;
            float oldHealth = target.getHealth();
            level.tickNonPassenger(cloud);
            boolean pulse = age >= 20 && age < 620 && age % 20 == 0;
            helper.assertTrue(target.getHealth() == oldHealth - (pulse ? 8 : 0), "Only one eight-point pulse per second");
            if (pulse) { assertSource(helper, target, player, cloud); }
        }
        helper.assertTrue(cloud.isRemoved() && target.getHealth() == 740 && player.getHealth() == ownerHealth, "Thirty pulses, owner unharmed");
        var saved = new PlayerGenes();
        saved.load(GeneCapability.get(player).save());
        helper.assertTrue(GeneSkills.readyAt(saved, DragonGenes.DRAGON_BREATH) == now + 100, "Cooldown survives NBT");
        target.discard();
        var wall = BlockPos.containing(player.getEyePosition().add(0, 0, 4));
        var old = level.getBlockState(wall);
        level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        try {
            helper.assertTrue(GeneDragonFireball.fire(player).equals("ok"), "Second isolated projectile spawns");
            var againstWall = level.getEntitiesOfClass(GeneDragonFireball.class, area, value -> value.getOwner() == player).get(0);
            for (int i = 0; i < 20 && !againstWall.isRemoved(); i++) { level.tickNonPassenger(againstWall); }
            helper.assertTrue(againstWall.isRemoved() && level.getBlockState(wall).is(Blocks.STONE), "Block collision leaves block intact");
            var second = level.getEntitiesOfClass(GeneDragonBreathCloud.class, area, value -> value.getOwner() == player);
            helper.assertTrue(second.size() == 1, "Block collision creates one cloud");
            var protectedPlayer = player(helper);
            protectedPlayer.setPos(second.get(0).position());
            boolean originalPvp = player.server.isPvpAllowed();
            player.server.setPvpAllowed(false);
            try {
                for (int i = 0; i < 20; i++) { level.tickNonPassenger(second.get(0)); }
                helper.assertTrue(protectedPlayer.getHealth() == 20, "Disabled PVP protects cloud targets");
            } finally { player.server.setPvpAllowed(originalPvp); }
            player.setHealth(0);
            level.tickNonPassenger(second.get(0));
            helper.assertTrue(second.get(0).isRemoved(), "Cloud stops when owner dies");
        } finally { level.setBlockAndUpdate(wall, old); }
        helper.succeed();
    }

    private static void assertSource(GameTestHelper helper, LivingEntity target, ServerPlayer owner, net.minecraft.world.entity.Entity direct) {
        var source = target.getLastDamageSource();
        helper.assertTrue(source != null && source.is(DamageTypes.INDIRECT_MAGIC) && source.getEntity() == owner && source.getDirectEntity() == direct, "Damage owner and direct source");
    }

    @GameTest(template = "shop_empty")
    public static void newSynthesisRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        var sources = Map.of(DragonGenes.WITHER_CORE, "minecraft:wither", DragonGenes.ENDER_DRAGON_BRAIN, "minecraft:ender_dragon",
                DragonGenes.BERSERKER_TENDONS, "mutantmore:mutant_hoglin", DragonGenes.VOID_CARAPACE, "mutantmore:mutant_shulker");
        for (var gene : List.of(DragonGenes.WITHER_CORE, DragonGenes.ENDER_DRAGON_BRAIN, DragonGenes.BERSERKER_TENDONS, DragonGenes.VOID_CARAPACE)) {
            var recipe = level.getRecipeManager().getAllRecipesFor(ModGeneRecipes.TYPE.get()).stream().filter(value -> value.getId().equals(gene)).findFirst().orElseThrow();
            var state = ModBlocks.GENE_SYNTHESIZER.get().defaultBlockState();
            var machine = new GeneSynthesizerBlockEntity(helper.absolutePos(BlockPos.ZERO), state);
            machine.setLevel(level);
            var inputs = new SimpleContainer(4);
            for (var input : recipe.inputs()) {
                ItemStack stack = input.slot() == 0 ? ContainmentExtractItem.create(sources.get(gene)) : input.ingredient().getItems()[0].copy();
                stack.setCount(input.count());
                int slot = (input.slot() + 1) % 4;
                inputs.setItem(slot, stack);
                machine.inventory.setStackInSlot(slot, stack.copy());
            }
            helper.assertTrue(recipe.matchesInputs(inputs, GeneTypes.Mode.SYNTHESIS) && recipe.duration() == 600, "Correct source and shuffled slots");
            var wrong = ContainmentExtractItem.create("yixiangshouronglu:young_ender_dragon");
            wrong.setCount(4);
            inputs.setItem(1, wrong);
            helper.assertTrue(!recipe.matchesInputs(inputs, GeneTypes.Mode.SYNTHESIS), "Young dragon is no substitute");
            for (int tick = 0; tick < 599; tick++) { GeneSynthesizerBlockEntity.tick(level, machine.getBlockPos(), state, machine); }
            helper.assertTrue(machine.inventory.getStackInSlot(4).isEmpty(), "No result before 600 ticks");
            GeneSynthesizerBlockEntity.tick(level, machine.getBlockPos(), state, machine);
            var data = GeneModuleItem.read(machine.inventory.getStackInSlot(4));
            helper.assertTrue(data != null && data.id().equals(gene) && data.quality() == GeneTypes.Quality.ALPHA, "Correct alpha result");
            for (int slot = 0; slot < 4; slot++) { helper.assertTrue(machine.inventory.getStackInSlot(slot).isEmpty(), "Exact material consumption"); }
        }
        helper.succeed();
    }
}
