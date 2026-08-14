package xyz.whatsyouss.frostyautofish;

final class InputLockSelfTest {
    private InputLockSelfTest() {
    }

    static void run() {
        stateActivatesOnlyOnTheRisingEdge();
        blockingRequiresAnUnobstructedWorld();
        suspendedLockSanitizesGuiAndOverlayInput();
        policyBlocksOnlyGameplayPressesAndRepeats();
        releasesAndAllowedCollisionsPassForCleanup();
        toggleRestorePolicyOnlySuppressesActiveBlockedMappings();
    }

    private static void stateActivatesOnlyOnTheRisingEdge() {
        InputLockState state = new InputLockState();
        check(!state.isActive(), "lock starts inactive");
        check(state.setActive(true), "first activation reports rising edge");
        check(!state.setActive(true), "repeated activation has no edge");
        check(!state.setActive(false), "deactivation is not an activation edge");
        check(!state.isActive(), "lock deactivates");
    }

    private static void blockingRequiresAnUnobstructedWorld() {
        InputLockState state = new InputLockState();
        state.setActive(true);
        check(state.isBlocking(false, false), "active lock blocks in world");
        check(!state.isBlocking(true, false), "screen suspends blocking");
        check(!state.isBlocking(false, true), "overlay suspends blocking");
    }

    private static void policyBlocksOnlyGameplayPressesAndRepeats() {
        check(decide(true, true, false, InputLockPolicy.Phase.PRESS)
                        == InputLockPolicy.Decision.BLOCK,
                "blocked press is cancelled");
        check(decide(true, true, false, InputLockPolicy.Phase.REPEAT)
                        == InputLockPolicy.Decision.BLOCK,
                "blocked repeat is cancelled");
        check(decide(false, true, false, InputLockPolicy.Phase.PRESS)
                        == InputLockPolicy.Decision.PASS,
                "inactive lock passes input");
        check(decide(true, false, false, InputLockPolicy.Phase.PRESS)
                        == InputLockPolicy.Decision.PASS,
                "unblocked binding passes input");
    }

    private static void suspendedLockSanitizesGuiAndOverlayInput() {
        InputLockState state = new InputLockState();
        state.setActive(true);
        check(state.isSuspended(true, false), "active screen input is sanitized");
        check(state.isSuspended(false, true), "active overlay input is sanitized");
        check(!state.isSuspended(false, false), "world input is not suspended");
        state.setActive(false);
        check(!state.isSuspended(false, true), "inactive overlay input is untouched");
    }

    private static void releasesAndAllowedCollisionsPassForCleanup() {
        check(decide(true, true, false, InputLockPolicy.Phase.RELEASE)
                        == InputLockPolicy.Decision.PASS_AND_RESTORE,
                "blocked release runs original cleanup");
        check(decide(true, true, true, InputLockPolicy.Phase.PRESS)
                        == InputLockPolicy.Decision.PASS_AND_RESTORE,
                "allowed shared press runs then restores gameplay state");
        check(decide(true, true, true, InputLockPolicy.Phase.REPEAT)
                        == InputLockPolicy.Decision.PASS_AND_RESTORE,
                "allowed shared repeat runs then restores gameplay state");
    }

    private static void toggleRestorePolicyOnlySuppressesActiveBlockedMappings() {
        check(InputLockPolicy.shouldSuppressToggleRestore(true, true),
                "active blocked toggle restore is suppressed");
        check(!InputLockPolicy.shouldSuppressToggleRestore(false, true),
                "inactive blocked toggle restore passes");
        check(!InputLockPolicy.shouldSuppressToggleRestore(true, false),
                "active allowed toggle restore passes");
    }

    private static InputLockPolicy.Decision decide(
            boolean blocking,
            boolean blocked,
            boolean allowed,
            InputLockPolicy.Phase phase
    ) {
        return InputLockPolicy.decide(blocking, blocked, allowed, phase);
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Failed check: " + name);
        }
    }
}
