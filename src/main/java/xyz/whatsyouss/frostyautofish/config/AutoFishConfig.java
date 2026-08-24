package xyz.whatsyouss.frostyautofish.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class AutoFishConfig {
    public boolean autoThrow = true;
    public boolean antiAfk = true;
    public boolean backgroundRun = true;
    public boolean lockControls = false;
    public int maxWaitSeconds = 30;
    public int dryTimeoutSeconds = 15;
    public boolean autoKill = true;
    public int triggerAmount = 3;
    public boolean useAbility = false;
    public int abilityDelayMillis = 150;
    public AbilityAim abilityAim = AbilityAim.MOB;
    public int weaponSlot = 1;
    public BiteDetection biteDetection = BiteDetection.BOTH;
    public List<String> namedPlayerTargets = new ArrayList<>();
    public List<String> highValueTargets = new ArrayList<>();
    public boolean highValueEnabled = true;
    public boolean showHighValueCollision = true;
    public boolean showHighValueHud = true;
    public boolean autoAttackHighValue = false;
    public int highValueAttackCount = 1;

    public void normalize() {
        maxWaitSeconds = clamp(maxWaitSeconds, 5, 60);
        dryTimeoutSeconds = clamp(dryTimeoutSeconds, 5, 60);
        triggerAmount = clamp(triggerAmount, 1, 15);
        abilityDelayMillis = clamp(abilityDelayMillis, 50, 1000);
        weaponSlot = clamp(weaponSlot, 1, 9);
        highValueAttackCount = clamp(highValueAttackCount, 1, 10);
        abilityAim = Objects.requireNonNullElse(abilityAim, AbilityAim.MOB);
        biteDetection = Objects.requireNonNullElse(biteDetection, BiteDetection.BOTH);
        namedPlayerTargets = normalizeTargetNames(namedPlayerTargets);
        highValueTargets = normalizeTargetNames(highValueTargets);
    }

    public AutoFishConfig copy() {
        AutoFishConfig copy = new AutoFishConfig();
        copy.autoThrow = autoThrow;
        copy.antiAfk = antiAfk;
        copy.backgroundRun = backgroundRun;
        copy.lockControls = lockControls;
        copy.maxWaitSeconds = maxWaitSeconds;
        copy.dryTimeoutSeconds = dryTimeoutSeconds;
        copy.autoKill = autoKill;
        copy.triggerAmount = triggerAmount;
        copy.useAbility = useAbility;
        copy.abilityDelayMillis = abilityDelayMillis;
        copy.abilityAim = abilityAim;
        copy.weaponSlot = weaponSlot;
        copy.biteDetection = biteDetection;
        copy.namedPlayerTargets = new ArrayList<>(namedPlayerTargets);
        copy.highValueTargets = new ArrayList<>(highValueTargets);
        copy.highValueEnabled = highValueEnabled;
        copy.showHighValueCollision = showHighValueCollision;
        copy.showHighValueHud = showHighValueHud;
        copy.autoAttackHighValue = autoAttackHighValue;
        copy.highValueAttackCount = highValueAttackCount;
        return copy;
    }

    public void copyFrom(AutoFishConfig other) {
        autoThrow = other.autoThrow;
        antiAfk = other.antiAfk;
        backgroundRun = other.backgroundRun;
        lockControls = other.lockControls;
        maxWaitSeconds = other.maxWaitSeconds;
        dryTimeoutSeconds = other.dryTimeoutSeconds;
        autoKill = other.autoKill;
        triggerAmount = other.triggerAmount;
        useAbility = other.useAbility;
        abilityDelayMillis = other.abilityDelayMillis;
        abilityAim = other.abilityAim;
        weaponSlot = other.weaponSlot;
        biteDetection = other.biteDetection;
        namedPlayerTargets = new ArrayList<>(other.namedPlayerTargets);
        highValueTargets = new ArrayList<>(other.highValueTargets);
        highValueEnabled = other.highValueEnabled;
        showHighValueCollision = other.showHighValueCollision;
        showHighValueHud = other.showHighValueHud;
        autoAttackHighValue = other.autoAttackHighValue;
        highValueAttackCount = other.highValueAttackCount;
        normalize();
    }

    private static List<String> normalizeTargetNames(List<String> names) {
        if (names == null) {
            return new ArrayList<>();
        }
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String name : names) {
            String normalized = TargetNameMatcher.normalize(name);
            if (!normalized.isEmpty() && normalized.length() <= TargetNameMatcher.MAX_RULE_LENGTH) {
                unique.putIfAbsent(normalized.toLowerCase(Locale.ROOT), normalized);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum AbilityAim {
        MOB("Mob"),
        DOWN("Down");

        private final String label;

        AbilityAim(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public enum BiteDetection {
        BOTH("Hypixel + Vanilla"),
        HYPIXEL("Hypixel only"),
        VANILLA("Vanilla only");

        private final String label;

        BiteDetection(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
