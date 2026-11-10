package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ore.OreTable;
import com.kaduvill.asjeialldrops.ritual.AstralEvorsioReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;

/** All concrete placements retain separate indexed outputs, including outputs on hidden pages. */
final class EvorsioRecipeWrapper extends RitualRecipeWrapper {
    private static final int DESCRIPTION_Y = 26, ARROW_X = 72, HIDDEN = -10000;
    private static final IIngredientRenderer<ItemStack> HIDDEN_RENDERER = (minecraft, x, y, stack) -> { };
    private final AstralEvorsioReader.Snapshot data;
    private final RitualOutputGridLayout layout;
    private final int arrowY;
    private int page;

    EvorsioRecipeWrapper(AstralEvorsioReader.Snapshot data) {
        super(true, data.range, "ritual.corrupted.evorsio");
        this.data = data;
        arrowY = DESCRIPTION_Y + paragraphHeight("evorsio.placement") + 4;
        layout = new RitualOutputGridLayout(data.table.outputs.size(), arrowY + 29);
    }

    @Override int height() { return layout.height; }

    @Override boolean showSpeedHint() { return true; }

    @Override List<List<ItemStack>> outputLists() {
        List<List<ItemStack>> result = new ArrayList<>();
        for (OreTable.Output output : data.table.outputs) result.add(Collections.singletonList(output.getStack()));
        return result;
    }

    static int findOutput(List<OreTable.Output> outputs, ItemStack searched, IIngredientHelper<ItemStack> helper) {
        if (searched.isEmpty()) return -1;
        String focusId = helper.getUniqueId(searched);
        String wildcardId = helper.getWildcardId(searched);
        int fallback = -1;
        for (int i = 0; i < outputs.size(); i++) {
            String outputId = helper.getUniqueId(outputs.get(i).getStack());
            // Follow JEI's exact identity before its wildcard fallback; unrelated NBT is not an identity.
            if (outputId.equals(focusId)) return i;
            if (fallback < 0 && outputId.equals(wildcardId)) fallback = i;
        }
        return fallback;
    }

    @Override void bind(IRecipeLayout recipeLayout, RitualRecipeCategory category) {
        super.bind(recipeLayout, category);
        IFocus<?> focus = recipeLayout.getFocus();
        if (focus != null && focus.getMode() == IFocus.Mode.OUTPUT && focus.getValue() instanceof ItemStack) {
            int selected = findOutput(data.table.outputs, (ItemStack) focus.getValue(), category.itemHelper);
            if (selected >= 0) page = selected / layout.pageSize;
        }
        page = Math.max(0, Math.min(page, layout.pages(data.table.outputs.size()) - 1));
        int first = page * layout.pageSize;
        for (int i = 0; i < data.table.outputs.size(); i++)
            initSlot(i, i >= first && i < first + layout.pageSize ? i - first : -1);
        items.addTooltipCallback((slot, input, ingredient, tooltip) -> {
            if (input || slot < 1 || slot > data.table.outputs.size()) return;
            tooltip.add(text("chance", JeiFormats.percentage(data.table.outputs.get(slot - 1).probability)));
            tooltip.add(text("evorsio.per_placement"));
        });
    }

    private void initSlot(int index, int visible) {
        if (visible < 0) items.init(index + 1, false, HIDDEN_RENDERER, HIDDEN, HIDDEN, 18, 18, 1, 1);
        else items.init(index + 1, false, layout.gridX(visible), layout.gridY(visible));
        items.set(index + 1, data.table.outputs.get(index).getStack());
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        super.drawInfo(minecraft, width, height, mouseX, mouseY);
        if (category == null) return;
        paragraph(minecraft, "evorsio.placement", DESCRIPTION_Y, 0x404040);
        category.drawArrow(minecraft, ARROW_X, arrowY, true);
        for (int i = 0; i < layout.pageSize; i++) category.slot.draw(minecraft, layout.gridX(i), layout.gridY(i));
        FontRenderer font = minecraft.fontRenderer;
        if (layout.footerY >= 0) {
            String pages = text("page", page + 1, layout.pages(data.table.outputs.size()));
            font.drawString(pages, (width - font.getStringWidth(pages)) / 2, layout.footerY + 2, 0x606060);
            font.drawString("<", 6, layout.footerY + 2, page > 0 ? 0x404040 : 0xA0A0A0);
            font.drawString(">", width - 12, layout.footerY + 2,
                    page + 1 < layout.pages(data.table.outputs.size()) ? 0x404040 : 0xA0A0A0);
        }
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (x < 0 || x >= RitualRecipeCategory.WIDTH) return Collections.emptyList();
        if (x >= ARROW_X && x < ARROW_X + 18 && y >= arrowY && y < arrowY + 24) {
            List<String> tooltip = new ArrayList<>(rangeTooltip());
            if (data.emptyOreTable) tooltip.add(text("evorsio.empty_table"));
            return tooltip;
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
        if (target < 0 || target >= layout.pages(data.table.outputs.size())) return true;
        int old = page;
        page = target;
        positionPage(old, false);
        positionPage(page, true);
        return true;
    }

    private void positionPage(int target, boolean visible) {
        int first = target * layout.pageSize;
        for (int i = first; i < Math.min(first + layout.pageSize, data.table.outputs.size()); i++)
            initSlot(i, visible ? i - first : -1);
    }
}
