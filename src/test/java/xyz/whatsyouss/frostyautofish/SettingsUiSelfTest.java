package xyz.whatsyouss.frostyautofish;

import xyz.whatsyouss.frostyautofish.SettingsTranslations.Key;
import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;

final class SettingsUiSelfTest {
    private SettingsUiSelfTest() {
    }

    static void run() {
        responsiveLayoutFitsSupportedWidths();
        translationsAreComplete();
        translatedValuesAndStatesAreStable();
    }

    private static void responsiveLayoutFitsSupportedWidths() {
        checkLayout(320, true, 72);
        checkLayout(499, true, 72);
        checkLayout(500, false, 112);
        checkLayout(854, false, 112);
    }

    private static void checkLayout(int width, boolean stacked, int navWidth) {
        SettingsLayoutPolicy.Dimensions dimensions = SettingsLayoutPolicy.forWidth(width);
        check(dimensions.stacked() == stacked, "stacked policy at " + width);
        check(dimensions.navigationWidth() == navWidth, "navigation width at " + width);
        check(dimensions.panelWidth() <= width - SettingsLayoutPolicy.SCREEN_MARGIN * 2,
                "panel fits at " + width);
        check(dimensions.navigationWidth() + SettingsLayoutPolicy.COLUMN_GAP
                        + dimensions.viewportWidth() == dimensions.panelWidth(),
                "main columns fill panel at " + width);
        check(dimensions.contentWidth() + SettingsLayoutPolicy.SCROLL_RESERVE
                        == dimensions.viewportWidth(),
                "scroll content fits viewport at " + width);
        if (stacked) {
            check(dimensions.controlWidth() == dimensions.contentWidth(),
                    "stacked control fills content at " + width);
        } else {
            check(dimensions.textWidth() + SettingsLayoutPolicy.COLUMN_GAP
                            + dimensions.controlWidth() == dimensions.contentWidth(),
                    "wide row fits content at " + width);
            check(dimensions.controlWidth() >= 110 && dimensions.controlWidth() <= 160,
                    "wide control range at " + width);
        }
    }

    private static void translationsAreComplete() {
        for (Key key : Key.values()) {
            String english = SettingsTranslations.text(AutoFishConfig.Language.ENGLISH, key);
            String chinese = SettingsTranslations.text(AutoFishConfig.Language.TRADITIONAL_CHINESE, key);
            check(english != null && !english.isBlank(), "English translation for " + key);
            check(chinese != null && !chinese.isBlank(), "Chinese translation for " + key);
        }
        check(SettingsTranslations.dictionary(AutoFishConfig.Language.ENGLISH).size() == Key.values().length,
                "English dictionary complete");
        check(SettingsTranslations.dictionary(AutoFishConfig.Language.TRADITIONAL_CHINESE).size()
                        == Key.values().length,
                "Chinese dictionary complete");
    }

    private static void translatedValuesAndStatesAreStable() {
        for (AutoFishConfig.Language uiLanguage : AutoFishConfig.Language.values()) {
            check(SettingsTranslations.text(uiLanguage, Key.LANGUAGE_ENGLISH).equals("English"),
                    "English language option has a fixed self-name");
            check(SettingsTranslations.text(uiLanguage, Key.LANGUAGE_TRADITIONAL_CHINESE).equals("中文"),
                    "Chinese language option has a fixed self-name");
        }
        check(SettingsTranslations.text(AutoFishConfig.Language.TRADITIONAL_CHINESE, Key.ON).equals("開啟"),
                "boolean value translated");
        check(SettingsTranslations.state(AutoFishConfig.Language.TRADITIONAL_CHINESE, "Waiting")
                        .equals("等待咬鉤"),
                "state translated");
        check(SettingsTranslations.state(AutoFishConfig.Language.TRADITIONAL_CHINESE, "Unknown")
                        .equals("Unknown"),
                "unknown state preserved");
        check(SettingsTranslations.text(AutoFishConfig.Language.TRADITIONAL_CHINESE,
                        Key.PAGE_NUMBER, 2, 4).equals("第 2 / 4 頁"),
                "translation arguments substituted");
    }

    private static void check(boolean condition, String name) {
        if (!condition) {
            throw new AssertionError("Failed check: " + name);
        }
    }
}
