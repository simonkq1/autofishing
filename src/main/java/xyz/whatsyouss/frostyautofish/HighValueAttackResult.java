package xyz.whatsyouss.frostyautofish;

enum HighValueAttackResult {
    NONE, MELEE, ABILITY;

    boolean performedAttack() {
        return this != NONE;
    }

    boolean consumesFishingTick() {
        return this == ABILITY;
    }
}
