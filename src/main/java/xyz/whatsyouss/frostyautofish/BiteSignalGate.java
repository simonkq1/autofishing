package xyz.whatsyouss.frostyautofish;

final class BiteSignalGate {
    static final int ARMING_TICKS = 10;

    private long acceptAfterTick;
    private boolean hypixelObservedClear;
    private boolean vanillaObservedClear;

    void reset(long currentTick, boolean hypixelPresent, boolean vanillaBiting) {
        acceptAfterTick = currentTick + ARMING_TICKS;
        hypixelObservedClear = !hypixelPresent;
        vanillaObservedClear = !vanillaBiting;
    }

    boolean acceptsHypixel(long currentTick, boolean markerPresent) {
        if (!markerPresent) {
            hypixelObservedClear = true;
            return false;
        }
        return currentTick >= acceptAfterTick && hypixelObservedClear;
    }

    boolean acceptsVanilla(long currentTick, boolean biting) {
        if (!biting) {
            vanillaObservedClear = true;
            return false;
        }
        return currentTick >= acceptAfterTick && vanillaObservedClear;
    }
}
