package xyz.whatsyouss.frostyautofish;

final class InputLockPolicy {
    private InputLockPolicy() {
    }

    static Decision decide(
            boolean blocking,
            boolean blockedBinding,
            boolean allowedBinding,
            Phase phase
    ) {
        if (!blocking || !blockedBinding) {
            return Decision.PASS;
        }
        if (phase == Phase.RELEASE || allowedBinding) {
            return Decision.PASS_AND_RESTORE;
        }
        return Decision.BLOCK;
    }

    static boolean shouldSuppressToggleRestore(boolean lockActive, boolean blockedMapping) {
        return lockActive && blockedMapping;
    }

    static int reconcileHotkeyClicks(
            Phase phase,
            boolean matches,
            int beforeEvent,
            int afterEvent
    ) {
        if (!matches) {
            return afterEvent;
        }
        return switch (phase) {
            case PRESS -> Math.max(afterEvent, beforeEvent + 1);
            case REPEAT -> beforeEvent;
            case RELEASE -> afterEvent;
        };
    }

    static Phase normalizeMouseHotkeyPhase(
            Phase phase,
            boolean matches,
            boolean repeatedPress
    ) {
        if (matches && phase == Phase.PRESS && repeatedPress) {
            return Phase.REPEAT;
        }
        return phase;
    }

    enum Phase {
        PRESS,
        REPEAT,
        RELEASE
    }

    enum Decision {
        PASS,
        BLOCK,
        PASS_AND_RESTORE
    }
}
