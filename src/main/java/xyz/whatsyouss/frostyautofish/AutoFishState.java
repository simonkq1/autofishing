package xyz.whatsyouss.frostyautofish;

enum AutoFishState {
    DISABLED("Disabled"),
    READY_TO_CAST("Ready"),
    CAST_PENDING("Casting"),
    WAITING_FOR_BITE("Waiting"),
    REELING_HOOKED_ENTITY("Clearing hooked entity"),
    REEL_DELAY("Bite detected"),
    COLLECTING("Collecting creatures"),
    ABILITY("Using ability"),
    CHASING("Chasing creatures"),
    RETURNING("Returning"),
    RESTORING_ROTATION("Restoring view");

    private final String label;

    AutoFishState(String label) {
        this.label = label;
    }

    String label() {
        return label;
    }

    boolean canTransitionTo(AutoFishState next) {
        if (next == DISABLED) {
            return true;
        }
        return switch (this) {
            case DISABLED -> next == READY_TO_CAST;
            case READY_TO_CAST -> next == CAST_PENDING || next == WAITING_FOR_BITE;
            case CAST_PENDING -> next == WAITING_FOR_BITE || next == READY_TO_CAST;
            case WAITING_FOR_BITE -> next == REEL_DELAY
                    || next == REELING_HOOKED_ENTITY
                    || next == READY_TO_CAST;
            case REELING_HOOKED_ENTITY -> next == READY_TO_CAST;
            case REEL_DELAY -> next == COLLECTING || next == READY_TO_CAST;
            case COLLECTING -> next == ABILITY || next == CHASING || next == READY_TO_CAST;
            case ABILITY, CHASING -> next == RETURNING || next == RESTORING_ROTATION;
            case RETURNING -> next == RESTORING_ROTATION;
            case RESTORING_ROTATION -> next == READY_TO_CAST;
        };
    }
}
