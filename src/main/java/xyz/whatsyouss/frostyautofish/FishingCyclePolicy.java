package xyz.whatsyouss.frostyautofish;

final class FishingCyclePolicy {
    static final int MIN_REEL_DELAY_MILLIS = 10;
    static final int MAX_REEL_DELAY_MILLIS = 300;
    static final int DEFAULT_DRY_CAST_TIMEOUT_SECONDS = 15;
    static final int MAX_CONSECUTIVE_DRY_CASTS = 2;

    private FishingCyclePolicy() {
    }

    static int reelDelayMillis(int randomRoll) {
        if (randomRoll < 0 || randomRoll > MAX_REEL_DELAY_MILLIS - MIN_REEL_DELAY_MILLIS) {
            throw new IllegalArgumentException("randomRoll must be in [0, 290]");
        }
        return MIN_REEL_DELAY_MILLIS + randomRoll;
    }

    static boolean hasTimedOut(long currentTick, long startedTick, int maximumSeconds) {
        return currentTick - startedTick >= maximumSeconds * 20L;
    }

    static boolean shouldStartCombat(int trackedTargets, int triggerAmount) {
        return trackedTargets >= triggerAmount;
    }

    static boolean shouldStopAfterDryCast(int consecutiveDryCasts) {
        if (consecutiveDryCasts < 1) {
            throw new IllegalArgumentException("consecutiveDryCasts must be positive");
        }
        return consecutiveDryCasts >= MAX_CONSECUTIVE_DRY_CASTS;
    }
}
