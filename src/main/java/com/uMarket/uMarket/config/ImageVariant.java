package com.uMarket.uMarket.config;

public enum ImageVariant {
    THUMB(200, 200),
    MEDIUM(600, 600),
    FULL(0, 0);

    private final int maxWidth;
    private final int maxHeight;

    ImageVariant(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public boolean isResize() {
        return maxWidth > 0;
    }
}