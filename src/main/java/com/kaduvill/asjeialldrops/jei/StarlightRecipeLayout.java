package com.kaduvill.asjeialldrops.jei;

/** Fixed geometry for at most three inputs and two outputs. */
final class StarlightRecipeLayout {
    static final int WIDTH = 162;
    static final int HEIGHT = 44;
    static final int SLOT = 18;
    static final int FIRST_INPUT_X = 8;
    static final int SECOND_INPUT_X = 30;
    static final int CENTER_Y = 13;
    static final int ARROW_X = 66;
    static final int ARROW_Y = 13;
    static final int LABEL_Y = 34;
    static final int OUTPUT_INDEX = 2;

    private StarlightRecipeLayout() {}

    static int itemY(int count, int index) {
        return count == 1 ? CENTER_Y : 2 + index * 22;
    }

    static int fluidX(int index) { return FIRST_INPUT_X + index * 22; }

    static int outputX(int count, int index) {
        return count == 1 ? 115 : 105 + index * 20;
    }
}
