package xyz.whatsyouss.frostyautofish;

import java.util.List;

final class HighValueTargetPolicySelfTest {
    private HighValueTargetPolicySelfTest() {
    }

    static void run() {
        matchingUsesConfiguredRules();
        trackingRejectsOwnAndOrdinaryTargets();
        trackingRejectsOwnCaptureAreaAndDistantTargets();
        captureAreaOnlyAppliesDuringCurrentCollectionWindow();
        autoAttackRequiresSafeStateAndAttackBudget();
        trackingContextResetsAtWorldAndPlayerBoundaries();
        displayRequiresValidWorldAndNoGui();
        scanPlanSeparatesMasterAndCaptureBehavior();
    }

    private static void matchingUsesConfiguredRules() {
        check(HighValueTargetPolicy.matchesAny(
                List.of("Shadow Assassin"),
                List.of("Steve", "[Lv500] SHADOW ASSASSIN 12M/12M")
        ), "high value policy matches formatted observed name");
        check(!HighValueTargetPolicy.matchesAny(
                List.of("Shadow Assassin"),
                List.of("Water Hydra", "Lava Blaze")
        ), "high value policy rejects unrelated names");
    }

    private static void trackingRejectsOwnAndOrdinaryTargets() {
        check(HighValueTargetPolicy.shouldTrack(
                true, false, true, false, false, false, 12.0, 32.0
        ), "matched external living player model tracks");
        check(!HighValueTargetPolicy.shouldTrack(
                true, true, true, false, false, false, 12.0, 32.0
        ), "self player is rejected");
        check(!HighValueTargetPolicy.shouldTrack(
                true, false, true, true, false, false, 12.0, 32.0
        ), "ordinary Auto Kill target is rejected");
        check(!HighValueTargetPolicy.shouldTrack(
                true, false, true, false, true, false, 12.0, 32.0
        ), "approved own reel player target is rejected");
    }

    private static void trackingRejectsOwnCaptureAreaAndDistantTargets() {
        check(!HighValueTargetPolicy.shouldTrack(
                true, false, true, false, false, true, 12.0, 32.0
        ), "own capture area target is rejected");
        check(!HighValueTargetPolicy.shouldTrack(
                true, false, true, false, false, false, 40.0, 32.0
        ), "distant target is rejected");
        check(!HighValueTargetPolicy.shouldTrack(
                false, false, true, false, false, false, 12.0, 32.0
        ), "unmatched target is rejected");
    }

    private static void captureAreaOnlyAppliesDuringCurrentCollectionWindow() {
        check(HighValueTargetPolicy.shouldApplyOwnCaptureArea(true, 100L, 120L),
                "own capture area applies during collection window");
        check(HighValueTargetPolicy.shouldApplyOwnCaptureArea(true, 120L, 120L),
                "own capture area applies through collection window boundary");
        check(!HighValueTargetPolicy.shouldApplyOwnCaptureArea(false, 100L, 120L),
                "stale capture area is ignored outside collecting state");
        check(!HighValueTargetPolicy.shouldApplyOwnCaptureArea(true, 121L, 120L),
                "stale capture area is ignored after collection window");
        check(HighValueTargetPolicy.shouldTrack(
                true, false, true, false, false,
                HighValueTargetPolicy.shouldApplyOwnCaptureArea(false, 100L, 120L),
                12.0, 32.0
        ), "external target tracks when stale capture area is ignored");
        check(HighValueTargetPolicy.shouldExcludeOwnCapturedPlayerTarget(true, false),
                "current own capture area excludes target");
        check(HighValueTargetPolicy.shouldExcludeOwnCapturedPlayerTarget(false, true),
                "known own captured player target remains excluded");
        check(!HighValueTargetPolicy.shouldTrack(
                true, false, true, false, false,
                HighValueTargetPolicy.shouldExcludeOwnCapturedPlayerTarget(false, true),
                12.0, 32.0
        ), "known own captured player target does not become external high value later");
        check(!HighValueTargetPolicy.shouldExcludeOwnCapturedPlayerTarget(false, false),
                "external target is not excluded without current or known own capture");
    }

    private static void autoAttackRequiresSafeStateAndAttackBudget() {
        check(HighValueTargetPolicy.canAutoAttack(
                true, true, true, true, false, false, true, true, 0, 1
        ), "auto attack is allowed in safe ready state");
        check(!HighValueTargetPolicy.canAutoAttack(
                false, true, true, true, false, false, true, true, 0, 1
        ), "disabled master blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, false, true, true, false, false, true, true, 0, 1
        ), "disabled macro blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, true, true, false, false, false, true, true, 0, 1
        ), "missing game mode blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, true, true, true, true, false, true, true, 0, 1
        ), "combat state blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, true, true, true, false, true, true, true, 0, 1
        ), "screen or overlay blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, true, true, true, false, false, true, true, 1, 1
        ), "attack count limit blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, true, true, true, false, false, true, false, 0, 1
        ), "attack cooldown blocks high value auto attack");
        check(HighValueTargetPolicy.clampAttackCount(0) == 1, "attack count clamps low");
        check(HighValueTargetPolicy.clampAttackCount(99) == 10, "attack count clamps high");
    }

    private static void trackingContextResetsAtWorldAndPlayerBoundaries() {
        check(!HighValueTargetPolicy.shouldResetTrackingContext(
                true, true, true, true, true
        ), "same living player and level preserve tracked targets");
        check(HighValueTargetPolicy.shouldResetTrackingContext(
                true, true, true, false, true
        ), "level identity change resets tracked targets");
        check(HighValueTargetPolicy.shouldResetTrackingContext(
                true, true, true, true, false
        ), "player identity change resets tracked targets");
        check(HighValueTargetPolicy.shouldResetTrackingContext(
                false, false, false, false, false
        ), "missing world and player reset tracked targets");
        check(HighValueTargetPolicy.shouldResetTrackingContext(
                true, true, false, true, true
        ), "dead player resets tracked targets");
    }

    private static void displayRequiresValidWorldAndNoGui() {
        check(HighValueTargetPolicy.shouldShow(true, true, true, false),
                "configured display shows with valid world and no GUI");
        check(!HighValueTargetPolicy.shouldShow(false, true, true, false),
                "disabled master hides high value display");
        check(!HighValueTargetPolicy.shouldShow(true, false, true, false),
                "disabled display setting remains hidden");
        check(!HighValueTargetPolicy.shouldShow(true, true, false, false),
                "invalid tracking context hides stale display");
        check(!HighValueTargetPolicy.shouldShow(true, true, true, true),
                "screen or overlay hides high value display");
    }

    private static void scanPlanSeparatesMasterAndCaptureBehavior() {
        checkPlan(false, false, false, false, false, false,
                "master off preserves tracked targets without scanning");
        checkPlan(false, false, true, false, true, false,
                "master off keeps capture bookkeeping without clearing");
        checkPlan(false, true, false, false, false, false,
                "master off ignores configured target matching");
        checkPlan(false, true, true, false, true, false,
                "master off only records capture IDs during capture window");
        checkPlan(true, false, false, false, false, true,
                "master on clears tracked targets for empty target list");
        checkPlan(true, false, true, false, true, true,
                "master on empty list clears and records capture IDs");
        checkPlan(true, true, false, true, false, false,
                "master on configured list performs matching");
        checkPlan(true, true, true, true, false, false,
                "full matching includes capture bookkeeping itself");
    }

    private static void checkPlan(
            boolean masterEnabled,
            boolean hasTargets,
            boolean captureActive,
            boolean matchTargets,
            boolean recordCaptureOnly,
            boolean clearTrackedTargets,
            String name
    ) {
        HighValueTargetPolicy.ScanPlan plan = HighValueTargetPolicy.scanPlan(
                masterEnabled,
                hasTargets,
                captureActive
        );
        check(plan.matchTargets() == matchTargets
                        && plan.recordCaptureOnly() == recordCaptureOnly
                        && plan.clearTrackedTargets() == clearTrackedTargets,
                name);
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Failed check: " + name);
        }
    }
}
