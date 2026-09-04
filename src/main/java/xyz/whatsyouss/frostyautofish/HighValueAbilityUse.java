package xyz.whatsyouss.frostyautofish;

import java.util.function.Consumer;

/** The runtime transaction is also exercised with a fake client by the dependency-free self-test. */
final class HighValueAbilityUse {
    interface Client {
        int selectedSlot();
        float yaw();
        float pitch();
        void selectSlot(int slot);
        void rotate(float yaw, float pitch);
        void useMainHand();
        void swingMainHand();
        void syncRestoredSlot(int slot);
        void syncRestoredRotation(float yaw, float pitch);
    }

    private HighValueAbilityUse() {
    }

    static float[] aim(boolean down, float savedYaw, float[] mobRotation) {
        return down ? new float[]{savedYaw, 90.0F} : mobRotation;
    }

    static boolean perform(Client client, int weaponSlot, float yaw, float pitch,
                           Consumer<RuntimeException> reportFailure) {
        int savedSlot = client.selectedSlot();
        float savedYaw = client.yaw();
        float savedPitch = client.pitch();
        boolean used = false;
        try {
            client.selectSlot(weaponSlot);
            client.rotate(yaw, pitch);
            // A normal return counts even for PASS: vanilla may already have sent the use packet.
            client.useMainHand();
            used = true;
            client.swingMainHand();
        } catch (RuntimeException failure) {
            reportFailure.accept(failure);
        } finally {
            try {
                client.selectSlot(savedSlot);
            } catch (RuntimeException failure) {
                reportFailure.accept(failure);
            } finally {
                try {
                    client.rotate(savedYaw, savedPitch);
                } catch (RuntimeException failure) {
                    reportFailure.accept(failure);
                } finally {
                    try {
                        if (weaponSlot != savedSlot) {
                            client.syncRestoredSlot(savedSlot);
                        }
                    } catch (RuntimeException failure) {
                        reportFailure.accept(failure);
                    } finally {
                        // useItem sends aim to the server, without changing LocalPlayer's last-sent
                        // rotation cache. Restoring locally alone cannot trigger a correcting packet.
                        try {
                            client.syncRestoredRotation(savedYaw, savedPitch);
                        } catch (RuntimeException failure) {
                            reportFailure.accept(failure);
                        }
                    }
                }
            }
        }
        return used;
    }
}
