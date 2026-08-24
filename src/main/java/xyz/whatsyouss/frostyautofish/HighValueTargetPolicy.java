package xyz.whatsyouss.frostyautofish;

import xyz.whatsyouss.frostyautofish.config.TargetNameMatcher;

import java.util.List;

final class HighValueTargetPolicy {
    static final int MIN_ATTACK_COUNT = 1;
    static final int MAX_ATTACK_COUNT = 10;

    record ScanPlan(
            boolean matchTargets,
            boolean recordCaptureOnly,
            boolean clearTrackedTargets
    ) {
    }

    private HighValueTargetPolicy() {
    }

    static boolean matchesAny(List<String> rules, List<String> observedNames) {
        if (rules == null || observedNames == null) {
            return false;
        }
        for (String observedName : observedNames) {
            for (String rule : rules) {
                if (TargetNameMatcher.matches(rule, observedName)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldTrack(
            boolean matched,
            boolean self,
            boolean alive,
            boolean ordinaryAutoKillTarget,
            boolean approvedPlayerTarget,
            boolean insideOwnCaptureArea,
            double distance,
            double keepRange
    ) {
        return matched
                && !self
                && alive
                && !ordinaryAutoKillTarget
                && !approvedPlayerTarget
                && !insideOwnCaptureArea
                && distance <= keepRange;
    }

    static boolean canAutoAttack(
            boolean masterEnabled,
            boolean macroEnabled,
            boolean enabled,
            boolean gameModeAvailable,
            boolean combatState,
            boolean blockedByScreenOrOverlay,
            boolean inAttackRange,
            boolean attackReady,
            int attacksDone,
            int attackLimit
    ) {
        return masterEnabled
                && macroEnabled
                && enabled
                && gameModeAvailable
                && !combatState
                && !blockedByScreenOrOverlay
                && inAttackRange
                && attackReady
                && attacksDone < clampAttackCount(attackLimit);
    }

    static boolean shouldResetTrackingContext(
            boolean levelAvailable,
            boolean playerAvailable,
            boolean playerAlive,
            boolean sameLevel,
            boolean samePlayer
    ) {
        return !levelAvailable
                || !playerAvailable
                || !playerAlive
                || !sameLevel
                || !samePlayer;
    }

    static boolean shouldShow(
            boolean masterEnabled,
            boolean configured,
            boolean validTrackingContext,
            boolean blockedByScreenOrOverlay
    ) {
        return masterEnabled
                && configured
                && validTrackingContext
                && !blockedByScreenOrOverlay;
    }

    static ScanPlan scanPlan(
            boolean masterEnabled,
            boolean hasConfiguredTargets,
            boolean ownCaptureAreaActive
    ) {
        if (!masterEnabled) {
            return new ScanPlan(false, ownCaptureAreaActive, false);
        }
        if (hasConfiguredTargets) {
            return new ScanPlan(true, false, false);
        }
        return new ScanPlan(false, ownCaptureAreaActive, true);
    }

    static boolean shouldApplyOwnCaptureArea(boolean collectingState, long currentTick, long reelScanUntil) {
        return collectingState && currentTick <= reelScanUntil;
    }

    static boolean shouldExcludeOwnCapturedPlayerTarget(
            boolean insideCurrentOwnCaptureArea,
            boolean knownOwnCapturedPlayerTarget
    ) {
        return insideCurrentOwnCaptureArea || knownOwnCapturedPlayerTarget;
    }

    static int clampAttackCount(int value) {
        return Math.max(MIN_ATTACK_COUNT, Math.min(MAX_ATTACK_COUNT, value));
    }
}
