package xyz.whatsyouss.frostyautofish;

final class InputLockState {
    private boolean active;

    boolean setActive(boolean active) {
        boolean activated = active && !this.active;
        this.active = active;
        return activated;
    }

    boolean isActive() {
        return active;
    }

    boolean isBlocking(boolean screenOpen, boolean overlayOpen) {
        return active && !screenOpen && !overlayOpen;
    }

    boolean isSuspended(boolean screenOpen, boolean overlayOpen) {
        return active && (screenOpen || overlayOpen);
    }
}
