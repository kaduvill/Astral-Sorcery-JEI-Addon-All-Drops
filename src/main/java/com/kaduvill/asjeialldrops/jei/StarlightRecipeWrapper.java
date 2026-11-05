package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.fluid.StarlightRecipes;
import hellfirepvp.astralsorcery.common.data.research.EnumGatedKnowledge;
import hellfirepvp.astralsorcery.common.data.research.ResearchManager;
import hellfirepvp.astralsorcery.common.item.crystal.base.ItemTunedCrystalBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public final class StarlightRecipeWrapper implements IRecipeWrapper {
    private final StarlightRecipes.Recipe recipe;
    private final List<List<ItemStack>> itemInputs;
    private final List<List<FluidStack>> fluidInputs;
    private final List<ItemStack> outputs;
    private final List<List<ItemStack>> outputAlternatives;
    private final List<ItemStack> clusterRepresentatives;
    private IDrawable slot;
    private IGuiItemStackGroup itemStacks;
    private List<StarlightRecipes.Variant> cycleVariants = Collections.emptyList();
    private List<ItemStack> cycleInputs = Collections.emptyList();
    private ITickTimer cycleTimer;
    private int displayedVariant = -1;

    StarlightRecipeWrapper(StarlightRecipes.Recipe recipe) {
        this.recipe = recipe;
        itemInputs = recipe.getItemInputs();
        fluidInputs = recipe.getFluidInputs();
        if (itemInputs.size() > 2 || fluidInputs.size() > 2
                || itemInputs.size() + fluidInputs.size() > 3 || recipe.outputs.size() > 2) {
            throw new IllegalArgumentException("Starlight interaction exceeds category bounds");
        }
        // The amount fills the icon only. Input tooltips describe source/contact semantics.
        for (List<FluidStack> alternatives : fluidInputs) {
            for (FluidStack fluid : alternatives) fluid.amount = 1000;
        }
        clusterRepresentatives = representativeCrystals();
        outputs = new ArrayList<>(recipe.outputs.size());
        for (StarlightRecipes.Output output : recipe.outputs) outputs.add(output.getOutput());
        outputAlternatives = new ArrayList<>(outputs.size());
        for (int i = 0; i < outputs.size(); i++) {
            List<ItemStack> alternatives = new ArrayList<>();
            if (recipe.getVariants().isEmpty()) alternatives.add(outputs.get(i));
            else for (StarlightRecipes.Variant variant : recipe.getVariants()) {
                ItemStack candidate = variant.outputs.get(i).getOutput();
                boolean duplicate = false;
                for (ItemStack existing : alternatives) {
                    if (ItemStack.areItemStacksEqual(existing, candidate)) { duplicate = true; break; }
                }
                if (!duplicate) alternatives.add(candidate);
            }
            outputAlternatives.add(alternatives);
        }
    }

    private boolean isClusterCreation() {
        return recipe.kind == StarlightRecipes.Kind.CELESTIAL_CLUSTER
                || recipe.kind == StarlightRecipes.Kind.GEM_CLUSTER;
    }

    private List<ItemStack> representativeCrystals() {
        if (!isClusterCreation()) return Collections.emptyList();
        Map<Item, ItemStack> forms = new LinkedHashMap<>();
        for (ItemStack crystal : itemInputs.get(0)) forms.putIfAbsent(crystal.getItem(), crystal);
        List<ItemStack> representatives = new ArrayList<>(forms.values());
        // Ordinary Rock/Celestial first, then one valid example of each attuned form.
        representatives.sort((a, b) -> Boolean.compare(a.getItem() instanceof ItemTunedCrystalBase,
                b.getItem() instanceof ItemTunedCrystalBase));
        return representatives;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, itemInputs);
        ingredients.setInputLists(VanillaTypes.FLUID, fluidInputs);
        if (recipe.getVariants().isEmpty()) ingredients.setOutputs(VanillaTypes.ITEM, outputs);
        else ingredients.setOutputLists(VanillaTypes.ITEM, outputAlternatives);
    }

    void bindLayout(IGuiItemStackGroup items, IGuiFluidStackGroup fluids, IDrawable slot,
                    IIngredientRenderer<FluidStack> fluidRenderer, IIngredientRenderer<ItemStack> itemRenderer,
                    IFocus<?> focus, IGuiHelper guiHelper, IIngredientHelper<ItemStack> ingredientHelper) {
        this.slot = slot;
        this.itemStacks = items;
        cycleTimer = null;
        displayedVariant = -1;
        for (int i = 0; i < itemInputs.size(); i++) {
            items.init(i, true, itemRenderer, StarlightRecipeLayout.SECOND_INPUT_X,
                    StarlightRecipeLayout.itemY(itemInputs.size(), i), 18, 18, 1, 1);
            items.set(i, itemInputs.get(i));
        }
        for (int i = 0; i < fluidInputs.size(); i++) {
            fluids.init(i, true, new FluidRenderer(fluidRenderer), StarlightRecipeLayout.fluidX(i) + 1,
                    StarlightRecipeLayout.CENTER_Y + 1, 16, 16, 0, 0);
            fluids.set(i, fluidInputs.get(i));
        }
        for (int i = 0; i < outputs.size(); i++) {
            int index = StarlightRecipeLayout.OUTPUT_INDEX + i;
            items.init(index, false, itemRenderer, StarlightRecipeLayout.outputX(outputs.size(), i),
                    StarlightRecipeLayout.CENTER_Y, 18, 18, 1, 1);
            items.set(index, outputs.get(i));
        }
        items.addTooltipCallback((index, input, ingredient, tooltip) -> {
            if (input && index >= 0 && index < itemInputs.size()) {
                addHints(tooltip, recipe.getItemHints(index));
                if (index == 0 && isClusterCreation() && ingredient.getItem() instanceof ItemTunedCrystalBase) {
                    tooltip.add(text("any_attunement"));
                }
            } else if (!input) {
                int outputIndex = index - StarlightRecipeLayout.OUTPUT_INDEX;
                if (outputIndex >= 0 && outputIndex < recipe.outputs.size()) {
                    addOutputTooltip(recipe.outputs.get(outputIndex), tooltip);
                }
            }
        });
        fluids.addTooltipCallback((index, input, ingredient, tooltip) -> {
            if (index >= 0 && index < fluidInputs.size()) addHints(tooltip, recipe.getFluidHints(index));
        });
        cycleVariants = matchingVariants(focus, ingredientHelper);
        cycleInputs = clusterDisplayInputs(focus, ingredientHelper);
        int count = cycleInputs.isEmpty() ? cycleVariants.size() : cycleInputs.size();
        if (count > 0) {
            if (count > 1) {
                // JEI's display animation clock is read only while this recipe is rendered.
                cycleTimer = guiHelper.createTickTimer(20 * count, count - 1, false);
            }
            synchronizeVariant(0);
        }
    }

    private List<ItemStack> clusterDisplayInputs(IFocus<?> focus, IIngredientHelper<ItemStack> helper) {
        if (clusterRepresentatives.isEmpty()) return Collections.emptyList();
        if (focus != null && focus.getMode() == IFocus.Mode.INPUT && focus.getValue() instanceof ItemStack) {
            ItemStack matching = helper.getMatch(itemInputs.get(0), (ItemStack) focus.getValue());
            if (matching != null) return Collections.singletonList(matching);
        }
        return clusterRepresentatives;
    }

    private List<StarlightRecipes.Variant> matchingVariants(IFocus<?> focus, IIngredientHelper<ItemStack> helper) {
        List<StarlightRecipes.Variant> all = recipe.getVariants();
        if (all.isEmpty() || focus == null || !(focus.getValue() instanceof ItemStack)) return all;
        ItemStack searched = (ItemStack) focus.getValue();
        List<StarlightRecipes.Variant> matching = new ArrayList<>();
        for (StarlightRecipes.Variant variant : all) {
            if (focus.getMode() == IFocus.Mode.INPUT) {
                if (matches(helper, variant.getInput(), searched)) matching.add(variant);
            } else {
                for (StarlightRecipes.Output output : variant.outputs) {
                    if (matches(helper, output.getOutput(), searched)) { matching.add(variant); break; }
                }
            }
        }
        return matching.isEmpty() ? all : matching;
    }

    private static boolean matches(IIngredientHelper<ItemStack> helper, ItemStack candidate, ItemStack searched) {
        return helper.getMatch(Collections.singletonList(candidate), searched) != null;
    }

    /** Pin the real JEI slots together, so rendering, tooltips, clicks and helpers see the same pair. */
    void synchronizeVariant(int index) {
        int count = cycleInputs.isEmpty() ? cycleVariants.size() : cycleInputs.size();
        if (count == 0) return;
        index = Math.floorMod(index, count);
        if (index == displayedVariant) return;
        displayedVariant = index;
        if (!cycleInputs.isEmpty()) {
            pin(0, true, cycleInputs.get(index), itemInputs.get(0));
        } else {
            StarlightRecipes.Variant variant = cycleVariants.get(index);
            pin(0, true, variant.getInput(), itemInputs.get(0));
            for (int i = 0; i < outputs.size(); i++) {
                pin(StarlightRecipeLayout.OUTPUT_INDEX + i, false,
                        variant.outputs.get(i).getOutput(), outputAlternatives.get(i));
            }
        }
        itemStacks.setOverrideDisplayFocus(null);
    }

    private void pin(int index, boolean input, ItemStack selected, List<ItemStack> alternatives) {
        // Keep all alternatives available to JEI/filter helpers, with the exact selected NBT first.
        List<ItemStack> ordered = new ArrayList<>(alternatives.size());
        ordered.add(selected);
        for (ItemStack alternative : alternatives) {
            if (!ItemStack.areItemStacksEqual(selected, alternative)) ordered.add(alternative);
        }
        itemStacks.setOverrideDisplayFocus(new DisplayFocus(input ? IFocus.Mode.INPUT : IFocus.Mode.OUTPUT, selected));
        itemStacks.set(index, ordered);
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        if (slot == null) return;
        if (cycleTimer != null && !GuiScreen.isShiftKeyDown()) synchronizeVariant(cycleTimer.getValue());
        GlStateManager.color(1, 1, 1, 1);
        for (int i = 0; i < itemInputs.size(); i++) {
            slot.draw(minecraft, StarlightRecipeLayout.SECOND_INPUT_X,
                    StarlightRecipeLayout.itemY(itemInputs.size(), i));
        }
        for (int i = 0; i < fluidInputs.size(); i++) {
            slot.draw(minecraft, StarlightRecipeLayout.fluidX(i), StarlightRecipeLayout.CENTER_Y);
        }
        for (int i = 0; i < outputs.size(); i++) {
            slot.draw(minecraft, StarlightRecipeLayout.outputX(outputs.size(), i), StarlightRecipeLayout.CENTER_Y);
        }
        String operation = text("operation." + recipe.kind.key);
        minecraft.fontRenderer.drawString(operation, StarlightRecipeLayout.ARROW_X + 12
                - minecraft.fontRenderer.getStringWidth(operation) / 2, StarlightRecipeLayout.LABEL_Y, 0x808080);
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (x < StarlightRecipeLayout.ARROW_X || x >= StarlightRecipeLayout.ARROW_X + 24
                || y < StarlightRecipeLayout.ARROW_Y || y >= StarlightRecipeLayout.ARROW_Y + 17) {
            return Collections.emptyList();
        }
        List<String> tooltip = new ArrayList<>(recipe.arrowHints.size());
        addHints(tooltip, recipe.arrowHints);
        return tooltip;
    }

    private static void addOutputTooltip(StarlightRecipes.Output output, List<String> tooltip) {
        if (output.probability < 1) {
            tooltip.add(I18n.format("jei.asjeialldrops.chance", JeiFormats.percentage(output.probability)));
        }
        // AS uses the same knowledge gate for crystal/tool size, purity and cutting.
        if (output.exampleProperties && ResearchManager.clientProgress != null
                && EnumGatedKnowledge.CRYSTAL_SIZE.canSee(ResearchManager.clientProgress.getTierReached())
                && GuiScreen.isShiftKeyDown()) {
            tooltip.add(text("example_properties"));
        }
        addHints(tooltip, output.hints);
    }

    private static void addHints(List<String> tooltip, List<StarlightRecipes.Hint> hints) {
        for (StarlightRecipes.Hint hint : hints) tooltip.add(text(hint.key, hint.getArgs()));
    }

    private static String text(String key, Object... arguments) {
        return I18n.format("jei.asjeialldrops.starlight." + key, arguments);
    }

    private static final class DisplayFocus implements IFocus<ItemStack> {
        private final Mode mode;
        private final ItemStack value;
        DisplayFocus(Mode mode, ItemStack value) { this.mode = mode; this.value = value; }
        @Override public Mode getMode() { return mode; }
        @Override public ItemStack getValue() { return value; }
    }

    private static final class FluidRenderer implements IIngredientRenderer<FluidStack> {
        private final IIngredientRenderer<FluidStack> delegate;

        FluidRenderer(IIngredientRenderer<FluidStack> delegate) { this.delegate = delegate; }

        @Override public void render(Minecraft minecraft, int x, int y, FluidStack ingredient) {
            delegate.render(minecraft, x, y, ingredient);
        }

        @Override public List<String> getTooltip(Minecraft minecraft, FluidStack ingredient, ITooltipFlag flag) {
            return new ArrayList<>(Collections.singletonList(ingredient.getLocalizedName()));
        }
    }
}
