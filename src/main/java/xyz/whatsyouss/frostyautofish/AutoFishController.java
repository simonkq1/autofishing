package xyz.whatsyouss.frostyautofish;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;
import xyz.whatsyouss.frostyautofish.config.TargetNameMatcher;
import xyz.whatsyouss.frostyautofish.mixin.FishingHookAccessor;
import xyz.whatsyouss.frostyautofish.path.GroundPathService;
import xyz.whatsyouss.frostyautofish.path.NavMeshGenerator;
import xyz.whatsyouss.frostyautofish.path.NavMeshPath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class AutoFishController {
    private static final double SEA_CREATURE_KEEP_RANGE = 32.0;
    private static final double ATTACK_RANGE = 3.5;
    private static final double SMALL_TARGET_ATTACK_RANGE = 2.35;
    private static final double PATH_GOAL_RECOMPUTE_DISTANCE = 2.0;
    private static final double PATH_REACHED_XZ = 0.5;
    private static final double RETURN_REACHED_XZ = 0.45;
    private static final int PATH_RECOMPUTE_TICKS = 30;
    private static final int PATH_RANGE = 96;
    private static final int RESTORE_TIMEOUT_TICKS = 80;
    private static final int SLIME_SPLIT_SCAN_TICKS = 20;
    private static final int ABILITY_RETRY_TICKS = 8;
    private static final int ABILITY_MAX_ATTEMPTS = 12;
    private static final int ABILITY_TARGET_TIMEOUT_TICKS = 120;
    private static final float ROTATION_SMOOTHING = 4.0F;

    private final Minecraft minecraft;
    private final AutoFishConfig config;
    private final RotationHelper rotation;
    private final BiteSignalGate biteSignalGate = new BiteSignalGate();
    private final GroundPathService pathService = new GroundPathService();
    private final Random random = new Random();

    private AutoFishState state = AutoFishState.DISABLED;
    private ClientLevel activeLevel;
    private InteractionHand rodHand;
    private int rodSlot;
    private int recoveringHookId = -1;
    private long stateSinceTick;
    private long waitStartedTick;
    private int consecutiveDryCasts;
    private long reelDeadlineNanos;

    private Vec3 startPosition;
    private float startYaw;
    private float startPitch;
    private float preAbilityYaw;
    private float preAbilityPitch;
    private boolean abilityRotationSaved;
    private int abilityCooldown;
    private int abilityAttempts;
    private long abilityTargetStartedTick;
    private long abilityFirstUseDeadlineNanos;

    private Vec3 reelHookAnchor;
    private Vec3 reelPlayerAnchor;
    private long reelScanUntil;
    private final Set<Integer> reelSnapshot = new HashSet<>();
    private final Set<Integer> approvedPlayerTargetIds = new HashSet<>();
    private final List<Entity> targets = new ArrayList<>();
    private Entity currentTarget;
    private Vec3 lastKilledSlimePosition;
    private long slimeSplitScanUntil;

    private List<Vec3> currentPath = new ArrayList<>();
    private int pathIndex;
    private int pathRecomputeTick;
    private Vec3 lastPathGoal;
    private int restoreTicks;
    private int attackCooldown;
    private boolean movementOwned;

    private long nextAntiAfkTick;
    private long antiAfkRestoreTick;
    private float antiAfkOriginalYaw;
    private boolean antiAfkOffset;

    public AutoFishController(Minecraft minecraft, AutoFishConfig config) {
        this.minecraft = minecraft;
        this.config = config;
        this.rotation = new RotationHelper(minecraft);
    }

    public boolean isEnabled() {
        return state != AutoFishState.DISABLED;
    }

    public String stateName() {
        return state.label();
    }

    public void toggle() {
        if (isEnabled()) {
            disable(true, "Disabled");
        } else {
            enable();
        }
    }

    public void tick() {
        if (!isEnabled()) {
            BackgroundRunState.setActive(false);
            return;
        }
        BackgroundRunState.setActive(config.backgroundRun);
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            disable(false, null);
            return;
        }
        if (minecraft.level != activeLevel || !minecraft.player.isAlive()) {
            disable(false, "Disabled after world/player change");
            return;
        }

        pathService.tick();
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (abilityCooldown > 0) {
            abilityCooldown--;
        }

        if (minecraft.screen != null) {
            restoreAntiAfkOffset();
            releaseMovement();
            return;
        }

        minecraft.player.setSprinting(false);
        tickAntiAfk();

        if (!isRodStillAvailable()) {
            disable(false, "Fishing rod missing; disabled");
            return;
        }
        if (!isCombatState() && !isRodSelectedForFishing()) {
            disable(false, "Fishing rod deselected; disabled");
            return;
        }

        switch (state) {
            case READY_TO_CAST -> tickReadyToCast();
            case CAST_PENDING -> tickCastPending();
            case WAITING_FOR_BITE -> tickWaitingForBite();
            case REELING_HOOKED_ENTITY -> tickReelingHookedEntity();
            case REEL_DELAY -> tickReelDelay();
            case COLLECTING -> tickCollecting();
            case ABILITY -> tickAbility();
            case CHASING -> tickChasing();
            case RETURNING -> tickReturning();
            case RESTORING_ROTATION -> tickRestoringRotation();
            case DISABLED -> {
            }
        }

        renderDebugGizmos();
    }

    public void disableForConfigScreen() {
        restoreAntiAfkOffset();
        releaseMovement();
    }

    public void onEntityLoad(Entity entity, ClientLevel level) {
        if (!isEnabled() || level != activeLevel || minecraft.player == null) {
            return;
        }
        if (state == AutoFishState.COLLECTING
                && !reelSnapshot.contains(entity.getId())
                && isNewSeaCreatureCandidate(entity)) {
            addTarget(entity);
        }
        if (isCombatState() && isSlimeSplitCandidate(entity)) {
            addTarget(entity);
        }
    }

    private void enable() {
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            return;
        }

        ItemStack mainHand = minecraft.player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = minecraft.player.getItemInHand(InteractionHand.OFF_HAND);
        if (mainHand.getItem() instanceof FishingRodItem) {
            rodHand = InteractionHand.MAIN_HAND;
            rodSlot = minecraft.player.getInventory().getSelectedSlot();
        } else if (offHand.getItem() instanceof FishingRodItem) {
            rodHand = InteractionHand.OFF_HAND;
            rodSlot = -1;
        } else {
            message("Hold a fishing rod before enabling");
            return;
        }

        activeLevel = minecraft.level;
        startPosition = minecraft.player.position();
        startYaw = minecraft.player.getYRot();
        startPitch = minecraft.player.getXRot();
        clearTargets();
        currentTarget = null;
        currentPath.clear();
        reelSnapshot.clear();
        reelHookAnchor = null;
        reelPlayerAnchor = null;
        reelScanUntil = 0;
        recoveringHookId = -1;
        lastKilledSlimePosition = null;
        slimeSplitScanUntil = 0;
        abilityRotationSaved = false;
        abilityCooldown = 0;
        abilityAttempts = 0;
        consecutiveDryCasts = 0;
        antiAfkOffset = false;
        scheduleAntiAfk();
        setState(AutoFishState.READY_TO_CAST);
        BackgroundRunState.setActive(config.backgroundRun);
        message("Enabled | Auto Kill: "
                + (config.autoKill ? "ON (" + config.triggerAmount + ")" : "OFF"));
    }

    private void disable(boolean retractHook, String reason) {
        if (!isEnabled()) {
            return;
        }
        restoreAntiAfkOffset();
        releaseMovement();
        if (retractHook && minecraft.player != null && minecraft.gameMode != null
                && minecraft.player.fishing != null && rodHand != null) {
            restoreRodSlot();
            useRod();
        }
        state = AutoFishState.DISABLED;
        BackgroundRunState.setActive(false);
        clearTargets();
        currentTarget = null;
        currentPath.clear();
        reelSnapshot.clear();
        recoveringHookId = -1;
        activeLevel = null;
        if (reason != null) {
            message(reason);
        }
    }

    private void tickReadyToCast() {
        if (minecraft.player.fishing != null) {
            beginWaitingForBite();
            return;
        }
        if (!config.autoThrow) {
            return;
        }
        if (useRod()) {
            setState(AutoFishState.CAST_PENDING);
        }
    }

    private void tickCastPending() {
        if (minecraft.player.fishing != null) {
            beginWaitingForBite();
        } else if (ticksInState() >= 20) {
            setState(AutoFishState.READY_TO_CAST);
        }
    }

    private void tickWaitingForBite() {
        if (minecraft.player.fishing == null) {
            if (ticksInState() >= 20) {
                setState(AutoFishState.READY_TO_CAST);
            }
            return;
        }

        Entity hooked = minecraft.player.fishing.getHookedIn();
        if (isUnwantedHookedEntity(hooked)) {
            message("Hooked " + hooked.getName().getString() + "; recasting");
            recoveringHookId = minecraft.player.fishing.getId();
            useRod();
            setState(AutoFishState.REELING_HOOKED_ENTITY);
            return;
        }

        if (hasBite()) {
            consecutiveDryCasts = 0;
            int delayMillis = FishingCyclePolicy.reelDelayMillis(random.nextInt(291));
            reelDeadlineNanos = System.nanoTime() + delayMillis * 1_000_000L;
            setState(AutoFishState.REEL_DELAY);
            return;
        }

        if (FishingCyclePolicy.hasTimedOut(
                gameTick(),
                waitStartedTick,
                config.dryTimeoutSeconds
        )) {
            handleDryCastTimeout();
        }
    }

    private void handleDryCastTimeout() {
        consecutiveDryCasts++;
        var ownHook = minecraft.player.fishing;
        recoveringHookId = ownHook == null ? -1 : ownHook.getId();
        useRod();

        if (FishingCyclePolicy.shouldStopAfterDryCast(consecutiveDryCasts)) {
            disable(false, "No bite for " + config.dryTimeoutSeconds
                    + "s twice; disabled and sending /is");
            if (minecraft.getConnection() != null) {
                minecraft.getConnection().sendCommand("is");
            }
            return;
        }

        message("No bite for " + config.dryTimeoutSeconds + "s; recasting (1/2)");
        setState(AutoFishState.REELING_HOOKED_ENTITY);
    }

    private void tickReelingHookedEntity() {
        var ownHook = minecraft.player.fishing;
        if (ownHook == null || ownHook.isRemoved() || ownHook.getId() != recoveringHookId) {
            recoveringHookId = -1;
            setState(AutoFishState.READY_TO_CAST);
        } else if (ticksInState() >= 40) {
            disable(false, "Could not clear hooked entity; disabled");
        }
    }

    private void beginWaitingForBite() {
        waitStartedTick = gameTick();
        boolean hypixelPresent = config.biteDetection != AutoFishConfig.BiteDetection.VANILLA
                && hasHypixelBiteMarker();
        boolean vanillaBiting = config.biteDetection != AutoFishConfig.BiteDetection.HYPIXEL
                && isVanillaBiting();
        biteSignalGate.reset(gameTick(), hypixelPresent, vanillaBiting);
        setState(AutoFishState.WAITING_FOR_BITE);
    }

    private void tickReelDelay() {
        if (minecraft.player.fishing == null) {
            setState(AutoFishState.READY_TO_CAST);
            return;
        }
        if (System.nanoTime() < reelDeadlineNanos) {
            return;
        }

        snapshotBeforeReel();
        if (config.autoKill) {
            reelHookAnchor = minecraft.player.fishing.position();
            reelPlayerAnchor = minecraft.player.position();
            reelScanUntil = gameTick() + ReelCapturePolicy.COLLECTION_TICKS;
            setState(AutoFishState.COLLECTING);
            useRod();
        } else {
            useRod();
            clearTargets();
            setState(AutoFishState.READY_TO_CAST);
        }
    }

    private void tickCollecting() {
        releaseMovement();
        scanForNewTargets();
        pruneTargets();
        boolean thresholdReached = FishingCyclePolicy.shouldStartCombat(
                targets.size(), config.triggerAmount
        );
        if (gameTick() < reelScanUntil && !(config.useAbility && thresholdReached)) {
            return;
        }

        if (!thresholdReached) {
            restoreRodAndContinue();
            return;
        }

        selectNextTarget();
        if (rodHand == InteractionHand.MAIN_HAND && config.weaponSlot - 1 == rodSlot) {
            message("Weapon Slot cannot be the fishing rod slot");
            restoreRodAndContinue();
            return;
        }
        minecraft.player.getInventory().setSelectedSlot(config.weaponSlot - 1);
        abilityRotationSaved = false;
        message("Auto Kill started: " + targets.size()
                + " target(s), slot " + config.weaponSlot);
        setState(config.useAbility ? AutoFishState.ABILITY : AutoFishState.CHASING);
    }

    private void tickAbility() {
        if (!ensureTarget()) {
            return;
        }
        if (!abilityRotationSaved) {
            preAbilityYaw = minecraft.player.getYRot();
            preAbilityPitch = minecraft.player.getXRot();
            abilityRotationSaved = true;
        }

        minecraft.player.getInventory().setSelectedSlot(config.weaponSlot - 1);
        if (abilityAttempts == 0 && !AbilityUsePolicy.isInitialDelayElapsed(
                System.nanoTime(), abilityFirstUseDeadlineNanos
        )) {
            return;
        }
        if (abilityCooldown > 0) {
            return;
        }

        if (config.abilityAim == AutoFishConfig.AbilityAim.MOB) {
            Vec3 aim = currentTarget.position().add(
                    0.0,
                    Math.max(0.1, currentTarget.getBbHeight() / 3.0),
                    0.0
            );
            float[] angles = RotationHelper.rotationTo(minecraft.player.getEyePosition(), aim);
            rotation.snapTo(angles[0], angles[1]);
        } else {
            rotation.snapTo(minecraft.player.getYRot(), 90.0F);
        }

        useWeaponAbility();
        abilityAttempts++;
        abilityCooldown = ABILITY_RETRY_TICKS;

        if (abilityAttempts >= ABILITY_MAX_ATTEMPTS
                || gameTick() - abilityTargetStartedTick >= ABILITY_TARGET_TIMEOUT_TICKS) {
            message("Ability target timed out; discarded: " + currentTarget.getName().getString());
            discardCurrentTarget();
        }
    }

    private void tickChasing() {
        if (!ensureTarget()) {
            return;
        }

        double desiredRange = desiredAttackRange(currentTarget);
        if (minecraft.player.distanceTo(currentTarget) <= desiredRange - 0.2) {
            releaseMovement();
            Vec3 aim = currentTarget.position().add(0.0, currentTarget.getBbHeight() / 2.0, 0.0);
            rotation.aimAt(aim, ROTATION_SMOOTHING);
            if (attackCooldown == 0 && canAttack(currentTarget, aim)) {
                minecraft.gameMode.attack(minecraft.player, currentTarget);
                minecraft.player.swing(InteractionHand.MAIN_HAND);
                attackCooldown = 5;
            }
            return;
        }

        Vec3 goal = findStandableBeside(currentTarget);
        if (goal != null) {
            pathRecomputeTick++;
            if (currentPath.isEmpty()
                    || pathRecomputeTick >= PATH_RECOMPUTE_TICKS
                    || lastPathGoal == null
                    || lastPathGoal.distanceTo(goal) > PATH_GOAL_RECOMPUTE_DISTANCE) {
                computePath(goal);
            }
            followPath(goal);
            return;
        }

        currentPath.clear();
        walkToward(currentTarget.position());
    }

    private void tickReturning() {
        if (startPosition == null) {
            beginRotationRestore();
            return;
        }
        if (isAt(startPosition, RETURN_REACHED_XZ)) {
            releaseMovement();
            beginRotationRestore();
            return;
        }

        pathRecomputeTick++;
        if (currentPath.isEmpty()
                || pathRecomputeTick >= PATH_RECOMPUTE_TICKS
                || lastPathGoal == null
                || lastPathGoal.distanceTo(startPosition) > PATH_GOAL_RECOMPUTE_DISTANCE) {
            computePath(startPosition);
        }
        followPath(startPosition);
    }

    private void tickRestoringRotation() {
        releaseMovement();
        restoreTicks++;
        float targetYaw = config.useAbility && abilityRotationSaved ? preAbilityYaw : startYaw;
        float targetPitch = config.useAbility && abilityRotationSaved ? preAbilityPitch : startPitch;
        rotation.setYaw(targetYaw, ROTATION_SMOOTHING);
        rotation.setPitch(targetPitch, ROTATION_SMOOTHING);
        if (rotation.closeTo(targetYaw, targetPitch, 1.0F) || restoreTicks >= RESTORE_TIMEOUT_TICKS) {
            // Smoothing is quantized to the configured mouse sensitivity. Always
            // finish on the exact saved angles so the last sub-degree remainder
            // cannot accumulate into a persistent left/down camera offset.
            rotation.snapTo(targetYaw, targetPitch);
            restoreRodAndContinue();
        }
    }

    private boolean hasBite() {
        boolean hypixel = config.biteDetection != AutoFishConfig.BiteDetection.VANILLA
                && biteSignalGate.acceptsHypixel(gameTick(), hasHypixelBiteMarker());
        boolean vanilla = config.biteDetection != AutoFishConfig.BiteDetection.HYPIXEL
                && biteSignalGate.acceptsVanilla(gameTick(), isVanillaBiting());
        return hypixel || vanilla;
    }

    private boolean isVanillaBiting() {
        return minecraft.player.fishing != null
                && ((FishingHookAccessor) minecraft.player.fishing).frostyAutoFish$isBiting();
    }

    private boolean isUnwantedHookedEntity(Entity entity) {
        return entity instanceof LivingEntity
                && entity != minecraft.player
                && !(entity instanceof Player)
                && !(entity instanceof ArmorStand);
    }

    private boolean hasHypixelBiteMarker() {
        if (minecraft.player.fishing == null) {
            return false;
        }
        List<ArmorStand> stands = minecraft.level.getEntitiesOfClass(
                ArmorStand.class,
                minecraft.player.fishing.getBoundingBox().inflate(2.0),
                stand -> stand.getCustomName() != null
        );
        for (ArmorStand stand : stands) {
            if (stand.getCustomName().getString().contains("!!!")) {
                return true;
            }
        }
        return false;
    }

    private void snapshotBeforeReel() {
        reelSnapshot.clear();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity) {
                reelSnapshot.add(entity.getId());
            }
        }
    }

    private void scanForNewTargets() {
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!reelSnapshot.contains(entity.getId()) && isNewSeaCreatureCandidate(entity)) {
                addTarget(entity);
            }
        }
    }

    private boolean isNewSeaCreatureCandidate(Entity entity) {
        if (!(entity instanceof LivingEntity)
                || entity == minecraft.player
                || entity instanceof ArmorStand
                || !entity.isAlive()) {
            return false;
        }
        boolean approvedPlayerModel = entity instanceof Player playerModel
                && isConfiguredPlayerModelTarget(playerModel);
        if (entity instanceof Player && !approvedPlayerModel) {
            return false;
        }
        if (minecraft.player.distanceTo(entity) > SEA_CREATURE_KEEP_RANGE) {
            return false;
        }
        if (reelHookAnchor == null || reelPlayerAnchor == null) {
            return false;
        }
        boolean insideCaptureArea = ReelCapturePolicy.isInsideCaptureArea(
                entity.position().distanceTo(reelHookAnchor),
                entity.position().distanceTo(reelPlayerAnchor),
                minecraft.player.distanceTo(entity)
        );
        if (insideCaptureArea && approvedPlayerModel) {
            approvedPlayerTargetIds.add(entity.getId());
        }
        return insideCaptureArea;
    }

    private boolean isConfiguredPlayerModelTarget(Player playerModel) {
        if (config.namedPlayerTargets.isEmpty()) {
            return false;
        }
        if (matchesConfiguredName(playerModel.getName().getString())
                || matchesConfiguredName(playerModel.getDisplayName().getString())
                || playerModel.getCustomName() != null
                && matchesConfiguredName(playerModel.getCustomName().getString())) {
            return true;
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
            if (label.getCustomName() != null
                    && matchesConfiguredName(label.getCustomName().getString())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesConfiguredName(String observedName) {
        for (String rule : config.namedPlayerTargets) {
            if (TargetNameMatcher.matches(rule, observedName)) {
                return true;
            }
        }
        return false;
    }

    private boolean ensureTarget() {
        scanSlimeSplits();
        pruneTargets();
        if (!validTarget(currentTarget)) {
            selectNextTarget();
        }
        if (currentTarget == null) {
            beginRestore();
            return false;
        }
        return true;
    }

    private void beginRestore() {
        currentTarget = null;
        currentPath.clear();
        pathIndex = 0;
        pathRecomputeTick = 0;
        restoreTicks = 0;
        if (config.useAbility) {
            setState(AutoFishState.RESTORING_ROTATION);
        } else {
            setState(AutoFishState.RETURNING);
        }
    }

    private void beginRotationRestore() {
        restoreTicks = 0;
        setState(AutoFishState.RESTORING_ROTATION);
    }

    private void restoreRodAndContinue() {
        restoreRodSlot();
        currentTarget = null;
        currentPath.clear();
        pathIndex = 0;
        lastPathGoal = null;
        abilityRotationSaved = false;
        setState(AutoFishState.READY_TO_CAST);
    }

    private void restoreRodSlot() {
        if (rodHand == InteractionHand.MAIN_HAND && rodSlot >= 0) {
            minecraft.player.getInventory().setSelectedSlot(rodSlot);
        }
    }

    private boolean useRod() {
        if (minecraft.gameMode == null || minecraft.player == null || rodHand == null) {
            return false;
        }
        restoreRodSlot();
        minecraft.gameMode.useItem(minecraft.player, rodHand);
        minecraft.player.swing(rodHand);
        return true;
    }

    private void useWeaponAbility() {
        minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
        minecraft.player.swing(InteractionHand.MAIN_HAND);
    }

    private boolean isRodStillAvailable() {
        if (rodHand == InteractionHand.OFF_HAND) {
            return minecraft.player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof FishingRodItem;
        }
        return rodSlot >= 0
                && minecraft.player.getInventory().getItem(rodSlot).getItem() instanceof FishingRodItem;
    }

    private boolean isRodSelectedForFishing() {
        return rodHand == InteractionHand.OFF_HAND
                || minecraft.player.getInventory().getSelectedSlot() == rodSlot;
    }

    private void selectNextTarget() {
        pruneTargets();
        currentTarget = targets.isEmpty() ? null : targets.getFirst();
        abilityCooldown = 0;
        abilityAttempts = 0;
        abilityTargetStartedTick = gameTick();
        int jitterPermille = random.nextInt(
                AbilityUsePolicy.MAX_JITTER_PERMILLE - AbilityUsePolicy.MIN_JITTER_PERMILLE + 1
        ) + AbilityUsePolicy.MIN_JITTER_PERMILLE;
        int delayMillis = AbilityUsePolicy.jitteredDelayMillis(
                config.abilityDelayMillis, jitterPermille
        );
        abilityFirstUseDeadlineNanos = AbilityUsePolicy.initialDeadline(
                System.nanoTime(), delayMillis
        );
        currentPath.clear();
        pathIndex = 0;
        lastPathGoal = null;
    }

    private void discardCurrentTarget() {
        if (currentTarget != null) {
            approvedPlayerTargetIds.remove(currentTarget.getId());
        }
        targets.remove(currentTarget);
        currentTarget = null;
    }

    private void addTarget(Entity entity) {
        if (!targets.contains(entity)) {
            targets.add(entity);
            message("Tracked " + entity.getName().getString()
                    + " (" + targets.size() + "/" + config.triggerAmount + ")");
        }
    }

    private void pruneTargets() {
        Iterator<Entity> iterator = targets.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (!validTarget(entity)) {
                rememberKilledSlime(entity);
                approvedPlayerTargetIds.remove(entity.getId());
                iterator.remove();
            }
        }
    }

    private boolean validTarget(Entity entity) {
        return entity != null
                && entity.isAlive()
                && !entity.isRemoved()
                && (!(entity instanceof Player)
                || approvedPlayerTargetIds.contains(entity.getId()))
                && minecraft.player.distanceTo(entity) <= SEA_CREATURE_KEEP_RANGE;
    }

    private void clearTargets() {
        targets.clear();
        approvedPlayerTargetIds.clear();
    }

    private void rememberKilledSlime(Entity entity) {
        if (entity instanceof Slime) {
            lastKilledSlimePosition = entity.position();
            slimeSplitScanUntil = gameTick() + SLIME_SPLIT_SCAN_TICKS;
        }
    }

    private void scanSlimeSplits() {
        if (lastKilledSlimePosition == null || gameTick() > slimeSplitScanUntil) {
            return;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (isSlimeSplitCandidate(entity)) {
                addTarget(entity);
            }
        }
    }

    private boolean isSlimeSplitCandidate(Entity entity) {
        return entity instanceof Slime
                && lastKilledSlimePosition != null
                && gameTick() <= slimeSplitScanUntil
                && entity.isAlive()
                && entity.position().distanceTo(lastKilledSlimePosition) <= 4.0
                && minecraft.player.distanceTo(entity) <= SEA_CREATURE_KEEP_RANGE;
    }

    private Vec3 findStandableBeside(Entity target) {
        BlockPos base = target.blockPosition();
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int y = -2; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    BlockPos position = base.offset(x, y, z);
                    if (!NavMeshGenerator.isWalkable(position, minecraft.level)) {
                        continue;
                    }
                    Vec3 center = new Vec3(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
                    if (center.distanceTo(target.position()) > desiredAttackRange(target) - 0.2) {
                        continue;
                    }
                    double distance = center.distanceTo(minecraft.player.position());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = center;
                    }
                }
            }
        }
        return best;
    }

    private void computePath(Vec3 goal) {
        pathRecomputeTick = 0;
        lastPathGoal = goal;
        NavMeshPath path = pathService.find(minecraft.player.position(), goal, minecraft.level, PATH_RANGE);
        currentPath = path.isFound() ? new ArrayList<>(path.getWaypoints()) : new ArrayList<>();
        pathIndex = currentPath.size() > 1 ? 1 : 0;
    }

    private void followPath(Vec3 fallback) {
        if (currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            walkToward(fallback);
            return;
        }
        Vec3 next = currentPath.get(pathIndex);
        if (isAt(next, PATH_REACHED_XZ)) {
            pathIndex++;
            releaseMovement();
            return;
        }
        walkToward(next);
    }

    private void walkToward(Vec3 target) {
        Vec3 position = minecraft.player.position();
        double x = target.x - position.x;
        double z = target.z - position.z;
        minecraft.player.setYRot((float) Math.toDegrees(Math.atan2(-x, z)));
        minecraft.options.keyUp.setDown(true);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(target.y - position.y > 0.45);
        minecraft.options.keyShift.setDown(false);
        movementOwned = true;
    }

    private void releaseMovement() {
        if (!movementOwned || minecraft.options == null) {
            return;
        }
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyDown.setDown(false);
        minecraft.options.keyLeft.setDown(false);
        minecraft.options.keyRight.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyShift.setDown(false);
        movementOwned = false;
    }

    private boolean isAt(Vec3 target, double xzThreshold) {
        Vec3 position = minecraft.player.position();
        double x = position.x - target.x;
        double z = position.z - target.z;
        return Math.sqrt(x * x + z * z) <= xzThreshold
                && Math.abs(position.y - target.y) <= 0.85;
    }

    private boolean canAttack(Entity target, Vec3 aim) {
        double range = desiredAttackRange(target);
        if (rotation.canHit(target, range)) {
            return true;
        }
        if (!isSmallTarget(target) || minecraft.player.distanceTo(target) > SMALL_TARGET_ATTACK_RANGE) {
            return false;
        }
        float[] targetRotation = RotationHelper.rotationTo(minecraft.player.getEyePosition(), aim);
        return rotation.closeTo(targetRotation[0], targetRotation[1], 4.0F);
    }

    private double desiredAttackRange(Entity entity) {
        return isSmallTarget(entity) ? SMALL_TARGET_ATTACK_RANGE : ATTACK_RANGE;
    }

    private boolean isSmallTarget(Entity entity) {
        return entity != null && (entity.getBbHeight() <= 0.55 || entity.getBbWidth() <= 0.55);
    }

    private void tickAntiAfk() {
        if (!config.antiAfk || isCombatState()) {
            restoreAntiAfkOffset();
            return;
        }
        long tick = gameTick();
        if (antiAfkOffset && tick >= antiAfkRestoreTick) {
            restoreAntiAfkOffset();
            scheduleAntiAfk();
        } else if (!antiAfkOffset && tick >= nextAntiAfkTick) {
            antiAfkOriginalYaw = minecraft.player.getYRot();
            float direction = random.nextBoolean() ? 1.0F : -1.0F;
            minecraft.player.setYRot(antiAfkOriginalYaw + direction);
            antiAfkOffset = true;
            antiAfkRestoreTick = tick + 100 + random.nextInt(13);
        }
    }

    private void scheduleAntiAfk() {
        if (minecraft.level != null) {
            nextAntiAfkTick = gameTick() + 200 + random.nextInt(201);
        }
    }

    private void restoreAntiAfkOffset() {
        if (antiAfkOffset && minecraft.player != null) {
            minecraft.player.setYRot(antiAfkOriginalYaw);
        }
        antiAfkOffset = false;
    }

    private void renderDebugGizmos() {
        if (minecraft.levelRenderer == null || (currentTarget == null && currentPath.isEmpty())) {
            return;
        }

        try (Gizmos.TemporaryCollection ignored = minecraft.levelRenderer.collectPerFrameGizmos()) {
            if (validTarget(currentTarget)) {
                Gizmos.cuboid(
                        currentTarget.getBoundingBox(),
                        GizmoStyle.strokeAndFill(0xFF00FFFF, 2.0F, 0x3000FFFF)
                ).persistForMillis(80);
            }
            for (int index = 0; index < currentPath.size(); index++) {
                Vec3 point = currentPath.get(index);
                int color = index < pathIndex ? 0xFF50DC78 : 0xFF00FFFF;
                AABB pointBox = new AABB(
                        point.x - 0.16, point.y + 0.02, point.z - 0.16,
                        point.x + 0.16, point.y + 0.34, point.z + 0.16
                );
                Gizmos.cuboid(pointBox, GizmoStyle.stroke(color, 1.5F)).persistForMillis(80);
                if (index > 0) {
                    Gizmos.line(
                            currentPath.get(index - 1).add(0.0, 0.2, 0.0),
                            point.add(0.0, 0.2, 0.0),
                            0xFF00FFFF,
                            2.0F
                    ).persistForMillis(80);
                }
            }
        }
    }

    private boolean isCombatState() {
        return state == AutoFishState.COLLECTING
                || state == AutoFishState.ABILITY
                || state == AutoFishState.CHASING
                || state == AutoFishState.RETURNING
                || state == AutoFishState.RESTORING_ROTATION;
    }

    private void setState(AutoFishState next) {
        if (!state.canTransitionTo(next)) {
            throw new IllegalStateException("Invalid AutoFish transition: " + state + " -> " + next);
        }
        state = next;
        stateSinceTick = minecraft.level == null ? 0 : gameTick();
    }

    private long ticksInState() {
        return gameTick() - stateSinceTick;
    }

    private long gameTick() {
        return minecraft.level.getGameTime();
    }

    private void message(String text) {
        if (minecraft.gui != null) {
            minecraft.gui.getChat().addClientSystemMessage(
                    Component.literal("§b[Frosty AutoFish] §f" + text)
            );
        }
    }
}
