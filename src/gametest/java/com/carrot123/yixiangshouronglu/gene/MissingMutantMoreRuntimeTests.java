package com.carrot123.yixiangshouronglu.gene;

import com.carrot123.yixiangshouronglu.item.GeneModuleItem;
import com.carrot123.yixiangshouronglu.registry.ModGeneRecipes;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;
import java.util.UUID;

@GameTestHolder("acc_missing")
@PrefixGameTestTemplate(false)
public final class MissingMutantMoreRuntimeTests {
    @GameTest(template="shop_empty", timeoutTicks=100)
    public static void missingModKeepsGenesAndRejectsDomainWithoutCooldown(GameTestHelper helper) {
        helper.assertTrue(!ModList.get().isLoaded("mutantmore"), "Missing-mod profile required");
        var player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), new GameProfile(UUID.randomUUID(), "missing-mm-test"));
        var connection = new Connection(PacketFlow.SERVERBOUND); new EmbeddedChannel(connection); connection.setProtocol(ConnectionProtocol.PLAY);
        player.server.getPlayerList().placeNewPlayer(connection, player); player.setHealth(20);
        GeneCapability.get(player).set(GeneTypes.Region.NERVOUS, 0, GeneModuleItem.create(ElementalGenes.BLAZING_NERVES, GeneTypes.Quality.ALPHA));
        helper.assertTrue(GeneSkills.request(player, ElementalGenes.BLAZING_DOMAIN).status().equals("heat_unavailable"), "Missing particle refuses activation");
        helper.assertTrue(!BlazingDomain.active(player) && GeneSkills.readyAt(GeneCapability.get(player), ElementalGenes.BLAZING_DOMAIN) == 0, "Failed activation takes no cooldown");
        helper.assertTrue(helper.getLevel().getRecipeManager().getAllRecipesFor(ModGeneRecipes.TYPE.get()).stream().noneMatch(recipe -> recipe.getId().equals(ElementalGenes.BLAZING_NERVES) || recipe.getId().equals(ElementalGenes.WITHERED_RIBS)), "Optional initial recipes disabled");
        helper.succeed();
    }
}
