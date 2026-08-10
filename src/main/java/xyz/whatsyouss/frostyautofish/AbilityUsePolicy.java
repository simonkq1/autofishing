package xyz.whatsyouss.frostyautofish;

final class AbilityUsePolicy {
    static final int INITIAL_DELAY_MILLIS = 150;
    static final int MIN_JITTER_PERMILLE = -100;
    static final int MAX_JITTER_PERMILLE = 100;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private AbilityUsePolicy() {
    }

    static int jitteredDelayMillis(int configuredDelayMillis, int jitterPermille) {
        if (jitterPermille < MIN_JITTER_PERMILLE || jitterPermille > MAX_JITTER_PERMILLE) {
            throw new IllegalArgumentException("jitterPermille must be in [-100, 100]");
        }
        return (int) Math.round(configuredDelayMillis * (1000 + jitterPermille) / 1000.0);
    }

    static long initialDeadline(long nowNanos, int delayMillis) {
        return nowNanos + delayMillis * NANOS_PER_MILLI;
    }

    static boolean isInitialDelayElapsed(long nowNanos, long deadlineNanos) {
        return nowNanos >= deadlineNanos;
    }
}
