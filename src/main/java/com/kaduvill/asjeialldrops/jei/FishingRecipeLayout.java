package com.kaduvill.asjeialldrops.jei;

/** Junk Seller/Business-style layout: one source, downward arrow, nine-column paginated grid. */
final class FishingRecipeLayout {
    static final int COLUMNS = 9;
    static final int SLOT = 18;
    static final int SOURCE_X = 72;
    static final int SOURCE_Y = 26;
    static final int ARROW_X = 72;
    static final int ARROW_Y = 52;
    static final int GRID_Y = 81;
    static final int FOOTER_HEIGHT = 13;
    final int pageSize;
    final int footerY;
    final int height;

    FishingRecipeLayout(int entries) {
        if (entries < 0) throw new IllegalArgumentException("Negative fishing output count");
        int rows = Math.max(1, Math.min(5, (entries + COLUMNS - 1) / COLUMNS));
        pageSize = rows * COLUMNS;
        footerY = entries > pageSize ? GRID_Y + rows * SLOT + 2 : -1;
        height = footerY < 0 ? GRID_Y + rows * SLOT : footerY + FOOTER_HEIGHT;
    }

    int gridX(int index) { return index % COLUMNS * SLOT; }
    int gridY(int index) { return GRID_Y + index / COLUMNS * SLOT; }
    int pages(int count) { return Math.max(1, (count + pageSize - 1) / pageSize); }
}
