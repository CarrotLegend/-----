package com.carrot123.yixiangshouronglu.registry;

import com.carrot123.yixiangshouronglu.AnomalousContainmentChronicles;
import com.carrot123.yixiangshouronglu.menu.ResonanceMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AnomalousContainmentChronicles.MOD_ID);
    public static final RegistryObject<MenuType<ResonanceMenu>> RESONANCE = MENUS.register("resonance", () -> IForgeMenuType.create(ResonanceMenu::new));

    private ModMenus() {
    }
}
