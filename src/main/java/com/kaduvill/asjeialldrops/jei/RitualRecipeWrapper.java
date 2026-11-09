package com.kaduvill.asjeialldrops.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

public abstract class RitualRecipeWrapper implements IRecipeWrapper {
    final boolean corrupted;
    final double range;
    private final String title;
    RitualRecipeCategory category;
    IGuiItemStackGroup items;

    RitualRecipeWrapper(boolean corrupted, double range, String title) {
        this.corrupted = corrupted;
        this.range = range;
        this.title = title;
    }

    List<List<ItemStack>> inputLists() {
        // Ritual crystals are category catalysts, not transferable recipe ingredients.
        return Collections.emptyList();
    }

    List<List<ItemStack>> outputLists() { return Collections.emptyList(); }

    abstract int height();

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, inputLists());
        ingredients.setOutputLists(VanillaTypes.ITEM, outputLists());
    }

    void bind(IRecipeLayout layout, RitualRecipeCategory category) {
        this.category = category;
        items = layout.getItemStacks();
        // Example slots control their synchronized display independently of lookup focus.
        items.setOverrideDisplayFocus(null);
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        if (category == null) return;
        String label = text(title);
        minecraft.fontRenderer.drawString(label, (width - minecraft.fontRenderer.getStringWidth(label)) / 2,
                corrupted ? 0 : 5, 0x404040);
        if (corrupted) {
            String requirement = text("ritual.alcara");
            minecraft.fontRenderer.drawString(requirement,
                    (width - minecraft.fontRenderer.getStringWidth(requirement)) / 2, 11, 0x806080);
        }
        // Text rendering leaves its tint active; restore white before drawing slots and icons.
        GlStateManager.color(1, 1, 1, 1);
    }

    final int paragraph(Minecraft minecraft, String key, int y, int color) {
        for (String line : minecraft.fontRenderer.listFormattedStringToWidth(text(key), RitualRecipeCategory.WIDTH - 4)) {
            minecraft.fontRenderer.drawString(line, (RitualRecipeCategory.WIDTH
                    - minecraft.fontRenderer.getStringWidth(line)) / 2, y, color);
            y += minecraft.fontRenderer.FONT_HEIGHT + 1;
        }
        return y;
    }

    final int paragraphHeight(String key) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        return font.listFormattedStringToWidth(text(key), RitualRecipeCategory.WIDTH - 4).size()
                * (font.FONT_HEIGHT + 1);
    }

    List<String> rangeTooltip() {
        return rangeTooltip(range, rangeBoxSize());
    }

    double rangeBoxSize() { return range * 2 + 1; }

    static List<String> rangeTooltip(double range, double boxSize) {
        List<String> result = new ArrayList<>();
        result.add(text("range", number(range)));
        String size = number(boxSize);
        result.add(text("range_box", size, size, size));
        result.add(text("range_modifiers"));
        return result;
    }

    private static String number(double value) {
        // AS loads fractional ranges as floats; avoid displaying their widened precision.
        return value == (int) value ? Integer.toString((int) value) : Float.toString((float) value);
    }

    static String text(String key, Object... args) { return I18n.format("jei.asjeialldrops." + key, args); }
}
