package xyz.whatsyouss.frostyautofish;

import xyz.whatsyouss.frostyautofish.config.AutoFishConfigSelfTest;

public final class AutoFishSelfTest {
    private AutoFishSelfTest() {
    }

    public static void main(String[] args) {
        AutoFishConfigSelfTest.run();
        AutoFishStateSelfTest.run();
        BackgroundRunState.setActive(false);
        if (BackgroundRunState.isActive()) {
            throw new AssertionError("background run should start inactive");
        }
        BackgroundRunState.setActive(true);
        if (!BackgroundRunState.isActive()) {
            throw new AssertionError("background run should reflect enabled state");
        }
        BackgroundRunState.setActive(false);
        System.out.println("AutoFishSelfTest: all checks passed");
    }
}
