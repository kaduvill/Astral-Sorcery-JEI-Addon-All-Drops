package com.kaduvill.asjeialldrops.jei;

import com.kaduvill.asjeialldrops.neromantic.NeromanticProbabilities;
import com.kaduvill.asjeialldrops.neromantic.NeromanticRecipes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.fluids.FluidStack;

/** One complete output overview. Only UI page/slot bindings change after registration. */
public final class NeromanticRecipeWrapper implements IRecipeWrapper {
    private final NeromanticRecipes.Snapshot data;
    private final NeromanticRecipeLayout layout;
    private final List<List<Hint>> outputHints;
    private final List<Hint> productionHints;
    private IGuiFluidStackGroup fluids;
    private IDrawable slot;
    private List<IIngredientRenderer<FluidStack>> renderers;
    private int page;

    NeromanticRecipeWrapper(NeromanticRecipes.Snapshot data, NeromanticRecipeLayout layout) {
        this.data = data;
        this.layout = layout;
        List<List<Hint>> hints = new ArrayList<>();
        for (NeromanticRecipes.Output output : data.outputs) hints.add(Collections.unmodifiableList(describe(output)));
        outputHints = Collections.unmodifiableList(hints);
        List<Hint> production = new ArrayList<>();
        if (NeromanticProbabilities.validQuantity(data.rules.quantity)) {
            // Compact display uses exclusive upper bounds; actual draws are Q..2Q-1 and 20..29.
            production.add(new Hint("output", data.rules.quantity, 2L * data.rules.quantity));
        } else production.add(new Hint("output_unknown"));
        production.add(new Hint("interval", 20, 30));
        productionHints = Collections.unmodifiableList(production);
    }

    @Override public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, data.getFuel());
        // Separate outputs, never a single alternatives slot: this is also the complete filter list.
        ingredients.setOutputs(VanillaTypes.FLUID, data.getOutputs());
    }

    void bind(IGuiFluidStackGroup fluids, IFocus<?> focus, IDrawable slot,
            IIngredientRenderer<FluidStack> renderer, IIngredientHelper<FluidStack> helper) {
        this.fluids = fluids;
        this.slot = slot;
        page = Math.max(0, Math.min(page, layout.pages - 1));
        if (focus != null && focus.getMode() == IFocus.Mode.OUTPUT && focus.getValue() instanceof FluidStack) {
            FluidStack searched = (FluidStack) focus.getValue();
            for (int i = 0; i < data.outputs.size(); i++) {
                if (helper.getMatch(Collections.singletonList(data.outputs.get(i).getStack()), searched) != null) {
                    page = i / layout.pageSize;
                    break;
                }
            }
        }
        fluids.init(0, true, new FluidRenderer(-1, renderer, data.getFuel()),
                layout.inputX + 1, layout.inputY + 1, 16, 16, 0, 0);
        fluids.set(0, data.getFuel());
        fluids.addTooltipCallback((index, input, ingredient, tooltip) -> {
            if (input) {
                add(tooltip, new Hint("fuel_consumption", 1));
            } else if (index > 0 && index <= outputHints.size()) {
                for (Hint hint : outputHints.get(index - 1)) add(tooltip, hint);
            }
        });
        renderers = new ArrayList<>();
        for (int i = 0; i < data.outputs.size(); i++) {
            renderers.add(new FluidRenderer(i, renderer, data.outputs.get(i).getStack()));
        }
        // Every output is a real independent JEI slot, even when its page is offscreen.
        for (int i = 0; i < data.outputs.size(); i++) initOutput(i);
    }

    private void initOutput(int index) {
        int relative = index - page * layout.pageSize;
        int count = layout.visible(data.outputs.size(), page);
        boolean visible = relative >= 0 && relative < count;
        fluids.init(index + 1, false, renderers.get(index),
                visible ? layout.outputX(relative, count) + 1 : NeromanticRecipeLayout.HIDDEN,
                visible ? layout.outputY(relative) + 1 : NeromanticRecipeLayout.HIDDEN, 16, 16, 0, 0);
        fluids.set(index + 1, data.outputs.get(index).getStack());
    }

    @Override public void drawInfo(Minecraft minecraft, int width, int height, int mouseX, int mouseY) {
        if (slot == null) return;
        int count = layout.visible(data.outputs.size(), page);
        for (int i = 0; i < count; i++) slot.draw(minecraft, layout.outputX(i, count), layout.outputY(i));
        if (layout.pages < 2) return;
        try {
            String counter = I18n.format("jei.asjeialldrops.page", page + 1, layout.pages);
            minecraft.fontRenderer.drawString(counter, (width - minecraft.fontRenderer.getStringWidth(counter)) / 2,
                    layout.footerY + 2, 0x606060);
            minecraft.fontRenderer.drawString("<", 3, layout.footerY + 2, page > 0 ? 0x404040 : 0xA0A0A0);
            minecraft.fontRenderer.drawString(">", width - 9, layout.footerY + 2, page + 1 < layout.pages ? 0x404040 : 0xA0A0A0);
        } finally {
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    @Override public boolean handleClick(Minecraft minecraft, int x, int y, int button) {
        if (button != 0 || fluids == null || layout.pages < 2 || y < layout.footerY || y >= layout.height) return false;
        int next = page;
        if (x >= 0 && x < 18) next--;
        else if (x >= NeromanticRecipeLayout.WIDTH - 18 && x < NeromanticRecipeLayout.WIDTH) next++;
        else return false;
        if (next < 0 || next >= layout.pages) return true;
        int old = page;
        page = next;
        positionPage(old);
        positionPage(page);
        return true;
    }

    private void positionPage(int target) {
        int first = target * layout.pageSize;
        int end = first + layout.visible(data.outputs.size(), target);
        for (int i = first; i < end; i++) initOutput(i);
    }

    @Override public List<String> getTooltipStrings(int x, int y) {
        if (!layout.isOverArrow(x, y)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Hint hint : productionHints) add(result, hint);
        return result;
    }

    private List<Hint> describe(NeromanticRecipes.Output output) {
        List<Hint> result = new ArrayList<>();
        if (output.waterFallback) {
            result.add(new Hint("water_fallback"));
        } else {
            if (data.rules.dimensionsAvailable) {
                int[] dimensions = output.getDimensions();
                result.add(dimensions.length == 0 ? new Hint("all_dimensions") : new Hint(dimensions.length == 1 ? "dimension" : "dimensions",
                        Arrays.stream(dimensions).mapToObj(String::valueOf).collect(Collectors.joining(", "))));
            }
            if (data.rules.reserveKnown) {
                Map<List<Hint>, List<NeromanticRecipes.Pool>> reservoirs = new LinkedHashMap<>();
                Map<String, List<NeromanticRecipes.Pool>> occurrences = new LinkedHashMap<>();
                int relevant = 0;
                for (NeromanticRecipes.Pool pool : data.pools) {
                    if (Collections.disjoint(output.entries, pool.entries)) continue;
                    relevant++;
                    if (!pool.valid()) continue;
                    List<Hint> amounts = describeReservoirs(output, pool);
                    if (!amounts.isEmpty()) reservoirs.computeIfAbsent(amounts, key -> new ArrayList<>()).add(pool);
                    double occurrence = data.occurrence(output, pool);
                    if (!Double.isNaN(occurrence)) occurrences.computeIfAbsent(JeiFormats.compactPercentage(occurrence),
                            key -> new ArrayList<>()).add(pool);
                }
                final int total = relevant;
                // Group presentation only after each pool's tickets and reserve branches are analyzed.
                reservoirs.forEach((hints, pools) -> {
                    Hint scope = pools.size() == total ? null : dimensionScope(pools);
                    for (Hint hint : hints) result.add(hint.scoped(scope));
                });
                occurrences.forEach((percentage, pools) -> result.add(new Hint("occurrence", percentage)
                        .scoped(pools.size() == total ? null : dimensionScope(pools))));
            }
        }
        return result;
    }

    private List<Hint> describeReservoirs(NeromanticRecipes.Output output, NeromanticRecipes.Pool pool) {
        List<NeromanticProbabilities.Amount> amounts = new ArrayList<>();
        boolean infinite = false;
        for (int index : output.entries) {
            if (pool.share(index) == 0) continue;
            NeromanticProbabilities.Reserve reserve = data.reserve(index);
            if (!reserve.valid) return Collections.emptyList();
            amounts.addAll(reserve.finite);
            infinite |= reserve.infinite;
        }
        List<Hint> alternatives = new ArrayList<>();
        for (NeromanticProbabilities.Amount amount : NeromanticProbabilities.mergeAmounts(amounts)) {
            alternatives.add(amount.minimum == amount.maximum ? new Hint("amount", amount.minimum)
                    : new Hint("amount_range", amount.minimum, amount.maximum));
        }
        if (infinite) alternatives.add(new Hint("infinite"));
        return Collections.singletonList(alternatives.isEmpty() ? new Hint("reservoir_empty")
                : new Hint("reservoir", new Alternatives(alternatives)));
    }

    private Hint dimensionScope(List<NeromanticRecipes.Pool> pools) {
        TreeSet<Integer> ids = new TreeSet<>();
        boolean other = false;
        for (NeromanticRecipes.Pool pool : pools) { ids.addAll(pool.dimensions); other |= pool.otherDimensions; }
        if (other) {
            TreeSet<Integer> excluded = new TreeSet<>();
            for (NeromanticRecipes.Pool pool : data.pools) excluded.addAll(pool.dimensions);
            excluded.removeAll(ids);
            return new Hint("all_except_dimensions", joinIds(excluded));
        }
        return new Hint(ids.size() == 1 ? "dimension_id" : "dimension_ids", joinIds(ids));
    }

    private static String joinIds(Iterable<Integer> ids) {
        List<String> result = new ArrayList<>();
        for (int id : ids) result.add(String.valueOf(id));
        return String.join(", ", result);
    }

    private static void add(List<String> tooltip, Hint hint) {
        // Keep individual lines readable on ordinary JEI screens; no registry access while drawing.
        tooltip.addAll(Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(hint.format(), 280));
    }

    private static String text(String key, Object... args) { return I18n.format("jei.asjeialldrops.neromantic." + key, args); }

    private static final class Hint {
        private final String key;
        private final Object[] args;
        private final Hint scope;
        Hint(String key, Object... args) { this(key, args, null); }
        private Hint(String key, Object[] args, Hint scope) { this.key = key; this.args = args.clone(); this.scope = scope; }
        Hint scoped(Hint scope) { return new Hint(key, args, scope); }
        String format() {
            Object[] values = args.clone();
            for (int i = 0; i < values.length; i++) if (values[i] instanceof Alternatives) values[i] = ((Alternatives) values[i]).format();
            String value = text(key, values);
            return scope == null ? value : text("in_dimensions", value, scope.format());
        }
        @Override public boolean equals(Object other) {
            if (!(other instanceof Hint)) return false;
            Hint hint = (Hint) other;
            return key.equals(hint.key) && Arrays.deepEquals(args, hint.args) && Objects.equals(scope, hint.scope);
        }
        @Override public int hashCode() { return Objects.hash(key, Arrays.deepHashCode(args), scope); }
    }

    private static final class Alternatives {
        private final List<Hint> values;
        Alternatives(List<Hint> values) { this.values = Collections.unmodifiableList(new ArrayList<>(values)); }
        String format() { return values.stream().map(Hint::format).collect(Collectors.joining(" " + text("or") + " ")); }
        @Override public boolean equals(Object other) { return other instanceof Alternatives && values.equals(((Alternatives) other).values); }
        @Override public int hashCode() { return values.hashCode(); }
    }

    private final class FluidRenderer implements IIngredientRenderer<FluidStack> {
        private final int index;
        private final IIngredientRenderer<FluidStack> delegate;
        private final FluidStack full;
        FluidRenderer(int index, IIngredientRenderer<FluidStack> delegate, FluidStack identity) {
            this.index = index;
            this.delegate = delegate;
            full = identity.copy();
            full.amount = 1000;
        }
        @Override public void render(Minecraft minecraft, int x, int y, FluidStack ingredient) {
            if (index < 0 || index / layout.pageSize == page) delegate.render(minecraft, x, y, full);
        }
        @Override public List<String> getTooltip(Minecraft minecraft, FluidStack ingredient, ITooltipFlag flag) {
            // The full icon is cosmetic; do not invent a fixed volume of fuel/output.
            return new ArrayList<>(Collections.singletonList(ingredient.getLocalizedName()));
        }
    }
}
