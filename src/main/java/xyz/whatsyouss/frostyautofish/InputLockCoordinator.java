package xyz.whatsyouss.frostyautofish;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import xyz.whatsyouss.frostyautofish.mixin.KeyMappingAccessor;
import xyz.whatsyouss.frostyautofish.mixin.MouseHandlerAccessor;
import xyz.whatsyouss.frostyautofish.mixin.ToggleKeyMappingAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class InputLockCoordinator {
    private static InputLockCoordinator instance;

    private final Minecraft minecraft;
    private final KeyMapping highValueToggleKey;
    private final KeyMapping toggleKey;
    private final KeyMapping configKey;
    private final InputLockState state = new InputLockState();
    private List<KeyMapping> blockedMappings = List.of();
    private List<KeyMapping> allowedMappings = List.of();
    private boolean mappingsReady;
    private boolean cursorReleasedByLock;
    private EventSnapshot keyboardSnapshot;
    private EventSnapshot mouseSnapshot;
    private HotkeyClickSnapshot highValueClickSnapshot;
    private int highValueMouseButtonDown = -1;

    private InputLockCoordinator(
            Minecraft minecraft,
            KeyMapping highValueToggleKey,
            KeyMapping toggleKey,
            KeyMapping configKey
    ) {
        this.minecraft = minecraft;
        this.highValueToggleKey = highValueToggleKey;
        this.toggleKey = toggleKey;
        this.configKey = configKey;
    }

    public static InputLockCoordinator initialize(
            Minecraft minecraft,
            KeyMapping highValueToggleKey,
            KeyMapping toggleKey,
            KeyMapping configKey
    ) {
        instance = new InputLockCoordinator(minecraft, highValueToggleKey, toggleKey, configKey);
        return instance;
    }

    public static boolean blockMouseTurn() {
        return instance != null && instance.isBlocking();
    }

    public static boolean blockWorldScroll() {
        return instance != null && instance.isBlocking();
    }

    public static boolean beginKeyboardEvent(long window, KeyEvent event, int action) {
        return instance != null && instance.beginKeyboard(window, event, action);
    }

    public static void endKeyboardEvent() {
        if (instance != null) {
            instance.reconcileHighValueClick();
            instance.keyboardSnapshot = instance.restore(instance.keyboardSnapshot);
        }
    }

    public static boolean beginMouseButtonEvent(long window, MouseButtonInfo buttonInfo, int action) {
        return instance != null && instance.beginMouse(window, buttonInfo, action);
    }

    public static void endMouseButtonEvent() {
        if (instance != null) {
            instance.reconcileHighValueClick();
            instance.mouseSnapshot = instance.restore(instance.mouseSnapshot);
        }
    }

    public static boolean gateSetAll() {
        return instance != null && instance.state.isActive() && instance.ensureMappingsReady();
    }

    public static void beforeToggleRestore() {
        if (instance == null || !instance.state.isActive() || !instance.ensureMappingsReady()) {
            return;
        }
        instance.clearBlockedPendingToggleRestores();
    }

    public void setActive(boolean active) {
        boolean ready = ensureMappingsReady();
        boolean wasActive = state.isActive();
        boolean activated = state.setActive(active);
        if (active && ready && activated) {
            clearStaleInput();
        } else if (wasActive && !active) {
            keyboardSnapshot = null;
            mouseSnapshot = null;
            highValueClickSnapshot = null;
            if (ready) {
                restoreHeldKeyboardInput();
            }
        }
        syncCursorVisibility();
    }

    private boolean beginKeyboard(long window, KeyEvent event, int action) {
        keyboardSnapshot = null;
        beginHighValueClick(window, event, action);
        if (!state.isActive() || !ensureMappingsReady()) {
            return false;
        }
        List<KeyMapping> matched = matchingKeyboardMappings(event);
        if (state.isSuspended(minecraft.screen != null, minecraft.getOverlay() != null)) {
            if (!matched.isEmpty()) {
                keyboardSnapshot = snapshot(matched, action != GLFW.GLFW_RELEASE);
            }
            return false;
        }
        InputLockPolicy.Decision decision = InputLockPolicy.decide(
                true,
                !matched.isEmpty(),
                event.key() == GLFW.GLFW_KEY_ESCAPE || matchesAllowedKeyboard(event),
                phase(action)
        );
        if (decision == InputLockPolicy.Decision.PASS_AND_RESTORE) {
            keyboardSnapshot = snapshot(matched, action != GLFW.GLFW_RELEASE);
        }
        return decision == InputLockPolicy.Decision.BLOCK;
    }

    private void beginHighValueClick(long window, KeyEvent event, int action) {
        boolean matches = window == minecraft.getWindow().handle() && highValueToggleKey.matches(event);
        beginHighValueClick(matches, phase(action));
    }

    private void beginHighValueClick(boolean matches, InputLockPolicy.Phase phase) {
        int clickCount = ((KeyMappingAccessor) highValueToggleKey).frostyAutoFish$getClickCount();
        highValueClickSnapshot = new HotkeyClickSnapshot(matches, phase, clickCount);
    }

    private void reconcileHighValueClick() {
        HotkeyClickSnapshot snapshot = highValueClickSnapshot;
        highValueClickSnapshot = null;
        if (snapshot == null) {
            return;
        }
        KeyMappingAccessor accessor = (KeyMappingAccessor) highValueToggleKey;
        int current = accessor.frostyAutoFish$getClickCount();
        int reconciled = InputLockPolicy.reconcileHotkeyClicks(
                snapshot.phase,
                snapshot.matches,
                snapshot.clickCount,
                current
        );
        if (reconciled != current) {
            accessor.frostyAutoFish$setClickCount(reconciled);
        }
    }

    private boolean beginMouse(long window, MouseButtonInfo buttonInfo, int action) {
        mouseSnapshot = null;
        beginHighValueMouseClick(window, buttonInfo, action);
        if (!isBlocking()) {
            return false;
        }
        MouseButtonEvent event = new MouseButtonEvent(0.0, 0.0, buttonInfo);
        List<KeyMapping> matched = matchingMouseMappings(event);
        InputLockPolicy.Decision decision = InputLockPolicy.decide(
                true,
                !matched.isEmpty(),
                matchesAllowedMouse(event),
                phase(action)
        );
        if (decision == InputLockPolicy.Decision.PASS_AND_RESTORE) {
            mouseSnapshot = snapshot(matched, action != GLFW.GLFW_RELEASE);
        }
        return decision == InputLockPolicy.Decision.BLOCK;
    }

    private void beginHighValueMouseClick(long window, MouseButtonInfo buttonInfo, int action) {
        boolean validWindow = window == minecraft.getWindow().handle();
        MouseButtonEvent event = new MouseButtonEvent(0.0, 0.0, buttonInfo);
        boolean matches = validWindow && highValueToggleKey.matchesMouse(event);
        InputLockPolicy.Phase eventPhase = phase(action);
        boolean repeatedPress = matches
                && eventPhase == InputLockPolicy.Phase.PRESS
                && highValueMouseButtonDown == buttonInfo.button();
        InputLockPolicy.Phase hotkeyPhase = InputLockPolicy.normalizeMouseHotkeyPhase(
                eventPhase,
                matches,
                repeatedPress
        );
        if (validWindow && eventPhase == InputLockPolicy.Phase.RELEASE
                && highValueMouseButtonDown == buttonInfo.button()) {
            highValueMouseButtonDown = -1;
        } else if (matches && eventPhase == InputLockPolicy.Phase.PRESS && !repeatedPress) {
            highValueMouseButtonDown = buttonInfo.button();
        }
        beginHighValueClick(matches, hotkeyPhase);
    }

    private boolean isBlocking() {
        return ensureMappingsReady()
                && state.isBlocking(minecraft.screen != null, minecraft.getOverlay() != null);
    }

    private void syncCursorVisibility() {
        boolean shouldReleaseCursor = ensureMappingsReady()
                && state.isBlocking(minecraft.screen != null, minecraft.getOverlay() != null);
        if (shouldReleaseCursor) {
            if (minecraft.mouseHandler.isMouseGrabbed()) {
                minecraft.mouseHandler.releaseMouse();
                cursorReleasedByLock = true;
            }
            return;
        }
        if (!cursorReleasedByLock || minecraft.screen != null || minecraft.getOverlay() != null) {
            return;
        }
        if (!minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.grabMouse();
        }
        cursorReleasedByLock = false;
    }

    private void clearStaleInput() {
        if (!ensureMappingsReady()) {
            return;
        }
        clearBlockedPendingToggleRestores();
        for (KeyMapping mapping : blockedMappings) {
            setDownExactly(mapping, false);
            drainClicks(mapping);
        }
        keyboardSnapshot = null;
        mouseSnapshot = null;
        if (minecraft.mouseHandler instanceof MouseHandlerAccessor accessor) {
            resetLockedMouseInput(accessor);
        }
    }

    private void restoreHeldKeyboardInput() {
        if (minecraft.screen == null && minecraft.getOverlay() == null) {
            KeyMapping.setAll();
        }
    }

    private List<KeyMapping> matchingKeyboardMappings(KeyEvent event) {
        if (!ensureMappingsReady()) {
            return List.of();
        }
        List<KeyMapping> matched = new ArrayList<>();
        for (KeyMapping mapping : blockedMappings) {
            if (mapping.matches(event)) {
                matched.add(mapping);
            }
        }
        return matched;
    }

    private List<KeyMapping> matchingMouseMappings(MouseButtonEvent event) {
        if (!ensureMappingsReady()) {
            return List.of();
        }
        List<KeyMapping> matched = new ArrayList<>();
        for (KeyMapping mapping : blockedMappings) {
            if (mapping.matchesMouse(event)) {
                matched.add(mapping);
            }
        }
        return matched;
    }

    private boolean matchesAllowedKeyboard(KeyEvent event) {
        if (!ensureMappingsReady()) {
            return false;
        }
        for (KeyMapping mapping : allowedMappings) {
            if (mapping.matches(event)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAllowedMouse(MouseButtonEvent event) {
        if (!ensureMappingsReady()) {
            return false;
        }
        for (KeyMapping mapping : allowedMappings) {
            if (mapping.matchesMouse(event)) {
                return true;
            }
        }
        return false;
    }

    private static InputLockPolicy.Phase phase(int action) {
        return switch (action) {
            case GLFW.GLFW_RELEASE -> InputLockPolicy.Phase.RELEASE;
            case GLFW.GLFW_REPEAT -> InputLockPolicy.Phase.REPEAT;
            default -> InputLockPolicy.Phase.PRESS;
        };
    }

    private static EventSnapshot snapshot(List<KeyMapping> mappings, boolean drainAfterward) {
        boolean[] previousDown = new boolean[mappings.size()];
        for (int index = 0; index < mappings.size(); index++) {
            previousDown[index] = mappings.get(index).isDown();
        }
        return new EventSnapshot(List.copyOf(mappings), previousDown, drainAfterward);
    }

    private EventSnapshot restore(EventSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        for (int index = 0; index < snapshot.mappings.size(); index++) {
            KeyMapping mapping = snapshot.mappings.get(index);
            setDownExactly(mapping, snapshot.previousDown[index]);
            if (snapshot.drainAfterward) {
                drainClicks(mapping);
            }
        }
        return null;
    }

    private static void drainClicks(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // Physical gameplay clicks must not survive activation or an allowed collision.
        }
    }

    private static void setDownExactly(KeyMapping mapping, boolean down) {
        ((KeyMappingAccessor) mapping).frostyAutoFish$setDownExactly(down);
    }

    private static void resetLockedMouseInput(MouseHandlerAccessor accessor) {
        accessor.frostyAutoFish$getSmoothTurnX().reset();
        accessor.frostyAutoFish$getSmoothTurnY().reset();
        accessor.frostyAutoFish$setAccumulatedDX(0.0);
        accessor.frostyAutoFish$setAccumulatedDY(0.0);
        accessor.frostyAutoFish$setLeftPressed(false);
        accessor.frostyAutoFish$setMiddlePressed(false);
        accessor.frostyAutoFish$setRightPressed(false);
        accessor.frostyAutoFish$setActiveButton(null);
        accessor.frostyAutoFish$setFakeRightMouse(0);
        accessor.frostyAutoFish$setClickDepth(0);
        accessor.frostyAutoFish$setMousePressedTime(0.0);
    }

    private void clearBlockedPendingToggleRestores() {
        if (!ensureMappingsReady()) {
            return;
        }
        for (KeyMapping mapping : blockedMappings) {
            if (mapping instanceof ToggleKeyMapping toggleMapping
                    && InputLockPolicy.shouldSuppressToggleRestore(state.isActive(), true)) {
                ((ToggleKeyMappingAccessor) toggleMapping)
                        .frostyAutoFish$setReleasedByScreenWhenDown(false);
            }
        }
    }

    private boolean ensureMappingsReady() {
        if (mappingsReady) {
            return true;
        }
        if (minecraft.options == null) {
            return false;
        }
        blockedMappings = createBlockedMappings(minecraft);
        allowedMappings = List.of(
                highValueToggleKey,
                toggleKey,
                configKey,
                minecraft.options.keyChat,
                minecraft.options.keyCommand,
                minecraft.options.keyInventory,
                minecraft.options.keyAdvancements,
                minecraft.options.keySocialInteractions,
                minecraft.options.keyQuickActions
        );
        mappingsReady = true;
        if (state.isActive()) {
            clearStaleInput();
        }
        return true;
    }

    private static List<KeyMapping> createBlockedMappings(Minecraft minecraft) {
        List<KeyMapping> mappings = new ArrayList<>(Arrays.asList(
                minecraft.options.keyAttack,
                minecraft.options.keyUse,
                minecraft.options.keyUp,
                minecraft.options.keyDown,
                minecraft.options.keyLeft,
                minecraft.options.keyRight,
                minecraft.options.keyJump,
                minecraft.options.keyShift,
                minecraft.options.keySprint,
                minecraft.options.keyPickItem
        ));
        mappings.addAll(Arrays.asList(minecraft.options.keyHotbarSlots));
        return List.copyOf(mappings);
    }

    private record EventSnapshot(
            List<KeyMapping> mappings,
            boolean[] previousDown,
            boolean drainAfterward
    ) {
    }

    private record HotkeyClickSnapshot(
            boolean matches,
            InputLockPolicy.Phase phase,
            int clickCount
    ) {
    }
}
