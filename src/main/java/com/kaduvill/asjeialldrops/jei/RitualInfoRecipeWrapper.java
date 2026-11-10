package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ritual.RitualEffects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;

final class RitualInfoRecipeWrapper extends RitualRecipeWrapper {
    private static final int ARROW_X = 69;
    private static final int ARROW_Y = 27;
    private static final int DESCRIPTION_Y = 52;
    private final String key;
    private final RitualEffects.Snapshot effect;

    RitualInfoRecipeWrapper(double range, String key) {
        super(true, range, "ritual.corrupted." + key);
        this.key = key;
        effect = null;
    }

    RitualInfoRecipeWrapper(RitualEffects.Snapshot effect, boolean corrupted) {
        super(corrupted, effect.range, effect.key + (corrupted ? ".corrupted" : ".normal") + ".title");
        this.effect = effect;
        key = effect.key;
    }

    private String prefix() { return key + (corrupted ? ".corrupted" : ".normal"); }

    private String descriptionKey() {
        if (effect == null) return key + ".corrupted_description";
        if (!corrupted && key.equals("evorsio")) return null;
        return prefix() + ".description" + (!corrupted && effect.denyAllSpawns
                && key.equals("lucerna") ? "_all" : "");
    }

    private boolean hasLensRange() { return effect.rangePerLens > 0 && !effect.dimensionWide(corrupted); }

    String noteKey() {
        if (effect == null)
            return key.equals("mineralis") ? "mineralis.normal_transmutation" : "octans.normal_fishing_loot";
        if (key.equals("armara"))
            return effect.denyAllSpawns ? "effects.detail.spawns_all" : "effects.detail.spawns_monsters";
        return null;
    }

    @Override int height() {
        String description = descriptionKey();
        int height = description == null ? ARROW_Y + 17 : DESCRIPTION_Y + paragraphHeight(description);
        String note = noteKey();
        if (note != null) height += 8 + paragraphHeight(note);
        return height + 2;
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        super.drawInfo(minecraft, width, height, mouseX, mouseY);
        if (category != null) category.drawArrow(minecraft, ARROW_X, ARROW_Y, false);
        String description = descriptionKey();
        int y = description == null ? ARROW_Y + 17 : paragraph(minecraft, description, DESCRIPTION_Y, 0x404040);
        String note = noteKey();
        if (note != null) paragraph(minecraft, note, y + 8, 0x808080);
    }

    @Override boolean showSpeedHint() {
        switch (key) {
            case "mineralis":
            case "octans":
            case "aevitas":
            case "evorsio":
            case "discidia":
                return true;
            case "horologium":
                return !corrupted;
            default:
                return false;
        }
    }

    @Override List<String> rangeTooltip() {
        if ((!corrupted && key.equals("lucerna")) || (corrupted && key.equals("horologium")))
            return rangeTooltip(range, text("range_sphere"), showSpeedHint());
        List<String> tooltip = super.rangeTooltip();
        if (key.equals("armara"))
            tooltip.add(tooltip.size() - 1, text("range_spawn_sphere"));
        return tooltip;
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (x < ARROW_X || x >= ARROW_X + 24 || y < ARROW_Y || y >= ARROW_Y + 17)
            return Collections.emptyList();
        if (effect != null) {
            List<String> tooltip = effect.dimensionWide(corrupted)
                    ? new ArrayList<>(Collections.singletonList(text("effects.dimension_scope"))) : rangeTooltip();
            if (hasLensRange()) tooltip.add(text("effects.lens_range", number(effect.rangePerLens)));
            for (RitualEffects.Detail detail : corrupted ? effect.corruptedDetails : effect.normalDetails) {
                Object[] arguments = detail.arguments();
                for (int i = 0; i < arguments.length; i++)
                    if (arguments[i] instanceof Number) arguments[i] = number(((Number) arguments[i]).doubleValue());
                tooltip.add(text(detail.key, arguments));
            }
            return tooltip;
        }
        return rangeTooltip();
    }
}
