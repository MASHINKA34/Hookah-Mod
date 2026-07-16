package com.hookahmod.registry;

import com.hookahmod.HookahMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, HookahMod.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, HookahMod.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> SWEET_WATER_TYPE = FLUID_TYPES.register(
            "sweet_water",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.hookahmod.sweet_water")
                    .density(1000)
                    .viscosity(1000))
    );
    public static final DeferredHolder<FluidType, FluidType> WHITE_MONSTER_TYPE = FLUID_TYPES.register(
            "white_monster",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.hookahmod.white_monster")
                    .density(1020)
                    .viscosity(1100))
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> SWEET_WATER = FLUIDS.register(
            "sweet_water",
            () -> new BaseFlowingFluid.Source(sweetWaterProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_SWEET_WATER = FLUIDS.register(
            "flowing_sweet_water",
            () -> new BaseFlowingFluid.Flowing(sweetWaterProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> WHITE_MONSTER = FLUIDS.register(
            "white_monster",
            () -> new BaseFlowingFluid.Source(whiteMonsterProperties())
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_WHITE_MONSTER = FLUIDS.register(
            "flowing_white_monster",
            () -> new BaseFlowingFluid.Flowing(whiteMonsterProperties())
    );

    private static BaseFlowingFluid.Properties sweetWaterProperties() {
        return new BaseFlowingFluid.Properties(SWEET_WATER_TYPE, SWEET_WATER, FLOWING_SWEET_WATER);
    }

    private static BaseFlowingFluid.Properties whiteMonsterProperties() {
        return new BaseFlowingFluid.Properties(WHITE_MONSTER_TYPE, WHITE_MONSTER, FLOWING_WHITE_MONSTER);
    }

    private ModFluids() {}
}
