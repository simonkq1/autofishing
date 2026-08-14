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
