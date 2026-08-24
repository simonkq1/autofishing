package xyz.whatsyouss.frostyautofish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;
import xyz.whatsyouss.frostyautofish.config.TargetNameMatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class HighValueTargetTracker {
    private static final int ATTACK_COOLDOWN_TICKS = 5;
    private static final double ATTACK_RANGE = 3.5;
    private static final float ROTATION_SMOOTHING = 4.0F;

    private final Minecraft minecraft;
    private final AutoFishConfig config;
    private final RotationHelper rotation;
    private final Map<Integer, TrackedTarget> trackedTargets = new LinkedHashMap<>();
    private final Set<Integer> ownCapturedPlayerTargetIds = new HashSet<>();

    private ClientLevel activeLevel;
    private Player activePlayer;
    private boolean displayReady;

    HighValueTargetTracker(Minecraft minecraft, AutoFishConfig config, RotationHelper rotation) {
        this.minecraft = minecraft;
        this.config = config;
        this.rotation = rotation;
    }

    boolean tick(
            boolean autoFishEnabled,
            boolean combatState,
            boolean allowAutoAttack,
            Set<Integer> ordinaryAutoKillTargetIds,
            Set<Integer> approvedPlayerTargetIds,
            Vec3 reelHookAnchor,
            Vec3 reelPlayerAnchor,
            double keepRange
    ) {
        boolean levelAvailable = minecraft.level != null;
        boolean playerAvailable = minecraft.player != null;
        boolean playerAlive = playerAvailable && minecraft.player.isAlive();
        boolean sameLevel = levelAvailable && activeLevel == minecraft.level;
        boolean samePlayer = playerAvailable && activePlayer == minecraft.player;
        if (HighValueTargetPolicy.shouldResetTrackingContext(
                levelAvailable,
                playerAvailable,
                playerAlive,
                sameLevel,
                samePlayer
        )) {
            clear();
            if (!levelAvailable || !playerAvailable || !playerAlive) {
                return false;
            }
            activeLevel = minecraft.level;
            activePlayer = minecraft.player;
        }
        if (!hasValidTrackingContext()) {
            return false;
        }

        HighValueTargetPolicy.ScanPlan scanPlan = HighValueTargetPolicy.scanPlan(
                config.highValueEnabled,
                !config.highValueTargets.isEmpty(),
                reelHookAnchor != null && reelPlayerAnchor != null
        );
        if (scanPlan.clearTrackedTargets()) {
            trackedTargets.clear();
        }
        if (scanPlan.recordCaptureOnly()) {
            recordOwnCapturedPlayerTargets(reelHookAnchor, reelPlayerAnchor);
        }
        if (!scanPlan.matchTargets()) {
            displayReady = config.highValueEnabled;
            return false;
        }

        displayReady = false;
        long tick = minecraft.level.getGameTime();
        scan(tick, ordinaryAutoKillTargetIds, approvedPlayerTargetIds, reelHookAnchor, reelPlayerAnchor, keepRange);
        prune(tick, ordinaryAutoKillTargetIds, approvedPlayerTargetIds, reelHookAnchor, reelPlayerAnchor, keepRange);
        displayReady = true;
        return tryAutoAttack(autoFishEnabled, combatState, allowAutoAttack);
    }

    HighValueTargetSnapshot bestSnapshot() {
        if (!HighValueTargetPolicy.shouldShow(
                config.highValueEnabled,
                config.showHighValueHud,
                hasValidTrackingContext(),
                isScreenOrOverlayOpen()
        ) || !displayReady) {
            return null;
        }
        TrackedTarget best = bestTarget();
        if (best == null) {
            return null;
        }
        return new HighValueTargetSnapshot(
                best.name,
                best.distance,
                best.attacksDone,
                HighValueTargetPolicy.clampAttackCount(config.highValueAttackCount),
                config.autoAttackHighValue
        );
    }

    boolean hasCollisionTargets() {
        return HighValueTargetPolicy.shouldShow(
                config.highValueEnabled,
                config.showHighValueCollision,
                hasValidTrackingContext(),
                isScreenOrOverlayOpen()
        ) && displayReady && !trackedTargets.isEmpty();
    }

    void renderGizmos() {
        if (!HighValueTargetPolicy.shouldShow(
                config.highValueEnabled,
                config.showHighValueCollision,
                hasValidTrackingContext(),
                isScreenOrOverlayOpen()
        ) || !displayReady) {
            return;
        }
        for (TrackedTarget tracked : trackedTargets.values()) {
            if (isAliveTrackedEntity(tracked.entity)) {
                Gizmos.cuboid(
                        tracked.entity.getBoundingBox(),
                        GizmoStyle.strokeAndFill(0xFFFFAA00, 2.0F, 0x30FFAA00)
                ).persistForMillis(80);
            }
        }
    }

    void clear() {
        trackedTargets.clear();
        ownCapturedPlayerTargetIds.clear();
        activeLevel = null;
        activePlayer = null;
        displayReady = false;
    }

    private void scan(
            long tick,
            Set<Integer> ordinaryAutoKillTargetIds,
            Set<Integer> approvedPlayerTargetIds,
            Vec3 reelHookAnchor,
            Vec3 reelPlayerAnchor,
            double keepRange
    ) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof Player playerModel)) {
                continue;
            }
            List<String> observedNames = observedNames(playerModel);
            String matchedName = matchingName(playerModel, observedNames);
            boolean insideOwnCaptureArea = isInsideOwnCaptureArea(entity, reelHookAnchor, reelPlayerAnchor);
            if (insideOwnCaptureArea) {
                ownCapturedPlayerTargetIds.add(entity.getId());
            }
            double distance = minecraft.player.distanceTo(entity);
            boolean shouldTrack = HighValueTargetPolicy.shouldTrack(
                    matchedName != null,
                    entity == minecraft.player,
                    isAliveTrackedEntity(entity),
                    ordinaryAutoKillTargetIds.contains(entity.getId()),
                    approvedPlayerTargetIds.contains(entity.getId()),
                    HighValueTargetPolicy.shouldExcludeOwnCapturedPlayerTarget(
                            insideOwnCaptureArea,
                            ownCapturedPlayerTargetIds.contains(entity.getId())
                    ),
                    distance,
                    keepRange
            );
            if (!shouldTrack) {
                trackedTargets.remove(entity.getId());
                continue;
            }
            trackedTargets.compute(entity.getId(), (id, existing) -> {
                TrackedTarget tracked = existing == null
                        ? new TrackedTarget(playerModel, matchedName)
                        : existing;
                tracked.entity = playerModel;
                tracked.name = matchedName;
                tracked.distance = distance;
                tracked.lastSeenTick = tick;
                return tracked;
            });
        }
    }

    private void recordOwnCapturedPlayerTargets(Vec3 reelHookAnchor, Vec3 reelPlayerAnchor) {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof Player
                    && entity != minecraft.player
                    && isInsideOwnCaptureArea(entity, reelHookAnchor, reelPlayerAnchor)) {
                ownCapturedPlayerTargetIds.add(entity.getId());
            }
        }
    }

    private void prune(
            long tick,
            Set<Integer> ordinaryAutoKillTargetIds,
            Set<Integer> approvedPlayerTargetIds,
            Vec3 reelHookAnchor,
            Vec3 reelPlayerAnchor,
            double keepRange
    ) {
        Iterator<TrackedTarget> iterator = trackedTargets.values().iterator();
        while (iterator.hasNext()) {
            TrackedTarget tracked = iterator.next();
            Entity entity = tracked.entity;
            if (tracked.lastSeenTick != tick
                    || !isAliveTrackedEntity(entity)
                    || entity == minecraft.player
                    || ordinaryAutoKillTargetIds.contains(entity.getId())
                    || approvedPlayerTargetIds.contains(entity.getId())
                    || ownCapturedPlayerTargetIds.contains(entity.getId())
                    || isInsideOwnCaptureArea(entity, reelHookAnchor, reelPlayerAnchor)
                    || minecraft.player.distanceTo(entity) > keepRange) {
                iterator.remove();
            }
        }
    }

    private boolean tryAutoAttack(boolean autoFishEnabled, boolean combatState, boolean allowAutoAttack) {
        TrackedTarget best = bestTarget();
        if (best == null || !allowAutoAttack) {
            return false;
        }
        Entity target = best.entity;
        Vec3 aim = target.position().add(0.0, target.getBbHeight() / 2.0, 0.0);
        boolean canTry = HighValueTargetPolicy.canAutoAttack(
                config.highValueEnabled,
                autoFishEnabled,
                config.autoAttackHighValue,
                minecraft.gameMode != null,
                combatState,
                isScreenOrOverlayOpen(),
                minecraft.player.distanceTo(target) <= ATTACK_RANGE,
                true,
                best.attacksDone,
                config.highValueAttackCount
        );
        if (!canTry) {
            return false;
        }

        rotation.aimAt(aim, ROTATION_SMOOTHING);
        if (!rotation.canHit(target, ATTACK_RANGE)) {
            return false;
        }

        minecraft.gameMode.attack(minecraft.player, target);
        minecraft.player.swing(InteractionHand.MAIN_HAND);
        best.attacksDone++;
        return true;
    }

    private boolean hasValidTrackingContext() {
        return activeLevel != null
                && activePlayer != null
                && minecraft.level == activeLevel
                && minecraft.player == activePlayer
                && activePlayer.isAlive();
    }

    private boolean isScreenOrOverlayOpen() {
        return minecraft.screen != null || minecraft.getOverlay() != null;
    }

    private TrackedTarget bestTarget() {
        TrackedTarget best = null;
        for (TrackedTarget tracked : trackedTargets.values()) {
            if (!isAliveTrackedEntity(tracked.entity)) {
                continue;
            }
            if (best == null || tracked.distance < best.distance) {
                best = tracked;
            }
        }
        return best;
    }

    private String matchingName(Player playerModel, List<String> observedNames) {
        if (!HighValueTargetPolicy.matchesAny(config.highValueTargets, observedNames)) {
            return null;
        }
        for (String observedName : observedNames) {
            for (String rule : config.highValueTargets) {
                if (TargetNameMatcher.matches(rule, observedName)) {
                    String normalized = TargetNameMatcher.normalize(observedName);
                    return normalized.isEmpty() ? TargetNameMatcher.normalize(rule) : normalized;
                }
            }
        }
        return playerModel.getName().getString();
    }

    private List<String> observedNames(Player playerModel) {
        List<String> names = new ArrayList<>();
        names.add(playerModel.getName().getString());
        names.add(playerModel.getDisplayName().getString());
        if (playerModel.getCustomName() != null) {
            names.add(playerModel.getCustomName().getString());
        }

        AABB labelArea = new AABB(
                playerModel.getX() - 1.25,
                playerModel.getY(),
                playerModel.getZ() - 1.25,
                playerModel.getX() + 1.25,
                playerModel.getY() + playerModel.getBbHeight() + 3.0,
                playerModel.getZ() + 1.25
        );
        for (ArmorStand label : minecraft.level.getEntitiesOfClass(ArmorStand.class, labelArea)) {
            if (label.getCustomName() != null) {
                names.add(label.getCustomName().getString());
            }
        }
        return names;
    }

    private boolean isInsideOwnCaptureArea(Entity entity, Vec3 reelHookAnchor, Vec3 reelPlayerAnchor) {
        if (reelHookAnchor == null || reelPlayerAnchor == null) {
            return false;
        }
        return ReelCapturePolicy.isInsideCaptureArea(
                entity.position().distanceTo(reelHookAnchor),
                entity.position().distanceTo(reelPlayerAnchor),
                minecraft.player.distanceTo(entity)
        );
    }

    private boolean isAliveTrackedEntity(Entity entity) {
        return entity != null && entity.isAlive() && !entity.isRemoved();
    }

    private static final class TrackedTarget {
        private Entity entity;
        private String name;
        private double distance;
        private int attacksDone;
        private long lastSeenTick;

        private TrackedTarget(Entity entity, String name) {
            this.entity = entity;
            this.name = name;
        }
    }
}
