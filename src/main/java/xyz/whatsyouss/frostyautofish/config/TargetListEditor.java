package xyz.whatsyouss.frostyautofish.config;

import java.util.List;
import java.util.Locale;

public final class TargetListEditor {
    private TargetListEditor() {
    }

    public static Result add(List<String> targets, String input) {
        String normalizedName = TargetNameMatcher.normalize(input);
        if (normalizedName.isEmpty()) {
            return Result.failure(Status.EMPTY_NAME, normalizedName, 0);
        }
        if (normalizedName.length() > TargetNameMatcher.MAX_RULE_LENGTH) {
            return Result.failure(Status.NAME_TOO_LONG, normalizedName, 0);
        }
        if (containsIgnoreCase(targets, normalizedName)) {
            return Result.failure(Status.DUPLICATE, normalizedName, 0);
        }

        targets.add(normalizedName);
        return Result.success(Status.ADDED, normalizedName, 1);
    }

    public static Result remove(List<String> targets, String input) {
        String normalizedName = TargetNameMatcher.normalize(input);
        boolean removed = targets.removeIf(existing -> equalsIgnoreCase(existing, normalizedName));
        if (!removed) {
            return Result.failure(Status.NOT_FOUND, normalizedName, 0);
        }
        return Result.success(Status.REMOVED, normalizedName, 1);
    }

    public static Result clear(List<String> targets) {
        int count = targets.size();
        targets.clear();
        return Result.success(Status.CLEARED, "", count);
    }

    private static boolean containsIgnoreCase(List<String> targets, String normalizedName) {
        return targets.stream().anyMatch(existing -> equalsIgnoreCase(existing, normalizedName));
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }

    public record Result(boolean success, Status status, String normalizedName, int count) {
        private static Result success(Status status, String normalizedName, int count) {
            return new Result(true, status, normalizedName, count);
        }

        private static Result failure(Status status, String normalizedName, int count) {
            return new Result(false, status, normalizedName, count);
        }
    }

    public enum Status {
        ADDED,
        REMOVED,
        CLEARED,
        EMPTY_NAME,
        NAME_TOO_LONG,
        DUPLICATE,
        NOT_FOUND
    }
}
