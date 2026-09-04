package xyz.whatsyouss.frostyautofish;

import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;

/** Independent, monotonic pre-use delay; the ordinary Auto Kill timer is never involved. */
final class HighValueAbilityWait {
    record Key(Object target, int slot, AutoFishConfig.AbilityAim aim, int delayMillis,
               int attackLimit, boolean ranged, boolean useAbility) {
        boolean matches(Key other) {
            return other != null && target == other.target && slot == other.slot && aim == other.aim
                    && delayMillis == other.delayMillis && attackLimit == other.attackLimit
                    && ranged == other.ranged && useAbility == other.useAbility;
        }
    }

    private Key pending;
    private long startedAt;

    boolean allow(boolean safe) {
        if (!safe) {
            cancel();
        }
        return safe;
    }

    boolean ready(Key key, long now, boolean cooldownReady) {
        if (!key.matches(pending)) {
            restart(key, now);
        }
        return cooldownReady && now - startedAt >= key.delayMillis() * 1_000_000L;
    }

    void restart(Key key, long now) {
        pending = key;
        startedAt = now;
    }

    void cancel() {
        pending = null;
    }
}
