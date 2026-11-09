package com.kaduvill.asjeialldrops.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Immutable category geometry; each wrapper owns its slots, focus, and animation state. */
public final class RitualRecipeCategory implements IRecipeCategory<RitualRecipeWrapper> {
    static final int WIDTH = 162;
    private final String key;
    private final IDrawable background;
    private final IDrawable icon;
    final IDrawable slot;
    private final IDrawableStatic emptyArrow;
    private final IDrawableAnimated arrow;
    IIngredientRenderer<ItemStack> itemRenderer;

    RitualRecipeCategory(IGuiHelper helper, String key, int height) {
        this.key = key;
        background = helper.createBlankDrawable(WIDTH, height);
        ItemStack pedestal = RitualItems.pedestal();
        icon = pedestal.isEmpty() ? null : helper.createDrawableIngredient(pedestal);
        slot = helper.getSlotDrawable();
        ResourceLocation texture = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
        emptyArrow = helper.createDrawable(texture, 24, 132, 24, 17);
        arrow = helper.createAnimatedDrawable(helper.createDrawable(texture, 82, 128, 24, 17),
                100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override public String getUid() { return "asjeialldrops." + key; }
    @Override public String getTitle() { return I18n.format("jei.asjeialldrops.category." + key); }
    @Override public String getModName() { return I18n.format("jei.asjeialldrops.mod_name"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    void drawArrow(Minecraft minecraft, int x, int y, boolean vertical) {
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.pushMatrix();
        try {
            if (vertical) {
                GlStateManager.translate(x + 17, y, 0);
                GlStateManager.rotate(90, 0, 0, 1);
            } else GlStateManager.translate(x, y, 0);
            emptyArrow.draw(minecraft);
            arrow.draw(minecraft);
        } finally {
            GlStateManager.popMatrix();
            GlStateManager.color(1, 1, 1, 1);
        }
    }

    @Override public void setRecipe(IRecipeLayout layout, RitualRecipeWrapper wrapper, IIngredients ingredients) {
        wrapper.bind(layout, this);
    }
}
