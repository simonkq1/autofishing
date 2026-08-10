package xyz.whatsyouss.frostyautofish.config;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TargetNameMatcher {
    public static final int MAX_RULE_LENGTH = 64;

    private static final Pattern FORMATTING_CODE = Pattern.compile("(?i)§[0-9A-FK-OR]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private TargetNameMatcher() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return WHITESPACE.matcher(FORMATTING_CODE.matcher(value).replaceAll(""))
                .replaceAll(" ")
                .trim();
    }

    public static boolean matches(String rule, String observedName) {
        String normalizedRule = normalize(rule).toLowerCase(Locale.ROOT);
        String normalizedObserved = normalize(observedName).toLowerCase(Locale.ROOT);
        return !normalizedRule.isEmpty() && normalizedObserved.contains(normalizedRule);
    }
}
