package xyz.whatsyouss.frostyautofish;

import xyz.whatsyouss.frostyautofish.config.AutoFishConfig.AbilityAim;

import java.util.ArrayList;
import java.util.List;

final class HighValueAbilitySelfTest {
    private static final long MS = 1_000_000L;

    static void run() {
        modeAndWeaponSafety();
        nearestVisibleCandidateAndLimits();
        independentDelayAndCooldown();
        changedTargetOrSettingsRestartDelay();
        cancelledWaitNeverCatchesUp();
        transactionRestoresActualSlotAndView();
        transactionFailuresStillRestoreAndPreserveDispatch();
        aimAndFishingTickResult();
    }

    private static void modeAndWeaponSafety() {
        check(HighValueTargetPolicy.attackMode(false, false) == HighValueTargetPolicy.AttackMode.MELEE,
                "default melee");
        check(HighValueTargetPolicy.attackMode(false, true) == HighValueTargetPolicy.AttackMode.MELEE,
                "Use Ability alone does not change High Value melee");
        check(HighValueTargetPolicy.attackMode(true, false) == HighValueTargetPolicy.AttackMode.NONE,
                "ranged without Use Ability never falls back to melee");
        check(HighValueTargetPolicy.attackMode(true, true) == HighValueTargetPolicy.AttackMode.ABILITY,
                "ranged Ability selected");
        check(weaponAllowed(new boolean[7]), "eligible weapon");
        for (int i = 0; i < 7; i++) {
            boolean[] blocked = new boolean[7];
            blocked[i] = true;
            check(!weaponAllowed(blocked), "weapon safety gate " + i);
        }
        // Each general safety gate remains required regardless of the ranged candidate's eligibility.
        check(safety(true, true, true, true, false, false), "general attack gates open");
        check(!safety(false, true, true, true, false, false), "F10 off");
        check(!safety(true, false, true, true, false, false), "F8 off");
        check(!safety(true, true, false, true, false, false), "Attack off");
        check(!safety(true, true, true, false, false, false), "gameMode absent");
        check(!safety(true, true, true, true, true, false), "Auto Kill combat");
        check(!safety(true, true, true, true, false, true), "Screen or overlay");
    }

    private static boolean weaponAllowed(boolean[] gate) {
        return HighValueTargetPolicy.canUseAbilityWeapon(gate[0], gate[1], gate[2], gate[3], gate[4], gate[5], gate[6]);
    }

    private static boolean safety(boolean master, boolean macro, boolean attack, boolean gameMode,
                                  boolean combat, boolean gui) {
        return HighValueTargetPolicy.canAutoAttack(master, macro, attack, gameMode, combat, gui,
                true, true, 0, 1);
    }

    private record Candidate(double distance, int attacksDone, boolean visible, boolean isAlive,
                             boolean isRemoved) implements HighValueTargetPolicy.AttackCandidateView {
    }

    private static Candidate candidate(double distance, int attempts, boolean visible) {
        return new Candidate(distance, attempts, visible, true, false);
    }

    private static Candidate select(List<Candidate> candidates, int limit) {
        return HighValueTargetPolicy.selectNearestAttackCandidate(candidates, HighValueTargetPolicy.ABILITY_RANGE,
                limit, Candidate::visible);
    }

    private static void nearestVisibleCandidateAndLimits() {
        Candidate exhausted = candidate(2, 1, true);
        Candidate hidden = candidate(5, 0, false);
        Candidate visible = candidate(31, 0, true);
        Candidate edge = candidate(32, 0, true);
        Candidate outside = candidate(32.001, 0, true);
        check(select(List.of(exhausted, hidden, visible, edge, outside), 1) == visible,
                "exhausted and occluded nearest do not block visible target");
        check(select(List.of(hidden, edge, outside), 1) == edge, "32 block edge included");
        check(select(List.of(outside), 1) == null, "outside 32 excluded");
        check(select(List.of(exhausted), 1) == null, "limit stops attempts");
        check(select(List.of(exhausted), 2) == exhausted, "raised limit continues retained count");
        check(select(List.of(exhausted), 0) == null, "lowered limit clamps and stops");
        check(select(List.of(new Candidate(1, 0, true, false, false),
                new Candidate(1, 0, true, true, true)), 1) == null, "dead and removed excluded");
        check(select(List.of(hidden), 1) == null, "Down and Mob share visibility requirement");
        check(select(List.of(candidate(2, 0, true)), 1) != null, "Ability also allows close targets");
    }

    private static HighValueAbilityWait.Key key(Object target) {
        return new HighValueAbilityWait.Key(target, 2, AbilityAim.MOB, 150, 3, true, true);
    }

    private static void independentDelayAndCooldown() {
        HighValueAbilityWait wait = new HighValueAbilityWait();
        HighValueAbilityWait.Key key = key(new Object());
        check(!wait.ready(key, 0, true), "first candidate waits before use");
        check(!wait.ready(key, 150 * MS - 1, true), "delay exact lower edge");
        check(!wait.ready(key, 150 * MS, false), "cooldown blocks elapsed delay");
        check(wait.ready(key, 250 * MS, true), "five tick cooldown expiry does not restart delay");
        wait.restart(key, 250 * MS);
        check(!wait.ready(key, 400 * MS - 1, true), "every attempt gets a full delay");
        check(wait.ready(key, 400 * MS, true), "second delay exact upper edge");
        HighValueAbilityWait another = new HighValueAbilityWait();
        check(!another.ready(key, 400 * MS, true), "no timer shared between coordinators");
    }

    private static void changedTargetOrSettingsRestartDelay() {
        Object target = new Object();
        HighValueAbilityWait.Key initial = key(target);
        List<HighValueAbilityWait.Key> changes = List.of(
                key(new Object()),
                new HighValueAbilityWait.Key(target, 4, AbilityAim.MOB, 150, 3, true, true),
                new HighValueAbilityWait.Key(target, 2, AbilityAim.DOWN, 150, 3, true, true),
                new HighValueAbilityWait.Key(target, 2, AbilityAim.MOB, 200, 3, true, true),
                new HighValueAbilityWait.Key(target, 2, AbilityAim.MOB, 150, 4, true, true),
                new HighValueAbilityWait.Key(target, 2, AbilityAim.MOB, 150, 3, false, true),
                new HighValueAbilityWait.Key(target, 2, AbilityAim.MOB, 150, 3, true, false));
        for (HighValueAbilityWait.Key changed : changes) {
            HighValueAbilityWait wait = new HighValueAbilityWait();
            wait.ready(initial, 0, true);
            check(!wait.ready(changed, 500 * MS, true), "changed key starts full delay");
            check(wait.ready(changed, (500 + changed.delayMillis()) * MS, true), "changed key finishes delay");
        }
        // Targets with equal values are still different entity identities.
        Object a = new String("entity"), b = new String("entity");
        check(!key(a).matches(key(b)), "target identity is not value equality");
    }

    private static void cancelledWaitNeverCatchesUp() {
        // Exercise the production safety-policy -> wait gate, not just repeated calls to cancel().
        boolean[] blockedPolicies = {
                safety(false, true, true, true, false, false),
                safety(true, false, true, true, false, false),
                safety(true, true, false, true, false, false),
                safety(true, true, true, false, false, false),
                safety(true, true, true, true, true, false),
                safety(true, true, true, true, false, true),
                HighValueTargetPolicy.canUseAbilityWeapon(false, true, false, false, false, false, false),
                HighValueTargetPolicy.canUseAbilityWeapon(false, false, false, false, false, false, true),
                select(List.of(candidate(2, 0, false)), 1) != null,
                select(List.of(candidate(33, 0, true)), 1) != null
        };
        for (boolean allowed : blockedPolicies) {
            HighValueAbilityWait wait = new HighValueAbilityWait();
            HighValueAbilityWait.Key key = key(new Object());
            wait.ready(key, 0, true);
            check(!wait.allow(allowed), "invalid production policy closes wait gate");
            check(wait.allow(true), "safe policy reopens wait gate");
            check(!wait.ready(key, 5_000 * MS, true), "reopened gate cannot catch up dispatch");
            check(wait.ready(key, 5_150 * MS, true), "reopened gate waits full delay");
        }
        HighValueAbilityWait wait = new HighValueAbilityWait();
        HighValueAbilityWait.Key key = key(new Object());
        wait.ready(key, 0, true);
        wait.cancel();
        check(!wait.ready(key, 5_000 * MS, true), "explicit lifecycle cancellation also discards pending delay");
    }

    private static void transactionRestoresActualSlotAndView() {
        for (int actualSlot : new int[]{0, 4, 8}) {
            FakeClient client = new FakeClient(actualSlot);
            check(HighValueAbilityUse.perform(client, 2, 90, -15, client.failures::add),
                    "normal use return (including PASS) is one attempt");
            check(client.uses == 1 && client.swings == 1, "exactly one main hand use and swing");
            check(client.slot == actualSlot && client.yaw == 10 && client.pitch == 20,
                    "restore actual selected slot, including arbitrary slot with offhand rod");
            check(client.events.equals(List.of("slot:2", "aim:90.0:-15.0", "use", "swing",
                    "slot:" + actualSlot, "aim:10.0:20.0", "sync:" + actualSlot,
                    "syncRotation:10.0:20.0")), "transaction order");
            client.nextMovementTick();
            check(client.serverYaw == 10 && client.serverPitch == 20 && client.normalRotationPackets == 0,
                    "explicit packet restores server aim even though last-sent cache prevents next-tick packet");
        }
        FakeClient sameSlot = new FakeClient(2);
        check(HighValueAbilityUse.perform(sameSlot, 2, 0, 90, sameSlot.failures::add), "same slot allowed for offhand rod");
        check(sameSlot.events.stream().noneMatch(event -> event.startsWith("sync:")),
                "no redundant restored slot packet when slot unchanged");
        sameSlot.nextMovementTick();
        check(sameSlot.serverYaw == 10 && sameSlot.serverPitch == 20 && sameSlot.normalRotationPackets == 0,
                "same-slot/offhand ability still restores server aim without mouse movement");
    }

    private static void transactionFailuresStillRestoreAndPreserveDispatch() {
        for (String step : List.of("use", "swing", "restoreSlot", "restoreView", "sync", "syncRotation")) {
            FakeClient client = new FakeClient(7);
            client.failAt = step;
            boolean dispatched = HighValueAbilityUse.perform(client, 2, 0, 90, client.failures::add);
            check(dispatched == !step.equals("use"), "post-use failure preserves attempt: " + step);
            check(client.failures.size() == 1, "failure is reported: " + step);
            check(client.events.contains("slot:7") && client.events.contains("aim:10.0:20.0")
                    && client.events.contains("sync:7") && client.events.contains("syncRotation:10.0:20.0"),
                    "all cleanup steps attempted: " + step);
            check(client.slot == 7 && client.yaw == 10 && client.pitch == 20, "state restored: " + step);
            check(client.uses == 1, "no retry within failed transaction: " + step);
            if (!step.equals("syncRotation")) {
                check(client.serverYaw == 10 && client.serverPitch == 20, "server rotation restored: " + step);
            }
        }
    }

    private static void aimAndFishingTickResult() {
        float[] mob = {45, -12};
        float[] down = HighValueAbilityUse.aim(true, 75, mob);
        check(down[0] == 75 && down[1] == 90, "Down uses saved yaw and straight down");
        check(HighValueAbilityUse.aim(false, 75, mob) == mob, "Mob uses target center rotation");
        check(!HighValueAttackResult.NONE.performedAttack() && !HighValueAttackResult.NONE.consumesFishingTick(),
                "waiting does not block fishing or reset cooldown");
        check(HighValueAttackResult.MELEE.performedAttack() && !HighValueAttackResult.MELEE.consumesFishingTick(),
                "melee behavior retained");
        check(HighValueAttackResult.ABILITY.performedAttack() && HighValueAttackResult.ABILITY.consumesFishingTick(),
                "Ability sets cooldown and consumes fishing tick");
    }

    private static final class FakeClient implements HighValueAbilityUse.Client {
        final List<String> events = new ArrayList<>();
        final List<RuntimeException> failures = new ArrayList<>();
        final int originalSlot;
        int slot;
        float yaw = 10, pitch = 20;
        float serverYaw = 10, serverPitch = 20, lastSentYaw = 10, lastSentPitch = 20;
        int normalRotationPackets;
        int uses, swings;
        String failAt = "";

        FakeClient(int slot) { this.slot = slot; originalSlot = slot; }
        public int selectedSlot() { return slot; }
        public float yaw() { return yaw; }
        public float pitch() { return pitch; }
        public void selectSlot(int slot) {
            events.add("slot:" + slot);
            this.slot = slot;
            if (slot == originalSlot) fail("restoreSlot");
        }
        public void rotate(float yaw, float pitch) {
            events.add("aim:" + yaw + ":" + pitch);
            this.yaw = yaw;
            this.pitch = pitch;
            if (yaw == 10 && pitch == 20) fail("restoreView");
        }
        public void useMainHand() {
            events.add("use");
            uses++;
            // Simulate the use packet applying server aim while leaving the local movement cache alone.
            serverYaw = yaw;
            serverPitch = pitch;
            fail("use");
        }
        public void swingMainHand() { events.add("swing"); swings++; fail("swing"); }
        public void syncRestoredSlot(int slot) { events.add("sync:" + slot); fail("sync"); }
        public void syncRestoredRotation(float yaw, float pitch) {
            events.add("syncRotation:" + yaw + ":" + pitch);
            fail("syncRotation");
            serverYaw = yaw;
            serverPitch = pitch;
        }
        void nextMovementTick() {
            if (yaw != lastSentYaw || pitch != lastSentPitch) {
                normalRotationPackets++;
                lastSentYaw = serverYaw = yaw;
                lastSentPitch = serverPitch = pitch;
            }
        }
        private void fail(String step) { if (step.equals(failAt)) throw new IllegalStateException(step); }
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
}
