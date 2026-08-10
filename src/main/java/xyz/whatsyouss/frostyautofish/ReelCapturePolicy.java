package xyz.whatsyouss.frostyautofish;

final class ReelCapturePolicy {
    static final int COLLECTION_TICKS = 20;
    static final double HOOK_ORIGIN_RANGE = 10.0;
    static final double PLAYER_ORIGIN_RANGE = 8.0;

    private ReelCapturePolicy() {
    }

    static boolean isInsideCaptureArea(
            double distanceToHookOrigin,
            double distanceToPlayerOrigin,
            double distanceToCurrentPlayer
    ) {
        return distanceToHookOrigin <= HOOK_ORIGIN_RANGE
                || distanceToPlayerOrigin <= PLAYER_ORIGIN_RANGE
                || distanceToCurrentPlayer <= PLAYER_ORIGIN_RANGE;
    }
}
