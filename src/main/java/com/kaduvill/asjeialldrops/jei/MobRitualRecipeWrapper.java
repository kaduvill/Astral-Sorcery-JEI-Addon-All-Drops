package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.ritual.RitualRecipes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Mob faces use eggs only as hidden navigation identities, never as recipe ingredients. */
final class MobRitualRecipeWrapper extends RitualRecipeWrapper {
    enum Mode { DAY, NIGHT, CONVERSION, HERDING }
    private static final long CYCLE_NANOS = 1_000_000_000L;
    private static final int INPUT = 1;
    private static final int OUTPUT = 2;
    private final Mode mode;
    private final List<RitualRecipes.Mob> examples;
    private final RitualRecipes.Conversion conversion;
    private final RitualMobFaces faces;
    private int index;
    private int displayed = -1;
    private long lastDraw = System.nanoTime();
    private long phase;

    MobRitualRecipeWrapper(RitualMobFaces faces, RitualRecipes.Snapshot snapshot, Mode mode,
                           List<RitualRecipes.Mob> examples, boolean corrupted) {
        super(corrupted, snapshot.range,
                mode == Mode.HERDING ? (corrupted ? "ritual.corrupted.bootes" : "bootes.herding")
                        : "pelotrio." + (mode == Mode.DAY ? "day" : "night"));
        this.faces = faces;
        this.mode = mode;
        this.examples = Collections.unmodifiableList(new ArrayList<>(examples));
        conversion = null;
    }

    MobRitualRecipeWrapper(RitualMobFaces faces, RitualRecipes.Snapshot snapshot, RitualRecipes.Conversion conversion) {
        super(true, snapshot.range, "ritual.corrupted.pelotrio");
        this.faces = faces;
        mode = Mode.CONVERSION;
        examples = Collections.emptyList();
        this.conversion = conversion;
    }

    private boolean hasMobInput() { return mode == Mode.CONVERSION || mode == Mode.HERDING; }

    private String captionKey() {
        return mode == Mode.HERDING ? (corrupted ? "bootes.corrupted_description" : "bootes.description")
                : mode == Mode.CONVERSION ? "pelotrio.conversion_description" : "pelotrio.examples";
    }

    @Override int height() {
        int height = 65 + paragraphHeight(captionKey());
        if (mode == Mode.HERDING) height += 3 + paragraphHeight("bootes.example_drops");
        return height + 2;
    }

    @Override List<List<ItemStack>> outputLists() {
        if (mode != Mode.HERDING) return Collections.emptyList();
        List<ItemStack> outputs = new ArrayList<>();
        for (RitualRecipes.Mob mob : examples) outputs.add(mob.loot());
        return Collections.singletonList(outputs);
    }

    boolean matchesEggAlias(IFocus.Mode direction, ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() != Items.SPAWN_EGG) return false;
        ResourceLocation id = ItemMonsterPlacer.getNamedIdFrom(stack);
        if (id == null) return false;
        if (conversion != null) return id.equals(direction == IFocus.Mode.INPUT ? conversion.input.id : conversion.output.id);
        if (direction != (hasMobInput() ? IFocus.Mode.INPUT : IFocus.Mode.OUTPUT)) return false;
        for (RitualRecipes.Mob mob : examples) if (id.equals(mob.id)) return true;
        return false;
    }

    @Override void bind(IRecipeLayout layout, RitualRecipeCategory category) {
        super.bind(layout, category);
        IFocus<?> focus = layout.getFocus();
        if (focus != null && focus.getValue() instanceof ItemStack && conversion == null) {
            ItemStack focused = (ItemStack) focus.getValue();
            for (int i = 0; i < examples.size(); i++) {
                boolean matches = mode == Mode.HERDING && focus.getMode() == IFocus.Mode.OUTPUT
                        ? ItemStack.areItemsEqual(examples.get(i).loot(), focused)
                        : examples.get(i).id.equals(ItemMonsterPlacer.getNamedIdFrom(focused));
                if (matches) { index = i; phase = i * CYCLE_NANOS; break; }
            }
        }
        lastDraw = System.nanoTime();
        // Native face hitboxes retain R/U behavior. The underlying eggs are display-only;
        // MobRitualRecipeLookup owns recipe discovery instead of IIngredients.
        if (hasMobInput()) items.init(INPUT, true, renderer(INPUT, true), 18, 34, 18, 18, 1, 1);
        items.init(OUTPUT, false, renderer(OUTPUT, mode != Mode.HERDING), 124, 34, 18, 18, 1, 1);
        displayed = -1;
        sync();
        items.addTooltipCallback((slot, input, ingredient, tooltip) -> {
            if (slot != OUTPUT) return;
            if (mode == Mode.HERDING)
                tooltip.add(text("bootes.ground"));
            // AS 1.10.27 applies amplifier 3; its death handler makes exactly 3 extra rolls.
            else if (mode == Mode.CONVERSION) tooltip.add(text("pelotrio.inner_wealth"));
        });
    }

    private void sync() {
        if (items == null || displayed == index) return;
        if (conversion != null) {
            items.set(INPUT, conversion.input.egg());
            items.set(OUTPUT, conversion.output.egg());
        } else if (!examples.isEmpty()) {
            RitualRecipes.Mob mob = examples.get(index);
            if (hasMobInput()) items.set(INPUT, mob.egg());
            items.set(OUTPUT, mode == Mode.HERDING ? mob.loot() : mob.egg());
        }
        displayed = index;
    }

    private IIngredientRenderer<ItemStack> renderer(int slot, boolean mobSlot) {
        return new IIngredientRenderer<ItemStack>() {
            @Override public void render(Minecraft minecraft, int x, int y, @Nullable ItemStack ingredient) {
                sync();
                ItemStack current = items.getGuiIngredients().get(slot).getDisplayedIngredient();
                if (current == null || current.isEmpty()) return;
                if (mobSlot) {
                    ResourceLocation id = ItemMonsterPlacer.getNamedIdFrom(current);
                    if (id != null && !faces.draw(minecraft, id, x, y))
                        minecraft.fontRenderer.drawString("?", x + 5, y + 4, 0x808080);
                } else category.itemRenderer.render(minecraft, x, y, current);
            }

            @Override public List<String> getTooltip(Minecraft minecraft, ItemStack ingredient, ITooltipFlag flag) {
                if (mobSlot) {
                    ResourceLocation id = ItemMonsterPlacer.getNamedIdFrom(ingredient);
                    String name = id == null ? null : EntityList.getTranslationName(id);
                    return Collections.singletonList(name == null ? String.valueOf(id) : I18n.format("entity." + name + ".name"));
                }
                return category.itemRenderer.getTooltip(minecraft, ingredient, flag);
            }

            @Override public FontRenderer getFontRenderer(Minecraft minecraft, ItemStack ingredient) {
                return category.itemRenderer.getFontRenderer(minecraft, ingredient);
            }
        };
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        super.drawInfo(minecraft, width, height, mouseX, mouseY);
        long now = System.nanoTime();
        if (examples.size() > 1 && !GuiScreen.isShiftKeyDown()) {
            phase = (phase + now - lastDraw) % (examples.size() * CYCLE_NANOS);
            index = (int) (phase / CYCLE_NANOS);
        }
        lastDraw = now;
        sync();
        if (category == null) return;
        category.drawArrow(minecraft, 69, 34, false);
        if (!hasMobInput()) minecraft.fontRenderer.drawString(text("pelotrio.spawning"), 8, 39, 0x808080);
        if (mode == Mode.HERDING) category.slot.draw(minecraft, 124, 34);
        int y = paragraph(minecraft, captionKey(), 65, 0x404040);
        if (mode == Mode.HERDING) paragraph(minecraft, "bootes.example_drops", y + 3, 0x808080);
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (x >= 69 && x < 93 && y >= 34 && y < 51) return rangeTooltip();
        return Collections.emptyList();
    }

    @Override boolean showSpeedHint() { return mode != Mode.HERDING || !corrupted; }

    @Override double rangeBoxSize() {
        // Corrupted Pelotrio grows a point box; the other rituals include the pedestal block.
        return mode == Mode.CONVERSION ? range * 2 : super.rangeBoxSize();
    }

}
