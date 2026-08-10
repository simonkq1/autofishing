package xyz.whatsyouss.frostyautofish;

final class AutoFishStateSelfTest {
    private AutoFishStateSelfTest() {
    }

    static void run() {
        normalFishingCycleIsAllowed();
        combatBranchesAndCleanupAreAllowed();
        invalidJumpsAreRejected();
        reelDelayBoundsAreStable();
        timeoutAndTriggerBoundariesAreStable();
        consecutiveDryCastPolicyIsStable();
        abilityInitialDelayIsStable();
        biteSignalsRequireArmingAndAClearState();
        reelCaptureUsesHookAndPlayerOrigins();
    }

    private static void normalFishingCycleIsAllowed() {
        allowed(AutoFishState.DISABLED, AutoFishState.READY_TO_CAST);
        allowed(AutoFishState.READY_TO_CAST, AutoFishState.CAST_PENDING);
        allowed(AutoFishState.CAST_PENDING, AutoFishState.WAITING_FOR_BITE);
        allowed(AutoFishState.WAITING_FOR_BITE, AutoFishState.REEL_DELAY);
        allowed(AutoFishState.WAITING_FOR_BITE, AutoFishState.REELING_HOOKED_ENTITY);
        allowed(AutoFishState.REELING_HOOKED_ENTITY, AutoFishState.READY_TO_CAST);
        allowed(AutoFishState.REEL_DELAY, AutoFishState.READY_TO_CAST);
        allowed(AutoFishState.WAITING_FOR_BITE, AutoFishState.READY_TO_CAST);
    }

    private static void combatBranchesAndCleanupAreAllowed() {
        allowed(AutoFishState.REEL_DELAY, AutoFishState.COLLECTING);
        allowed(AutoFishState.COLLECTING, AutoFishState.ABILITY);
        allowed(AutoFishState.COLLECTING, AutoFishState.CHASING);
        allowed(AutoFishState.ABILITY, AutoFishState.RESTORING_ROTATION);
        allowed(AutoFishState.CHASING, AutoFishState.RETURNING);
        allowed(AutoFishState.RETURNING, AutoFishState.RESTORING_ROTATION);
        allowed(AutoFishState.RESTORING_ROTATION, AutoFishState.READY_TO_CAST);
        for (AutoFishState state : AutoFishState.values()) {
            allowed(state, AutoFishState.DISABLED);
        }
    }

    private static void invalidJumpsAreRejected() {
        rejected(AutoFishState.DISABLED, AutoFishState.CHASING);
        rejected(AutoFishState.READY_TO_CAST, AutoFishState.ABILITY);
        rejected(AutoFishState.WAITING_FOR_BITE, AutoFishState.CHASING);
        rejected(AutoFishState.RETURNING, AutoFishState.CAST_PENDING);
    }

    private static void reelDelayBoundsAreStable() {
        check(FishingCyclePolicy.reelDelayMillis(0) == 10, "minimum reel delay");
        check(FishingCyclePolicy.reelDelayMillis(290) == 300, "maximum reel delay");
        for (int roll = 0; roll <= 290; roll++) {
            int delay = FishingCyclePolicy.reelDelayMillis(roll);
            check(delay >= 10 && delay <= 300, "reel delay range");
        }
        throwsIllegalArgument(() -> FishingCyclePolicy.reelDelayMillis(-1));
        throwsIllegalArgument(() -> FishingCyclePolicy.reelDelayMillis(291));
    }

    private static void timeoutAndTriggerBoundariesAreStable() {
        check(!FishingCyclePolicy.hasTimedOut(599, 0, 30), "timeout before boundary");
        check(FishingCyclePolicy.hasTimedOut(600, 0, 30), "timeout at boundary");
        check(!FishingCyclePolicy.shouldStartCombat(2, 3), "trigger below threshold");
        check(FishingCyclePolicy.shouldStartCombat(3, 3), "trigger at threshold");
        check(FishingCyclePolicy.shouldStartCombat(4, 3), "trigger above threshold");
    }

    private static void consecutiveDryCastPolicyIsStable() {
        long timeoutTicks = FishingCyclePolicy.DEFAULT_DRY_CAST_TIMEOUT_SECONDS * 20L;
        check(!FishingCyclePolicy.hasTimedOut(timeoutTicks - 1, 0,
                FishingCyclePolicy.DEFAULT_DRY_CAST_TIMEOUT_SECONDS), "dry cast before 15 seconds");
        check(FishingCyclePolicy.hasTimedOut(timeoutTicks, 0,
                FishingCyclePolicy.DEFAULT_DRY_CAST_TIMEOUT_SECONDS), "dry cast at 15 seconds");
        check(!FishingCyclePolicy.shouldStopAfterDryCast(1), "first dry cast recasts");
        check(FishingCyclePolicy.shouldStopAfterDryCast(2), "second dry cast stops");
        throwsIllegalArgument(() -> FishingCyclePolicy.shouldStopAfterDryCast(0));
    }

    private static void abilityInitialDelayIsStable() {
        long start = 5_000_000_000L;
        check(AbilityUsePolicy.jitteredDelayMillis(150, -100) == 135,
                "ability minimum jitter is -10 percent");
        check(AbilityUsePolicy.jitteredDelayMillis(150, 0) == 150,
                "ability zero jitter keeps configured delay");
        check(AbilityUsePolicy.jitteredDelayMillis(150, 100) == 165,
                "ability maximum jitter is +10 percent");
        throwsIllegalArgument(() -> AbilityUsePolicy.jitteredDelayMillis(150, -101));
        throwsIllegalArgument(() -> AbilityUsePolicy.jitteredDelayMillis(150, 101));
        long deadline = AbilityUsePolicy.initialDeadline(start, 150);
        check(deadline - start == 150_000_000L, "ability delay is 150ms");
        check(!AbilityUsePolicy.isInitialDelayElapsed(deadline - 1, deadline),
                "ability waits before deadline");
        check(AbilityUsePolicy.isInitialDelayElapsed(deadline, deadline),
                "ability fires at deadline");
    }

    private static void biteSignalsRequireArmingAndAClearState() {
        BiteSignalGate gate = new BiteSignalGate();
        gate.reset(100, true, true);
        check(!gate.acceptsHypixel(110, true), "initial Hypixel marker is ignored");
        check(!gate.acceptsVanilla(110, true), "initial vanilla biting is ignored");
        check(!gate.acceptsHypixel(111, false), "Hypixel clear state only arms");
        check(!gate.acceptsVanilla(111, false), "vanilla clear state only arms");
        check(gate.acceptsHypixel(112, true), "new Hypixel marker is accepted");
        check(gate.acceptsVanilla(112, true), "new vanilla biting is accepted");

        gate.reset(200, false, false);
        check(!gate.acceptsHypixel(209, true), "Hypixel marker before delay is ignored");
        check(!gate.acceptsVanilla(209, true), "vanilla biting before delay is ignored");
        check(gate.acceptsHypixel(210, true), "Hypixel marker at delay is accepted");
        check(gate.acceptsVanilla(210, true), "vanilla biting at delay is accepted");
    }

    private static void reelCaptureUsesHookAndPlayerOrigins() {
        check(ReelCapturePolicy.isInsideCaptureArea(2.0, 30.0, 30.0),
                "entity near own hook is captured");
        check(ReelCapturePolicy.isInsideCaptureArea(30.0, 2.0, 30.0),
                "entity near reel player origin is captured");
        check(ReelCapturePolicy.isInsideCaptureArea(30.0, 30.0, 2.0),
                "entity pulled near current player is captured");
        check(!ReelCapturePolicy.isInsideCaptureArea(11.0, 9.0, 9.0),
                "unrelated distant entity is rejected");
    }

    private static void allowed(AutoFishState from, AutoFishState to) {
        check(from.canTransitionTo(to), "expected transition " + from + " -> " + to);
    }

    private static void rejected(AutoFishState from, AutoFishState to) {
        check(!from.canTransitionTo(to), "unexpected transition " + from + " -> " + to);
    }

    private static void throwsIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Failed check: " + name);
        }
    }
}
