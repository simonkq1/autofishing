package xyz.whatsyouss.frostyautofish;

import xyz.whatsyouss.frostyautofish.config.TargetNameMatcher;

import java.util.List;
import java.util.function.Predicate;

final class HighValueTargetPolicy {
    static final int MIN_ATTACK_COUNT = 1;
    static final int MAX_ATTACK_COUNT = 10;
    static final double ABILITY_RANGE = 32.0;

    enum AttackMode {
        NONE, MELEE, ABILITY
    }

    static AttackMode attackMode(boolean ranged, boolean useAbility) {
        return !ranged ? AttackMode.MELEE : useAbility ? AttackMode.ABILITY : AttackMode.NONE;
    }

    static boolean canUseAbilityWeapon(boolean spectator, boolean usingItem, boolean empty,
                                       boolean fishingRod, boolean bow, boolean crossbow, boolean onCooldown) {
        return !spectator && !usingItem && !empty && !fishingRod && !bow && !crossbow && !onCooldown;
    }

    enum RaycastPath {
        ORDINARY_AUTO_KILL,
        HIGH_VALUE
    }

    interface AttackCandidateView {
        boolean isAlive();

        boolean isRemoved();

        double distance();

        int attacksDone();
    }

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

    static double raycastMaxDistanceArgument(double reach, RaycastPath path) {
        return path == RaycastPath.HIGH_VALUE ? reach * reach : reach;
    }

    static <T extends AttackCandidateView> T selectNearestAttackCandidate(
            Iterable<T> candidates,
            double attackRange,
            int attackLimit
    ) {
        return selectNearestAttackCandidate(candidates, attackRange, attackLimit, candidate -> true);
    }

    static <T extends AttackCandidateView> T selectNearestAttackCandidate(
            Iterable<T> candidates,
            double attackRange,
            int attackLimit,
            Predicate<T> visible
    ) {
        T best = null;
        for (T candidate : candidates) {
            if (!isAttackCandidate(candidate, attackRange, attackLimit) || !visible.test(candidate)) {
                continue;
            }
            if (best == null || candidate.distance() < best.distance()) {
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isAttackCandidate(
            AttackCandidateView candidate,
            double attackRange,
            int attackLimit
    ) {
        return candidate.isAlive()
                && !candidate.isRemoved()
                && candidate.distance() <= attackRange
                && candidate.attacksDone() < clampAttackCount(attackLimit);
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
