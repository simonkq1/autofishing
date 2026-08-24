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
        highValueHotkeyProducesOneClickAcrossGuiPaths();
        repeatedHighValueMousePressDoesNotRetoggle();
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

    private static void highValueHotkeyProducesOneClickAcrossGuiPaths() {
        check(InputLockPolicy.reconcileHotkeyClicks(
                InputLockPolicy.Phase.PRESS, true, 0, 0
        ) == 1, "consumed GUI press synthesizes the missing high value click");
        check(InputLockPolicy.reconcileHotkeyClicks(
                InputLockPolicy.Phase.PRESS, true, 2, 3
        ) == 3, "vanilla world press is not duplicated");
        check(InputLockPolicy.reconcileHotkeyClicks(
                InputLockPolicy.Phase.REPEAT, true, 2, 3
        ) == 2, "held high value key does not repeatedly toggle");
        check(InputLockPolicy.reconcileHotkeyClicks(
                InputLockPolicy.Phase.RELEASE, true, 2, 2
        ) == 2, "release does not create a high value click");
        check(InputLockPolicy.reconcileHotkeyClicks(
                InputLockPolicy.Phase.PRESS, false, 2, 5
        ) == 5, "unrelated key click state is untouched");
    }

    private static void repeatedHighValueMousePressDoesNotRetoggle() {
        InputLockPolicy.Phase firstPress = InputLockPolicy.normalizeMouseHotkeyPhase(
                InputLockPolicy.Phase.PRESS, true, false
        );
        check(firstPress == InputLockPolicy.Phase.PRESS,
                "first matching mouse press remains a press");
        check(InputLockPolicy.reconcileHotkeyClicks(firstPress, true, 0, 0) == 1,
                "first GUI mouse press synthesizes one high value click");

        InputLockPolicy.Phase repeatedPress = InputLockPolicy.normalizeMouseHotkeyPhase(
                InputLockPolicy.Phase.PRESS, true, true
        );
        check(repeatedPress == InputLockPolicy.Phase.REPEAT,
                "repeated matching mouse press is normalized to repeat");
        check(InputLockPolicy.reconcileHotkeyClicks(repeatedPress, true, 1, 2) == 1,
                "repeated mouse press removes a duplicate vanilla click");
        check(InputLockPolicy.normalizeMouseHotkeyPhase(
                InputLockPolicy.Phase.RELEASE, true, true
        ) == InputLockPolicy.Phase.RELEASE, "mouse release is never synthesized");
        check(InputLockPolicy.normalizeMouseHotkeyPhase(
                InputLockPolicy.Phase.PRESS, false, true
        ) == InputLockPolicy.Phase.PRESS, "unrelated mouse press is unchanged");
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
