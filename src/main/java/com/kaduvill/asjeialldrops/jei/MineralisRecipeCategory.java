package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.mineralis.MineralisDrop;
import hellfirepvp.astralsorcery.common.lib.Constellations;
import java.util.List;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public final class MineralisRecipeCategory implements IRecipeCategory<IRecipeWrapper> {
    public static final String UID = "asjeialldrops.mineralis";
    private final IDrawable slot;
    private final IDrawableStatic arrowBackground;
    private final IDrawableAnimated arrow;
    private final IDrawable background;
    private final MineralisRecipeLayout layout;
    private final RitualRecipeCategory infoCategory;

    public MineralisRecipeCategory(IGuiHelper guiHelper, int inputCount, int maxDrops, int infoHeight) {
        this.slot = guiHelper.getSlotDrawable();
        ResourceLocation texture = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
        this.arrowBackground = guiHelper.createDrawable(texture, 24, 132, 24, 17);
        IDrawableStatic arrowFill = guiHelper.createDrawable(texture, 82, 128, 24, 17);
        this.arrow = guiHelper.createAnimatedDrawable(arrowFill, 200, IDrawableAnimated.StartDirection.LEFT, false);
        infoCategory = new RitualRecipeCategory(guiHelper, "mineralis", Constellations.mineralis, infoHeight);
        layout = new MineralisRecipeLayout(inputCount, maxDrops);
        background = guiHelper.createBlankDrawable(MineralisRecipeLayout.WIDTH,
                Math.max(layout.height, infoCategory.getBackground().getHeight()));
    }

    @Override
    public String getUid() {
        return UID;
    }

    @Override
    public String getTitle() {
        return I18n.format("jei.asjeialldrops.category.mineralis");
    }

    @Override
    public String getModName() {
        return I18n.format("jei.asjeialldrops.mod_name");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override public IDrawable getIcon() { return infoCategory.getIcon(); }

    void drawGrid(Minecraft minecraft) {
        for (int index = 0; index < layout.inputCount; index++)
            slot.draw(minecraft, layout.inputX(index), layout.inputY(index));
        for (int index = 0; index < layout.pageSize; index++)
            slot.draw(minecraft, layout.outputX(index), layout.outputY(index));
        if (layout.inputCount > 0) drawArrow(minecraft);
    }

    private void drawArrow(Minecraft minecraft) {
        GlStateManager.pushMatrix();
        try {
            // Rotate the background and fill together, so the animation runs downward.
            GlStateManager.translate(MineralisRecipeLayout.ARROW_X + 17, layout.arrowY, 0);
            GlStateManager.rotate(90, 0, 0, 1);
            arrowBackground.draw(minecraft, 0, 0);
            arrow.draw(minecraft, 0, 0);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, IRecipeWrapper wrapper, IIngredients ingredients) {
        if (wrapper instanceof RitualInfoRecipeWrapper) {
            ((RitualInfoRecipeWrapper) wrapper).bind(recipeLayout, infoCategory);
            return;
        }
        MineralisRecipeWrapper recipeWrapper = (MineralisRecipeWrapper) wrapper;
        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
        recipeWrapper.bindLayout(itemStacks, layout, this, recipeLayout.getFocus());
        List<MineralisDrop> drops = recipeWrapper.getDrops();
        int outputOffset = recipeWrapper.getInputs().size();
        itemStacks.addTooltipCallback((slotIndex, input, ingredient, tooltip) -> {
            if (input) {
                recipeWrapper.addInputTooltip(tooltip);
                return;
            }
            int dropIndex = slotIndex - outputOffset;
            if (dropIndex < 0 || dropIndex >= drops.size()) return;
            double probability = drops.get(dropIndex).getProbability();
            tooltip.add(I18n.format("jei.asjeialldrops.chance", formatPercentage(probability)));
        });
    }

    static String formatPercentage(double probability) {
        return JeiFormats.percentage(probability);
    }
}
