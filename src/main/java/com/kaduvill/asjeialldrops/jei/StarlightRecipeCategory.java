package com.kaduvill.asjeialldrops.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public final class StarlightRecipeCategory implements IRecipeCategory<StarlightRecipeWrapper> {
    public static final String UID = "asjeialldrops.liquid_starlight";
    private final IDrawable background;
    private final IDrawable slot;
    private final IDrawableStatic arrowBackground;
    private final IDrawableAnimated arrow;
    private IIngredientRenderer<FluidStack> fluidRenderer;
    private IIngredientRenderer<ItemStack> itemRenderer;
    private IIngredientHelper<ItemStack> itemHelper;
    private final IGuiHelper guiHelper;

    StarlightRecipeCategory(IGuiHelper helper) {
        guiHelper = helper;
        background = helper.createBlankDrawable(StarlightRecipeLayout.WIDTH, StarlightRecipeLayout.HEIGHT);
        slot = helper.getSlotDrawable();
        ResourceLocation texture = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
        arrowBackground = helper.createDrawable(texture, 24, 132, 24, 17);
        arrow = helper.createAnimatedDrawable(helper.createDrawable(texture, 82, 128, 24, 17),
                50, IDrawableAnimated.StartDirection.LEFT, false);
    }

    void setRenderers(IIngredientRenderer<FluidStack> fluids, IIngredientRenderer<ItemStack> items,
            IIngredientHelper<ItemStack> itemHelper) {
        fluidRenderer = fluids;
        itemRenderer = items;
        this.itemHelper = itemHelper;
    }

    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return I18n.format("jei.asjeialldrops.category.liquid_starlight"); }
    @Override public String getModName() { return I18n.format("jei.asjeialldrops.mod_name"); }
    @Override public IDrawable getBackground() { return background; }

    @Override public void drawExtras(Minecraft minecraft) {
        arrowBackground.draw(minecraft, StarlightRecipeLayout.ARROW_X, StarlightRecipeLayout.ARROW_Y);
        arrow.draw(minecraft, StarlightRecipeLayout.ARROW_X, StarlightRecipeLayout.ARROW_Y);
    }

    @Override public void setRecipe(IRecipeLayout layout, StarlightRecipeWrapper wrapper, IIngredients ingredients) {
        wrapper.bindLayout(layout.getItemStacks(), layout.getFluidStacks(), slot, fluidRenderer, itemRenderer,
                layout.getFocus(), guiHelper, itemHelper);
    }
}
