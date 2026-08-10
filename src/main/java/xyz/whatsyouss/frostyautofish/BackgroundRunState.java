package xyz.whatsyouss.frostyautofish;

public final class BackgroundRunState {
    private static volatile boolean active;

    private BackgroundRunState() {
    }

    public static boolean isActive() {
        return active;
    }

    static void setActive(boolean value) {
        active = value;
    }
}
