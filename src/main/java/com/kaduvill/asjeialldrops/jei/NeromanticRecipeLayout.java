package com.kaduvill.asjeialldrops.jei;

/** Mineralis-style vertical flow: centered fuel, downward arrow and nine-column output grid. */
final class NeromanticRecipeLayout {
    static final int COLUMNS = 9, SLOT = 18, WIDTH = COLUMNS * SLOT;
    static final int INPUT_OUTPUT_GAP = 32, MAX_OUTPUT_ROWS = 9, FOOTER_HEIGHT = 13;
    static final int HIDDEN = -10000;
    private static final int ARROW_WIDTH = 17, ARROW_HEIGHT = 24;
    final int inputX = (WIDTH - SLOT) / 2;
    final int inputY = 0;
    final int outputY = SLOT + INPUT_OUTPUT_GAP;
    final int arrowY = outputY - (INPUT_OUTPUT_GAP + ARROW_HEIGHT) / 2;
    final int height, footerY, pages, pageSize;

    NeromanticRecipeLayout(int count) {
        if (count < 0) throw new IllegalArgumentException("Negative fluid count");
        int rows = count == 0 ? 1 : Math.min(MAX_OUTPUT_ROWS, 1 + (count - 1) / COLUMNS);
        pageSize = rows * COLUMNS;
        footerY = outputY + rows * SLOT + 2;
        pages = count == 0 ? 1 : 1 + (count - 1) / pageSize;
        height = footerY + (pages > 1 ? FOOTER_HEIGHT : 0);
    }

    int outputX(int relative, int visibleCount) {
        int rowStart = relative / COLUMNS * COLUMNS;
        int rowCount = Math.min(COLUMNS, visibleCount - rowStart);
        return (WIDTH - rowCount * SLOT) / 2 + relative % COLUMNS * SLOT;
    }
    int outputY(int relative) { return outputY + relative / COLUMNS * SLOT; }
    int visible(int count, int page) { return Math.max(0, Math.min(pageSize, count - page * pageSize)); }

    boolean isOverArrow(int x, int y) {
        // Covers the rotated 24x17 drawable, including its half-pixel horizontal placement.
        return x >= (WIDTH - ARROW_WIDTH) / 2 && x < (WIDTH + ARROW_WIDTH + 1) / 2
                && y >= arrowY && y < arrowY + ARROW_HEIGHT;
    }
}
