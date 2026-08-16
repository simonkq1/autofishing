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
                true, false, false, true, true, 0, 1
        ), "auto attack is allowed in safe ready state");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, true, false, true, true, 0, 1
        ), "combat state blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, false, true, true, true, 0, 1
        ), "screen or overlay blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, false, false, true, true, 1, 1
        ), "attack count limit blocks high value auto attack");
        check(!HighValueTargetPolicy.canAutoAttack(
                true, false, false, true, false, 0, 1
        ), "attack cooldown blocks high value auto attack");
        check(HighValueTargetPolicy.clampAttackCount(0) == 1, "attack count clamps low");
        check(HighValueTargetPolicy.clampAttackCount(99) == 10, "attack count clamps high");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Failed check: " + name);
        }
    }
}
