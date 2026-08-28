package xyz.whatsyouss.frostyautofish;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import xyz.whatsyouss.frostyautofish.SettingsTranslations.Key;
import xyz.whatsyouss.frostyautofish.config.ConfigManager;
import xyz.whatsyouss.frostyautofish.config.TargetListEditor;
import xyz.whatsyouss.frostyautofish.config.TargetNameMatcher;

import java.util.List;
import java.util.function.Function;

public final class TargetManagementScreen extends Screen {
    private static final int INPUT_WIDTH = 220;
    private static final int ADD_WIDTH = 56;
    private static final int ROW_WIDTH = 320;
    private static final int REMOVE_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 20;
    private static final int FOOTER_WIDTH = 82;
    private static final int FOOTER_GAP = 6;

    private final ConfigScreen parent;
    private final ConfigManager configManager;
    private final TargetListKind kind;
    private EditBox nameInput;
    private String pendingName = "";
    private Component statusMessage;
    private int statusColor = 0xFFAAAAAA;
    private int page;

    public TargetManagementScreen(ConfigScreen parent, ConfigManager configManager) {
        this(parent, configManager, TargetListKind.PLAYER_MODEL);
    }

    public TargetManagementScreen(ConfigScreen parent, ConfigManager configManager, TargetListKind kind) {
        super(parent.component(kind.title));
        this.parent = parent;
        this.configManager = configManager;
        this.kind = kind;
        statusMessage = parent.component(kind.description);
    }

    @Override
    protected void init() {
        page = Math.min(page, maxPage());

        int inputY = 54;
        int inputX = width / 2 - (INPUT_WIDTH + ADD_WIDTH + FOOTER_GAP) / 2;
        nameInput = new EditBox(font, inputX, inputY, INPUT_WIDTH, BUTTON_HEIGHT, text(Key.TARGET_INPUT));
        nameInput.setMaxLength(TargetNameMatcher.MAX_RULE_LENGTH);
        nameInput.setValue(pendingName);
        nameInput.setResponder(value -> pendingName = value);
        addRenderableWidget(nameInput);
        addRenderableWidget(new Button.Builder(text(Key.ADD), button -> addTarget())
                .bounds(inputX + INPUT_WIDTH + FOOTER_GAP, inputY, ADD_WIDTH, BUTTON_HEIGHT)
                .build());

        addTargetRows();
        addFooterButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractMenuBackground(graphics);
        graphics.centeredText(font, title, width / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(font, statusMessage, width / 2, 36, statusColor);

        List<String> targets = targets();
        if (targets.isEmpty()) {
            graphics.centeredText(font, text(kind.emptyMessage), width / 2, firstRowY() + 6,
                    0xFFAAAAAA);
        } else {
            int start = page * rowsPerPage();
            int rows = Math.min(rowsPerPage(), targets.size() - start);
            int textX = width / 2 - rowWidth() / 2;
            for (int row = 0; row < rows; row++) {
                int targetIndex = start + row;
                graphics.text(font, Component.literal(rowLabel(targetIndex)), textX, rowY(row) + 6, 0xFFFFFFFF);
            }
        }

        graphics.centeredText(
                font,
                text(Key.PAGE_NUMBER, page + 1, maxPage() + 1),
                width / 2,
                footerY() - 14,
                0xFFAAAAAA
        );
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameInput != null
                && nameInput.isFocused()
                && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            addTarget();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        parent.setOwnedChildScreen(null);
        minecraft.setScreen(parent);
    }

    ConfigScreen parentScreen() {
        return parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addTargetRows() {
        List<String> targets = targets();
        int start = page * rowsPerPage();
        int rows = Math.min(rowsPerPage(), targets.size() - start);
        int removeX = width / 2 + rowWidth() / 2 - REMOVE_WIDTH;
        for (int row = 0; row < rows; row++) {
            String target = targets.get(start + row);
            addRenderableWidget(new Button.Builder(Component.literal("×"), button -> removeTarget(target))
                    .createNarration(ignored -> Component.literal(text(Key.REMOVE).getString() + ": " + target))
                    .bounds(removeX, rowY(row), REMOVE_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void addFooterButtons() {
        int footerY = footerY();
        int footerWidth = footerButtonWidth();
        int firstX = width / 2 - footerWidth * 2 - FOOTER_GAP * 3 / 2;

        Button previous = new Button.Builder(text(Key.PREVIOUS), button -> {
            page--;
            rebuildWidgets();
        }).bounds(firstX, footerY, footerWidth, BUTTON_HEIGHT).build();
        previous.active = page > 0;
        addRenderableWidget(previous);

        Button next = new Button.Builder(text(Key.NEXT), button -> {
            page++;
            rebuildWidgets();
        }).bounds(firstX + footerWidth + FOOTER_GAP, footerY, footerWidth, BUTTON_HEIGHT).build();
        next.active = page < maxPage();
        addRenderableWidget(next);

        Button clearAll = new Button.Builder(text(Key.CLEAR_ALL), button -> confirmClear())
                .bounds(firstX + (footerWidth + FOOTER_GAP) * 2, footerY, footerWidth, BUTTON_HEIGHT)
                .build();
        clearAll.active = !targets().isEmpty();
        addRenderableWidget(clearAll);

        addRenderableWidget(new Button.Builder(text(Key.BACK), button -> onClose())
                .bounds(firstX + (footerWidth + FOOTER_GAP) * 3, footerY, footerWidth, BUTTON_HEIGHT)
                .build());
    }

    private void addTarget() {
        TargetListEditor.Result result = TargetListEditor.add(targets(), nameInput.getValue());
        if (!result.success()) {
            setFailureStatus(result);
            return;
        }

        pendingName = "";
        saveAndSync(text(kind.addedMessage, result.normalizedName()));
    }

    private void removeTarget(String target) {
        TargetListEditor.Result result = TargetListEditor.remove(targets(), target);
        if (!result.success()) {
            setFailureStatus(result);
            return;
        }

        saveAndSync(text(kind.removedMessage, result.normalizedName()));
    }

    private void confirmClear() {
        BooleanConsumer callback = confirmed -> {
            parent.setOwnedChildScreen(this);
            minecraft.setScreen(this);
            if (confirmed) {
                clearTargets();
            }
        };
        ConfirmScreen confirm = new ConfirmScreen(
                callback,
                text(kind.confirmTitle),
                text(kind.confirmBody),
                text(Key.CLEAR_ALL),
                text(Key.CANCEL)
        );
        parent.setOwnedChildScreen(confirm);
        minecraft.setScreen(confirm);
    }

    private void clearTargets() {
        TargetListEditor.Result result = TargetListEditor.clear(targets());
        saveAndSync(text(kind.clearedMessage, result.count()));
    }

    private String rowLabel(int targetIndex) {
        String prefix = (targetIndex + 1) + ". ";
        String target = targets().get(targetIndex);
        int availableWidth = rowWidth() - REMOVE_WIDTH - FOOTER_GAP - font.width(prefix);
        if (font.width(target) <= availableWidth) {
            return prefix + target;
        }
        return prefix + font.plainSubstrByWidth(target, availableWidth - font.width("...")) + "...";
    }

    private void saveAndSync(Component message) {
        configManager.save();
        kind.sync(parent);
        statusMessage = message;
        statusColor = 0xFF55FF55;
        page = Math.min(page, maxPage());
        rebuildWidgets();
    }

    private void setFailureStatus(TargetListEditor.Result result) {
        Component message = switch (result.status()) {
            case EMPTY_NAME -> text(Key.ERROR_EMPTY);
            case NAME_TOO_LONG -> text(Key.ERROR_TOO_LONG, TargetNameMatcher.MAX_RULE_LENGTH);
            case DUPLICATE -> text(Key.ERROR_DUPLICATE, result.normalizedName());
            case NOT_FOUND -> text(Key.ERROR_NOT_FOUND, result.normalizedName());
            default -> text(Key.ERROR_UPDATE);
        };
        statusMessage = message;
        statusColor = 0xFFFF5555;
    }

    private List<String> targets() {
        return kind.targets(configManager);
    }

    private int rowsPerPage() {
        return Math.max(1, (footerY() - firstRowY() - 18) / ROW_GAP);
    }

    private int maxPage() {
        int size = targets().size();
        return Math.max(0, (size + rowsPerPage() - 1) / rowsPerPage() - 1);
    }

    private int firstRowY() {
        return 88;
    }

    private int rowY(int row) {
        return firstRowY() + row * ROW_GAP;
    }

    private int footerY() {
        return height - 34;
    }

    private int rowWidth() {
        return Math.min(ROW_WIDTH, Math.max(1, width - 16));
    }

    private int footerButtonWidth() {
        return Math.min(FOOTER_WIDTH, Math.max(1, (width - 16 - FOOTER_GAP * 3) / 4));
    }

    private Component text(Key key, Object... arguments) {
        return parent.component(key, arguments);
    }

    public enum TargetListKind {
        PLAYER_MODEL(
                Key.PLAYER_TARGETS_TITLE,
                Key.PLAYER_TARGETS_DESCRIPTION,
                Key.NO_PLAYER_TARGETS,
                Key.ADDED_PLAYER_TARGET,
                Key.REMOVED_PLAYER_TARGET,
                Key.CLEARED_PLAYER_TARGETS,
                Key.CONFIRM_PLAYER_TITLE,
                Key.CONFIRM_PLAYER_BODY,
                configManager -> configManager.config().namedPlayerTargets
        ) {
            @Override
            void sync(ConfigScreen parent) {
                parent.syncNamedPlayerTargetsFromLiveConfig();
            }
        },
        HIGH_VALUE(
                Key.HIGH_VALUE_TARGETS_TITLE,
                Key.HIGH_VALUE_TARGETS_MANAGE_DESCRIPTION,
                Key.NO_HIGH_VALUE_TARGETS,
                Key.ADDED_HIGH_VALUE_TARGET,
                Key.REMOVED_HIGH_VALUE_TARGET,
                Key.CLEARED_HIGH_VALUE_TARGETS,
                Key.CONFIRM_HIGH_VALUE_TITLE,
                Key.CONFIRM_HIGH_VALUE_BODY,
                configManager -> configManager.config().highValueTargets
        ) {
            @Override
            void sync(ConfigScreen parent) {
                parent.syncHighValueTargetsFromLiveConfig();
            }
        };

        private final Key title;
        private final Key description;
        private final Key emptyMessage;
        private final Key addedMessage;
        private final Key removedMessage;
        private final Key clearedMessage;
        private final Key confirmTitle;
        private final Key confirmBody;
        private final Function<ConfigManager, List<String>> targets;

        TargetListKind(
                Key title,
                Key description,
                Key emptyMessage,
                Key addedMessage,
                Key removedMessage,
                Key clearedMessage,
                Key confirmTitle,
                Key confirmBody,
                Function<ConfigManager, List<String>> targets
        ) {
            this.title = title;
            this.description = description;
            this.emptyMessage = emptyMessage;
            this.addedMessage = addedMessage;
            this.removedMessage = removedMessage;
            this.clearedMessage = clearedMessage;
            this.confirmTitle = confirmTitle;
            this.confirmBody = confirmBody;
            this.targets = targets;
        }

        private List<String> targets(ConfigManager configManager) {
            return targets.apply(configManager);
        }

        abstract void sync(ConfigScreen parent);
    }
}
