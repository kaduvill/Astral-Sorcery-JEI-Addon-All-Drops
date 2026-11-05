package com.kaduvill.asjeialldrops.jei;

/** One row leaves the full label row clear, including the longer support conditions. */
final class ClusterRecipeLayout {
    static final int WIDTH = 162;
    static final int HEIGHT = 44;
    static final int ITEM_Y = 13;
    static final int ARROW_X = 66;
    static final int ARROW_Y = 13;
    static final int LABEL_Y = 34;
    static final int OUTPUT_INDEX = 2;

    private ClusterRecipeLayout() {}

    static int inputX(int count, int index) { return count == 1 ? 30 : 8 + index * 22; }
    static int outputX(int count, int index) { return count == 1 ? 115 : 105 + index * 20; }
}
