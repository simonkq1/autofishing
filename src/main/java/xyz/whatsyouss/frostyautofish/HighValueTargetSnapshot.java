package xyz.whatsyouss.frostyautofish;

public record HighValueTargetSnapshot(
        String name,
        double distance,
        int attacksDone,
        int attackLimit,
        boolean autoAttackEnabled
) {
}
