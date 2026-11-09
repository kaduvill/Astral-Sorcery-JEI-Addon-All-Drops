package com.kaduvill.asjeialldrops.jei;

import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;

final class RitualInfoRecipeWrapper extends RitualRecipeWrapper {
    private static final int ARROW_X = 69;
    private static final int ARROW_Y = 27;
    private static final int DESCRIPTION_Y = 52;
    private final String key;

    RitualInfoRecipeWrapper(double range, String key) {
        super(true, range, "ritual.corrupted." + key);
        this.key = key;
    }

    private String noteKey() {
        return key.equals("mineralis") ? "mineralis.normal_transmutation" : "octans.normal_fishing_loot";
    }

    @Override int height() {
        int height = DESCRIPTION_Y + paragraphHeight(key + ".corrupted_description");
        height += 8 + paragraphHeight(noteKey());
        return height + 2;
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        super.drawInfo(minecraft, width, height, mouseX, mouseY);
        if (category != null) category.drawArrow(minecraft, ARROW_X, ARROW_Y, false);
        int y = paragraph(minecraft, key + ".corrupted_description", DESCRIPTION_Y, 0x404040);
        paragraph(minecraft, noteKey(), y + 8, 0x808080);
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (x >= ARROW_X && x < ARROW_X + 24 && y >= ARROW_Y && y < ARROW_Y + 17)
            return rangeTooltip();
        return Collections.emptyList();
    }
}
