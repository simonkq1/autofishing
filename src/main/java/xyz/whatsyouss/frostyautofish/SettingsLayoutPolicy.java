package xyz.whatsyouss.frostyautofish;

final class SettingsLayoutPolicy {
    static final int BREAKPOINT = 500;
    static final int PANEL_MAX_WIDTH = 680;
    static final int SCREEN_MARGIN = 8;
    static final int COLUMN_GAP = 8;
    static final int NARROW_NAV_WIDTH = 72;
    static final int WIDE_NAV_WIDTH = 112;
    static final int SCROLL_RESERVE = 28;

    private SettingsLayoutPolicy() {
    }

    static Dimensions forWidth(int screenWidth) {
        int panelWidth = Math.max(1, Math.min(PANEL_MAX_WIDTH, screenWidth - SCREEN_MARGIN * 2));
        boolean stacked = screenWidth < BREAKPOINT;
        int navWidth = stacked ? NARROW_NAV_WIDTH : WIDE_NAV_WIDTH;
        navWidth = Math.min(navWidth, Math.max(1, panelWidth - COLUMN_GAP - SCROLL_RESERVE - 1));
        int viewportWidth = Math.max(SCROLL_RESERVE + 1, panelWidth - navWidth - COLUMN_GAP);
        int contentWidth = Math.max(1, viewportWidth - SCROLL_RESERVE);
        int controlWidth = stacked
                ? contentWidth
                : Math.min(160, Math.max(110, contentWidth / 3));
        int textWidth = stacked
                ? contentWidth
                : Math.max(1, contentWidth - controlWidth - COLUMN_GAP);
        return new Dimensions(panelWidth, navWidth, viewportWidth, contentWidth, textWidth, controlWidth, stacked);
    }

    record Dimensions(
            int panelWidth,
            int navigationWidth,
            int viewportWidth,
            int contentWidth,
            int textWidth,
            int controlWidth,
            boolean stacked
    ) {
    }
}
