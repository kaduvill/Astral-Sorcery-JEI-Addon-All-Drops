package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.cluster.ClusterRecipes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public final class ClusterRecipeWrapper implements IRecipeWrapper {
    private final ClusterRecipes.Recipe recipe;
    private final List<List<ItemStack>> inputs;
    private final List<ItemStack> outputs;
    private final List<ItemStack> stardustAmounts;
    private IDrawable slot;

    ClusterRecipeWrapper(ClusterRecipes.Recipe recipe) {
        this.recipe = recipe;
        inputs = recipe.getInputs();
        outputs = new ArrayList<>(recipe.outputs.size());
        for (ClusterRecipes.Output output : recipe.outputs) outputs.add(output.getStack());
        if (recipe.condition.isCelestial()) {
            ItemStack two = outputs.get(1).copy();
            two.setCount(2);
            stardustAmounts = Arrays.asList(outputs.get(1), two);
        } else stardustAmounts = Collections.emptyList();
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, inputs);
        ingredients.setOutputs(VanillaTypes.ITEM, outputs);
    }

    @SuppressWarnings("unchecked")
    void bindLayout(IGuiItemStackGroup items, IDrawable slot, IIngredientRenderer<ItemStack> renderer, IFocus<?> focus) {
        this.slot = slot;
        IFocus<ItemStack> itemFocus = focus != null && focus.getValue() instanceof ItemStack
                ? (IFocus<ItemStack>) focus : null;
        for (int i = 0; i < inputs.size(); i++) {
            items.init(i, true, renderer, ClusterRecipeLayout.inputX(inputs.size(), i),
                    ClusterRecipeLayout.ITEM_Y, 18, 18, 1, 1);
            if (i == 0) {
                // Show the growth sequence even when a search focuses one particular stage.
                items.setOverrideDisplayFocus(null);
                try {
                    items.set(i, inputs.get(i));
                } finally {
                    items.setOverrideDisplayFocus(itemFocus);
                }
            } else items.set(i, inputs.get(i));
        }
        for (int i = 0; i < outputs.size(); i++) {
            int index = ClusterRecipeLayout.OUTPUT_INDEX + i;
            items.init(index, false, renderer, ClusterRecipeLayout.outputX(outputs.size(), i),
                    ClusterRecipeLayout.ITEM_Y, 18, 18, 1, 1);
            if (isStardust(i)) {
                // Quantities share one ingredient identity; a recipe search must not pin the count to 1.
                items.setOverrideDisplayFocus(null);
                try {
                    items.set(index, stardustAmounts);
                } finally {
                    items.setOverrideDisplayFocus(itemFocus);
                }
            } else items.set(index, outputs.get(i));
        }
        items.addTooltipCallback((index, input, ingredient, tooltip) -> {
            if (input) {
                if (index == 0) {
                    ClusterRecipes.Stage stage = recipe.getStage(ingredient);
                    if (stage != null) {
                        String status = text(stage.isMature() ? "stage.mature" : "stage.growing",
                                stage.growth, stage.maximum);
                        tooltip.add(Math.min(1, tooltip.size()), status);
                    }
                    tooltip.add(text(recipe.condition.isCelestial() ? "celestial_growth" : "gem_growth"));
                    if (stage != null && stage.isMature() && !recipe.condition.isCelestial()) {
                        tooltip.add(text("can_regrow"));
                    }
                } else if (index == 1 && recipe.condition == ClusterRecipes.Condition.STARMETAL) {
                    tooltip.add(text("support_growth"));
                    tooltip.add(text("support_consumed"));
                }
            } else {
                int outputIndex = index - ClusterRecipeLayout.OUTPUT_INDEX;
                if (outputIndex < 0 || outputIndex >= recipe.outputs.size()) return;
                ClusterRecipes.Output output = recipe.outputs.get(outputIndex);
                if (isStardust(outputIndex)) tooltip.add(text("at_least_one"));
                else if (output.extraChance > 0) tooltip.add(text("extra_chance", JeiFormats.percentage(output.extraChance)));
                for (String hint : output.hints) tooltip.add(text(hint));
            }
        });
    }

    private boolean isStardust(int outputIndex) {
        return recipe.condition.isCelestial() && outputIndex == 1;
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        if (slot == null) return;
        GlStateManager.color(1, 1, 1, 1);
        for (int i = 0; i < inputs.size(); i++) {
            slot.draw(minecraft, ClusterRecipeLayout.inputX(inputs.size(), i), ClusterRecipeLayout.ITEM_Y);
        }
        for (int i = 0; i < outputs.size(); i++) {
            slot.draw(minecraft, ClusterRecipeLayout.outputX(outputs.size(), i), ClusterRecipeLayout.ITEM_Y);
        }
        String condition = text("condition." + recipe.condition.key);
        minecraft.fontRenderer.drawString(condition, ClusterRecipeLayout.ARROW_X + 12
                - minecraft.fontRenderer.getStringWidth(condition) / 2, ClusterRecipeLayout.LABEL_Y, 0x808080);
        GlStateManager.color(1, 1, 1, 1);
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (x < ClusterRecipeLayout.ARROW_X || x >= ClusterRecipeLayout.ARROW_X + 24
                || y < ClusterRecipeLayout.ARROW_Y || y >= ClusterRecipeLayout.ARROW_Y + 17) {
            return Collections.emptyList();
        }
        return Arrays.asList(text("harvest"), text(recipe.condition.isCelestial() ? "player" : "final_growth"));
    }

    private static String text(String key, Object... args) {
        return I18n.format("jei.asjeialldrops.cluster." + key, args);
    }
}
