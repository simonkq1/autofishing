package xyz.whatsyouss.frostyautofish.config;

import java.util.ArrayList;
import java.util.List;

public final class AutoFishConfigSelfTest {
    private AutoFishConfigSelfTest() {
    }

    public static void run() {
        defaultsAreStable();
        legacyAndUnknownFieldsAreIgnored();
        lockControlsTrueIsParsed();
        valuesAreClampedAndNullEnumsFallBack();
        malformedJsonFallsBackToDefaults();
        copyAndCopyFromKeepLockControls();
        lockControlsSurvivesSaveRoundTrip();
        namedPlayerTargetsAreNormalizedAndMatched();
        targetListEditorEditsNames();
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
        check(!config.lockControls, "legacy config keeps lock controls disabled");
    }

    private static void defaultsAreStable() {
        AutoFishConfig config = ConfigManager.parse("{}");
        check(config.autoThrow, "autoThrow default");
        check(config.antiAfk, "antiAfk default");
        check(config.backgroundRun, "backgroundRun default");
        check(!config.lockControls, "lockControls default");
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

    private static void lockControlsTrueIsParsed() {
        AutoFishConfig config = ConfigManager.parse("{\"lockControls\":true}");
        check(config.lockControls, "lockControls true parsed");
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
        check(!config.lockControls, "malformed JSON lockControls default");
    }

    private static void copyAndCopyFromKeepLockControls() {
        AutoFishConfig enabled = new AutoFishConfig();
        enabled.lockControls = true;
        AutoFishConfig copy = enabled.copy();
        check(copy.lockControls, "copy keeps lockControls true");

        AutoFishConfig target = new AutoFishConfig();
        target.copyFrom(enabled);
        check(target.lockControls, "copyFrom keeps lockControls true");

        target.copyFrom(new AutoFishConfig());
        check(!target.lockControls, "copyFrom keeps lockControls false");
    }

    private static void lockControlsSurvivesSaveRoundTrip() {
        AutoFishConfig source = new AutoFishConfig();
        source.lockControls = true;
        AutoFishConfig reloaded = ConfigManager.parse(ConfigManager.serialize(source));
        check(reloaded.lockControls, "serialized lockControls reloads true");
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

    private static void targetListEditorEditsNames() {
        List<String> targets = new ArrayList<>();

        TargetListEditor.Result added = TargetListEditor.add(targets, "  §cTrash   Gobbler  ");
        check(added.success(), "target editor add succeeds");
        check(added.normalizedName().equals("Trash Gobbler"), "target editor add normalizes");
        check(targets.size() == 1 && targets.getFirst().equals("Trash Gobbler"), "target editor stores normalized");

        TargetListEditor.Result duplicate = TargetListEditor.add(targets, "trash gobbler");
        check(!duplicate.success(), "target editor rejects case duplicate");
        check(duplicate.status() == TargetListEditor.Status.DUPLICATE, "target editor duplicate status");
        check(targets.size() == 1, "target editor duplicate does not write");

        TargetListEditor.Result empty = TargetListEditor.add(targets, "   ");
        check(!empty.success(), "target editor rejects empty");
        check(empty.status() == TargetListEditor.Status.EMPTY_NAME, "target editor empty status");

        String tooLongName = "x".repeat(TargetNameMatcher.MAX_RULE_LENGTH + 1);
        TargetListEditor.Result tooLong = TargetListEditor.add(targets, tooLongName);
        check(!tooLong.success(), "target editor rejects long name");
        check(tooLong.status() == TargetListEditor.Status.NAME_TOO_LONG, "target editor long status");

        TargetListEditor.Result removed = TargetListEditor.remove(targets, "TRASH GOBBLER");
        check(removed.success(), "target editor remove ignores case");
        check(targets.isEmpty(), "target editor remove writes");

        TargetListEditor.Result missing = TargetListEditor.remove(targets, "Trash Gobbler");
        check(!missing.success(), "target editor missing remove fails");
        check(missing.status() == TargetListEditor.Status.NOT_FOUND, "target editor missing status");

        TargetListEditor.add(targets, "Lava Blaze");
        TargetListEditor.add(targets, "Water Hydra");
        TargetListEditor.Result cleared = TargetListEditor.clear(targets);
        check(cleared.success(), "target editor clear succeeds");
        check(cleared.count() == 2, "target editor clear count");
        check(targets.isEmpty(), "target editor clear writes");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Failed check: " + name);
        }
    }
}
