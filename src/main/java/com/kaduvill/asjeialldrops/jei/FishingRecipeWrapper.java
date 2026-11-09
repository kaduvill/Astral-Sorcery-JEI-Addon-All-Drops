package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ritual.RitualRecipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

/** Every resolved drop has its own output slot, including outputs on hidden pages. */
final class FishingRecipeWrapper extends RitualRecipeWrapper {
    private static final int SOURCE_X = 72, SOURCE_Y = 26;
    private static final int ARROW_X = 72, ARROW_Y = 52, GRID_Y = 81;
    private static final int HIDDEN = -10000;
    private static final IIngredientRenderer<ItemStack> HIDDEN_RENDERER = (minecraft, x, y, stack) -> { };
    private final List<RitualRecipes.FishingDrop> drops;
    private final Map<String, Integer> outputIndices = new HashMap<>();
    private IIngredientHelper<ItemStack> ingredientHelper;
    final RitualOutputGridLayout layout;
    private int page;

    FishingRecipeWrapper(double range, List<RitualRecipes.FishingDrop> drops) {
        super(false, range, "octans.fishing");
        this.drops = Collections.unmodifiableList(new ArrayList<>(drops));
        layout = new RitualOutputGridLayout(drops.size(), GRID_Y);
    }

    @Override int height() { return layout.height; }

    @Override List<List<ItemStack>> outputLists() {
        List<List<ItemStack>> result = new ArrayList<>();
        for (RitualRecipes.FishingDrop drop : drops) result.add(Collections.singletonList(drop.stack()));
        return result;
    }

    @Override public void getIngredients(IIngredients ingredients) {
        super.getIngredients(ingredients);
        ingredients.setInput(VanillaTypes.FLUID, new FluidStack(FluidRegistry.WATER, 1000));
    }

    void indexOutputs(IIngredientHelper<ItemStack> ingredientHelper) {
        this.ingredientHelper = ingredientHelper;
        outputIndices.clear();
        for (int i = 0; i < drops.size(); i++) {
            for (ItemStack output : ingredientHelper.expandSubtypes(Collections.singletonList(drops.get(i).stack()))) {
                if (output != null && !output.isEmpty())
                    outputIndices.putIfAbsent(ingredientHelper.getUniqueId(output), i);
            }
        }
    }

    int findOutput(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        Integer selected = outputIndices.get(ingredientHelper.getUniqueId(stack));
        if (selected == null) selected = outputIndices.get(ingredientHelper.getWildcardId(stack));
        if (selected != null) return selected;
        for (int i = 0; i < drops.size(); i++) if (drops.get(i).matches(stack)) return i;
        return -1;
    }

    @Override void bind(IRecipeLayout recipeLayout, RitualRecipeCategory category) {
        super.bind(recipeLayout, category);
        IFocus<?> focus = recipeLayout.getFocus();
        ItemStack focused = focus != null && focus.getMode() == IFocus.Mode.OUTPUT
                && focus.getValue() instanceof ItemStack ? (ItemStack) focus.getValue() : ItemStack.EMPTY;
        int selected = findOutput(focused);
        if (selected >= 0) page = selected / layout.pageSize;
        page = Math.min(page, layout.pages(drops.size()) - 1);
        recipeLayout.getFluidStacks().init(0, true, SOURCE_X + 1,
                SOURCE_Y + 1, 16, 16, 1000, false, null);
        recipeLayout.getFluidStacks().set(0, new FluidStack(FluidRegistry.WATER, 1000));
        recipeLayout.getFluidStacks().addTooltipCallback((slot, input, ingredient, tooltip) -> {
            tooltip.add(text("octans.water_source"));
            tooltip.add(text("octans.source_retained"));
        });
        int first = page * layout.pageSize;
        for (int i = 0; i < drops.size(); i++) initSlot(i, i >= first && i < first + layout.pageSize ? i - first : -1);
        items.addTooltipCallback((slot, input, ingredient, tooltip) -> {
            if (slot < 1 || slot > drops.size()) return;
            RitualRecipes.FishingDrop drop = drops.get(slot - 1);
            if (drop.enchanted) tooltip.add(text("octans.random_enchantments"));
            if (drop.damaged) tooltip.add(text("octans.random_damage"));
            if (drop.amountsVary) tooltip.add(text("octans.amounts_vary"));
        });
    }

    private void initSlot(int index, int visible) {
        if (visible < 0) items.init(index + 1, false, HIDDEN_RENDERER, HIDDEN, HIDDEN, 18, 18, 1, 1);
        else items.init(index + 1, false, layout.gridX(visible), layout.gridY(visible));
        items.set(index + 1, drops.get(index).stack());
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        super.drawInfo(minecraft, width, height, mouseX, mouseY);
        if (category == null) return;
        category.slot.draw(minecraft, SOURCE_X, SOURCE_Y);
        category.drawArrow(minecraft, ARROW_X, ARROW_Y, true);
        FontRenderer font = minecraft.fontRenderer;
        for (int i = 0; i < layout.pageSize; i++) category.slot.draw(minecraft, layout.gridX(i), layout.gridY(i));
        if (drops.isEmpty()) paragraph(minecraft, "octans.unavailable", GRID_Y + 2, 0x808080);
        if (layout.footerY < 0) return;
        int y = layout.footerY + 2;
        String pages = text("page", page + 1, layout.pages(drops.size()));
        font.drawString(pages, (width - font.getStringWidth(pages)) / 2, y, 0x606060);
        font.drawString("<", 6, y, page > 0 ? 0x404040 : 0xA0A0A0);
        font.drawString(">", width - 12, y, page + 1 < layout.pages(drops.size()) ? 0x404040 : 0xA0A0A0);
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (x < 0 || x >= RitualRecipeCategory.WIDTH) return Collections.emptyList();
        if (x >= ARROW_X && x < ARROW_X + 18
                && y >= ARROW_Y && y < ARROW_Y + 24) {
            return rangeTooltip();
        }
        return Collections.emptyList();
    }

    @Override public boolean handleClick(Minecraft minecraft, int x, int y, int button) {
        if (button != 0 || items == null || layout.footerY < 0
                || y < layout.footerY || y >= layout.footerY + RitualOutputGridLayout.FOOTER_HEIGHT) return false;
        int target;
        if (x >= 0 && x < 18) target = page - 1;
        else if (x >= RitualRecipeCategory.WIDTH - 18 && x < RitualRecipeCategory.WIDTH) target = page + 1;
        else return false;
        if (target < 0 || target >= layout.pages(drops.size())) return true;
        int old = page;
        page = target;
        positionPage(old, false);
        positionPage(page, true);
        return true;
    }

    private void positionPage(int target, boolean visible) {
        int first = target * layout.pageSize;
        for (int i = first; i < Math.min(first + layout.pageSize, drops.size()); i++) initSlot(i, visible ? i - first : -1);
    }
}
