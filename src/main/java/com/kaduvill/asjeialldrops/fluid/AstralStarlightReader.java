package com.kaduvill.asjeialldrops.fluid;

import com.kaduvill.asjeialldrops.ASJeiAllDrops;
import com.kaduvill.asjeialldrops.fluid.StarlightRecipes.Hint;
import com.kaduvill.asjeialldrops.fluid.StarlightRecipes.Kind;
import com.kaduvill.asjeialldrops.fluid.StarlightRecipes.Output;
import com.kaduvill.asjeialldrops.fluid.StarlightRecipes.Recipe;
import hellfirepvp.astralsorcery.common.block.BlockCustomSandOre;
import hellfirepvp.astralsorcery.common.block.BlockInfusedWood;
import hellfirepvp.astralsorcery.common.block.fluid.FluidBlockLiquidStarlight;
import hellfirepvp.astralsorcery.common.constellation.ConstellationRegistry;
import hellfirepvp.astralsorcery.common.constellation.IConstellation;
import hellfirepvp.astralsorcery.common.constellation.IWeakConstellation;
import hellfirepvp.astralsorcery.common.data.config.Config;
import hellfirepvp.astralsorcery.common.item.crystal.CrystalProperties;
import hellfirepvp.astralsorcery.common.item.crystal.ItemCelestialCrystal;
import hellfirepvp.astralsorcery.common.item.crystal.ItemTunedCelestialCrystal;
import hellfirepvp.astralsorcery.common.item.crystal.ToolCrystalProperties;
import hellfirepvp.astralsorcery.common.item.crystal.base.ItemRockCrystalBase;
import hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase;
import hellfirepvp.astralsorcery.common.item.tool.ItemCrystalSword;
import hellfirepvp.astralsorcery.common.item.tool.ItemCrystalToolBase;
import hellfirepvp.astralsorcery.common.lib.BlocksAS;
import hellfirepvp.astralsorcery.common.lib.ItemsAS;
import hellfirepvp.astralsorcery.common.util.ItemUtils;
import hellfirepvp.astralsorcery.common.util.OreDictAlias;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

/** Describes the inspected AS 1.10.27 rules without executing world actions or random generators. */
public final class AstralStarlightReader {
    private static final int DISPLAY_VOLUME = 1000;
    private static final int CONTACT_TEMPERATURE = 300;

    public List<Recipe> read(Collection<ItemStack> registeredItems) {
        Flags flags = new Flags();
        Fluid starlight = FluidRegistry.getFluid("astralsorcery.liquidstarlight");
        if (starlight == null) return Collections.emptyList();
        List<ItemStack> logs = new ArrayList<>();
        List<ItemStack> glowstone = new ArrayList<>();
        Map<StackKey, ItemStack> crystals = new LinkedHashMap<>();
        Map<StackKey, ItemStack> tools = new LinkedHashMap<>();
        String glowstoneOre = OreDictAlias.ITEM_GLOWSTONE_DUST;
        List<IWeakConstellation> constellations = new ArrayList<>();
        if (flags.crystals || flags.celestial) {
            for (IConstellation constellation : new ArrayList<>(ConstellationRegistry.getAllConstellations())) {
                if (constellation instanceof IWeakConstellation) constellations.add((IWeakConstellation) constellation);
            }
        }
        for (ItemStack candidate : registeredItems) {
            if (candidate == null || candidate.isEmpty()) continue;
            try {
                // The Ore Dictionary test only reads IDs. Copy selected stacks, not the full JEI registry.
                if (flags.wood && ItemUtils.hasOreName(candidate, "logWood")) logs.add(single(candidate));
                if (flags.crystals && ItemUtils.hasOreName(candidate, glowstoneOre)) glowstone.add(single(candidate));
                if ((flags.crystals || flags.celestial) && candidate.getItem() instanceof ItemRockCrystalBase) {
                    ItemStack crystal = crystalTemplate(candidate);
                    if (crystal != null) addCrystalVariants(crystals, crystal, constellations);
                }
                if (flags.tools && isTool(candidate)) {
                    ItemStack tool = toolTemplate(candidate);
                    if (tool != null) tools.putIfAbsent(new StackKey(tool), tool);
                }
            } catch (RuntimeException | LinkageError failure) {
                ASJeiAllDrops.LOGGER.warn("Cannot describe AS starlight input {}", candidate.getItem().getRegistryName(), failure);
            }
        }
        List<Recipe> result = new ArrayList<>();
        List<ItemStack> crystalInputs = new ArrayList<>(crystals.values());
        if (flags.wood && !logs.isEmpty()) {
            addFixed(result, Kind.INFUSED_WOOD, Collections.singletonList(distinct(logs)), starlight,
                    BlockInfusedWood.WoodType.RAW.asStack(), false, false, 0,
                    Collections.singletonList(Collections.emptyList()));
        }
        if (flags.celestial && !crystalInputs.isEmpty()) {
            Item dust = Item.getByNameOrId("astralsorcery:itemcraftingcomponent");
            if (dust != null) addFixed(result, Kind.CELESTIAL_CLUSTER,
                    Arrays.asList(crystalInputs, Collections.singletonList(new ItemStack(dust, 1, 2))), starlight,
                    block(BlocksAS.celestialCrystals), true, true, 30,
                    Arrays.asList(Collections.emptyList(), hints("one_needed", "dust_consumption")));
        }
        if (flags.crystals && !crystalInputs.isEmpty()) {
            if (!glowstone.isEmpty()) addFixed(result, Kind.GEM_CLUSTER,
                    Arrays.asList(crystalInputs, distinct(glowstone)), starlight, block(BlocksAS.gemCrystals),
                    true, true, 60, Arrays.asList(Collections.emptyList(), hints("one_needed", "whole_stack")));
            for (ItemStack crystal : crystalInputs) {
                try {
                    addCrystalProcesses(result, crystal, starlight, flags.duplicates);
                } catch (RuntimeException | LinkageError failure) {
                    ASJeiAllDrops.LOGGER.warn("Cannot describe AS crystal treatment {}", crystal.getItem().getRegistryName(), failure);
                }
            }
        }
        for (ItemStack tool : tools.values()) {
            try {
                addToolProcess(result, tool, starlight);
            } catch (RuntimeException | LinkageError failure) {
                ASJeiAllDrops.LOGGER.warn("Cannot describe AS crystal tool treatment {}", tool.getItem().getRegistryName(), failure);
            }
        }
        addContacts(result, starlight, flags);
        return groupCrystalTreatments(result);
    }

    private static List<Recipe> groupCrystalTreatments(List<Recipe> recipes) {
        Map<Kind, List<Recipe>> rockGroups = new EnumMap<>(Kind.class);
        Map<Kind, List<Recipe>> celestialGroups = new EnumMap<>(Kind.class);
        for (Recipe recipe : recipes) {
            if (isCrystalTreatment(recipe.kind)) {
                Map<Kind, List<Recipe>> groups = isCelestialCrystal(recipe.getItemInputs().get(0).get(0))
                        ? celestialGroups : rockGroups;
                groups.computeIfAbsent(recipe.kind, key -> new ArrayList<>()).add(recipe);
            }
        }
        List<Recipe> grouped = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (isCrystalTreatment(recipe.kind)) {
                Map<Kind, List<Recipe>> groups = isCelestialCrystal(recipe.getItemInputs().get(0).get(0))
                        ? celestialGroups : rockGroups;
                List<Recipe> variants = groups.remove(recipe.kind);
                if (variants != null) grouped.add(Recipe.grouped(variants));
            } else grouped.add(recipe);
        }
        return Collections.unmodifiableList(grouped);
    }

    private static boolean isCrystalTreatment(Kind kind) {
        return kind == Kind.CRYSTAL_GROWTH || kind == Kind.CRYSTAL_REPAIR || kind == Kind.CRYSTAL_SPLIT;
    }

    private static boolean isCelestialCrystal(ItemStack crystal) {
        return crystal.getItem() instanceof ItemCelestialCrystal
                || crystal.getItem() instanceof ItemTunedCelestialCrystal;
    }

    private static void addFixed(List<Recipe> result, Kind kind, List<List<ItemStack>> items, Fluid starlight,
            ItemStack output, boolean consumed, boolean source, int seconds, List<List<Hint>> itemHints) {
        if (output.isEmpty()) return;
        List<Hint> arrow = new ArrayList<>();
        arrow.add(new Hint("drop_items"));
        if (seconds > 0) arrow.add(new Hint("minimum_wait", seconds));
        result.add(new Recipe(kind, items, oneFluid(starlight), itemHints,
                Collections.singletonList(hints(source ? "source_replaced" : consumed ? "source_consumed" : "not_consumed")),
                Collections.singletonList(new Output(output, 1, false, Collections.emptyList())), arrow));
    }

    private static void addCrystalProcesses(List<Recipe> result, ItemStack template, Fluid starlight, boolean duplicates) {
        CrystalProperties base = CrystalProperties.getCrystalProperties(template.copy());
        int max = CrystalProperties.getMaxSize(template.copy());
        int size = Math.min(100, max);
        int growthSize = Math.min(100, max - 1);
        if (growthSize < max) {
            ItemStack input = withCrystal(template, growthSize, base.getPurity(), base.getCollectiveCapability(), 0, base.getSizeOverride());
            ItemStack output = withCrystal(input, Math.min(growthSize + 80, max), base.getPurity(), base.getCollectiveCapability(), 0, base.getSizeOverride());
            result.add(treatment(Kind.CRYSTAL_GROWTH, input, starlight,
                    Collections.singletonList(new Output(output, 1, true, hints("growth"))),
                    60, hints("unfractured")));
        }
        ItemStack fractured = withCrystal(template, size, base.getPurity(), base.getCollectiveCapability(), 60, base.getSizeOverride());
        ItemStack repaired = withCrystal(fractured, size, base.getPurity(), base.getCollectiveCapability(), 30, base.getSizeOverride());
        result.add(treatment(Kind.CRYSTAL_REPAIR, fractured, starlight,
                Collections.singletonList(new Output(repaired, 1, true, hints("repair"))), 60, hints("fractured")));
        if (!duplicates) return;
        ItemStack maximum = withCrystal(template, max, base.getPurity(), base.getCollectiveCapability(), 0, base.getSizeOverride());
        ItemStack parent = withCrystal(maximum, 200, base.getPurity(), 50, 0, base.getSizeOverride());
        boolean celestial = isCelestialCrystal(template);
        Item childItem = celestial ? ItemsAS.celestialCrystal : ItemsAS.rockCrystal;
        if (childItem == null) return;
        ItemStack child = withCrystal(new ItemStack(childItem), 40, Math.min(base.getPurity() + 5, 100), 50, 0, base.getSizeOverride());
        result.add(treatment(Kind.CRYSTAL_SPLIT, maximum, starlight,
                Arrays.asList(new Output(parent, 1, true, hints("parent_retained")),
                        new Output(child, 1.0 / 6, true, hints("child_untuned"))),
                60, hints("max_size", "unfractured")));
    }

    private static void addToolProcess(List<Recipe> result, ItemStack template, Fluid starlight) {
        ToolCrystalProperties properties = toolProperties(template.copy());
        int max = CrystalProperties.getMaxSize(template.copy());
        int size = Math.min(100, max);
        ItemStack input = single(template);
        setToolProperties(input, new ToolCrystalProperties(size, properties.getPurity(), 80,
                properties.getFracturation(), properties.getSizeOverride()));
        ItemStack output = input.copy();
        setToolProperties(output, new ToolCrystalProperties(Math.min(size + 200, max), properties.getPurity(), 65,
                properties.getFracturation(), properties.getSizeOverride()));
        result.add(treatment(Kind.TOOL_GROWTH, input, starlight,
                Collections.singletonList(new Output(output, 1, true, hints("tool_growth"))),
                50, Collections.emptyList()));
    }

    private static Recipe treatment(Kind kind, ItemStack input, Fluid starlight, List<Output> outputs,
            int seconds, List<Hint> inputHints) {
        return new Recipe(kind, Collections.singletonList(Collections.singletonList(input)), oneFluid(starlight),
                Collections.singletonList(inputHints), Collections.singletonList(hints("source_consumed")), outputs,
                Arrays.asList(new Hint("drop_items"), new Hint("minimum_wait", seconds)));
    }

    private static void addContacts(List<Recipe> result, Fluid starlight, Flags flags) {
        List<FluidStack> cold = new ArrayList<>();
        List<FluidStack> hot = new ArrayList<>();
        List<Fluid> fluids = new ArrayList<>(FluidRegistry.getRegisteredFluids().values());
        Collections.sort(fluids, (a, b) -> a.getName().compareTo(b.getName()));
        for (Fluid fluid : fluids) {
            try {
                Block placed = fluid.getBlock();
                if (placed == null || placed instanceof FluidBlockLiquidStarlight) continue;
                IBlockState state = placed.getDefaultState();
                Material material = state.getMaterial();
                if (!material.isLiquid()) continue;
                int temperature;
                if (placed instanceof BlockFluidBase) temperature = ((BlockFluidBase) placed).getTemperature();
                else if (placed instanceof BlockLiquid) temperature = material == Material.LAVA
                        ? FluidRegistry.LAVA.getTemperature() : material == Material.WATER
                        ? FluidRegistry.WATER.getTemperature() : 100;
                else continue;
                (temperature <= CONTACT_TEMPERATURE ? cold : hot).add(new FluidStack(fluid, DISPLAY_VOLUME));
            } catch (RuntimeException | LinkageError failure) {
                ASJeiAllDrops.LOGGER.warn("Cannot describe AS fluid contact for {}", fluid.getName(), failure);
            }
        }
        if (!cold.isEmpty()) result.add(contact(Kind.COLD_CONTACT, starlight, cold,
                Collections.singletonList(new Output(new ItemStack(flags.ice ? Blocks.ICE : Blocks.COBBLESTONE), 1, false, Collections.emptyList())),
                "neighbor_cold"));
        if (!hot.isEmpty()) {
            List<Output> outputs = new ArrayList<>();
            if (!flags.sand) outputs.add(new Output(new ItemStack(Blocks.COBBLESTONE), 1, false, Collections.emptyList()));
            else if (!flags.aquamarine) outputs.add(new Output(new ItemStack(Blocks.SAND), 1, false, Collections.emptyList()));
            else {
                outputs.add(new Output(new ItemStack(Blocks.SAND), 899.0 / 900, false, Collections.emptyList()));
                ItemStack shale = BlockCustomSandOre.OreType.AQUAMARINE.asStack();
                if (!shale.isEmpty()) outputs.add(new Output(shale, 1.0 / 900, false, Collections.emptyList()));
            }
            result.add(contact(Kind.HOT_CONTACT, starlight, hot, outputs, "neighbor_hot"));
        }
    }

    private static Recipe contact(Kind kind, Fluid starlight, List<FluidStack> neighbors, List<Output> outputs, String temperatureHint) {
        return new Recipe(kind, Collections.emptyList(), Arrays.asList(Collections.singletonList(new FluidStack(starlight, DISPLAY_VOLUME)), neighbors),
                Collections.emptyList(), Arrays.asList(hints("source_replaced"), Arrays.asList(new Hint("not_consumed"), new Hint(temperatureHint, CONTACT_TEMPERATURE))),
                outputs, hints("contact"));
    }

    private static void addCrystalVariants(Map<StackKey, ItemStack> result, ItemStack crystal,
            List<IWeakConstellation> constellations) {
        if (!(crystal.getItem() instanceof ItemTunedCrystalBase)) {
            result.putIfAbsent(new StackKey(crystal), crystal);
            return;
        }
        // Native creative ingredients do not necessarily enumerate every attunement.
        for (IWeakConstellation constellation : constellations) {
            ItemStack variant = crystal.copy();
            ItemTunedCrystalBase.applyMainConstellation(variant, constellation);
            result.putIfAbsent(new StackKey(variant), variant);
        }
        // Preserve any already registered attunement (and its trait) as well.
        if (ItemTunedCrystalBase.getMainConstellation(crystal.copy()) != null)
            result.putIfAbsent(new StackKey(crystal), crystal);
    }

    private static ItemStack crystalTemplate(ItemStack candidate) {
        ItemStack copy = single(candidate);
        CrystalProperties existing = CrystalProperties.getCrystalProperties(copy);
        int override = existing == null ? -1 : existing.getSizeOverride();
        int max = CrystalProperties.getMaxSize(copy);
        if (max < 1) return null;
        return withCrystal(copy, Math.min(100, max), 80, 80, 0, override);
    }

    private static ItemStack toolTemplate(ItemStack candidate) {
        ItemStack copy = single(candidate);
        ToolCrystalProperties existing = toolProperties(copy);
        if (existing == null) return null;
        int max = CrystalProperties.getMaxSize(copy);
        if (max < 1) return null;
        setToolProperties(copy, new ToolCrystalProperties(Math.min(100, max), 80, 80,
                existing.getFracturation(), existing.getSizeOverride()));
        return copy;
    }

    private static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof ItemCrystalToolBase || stack.getItem() instanceof ItemCrystalSword;
    }
    private static ToolCrystalProperties toolProperties(ItemStack stack) {
        return stack.getItem() instanceof ItemCrystalSword ? ItemCrystalSword.getToolProperties(stack)
                : ItemCrystalToolBase.getToolProperties(stack);
    }
    private static void setToolProperties(ItemStack stack, ToolCrystalProperties properties) {
        if (stack.getItem() instanceof ItemCrystalSword) ItemCrystalSword.setToolProperties(stack, properties);
        else ItemCrystalToolBase.setToolProperties(stack, properties);
    }
    private static ItemStack withCrystal(ItemStack stack, int size, int purity, int cutting, int fractures, int override) {
        ItemStack copy = single(stack);
        CrystalProperties.applyCrystalProperties(copy, new CrystalProperties(size, purity, cutting, fractures, override));
        return copy;
    }
    private static ItemStack block(Block block) {
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }
    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        if (copy.getMetadata() == OreDictionary.WILDCARD_VALUE) copy.setItemDamage(0);
        return copy;
    }
    private static List<List<FluidStack>> oneFluid(Fluid fluid) {
        return Collections.singletonList(Collections.singletonList(new FluidStack(fluid, DISPLAY_VOLUME)));
    }
    private static List<Hint> hints(String... keys) {
        List<Hint> hints = new ArrayList<>(keys.length);
        for (String key : keys) hints.add(new Hint(key));
        return hints;
    }
    private static List<ItemStack> distinct(List<ItemStack> stacks) {
        Map<StackKey, ItemStack> result = new LinkedHashMap<>();
        for (ItemStack stack : stacks) result.putIfAbsent(new StackKey(stack), stack);
        return new ArrayList<>(result.values());
    }

    private static final class Flags {
        final boolean wood = Config.liquidStarlightInfusedWood;
        final boolean celestial = Config.craftingLiqCelestialCrystalForm;
        final boolean crystals = Config.craftingLiqCrystalGrowth;
        final boolean duplicates = Config.canCrystalGrowthYieldDuplicates;
        final boolean tools = Config.craftingLiqCrystalToolGrowth;
        final boolean ice = Config.liquidStarlightIce;
        final boolean sand = Config.liquidStarlightSand;
        final boolean aquamarine = Config.liquidStarlightAquamarine;
    }

    private static final class StackKey {
        final Item item;
        final int metadata;
        final NBTTagCompound tag;
        StackKey(ItemStack stack) {
            item = stack.getItem();
            metadata = stack.getMetadata();
            tag = stack.hasTagCompound() ? stack.getTagCompound().copy() : null;
        }
        @Override public int hashCode() {
            return 31 * (31 * System.identityHashCode(item) + metadata) + (tag == null ? 0 : tag.hashCode());
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof StackKey)) return false;
            StackKey key = (StackKey) other;
            return item == key.item && metadata == key.metadata && java.util.Objects.equals(tag, key.tag);
        }
    }
}

