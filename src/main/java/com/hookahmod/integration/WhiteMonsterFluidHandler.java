package com.hookahmod.integration;

import com.hookahmod.registry.ModFluids;
import com.hookahmod.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public final class WhiteMonsterFluidHandler implements IFluidHandlerItem {

    public static final int CAPACITY = 250;
    private ItemStack container;

    public WhiteMonsterFluidHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == 0 && container.is(ModItems.WHITE_MONSTER.get())
                ? new FluidStack(ModFluids.WHITE_MONSTER.get(), CAPACITY)
                : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == 0 ? CAPACITY : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0 && stack.is(ModFluids.WHITE_MONSTER.get());
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        if (container.getCount() != 1
                || !container.is(ModItems.EMPTY_WHITE_MONSTER.get())
                || resource.getAmount() < CAPACITY
                || !resource.is(ModFluids.WHITE_MONSTER.get())) {
            return 0;
        }
        if (action.execute()) {
            container = new ItemStack(ModItems.WHITE_MONSTER.get());
        }
        return CAPACITY;
    }

    @Override
    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
        if (!resource.is(ModFluids.WHITE_MONSTER.get()) || resource.getAmount() < CAPACITY) {
            return FluidStack.EMPTY;
        }
        return drain(CAPACITY, action);
    }

    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        if (container.getCount() != 1
                || !container.is(ModItems.WHITE_MONSTER.get())
                || maxDrain < CAPACITY) {
            return FluidStack.EMPTY;
        }
        FluidStack drained = new FluidStack(ModFluids.WHITE_MONSTER.get(), CAPACITY);
        if (action.execute()) {
            container = new ItemStack(ModItems.EMPTY_WHITE_MONSTER.get());
        }
        return drained;
    }
}
