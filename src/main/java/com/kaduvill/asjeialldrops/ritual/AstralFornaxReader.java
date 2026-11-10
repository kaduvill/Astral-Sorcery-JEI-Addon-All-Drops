package com.kaduvill.asjeialldrops.ritual;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import hellfirepvp.astralsorcery.common.base.WorldMeltables;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectFornax;
import hellfirepvp.astralsorcery.common.util.ItemUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/** AS 1.10.27's placed-block melting precedence, without creating a world or running effects. */
public final class AstralFornaxReader {
    public FornaxRecipes.Snapshot read(boolean includeCorrupted) {
        if (!CEffectFornax.enabled) return null;
        List<FornaxRecipes.Recipe> recipes = normal();
        if (includeCorrupted) corrupted(recipes);
        return new FornaxRecipes.Snapshot(CEffectFornax.searchRange, CEffectFornax.failChance, recipes);
    }

    private List<FornaxRecipes.Recipe> normal() {
        List<Melting> groups = new ArrayList<>();
        List<Block> blocks = new ArrayList<>();
        for (Block block : Block.REGISTRY) blocks.add(block);
        blocks.sort(Comparator.comparing(block -> String.valueOf(block.getRegistryName())));
        for (Block block : blocks) {
            try {
                for (IBlockState state : block.getBlockState().getValidStates()) {
                    ItemStack furnaceInput = ItemUtils.createBlockStack(state);
                    // Keep itemless normal world interactions out of inventory recipe browsing.
                    if (furnaceInput.isEmpty()) continue;
                    ItemStack input = displayStack(furnaceInput);
                    WorldMeltables builtin = builtin(state);
                    if (builtin != null) {
                        Melting group = null;
                        for (Melting existing : groups) if (existing.builtin == builtin) { group = existing; break; }
                        if (group == null) {
                            group = new Melting(builtin, builtin.getMeltResultState(), ItemStack.EMPTY);
                            groups.add(group);
                        }
                        group.add(input, furnaceInput);
                    } else {
                        ItemStack output = FurnaceRecipes.instance().getSmeltingResult(furnaceInput);
                        if (output.isEmpty()) continue;
                        IBlockState outputState = ItemUtils.createBlockState(output);
                        Melting group = null;
                        for (Melting existing : groups) {
                            if (existing.builtin == null && ItemStack.areItemStacksEqual(existing.furnaceOutput, output)
                                    && java.util.Objects.equals(existing.outputState, outputState)) {
                                group = existing;
                                break;
                            }
                        }
                        if (group == null) {
                            group = new Melting(null, outputState, output);
                            groups.add(group);
                        }
                        group.add(input, furnaceInput);
                    }
                }
            } catch (RuntimeException | LinkageError failure) {
                ASJeiAllDrops.LOGGER.warn("Cannot display Fornax melting for {}", block.getRegistryName(), failure);
            }
        }
        List<FornaxRecipes.Recipe> result = new ArrayList<>();
        // Present built-in melting first, followed by loaded furnace conversions.
        groups.sort(Comparator.comparing(group -> group.builtin == null));
        for (Melting group : groups) result.add(group.finish());
        return result;
    }

    private static WorldMeltables builtin(IBlockState state) {
        for (WorldMeltables melt : WorldMeltables.values()) {
            // In this pinned AS release, enum handlers only test their BlockStateCheck.
            if (melt.isMeltable(null, BlockPos.ORIGIN, state)) return melt;
        }
        return null;
    }

    private static ItemStack displayStack(ItemStack stack) {
        ItemStack display = stack.copy();
        // E.g. lapis ore's drop metadata is dye metadata (4), while its inventory item is meta 0.
        if (!display.isEmpty() && !display.getHasSubtypes()) display.setItemDamage(0);
        return display;
    }

    private static void corrupted(List<FornaxRecipes.Recipe> recipes) {
        recipes.add(freeze(FluidRegistry.WATER, Blocks.PACKED_ICE));
        recipes.add(freeze(FluidRegistry.LAVA, Blocks.OBSIDIAN));
        List<Fluid> fluids = new ArrayList<>(FluidRegistry.getRegisteredFluids().values());
        fluids.sort(Comparator.comparing(Fluid::getName));
        List<FluidStack> variable = new ArrayList<>();
        for (Fluid registered : fluids) {
            Block block = registered.getBlock();
            if (!(block instanceof BlockFluidBase)) continue;
            Fluid fluid = ((BlockFluidBase) block).getFluid();
            if (fluid == null || fluid != registered
                    || !block.getDefaultState().getPropertyKeys().contains(BlockFluidBase.LEVEL)) continue;
            if (!hasFixedWorldTemperature(fluid)) {
                variable.add(new FluidStack(fluid, 1000));
                continue;
            }
            FornaxRecipes.OutputTemperature band = FornaxRecipes.temperatureBand(fluid.getTemperature());
            recipes.add(freeze(fluid, band == FornaxRecipes.OutputTemperature.PACKED_ICE ? Blocks.PACKED_ICE
                    : band == FornaxRecipes.OutputTemperature.OBSIDIAN ? Blocks.OBSIDIAN : Blocks.STONE));
        }
        recipes.add(new FornaxRecipes.Recipe(true, false, FornaxRecipes.Output.BLOCK, "air",
                Collections.emptyList(), Collections.emptyList(), new ItemStack(Blocks.ICE), null, ItemStack.EMPTY));
        recipes.add(new FornaxRecipes.Recipe(true, false, FornaxRecipes.Output.AIR, "fire",
                Collections.emptyList(), Collections.emptyList(), ItemStack.EMPTY, null, ItemStack.EMPTY));
        // A location-dependent override cannot be evaluated safely during client registration.
        // Retain fluid U navigation to the rule without advertising a particular block result.
        recipes.add(new FornaxRecipes.Recipe(true, false, FornaxRecipes.Output.TEMPERATURE_RULE, null,
                Collections.emptyList(), variable, ItemStack.EMPTY, null, ItemStack.EMPTY));
    }

    static boolean hasFixedWorldTemperature(Fluid fluid) {
        try {
            return fluid.getClass().getMethod("getTemperature", World.class, BlockPos.class)
                    .getDeclaringClass() == Fluid.class;
        } catch (ReflectiveOperationException | SecurityException failure) {
            return false;
        }
    }

    private static FornaxRecipes.Recipe freeze(Fluid fluid, Block result) {
        return new FornaxRecipes.Recipe(true, false, FornaxRecipes.Output.BLOCK, null,
                Collections.emptyList(), Collections.singletonList(new FluidStack(fluid, 1000)),
                new ItemStack(result), null, ItemStack.EMPTY);
    }

    private static final class Melting {
        final WorldMeltables builtin;
        final IBlockState outputState;
        final ItemStack furnaceOutput;
        final List<FornaxRecipes.Input> inputs = new ArrayList<>();

        Melting(WorldMeltables builtin, IBlockState outputState, ItemStack furnaceOutput) {
            this.builtin = builtin;
            this.outputState = outputState;
            this.furnaceOutput = furnaceOutput.copy();
        }

        void add(ItemStack display, ItemStack furnace) {
            for (FornaxRecipes.Input existing : inputs) if (existing.matches(display, furnace)) return;
            inputs.add(new FornaxRecipes.Input(display, furnace));
        }

        FornaxRecipes.Recipe finish() {
            FluidStack fluid = null;
            ItemStack item = furnaceOutput;
            FornaxRecipes.Output kind = FornaxRecipes.Output.DROP;
            if (outputState != null) {
                Fluid f = FluidRegistry.lookupFluidForBlock(outputState.getBlock());
                if (f != null) {
                    fluid = new FluidStack(f, 1000);
                    item = ItemStack.EMPTY;
                    kind = FornaxRecipes.Output.FLUID;
                } else {
                    item = displayStack(ItemUtils.createBlockStack(outputState));
                    kind = FornaxRecipes.Output.BLOCK;
                }
            }
            return new FornaxRecipes.Recipe(false, builtin != null, kind, null,
                    inputs, Collections.emptyList(), item, fluid, furnaceOutput);
        }
    }
}
