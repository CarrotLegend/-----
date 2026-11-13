package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.menu.GeneResearchTableMenu;

import com.carrot123.yixiangshouronglu.menu.GeneModificationMenu;
import com.carrot123.yixiangshouronglu.menu.CarrotKingShopMenu;
import com.carrot123.yixiangshouronglu.menu.GeneSynthesizerMenu;
import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.menu.ContainmentChamberMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(
                    ForgeRegistries.MENU_TYPES,
                    AnomalousContainmentChronicles.MOD_ID
            );

    public static final RegistryObject<MenuType<ContainmentChamberMenu>> CONTAINMENT_CHAMBER =
            MENUS.register(
                    "containment_chamber",
                    () -> IForgeMenuType.create(ContainmentChamberMenu::new)
            );

    public static final RegistryObject<MenuType<GeneSynthesizerMenu>> GENE_SYNTHESIZER = MENUS.register("gene_synthesizer", () -> IForgeMenuType.create(GeneSynthesizerMenu::new));
    public static final RegistryObject<MenuType<GeneResearchTableMenu>> GENE_RESEARCH_TABLE = MENUS.register("gene_research_table", () -> IForgeMenuType.create(GeneResearchTableMenu::new));
    public static final RegistryObject<MenuType<GeneModificationMenu>> GENE_MODIFICATION_CHAMBER = MENUS.register("gene_modification_chamber", () -> IForgeMenuType.create(GeneModificationMenu::new));

    private ModMenus() {
    }
    public static final RegistryObject<MenuType<CarrotKingShopMenu>> CARROT_KING_SHOP = MENUS.register("carrot_king_shop", () -> IForgeMenuType.create(CarrotKingShopMenu::new));
}
