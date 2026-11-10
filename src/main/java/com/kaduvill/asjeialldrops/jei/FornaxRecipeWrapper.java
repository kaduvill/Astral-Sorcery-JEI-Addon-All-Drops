package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ritual.FornaxRecipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Smelter-style paired endpoints, with placed blocks/fluids distinguished from dropped items. */
final class FornaxRecipeWrapper extends RitualRecipeWrapper {
    private static final int INPUT_X = 18;
    private static final int OUTPUT_X = 124;
    private static final int SLOT_Y = 34;
    private static final int ARROW_X = 69;
    private static final int RULE_Y = 59;
    final FornaxRecipes.Recipe recipe;
    private final FornaxRecipes.Snapshot snapshot;
    private final List<ItemStack> inputStacks;
    private final List<FluidStack> inputFluids;
    private final ItemStack outputStack;
    private final FluidStack outputFluid;

    FornaxRecipeWrapper(FornaxRecipes.Snapshot snapshot, FornaxRecipes.Recipe recipe) {
        super(recipe.corrupted, snapshot.range, recipe.corrupted ? "ritual.corrupted.fornax"
                : recipe.builtIn ? "fornax.melting" : "fornax.smelting");
        this.snapshot = snapshot;
        this.recipe = recipe;
        inputStacks = recipe.inputs();
        inputFluids = recipe.fluidInputs();
        outputStack = recipe.output();
        outputFluid = recipe.fluidOutput();
    }

    private boolean rule() { return recipe.kind == FornaxRecipes.Output.TEMPERATURE_RULE; }

    @Override boolean showSpeedHint() { return true; }

    @Override int height() {
        return rule() ? RULE_Y + paragraphHeight("fornax.temperature_cold")
                + paragraphHeight("fornax.temperature_warm") + paragraphHeight("fornax.temperature_hot") + 2
                : SLOT_Y + 20;
    }

    @Override List<List<ItemStack>> inputLists() {
        return inputStacks.isEmpty() ? Collections.emptyList() : Collections.singletonList(inputStacks);
    }

    @Override List<List<ItemStack>> outputLists() {
        return outputStack.isEmpty() ? Collections.emptyList()
                : Collections.singletonList(Collections.singletonList(outputStack));
    }

    @Override public void getIngredients(IIngredients ingredients) {
        super.getIngredients(ingredients);
        ingredients.setInputLists(VanillaTypes.FLUID,
                inputFluids.isEmpty() ? Collections.emptyList() : Collections.singletonList(inputFluids));
        if (outputFluid != null) ingredients.setOutput(VanillaTypes.FLUID, outputFluid);
    }

    @Override void bind(IRecipeLayout layout, RitualRecipeCategory category) {
        super.bind(layout, category);
        IGuiFluidStackGroup fluids = layout.getFluidStacks();
        fluids.setOverrideDisplayFocus(null);
        IFocus<?> focus = layout.getFocus();
        if (!inputStacks.isEmpty()) {
            items.init(0, true, INPUT_X, SLOT_Y);
            items.set(0, inputStacks);
            if (focus != null && focus.getMode() == IFocus.Mode.INPUT && focus.getValue() instanceof ItemStack) {
                ItemStack focused = (ItemStack) focus.getValue();
                if (recipe.acceptsInput(focused)) {
                    ItemStack one = focused.copy();
                    one.setCount(1);
                    items.set(0, one);
                }
            }
        }
        if (!outputStack.isEmpty()) {
            items.init(1, false, OUTPUT_X, SLOT_Y);
            // Output focus never substitutes a searched quantity/NBT for the actual result.
            items.set(1, outputStack.copy());
        }
        if (!inputFluids.isEmpty()) {
            fluids.init(0, true, INPUT_X + 1, SLOT_Y + 1, 16, 16, 1000, false, null);
            fluids.set(0, inputFluids);
            if (focus != null && focus.getMode() == IFocus.Mode.INPUT && focus.getValue() instanceof FluidStack) {
                FluidStack focused = (FluidStack) focus.getValue();
                if (recipe.acceptsFluid(focused)) {
                    FluidStack source = focused.copy();
                    source.amount = 1000;
                    fluids.set(0, source);
                }
            }
        }
        if (outputFluid != null) {
            fluids.init(1, false, OUTPUT_X + 1, SLOT_Y + 1, 16, 16, 1000, false, null);
            fluids.set(1, outputFluid.copy());
        }
        fluids.addTooltipCallback((slot, input, ingredient, tooltip) -> {
            // 1000 mB fills one world-block icon; this is not a tank or bucket recipe.
            tooltip.clear();
            tooltip.add(ingredient.getLocalizedName());
        });
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        super.drawInfo(minecraft, width, height, mouseX, mouseY);
        if (category == null) return;
        if (rule()) {
            if (!inputFluids.isEmpty()) category.slot.draw(minecraft, INPUT_X, SLOT_Y);
            else endpointText(minecraft, "fornax.world.other_fluids", INPUT_X + 9, SLOT_Y);
            category.drawArrow(minecraft, ARROW_X, SLOT_Y, false);
            endpointText(minecraft, "fornax.temperature_result", OUTPUT_X + 9, SLOT_Y);
            int y = RULE_Y;
            y = paragraph(minecraft, "fornax.temperature_cold", y, 0x404040);
            y = paragraph(minecraft, "fornax.temperature_warm", y, 0x404040);
            paragraph(minecraft, "fornax.temperature_hot", y, 0x404040);
        } else {
            boolean ingredientInput = !inputStacks.isEmpty() || !inputFluids.isEmpty();
            if (ingredientInput) category.slot.draw(minecraft, INPUT_X, SLOT_Y);
            else endpointText(minecraft, "fornax.world." + recipe.worldInput, INPUT_X + 9, SLOT_Y);
            if (!outputStack.isEmpty() || outputFluid != null)
                category.slot.draw(minecraft, OUTPUT_X, SLOT_Y);
            else endpointText(minecraft, "fornax.world.air", OUTPUT_X + 9, SLOT_Y);
            category.drawArrow(minecraft, ARROW_X, SLOT_Y, false);
        }
        GlStateManager.color(1, 1, 1, 1);
    }

    private static void endpointText(Minecraft minecraft, String key, int centerX, int y) {
        FontRenderer font = minecraft.fontRenderer;
        for (String line : font.listFormattedStringToWidth(text(key), 54)) {
            font.drawString(line, centerX - font.getStringWidth(line) / 2, y, 0x606060);
            y += font.FONT_HEIGHT + 1;
        }
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (x < 0 || x >= RitualRecipeCategory.WIDTH) return Collections.emptyList();
        if (x < ARROW_X || x >= ARROW_X + 24 || y < SLOT_Y || y >= SLOT_Y + 17)
            return Collections.emptyList();
        List<String> tooltip = new ArrayList<>(rangeTooltip());
        if (!recipe.corrupted && snapshot.failChance > 0)
            tooltip.add(text("fornax.failure", JeiFormats.percentage(snapshot.failChance)));
        return tooltip;
    }
}
