package xyz.whatsyouss.frostyautofish.config;

public final class AutoFishConfigSelfTest {
    private AutoFishConfigSelfTest() {
    }

    public static void run() {
        defaultsAreStable();
        legacyAndUnknownFieldsAreIgnored();
        valuesAreClampedAndNullEnumsFallBack();
        malformedJsonFallsBackToDefaults();
        namedPlayerTargetsAreNormalizedAndMatched();
    }

    private static void legacyAndUnknownFieldsAreIgnored() {
        AutoFishConfig config = ConfigManager.parse("""
                {
                  "autoThrow": false,
                  "oldDelaySetting": 42,
                  "removedModuleOption": true
                }
                """);
        check(!config.autoThrow, "known field from older config");
        check(config.maxWaitSeconds == 30, "missing field keeps default");
        check(config.dryTimeoutSeconds == 15, "new dry timeout keeps default");
        check(config.biteDetection == AutoFishConfig.BiteDetection.BOTH, "new enum keeps default");
    }

    private static void defaultsAreStable() {
        AutoFishConfig config = ConfigManager.parse("{}");
        check(config.autoThrow, "autoThrow default");
        check(config.antiAfk, "antiAfk default");
        check(config.backgroundRun, "backgroundRun default");
        check(config.maxWaitSeconds == 30, "maxWaitSeconds default");
        check(config.dryTimeoutSeconds == 15, "dryTimeoutSeconds default");
        check(config.autoKill, "autoKill default");
        check(config.triggerAmount == 3, "triggerAmount default");
        check(!config.useAbility, "useAbility default");
        check(config.abilityDelayMillis == 150, "abilityDelayMillis default");
        check(config.abilityAim == AutoFishConfig.AbilityAim.MOB, "abilityAim default");
        check(config.weaponSlot == 1, "weaponSlot default");
        check(config.biteDetection == AutoFishConfig.BiteDetection.BOTH, "biteDetection default");
        check(config.namedPlayerTargets.isEmpty(), "namedPlayerTargets default");
    }

    private static void valuesAreClampedAndNullEnumsFallBack() {
        AutoFishConfig config = ConfigManager.parse("""
                {
                  "maxWaitSeconds": 999,
                  "dryTimeoutSeconds": 1,
                  "abilityDelayMillis": 5000,
                  "triggerAmount": -20,
                  "weaponSlot": 40,
                  "abilityAim": "UNKNOWN",
                  "biteDetection": "UNKNOWN"
                }
                """);
        check(config.maxWaitSeconds == 60, "maxWaitSeconds clamp");
        check(config.dryTimeoutSeconds == 5, "dryTimeoutSeconds clamp");
        check(config.abilityDelayMillis == 1000, "abilityDelayMillis clamp");
        check(config.triggerAmount == 1, "triggerAmount clamp");
        check(config.weaponSlot == 9, "weaponSlot clamp");
        check(config.abilityAim == AutoFishConfig.AbilityAim.MOB, "abilityAim fallback");
        check(config.biteDetection == AutoFishConfig.BiteDetection.BOTH, "biteDetection fallback");
    }

    private static void malformedJsonFallsBackToDefaults() {
        AutoFishConfig config = ConfigManager.parse("{this is not json");
        check(config.maxWaitSeconds == 30, "malformed JSON maxWaitSeconds");
        check(config.dryTimeoutSeconds == 15, "malformed JSON dryTimeoutSeconds");
        check(config.abilityDelayMillis == 150, "malformed JSON abilityDelayMillis");
        check(config.triggerAmount == 3, "malformed JSON triggerAmount");
        check(config.biteDetection == AutoFishConfig.BiteDetection.BOTH, "malformed JSON biteDetection");
    }

    private static void namedPlayerTargetsAreNormalizedAndMatched() {
        AutoFishConfig config = ConfigManager.parse("""
                {
                  "namedPlayerTargets": [
                    "  Trash   Gobbler  ",
                    "trash gobbler",
                    "§cLava Blaze",
                    "",
                    null
                  ]
                }
                """);
        check(config.namedPlayerTargets.size() == 2, "target names deduplicated");
        check(config.namedPlayerTargets.get(0).equals("Trash Gobbler"), "target whitespace normalized");
        check(config.namedPlayerTargets.get(1).equals("Lava Blaze"), "target color removed");
        check(TargetNameMatcher.matches(
                "Trash Gobbler",
                "§6[Lv100] TRASH GOBBLER §c120k/120k❤"
        ), "formatted overhead name matched");
        check(!TargetNameMatcher.matches("Trash Gobbler", "Water Hydra"),
                "unrelated overhead name rejected");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Failed check: " + name);
        }
    }
}
