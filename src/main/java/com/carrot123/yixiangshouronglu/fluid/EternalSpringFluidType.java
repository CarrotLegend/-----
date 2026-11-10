package com.carrot123.yixiangshouronglu.fluid;

import com.carrot123.yixiangshouronglu.client.EternalSpringFluidClient;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

public final class EternalSpringFluidType extends FluidType {
    public EternalSpringFluidType(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(EternalSpringFluidClient.create());
    }
}
