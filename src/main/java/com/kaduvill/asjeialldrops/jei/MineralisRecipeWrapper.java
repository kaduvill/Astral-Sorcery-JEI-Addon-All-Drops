package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.mineralis.MineralisDrop;
import com.kaduvill.asjeialldrops.mineralis.MineralisInput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public final class MineralisRecipeWrapper implements IRecipeWrapper {
    private static final int HIDDEN_POSITION = -10000;
    private static final IIngredientRenderer<ItemStack> HIDDEN_RENDERER = (minecraft, x, y, stack) -> { };
    private static final int PAGE_BUTTON_WIDTH = 18;
    private static final int LEFT_PAGE_X = 0;
    private static final int RIGHT_PAGE_X = MineralisRecipeLayout.WIDTH - PAGE_BUTTON_WIDTH;
    private final List<MineralisInput> inputs;
    private final List<MineralisDrop> drops;
    private final double noResultProbability;
    private final int searchRange;
    private final IIngredientHelper<ItemStack> ingredientHelper;
    private IGuiItemStackGroup itemStacks;
    private int page;
    private MineralisRecipeLayout layout;
    private MineralisRecipeCategory category;

    public MineralisRecipeWrapper(List<MineralisInput> inputs, List<MineralisDrop> drops,
                                  double noResultProbability, int searchRange,
                                  IIngredientHelper<ItemStack> ingredientHelper) {
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        this.drops = Collections.unmodifiableList(new ArrayList<>(drops));
        this.noResultProbability = noResultProbability;
        this.searchRange = searchRange;
        this.ingredientHelper = ingredientHelper;
    }

    public List<MineralisDrop> getDrops() {
        return drops;
    }

    public List<MineralisInput> getInputs() {
        return inputs;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        List<List<ItemStack>> inputLists = new ArrayList<>(inputs.size());
        for (MineralisInput input : inputs) inputLists.add(input.getVariants());
        ingredients.setInputLists(VanillaTypes.ITEM, inputLists);
        List<ItemStack> outputs = new ArrayList<>(drops.size());
        for (MineralisDrop drop : drops) outputs.add(drop.getStack());
        // Register every output, including later pages, for JEI recipe lookup and output helpers.
        ingredients.setOutputs(VanillaTypes.ITEM, outputs);
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        if (layout == null) return;
        category.drawGrid(minecraft);
        FontRenderer font = minecraft.fontRenderer;

        int pageCount = getPageCount();
        if (pageCount < 2) return;
        int textY = layout.footerY + (MineralisRecipeLayout.FOOTER_HEIGHT - font.FONT_HEIGHT) / 2;
        String pageText = I18n.format("jei.asjeialldrops.page", page + 1, pageCount);
        font.drawString(pageText, (recipeWidth - font.getStringWidth(pageText)) / 2, textY, 0x606060);
        font.drawString("<", LEFT_PAGE_X + (PAGE_BUTTON_WIDTH - font.getStringWidth("<")) / 2,
                textY, page > 0 ? 0x404040 : 0xA0A0A0);
        font.drawString(">", RIGHT_PAGE_X + (PAGE_BUTTON_WIDTH - font.getStringWidth(">")) / 2,
                textY, page + 1 < pageCount ? 0x404040 : 0xA0A0A0);
    }

    @Override
    public List<String> getTooltipStrings(int mouseX, int mouseY) {
        if (layout == null || layout.inputCount == 0
                || mouseX < MineralisRecipeLayout.ARROW_X || mouseX >= MineralisRecipeLayout.ARROW_X + 17
                || mouseY < layout.arrowY || mouseY >= layout.arrowY + 24) return Collections.emptyList();
        return RitualRecipeWrapper.rangeTooltip(searchRange, searchRange * 2 + 1);
    }

    @Override
    public boolean handleClick(Minecraft minecraft, int mouseX, int mouseY, int mouseButton) {
        int pageCount = getPageCount();
        if (mouseButton != 0 || layout == null || pageCount < 2
                || mouseY < layout.footerY || mouseY >= layout.footerY + MineralisRecipeLayout.FOOTER_HEIGHT) return false;
        if (mouseX >= LEFT_PAGE_X && mouseX < LEFT_PAGE_X + PAGE_BUTTON_WIDTH) {
            if (page > 0) setPage(page - 1);
            return true;
        }
        if (mouseX >= RIGHT_PAGE_X && mouseX < RIGHT_PAGE_X + PAGE_BUTTON_WIDTH) {
            if (page + 1 < pageCount) setPage(page + 1);
            return true;
        }
        return false;
    }

    void bindLayout(IGuiItemStackGroup itemStacks, MineralisRecipeLayout layout, MineralisRecipeCategory category,
                    IFocus<?> focus) {
        this.itemStacks = itemStacks;
        this.layout = layout;
        this.category = category;
        ItemStack focused = focus != null && focus.getMode() == IFocus.Mode.OUTPUT
                && focus.getValue() instanceof ItemStack ? (ItemStack) focus.getValue() : ItemStack.EMPTY;
        if (!focused.isEmpty()) {
            String focusId = ingredientHelper.getUniqueId(focused);
            String wildcardId = ingredientHelper.getWildcardId(focused);
            int match = -1;
            for (int index = 0; index < drops.size(); index++) {
                String outputId = ingredientHelper.getUniqueId(drops.get(index).getStack());
                // JEI checks the exact identity before its wildcard fallback.
                if (outputId.equals(focusId)) { match = index; break; }
                if (match < 0 && outputId.equals(wildcardId)) match = index;
            }
            if (match >= 0) page = match / layout.pageSize;
        }
        this.page = Math.max(0, Math.min(page, getPageCount() - 1));
        for (int index = 0; index < inputs.size(); index++) {
            itemStacks.init(index, true, layout.inputX(index), layout.inputY(index));
            itemStacks.set(index, inputs.get(index).getVariants());
        }
        int firstVisible = page * layout.pageSize;
        int lastVisible = Math.min(firstVisible + layout.pageSize, drops.size());
        for (int index = 0; index < drops.size(); index++) {
            initSlot(index, index >= firstVisible && index < lastVisible ? index - firstVisible : -1);
        }
    }

    void addInputTooltip(List<String> tooltip) {
        if (noResultProbability * 100.0 >= 0.05)
            tooltip.add(I18n.format("jei.asjeialldrops.no_conversion",
                    MineralisRecipeCategory.formatPercentage(noResultProbability)));
    }

    private void setPage(int newPage) {
        if (newPage == page || newPage < 0 || newPage >= getPageCount()) return;
        int oldPage = page;
        page = newPage;
        positionPage(oldPage, false);
        positionPage(page, true);
    }

    private void positionPage(int targetPage, boolean visible) {
        int first = targetPage * layout.pageSize;
        int last = Math.min(first + layout.pageSize, drops.size());
        for (int index = first; index < last; index++) initSlot(index, visible ? index - first : -1);
    }

    private void initSlot(int index, int visibleIndex) {
        // Each concrete drop stays in its own output slot, even when its page is hidden.
        int slotIndex = inputs.size() + index;
        if (visibleIndex < 0)
            itemStacks.init(slotIndex, false, HIDDEN_RENDERER, HIDDEN_POSITION, HIDDEN_POSITION, 18, 18, 1, 1);
        else itemStacks.init(slotIndex, false, layout.outputX(visibleIndex), layout.outputY(visibleIndex));
        itemStacks.set(slotIndex, drops.get(index).getStack());
    }

    private int getPageCount() {
        return layout != null ? Math.max(1, (drops.size() + layout.pageSize - 1) / layout.pageSize) : 1;
    }
}
