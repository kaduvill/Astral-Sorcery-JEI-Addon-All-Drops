package com.kaduvill.asjeialldrops.jei;

/** Shared nine-column ritual output grid; each ritual supplies the space above its outputs. */
final class RitualOutputGridLayout {
    static final int COLUMNS = 9;
    static final int SLOT = 18;
    static final int FOOTER_HEIGHT = 13;
    final int pageSize;
    final int footerY;
    final int height;
    private final int gridTop;

    RitualOutputGridLayout(int entries, int gridTop) {
        if (entries < 0 || gridTop < 0) throw new IllegalArgumentException("Negative ritual grid geometry");
        this.gridTop = gridTop;
        int rows = Math.max(1, Math.min(5, (entries + COLUMNS - 1) / COLUMNS));
        pageSize = rows * COLUMNS;
        footerY = entries > pageSize ? gridTop + rows * SLOT + 2 : -1;
        height = footerY < 0 ? gridTop + rows * SLOT : footerY + FOOTER_HEIGHT;
    }

    int gridX(int index) { return index % COLUMNS * SLOT; }
    int gridY(int index) { return gridTop + index / COLUMNS * SLOT; }
    int pages(int count) { return Math.max(1, (count + pageSize - 1) / pageSize); }
}
