package com.interior.platform.media.domain;

public enum DerivativeVariant {
    THUMBNAIL(400, 300),
    MEDIUM(1200, 900),
    LARGE(1920, 1080);

    private final int maxWidth;
    private final int maxHeight;

    DerivativeVariant(int maxWidth, int maxHeight) {
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }
}
