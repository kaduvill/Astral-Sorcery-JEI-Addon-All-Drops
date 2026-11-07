package com.kaduvill.asjeialldrops.jei;

import hellfirepvp.astralsorcery.common.tile.TileBore;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

public final class NeromanticRecipeCategory implements IRecipeCategory<NeromanticRecipeWrapper> {
    public static final String UID = "asjeialldrops.neromantic_prime";
    private final NeromanticRecipeLayout layout;
    private final IDrawable background, slot, icon;
    private final IDrawableStatic arrowBackground;
    private final IDrawableAnimated arrow;
    private IIngredientRenderer<FluidStack> renderer;
    private IIngredientHelper<FluidStack> ingredientHelper;

    NeromanticRecipeCategory(IGuiHelper helper, NeromanticRecipeLayout layout) {
        this.layout = layout;
        background = helper.createBlankDrawable(NeromanticRecipeLayout.WIDTH, layout.height);
        slot = helper.getSlotDrawable();
        ResourceLocation texture = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
        arrowBackground = helper.createDrawable(texture, 24, 132, 24, 17);
        // Match Mineralis' process animation. The tooltip supplies the actual production interval.
        arrow = helper.createAnimatedDrawable(helper.createDrawable(texture, 82, 128, 24, 17),
                200, IDrawableAnimated.StartDirection.LEFT, false);
        icon = helper.createDrawableIngredient(TileBore.BoreType.LIQUID.asStack());
    }

    void setRenderer(IIngredientRenderer<FluidStack> renderer, IIngredientHelper<FluidStack> helper) {
        this.renderer = renderer;
        ingredientHelper = helper;
    }
    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return I18n.format("jei.asjeialldrops.category.neromantic_prime"); }
    @Override public String getModName() { return I18n.format("jei.asjeialldrops.mod_name"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override public void drawExtras(Minecraft minecraft) {
        slot.draw(minecraft, layout.inputX, layout.inputY);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate((NeromanticRecipeLayout.WIDTH + arrowBackground.getHeight()) / 2.0F,
                    layout.arrowY, 0);
            GlStateManager.rotate(90, 0, 0, 1);
            arrowBackground.draw(minecraft, 0, 0);
            arrow.draw(minecraft, 0, 0);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override public void setRecipe(IRecipeLayout layout, NeromanticRecipeWrapper wrapper, IIngredients ingredients) {
        wrapper.bind(layout.getFluidStacks(), layout.getFocus(), slot, renderer, ingredientHelper);
    }
}
