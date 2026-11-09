package com.kaduvill.asjeialldrops.jei;

final class MineralisRecipeLayout {
    static final int COLUMNS = 9;
    static final int SLOT_SIZE = 18;
    static final int WIDTH = COLUMNS * SLOT_SIZE;
    static final int INPUT_OUTPUT_GAP = 32;
    static final int ARROW_X = (WIDTH - 17) / 2;
    static final int FOOTER_HEIGHT = 13;
    private static final int MAX_OUTPUT_ROWS = 9;

    final int inputCount;
    final int pageSize;
    final int outputY;
    final int arrowY;
    final int footerY;
    final int height;

    MineralisRecipeLayout(int inputCount, int dropCount) {
        if (inputCount < 0 || dropCount < 0) throw new IllegalArgumentException("Slot counts cannot be negative");
        this.inputCount = inputCount;
        int inputRows = (inputCount + COLUMNS - 1) / COLUMNS;
        int outputRows = Math.max(1, Math.min(MAX_OUTPUT_ROWS, (dropCount + COLUMNS - 1) / COLUMNS));
        pageSize = outputRows * COLUMNS;
        outputY = inputRows == 0 ? 0 : inputRows * SLOT_SIZE + INPUT_OUTPUT_GAP;
        arrowY = outputY - (INPUT_OUTPUT_GAP + 24) / 2;
        footerY = outputY + outputRows * SLOT_SIZE;
        height = footerY + FOOTER_HEIGHT;
    }

    int inputX(int index) {
        int rowStart = index / COLUMNS * COLUMNS;
        int rowCount = Math.min(COLUMNS, inputCount - rowStart);
        return (WIDTH - rowCount * SLOT_SIZE) / 2 + index % COLUMNS * SLOT_SIZE;
    }

    int inputY(int index) {
        return index / COLUMNS * SLOT_SIZE;
    }

    int outputX(int visibleIndex) {
        return visibleIndex % COLUMNS * SLOT_SIZE;
    }

    int outputY(int visibleIndex) {
        return outputY + visibleIndex / COLUMNS * SLOT_SIZE;
    }
}
