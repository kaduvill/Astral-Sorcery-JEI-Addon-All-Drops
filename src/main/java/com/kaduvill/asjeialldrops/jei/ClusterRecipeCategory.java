package com.kaduvill.asjeialldrops.jei;

import hellfirepvp.astralsorcery.common.block.BlockGemCrystals;
import hellfirepvp.astralsorcery.common.lib.BlocksAS;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class ClusterRecipeCategory implements IRecipeCategory<ClusterRecipeWrapper> {
    public static final String UID = "asjeialldrops.crystal_cluster_growth";
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawableStatic arrowBackground;
    private final IDrawableAnimated arrow;
    private IIngredientRenderer<ItemStack> itemRenderer;

    ClusterRecipeCategory(IGuiHelper helper) {
        ItemStack cluster = BlocksAS.gemCrystals == null ? ItemStack.EMPTY : new ItemStack(BlocksAS.gemCrystals, 1,
                BlocksAS.gemCrystals.getMetaFromState(BlocksAS.gemCrystals.getDefaultState()
                        .withProperty(BlockGemCrystals.STAGE, BlockGemCrystals.GrowthStageType.STAGE_0)));
        icon = cluster.isEmpty() ? null : helper.createDrawableIngredient(cluster);
        background = helper.createBlankDrawable(ClusterRecipeLayout.WIDTH, ClusterRecipeLayout.HEIGHT);
        slot = helper.getSlotDrawable();
        ResourceLocation texture = new ResourceLocation("jei", "textures/gui/gui_vanilla.png");
        arrowBackground = helper.createDrawable(texture, 24, 132, 24, 17);
        arrow = helper.createAnimatedDrawable(helper.createDrawable(texture, 82, 128, 24, 17),
                50, IDrawableAnimated.StartDirection.LEFT, false);
    }

    void setRenderer(IIngredientRenderer<ItemStack> renderer) { itemRenderer = renderer; }
    @Override public String getUid() { return UID; }
    @Override public String getTitle() { return I18n.format("jei.asjeialldrops.category.crystal_cluster_growth"); }
    @Override public String getModName() { return I18n.format("jei.asjeialldrops.mod_name"); }
    @Override public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }

    @Override public void drawExtras(Minecraft minecraft) {
        arrowBackground.draw(minecraft, ClusterRecipeLayout.ARROW_X, ClusterRecipeLayout.ARROW_Y);
        arrow.draw(minecraft, ClusterRecipeLayout.ARROW_X, ClusterRecipeLayout.ARROW_Y);
    }

    @Override public void setRecipe(IRecipeLayout layout, ClusterRecipeWrapper wrapper, IIngredients ingredients) {
        wrapper.bindLayout(layout.getItemStacks(), slot, itemRenderer, layout.getFocus());
    }
}
