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
        super(Component.literal(kind.title));
        this.parent = parent;
        this.configManager = configManager;
        this.kind = kind;
        statusMessage = Component.literal(kind.description);
    }

    @Override
    protected void init() {
        page = Math.min(page, maxPage());

        int inputY = 54;
        int inputX = width / 2 - (INPUT_WIDTH + ADD_WIDTH + FOOTER_GAP) / 2;
        nameInput = new EditBox(font, inputX, inputY, INPUT_WIDTH, BUTTON_HEIGHT, Component.literal("Target Name"));
        nameInput.setMaxLength(TargetNameMatcher.MAX_RULE_LENGTH);
        nameInput.setValue(pendingName);
        nameInput.setResponder(value -> pendingName = value);
        addRenderableWidget(nameInput);
        addRenderableWidget(new Button.Builder(Component.literal("Add"), button -> addTarget())
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
            graphics.centeredText(font, Component.literal("No " + kind.itemPlural), width / 2, firstRowY() + 6,
                    0xFFAAAAAA);
        } else {
            int start = page * rowsPerPage();
            int rows = Math.min(rowsPerPage(), targets.size() - start);
            int textX = width / 2 - ROW_WIDTH / 2;
            for (int row = 0; row < rows; row++) {
                int targetIndex = start + row;
                graphics.text(font, Component.literal(rowLabel(targetIndex)), textX, rowY(row) + 6, 0xFFFFFFFF);
            }
        }

        graphics.centeredText(
                font,
                Component.literal("Page " + (page + 1) + " / " + (maxPage() + 1)),
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
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addTargetRows() {
        List<String> targets = targets();
        int start = page * rowsPerPage();
        int rows = Math.min(rowsPerPage(), targets.size() - start);
        int removeX = width / 2 + ROW_WIDTH / 2 - REMOVE_WIDTH;
        for (int row = 0; row < rows; row++) {
            String target = targets.get(start + row);
            addRenderableWidget(new Button.Builder(Component.literal("X"), button -> removeTarget(target))
                    .bounds(removeX, rowY(row), REMOVE_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void addFooterButtons() {
        int footerY = footerY();
        int firstX = width / 2 - FOOTER_WIDTH * 2 - FOOTER_GAP * 3 / 2;

        Button previous = new Button.Builder(Component.literal("Previous"), button -> {
            page--;
            rebuildWidgets();
        }).bounds(firstX, footerY, FOOTER_WIDTH, BUTTON_HEIGHT).build();
        previous.active = page > 0;
        addRenderableWidget(previous);

        Button next = new Button.Builder(Component.literal("Next"), button -> {
            page++;
            rebuildWidgets();
        }).bounds(firstX + FOOTER_WIDTH + FOOTER_GAP, footerY, FOOTER_WIDTH, BUTTON_HEIGHT).build();
        next.active = page < maxPage();
        addRenderableWidget(next);

        Button clearAll = new Button.Builder(Component.literal("Clear All"), button -> confirmClear())
                .bounds(firstX + (FOOTER_WIDTH + FOOTER_GAP) * 2, footerY, FOOTER_WIDTH, BUTTON_HEIGHT)
                .build();
        clearAll.active = !targets().isEmpty();
        addRenderableWidget(clearAll);

        addRenderableWidget(new Button.Builder(Component.literal("Back"), button -> onClose())
                .bounds(firstX + (FOOTER_WIDTH + FOOTER_GAP) * 3, footerY, FOOTER_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void addTarget() {
        TargetListEditor.Result result = TargetListEditor.add(targets(), nameInput.getValue());
        if (!result.success()) {
            setFailureStatus(result);
            return;
        }

        pendingName = "";
        saveAndSync("Added " + kind.itemSingular + ": " + result.normalizedName());
    }

    private void removeTarget(String target) {
        TargetListEditor.Result result = TargetListEditor.remove(targets(), target);
        if (!result.success()) {
            setFailureStatus(result);
            return;
        }

        saveAndSync("Removed " + kind.itemSingular + ": " + result.normalizedName());
    }

    private void confirmClear() {
        BooleanConsumer callback = confirmed -> {
            minecraft.setScreen(this);
            if (confirmed) {
                clearTargets();
            }
        };
        minecraft.setScreen(new ConfirmScreen(
                callback,
                Component.literal("Clear All " + kind.confirmTitle + "?"),
                Component.literal("Remove every " + kind.itemSingular + " from the list?"),
                Component.literal("Clear All"),
                Component.literal("Cancel")
        ));
    }

    private void clearTargets() {
        TargetListEditor.Result result = TargetListEditor.clear(targets());
        saveAndSync("Cleared " + result.count() + " " + kind.itemSingular + "(s)");
    }

    private String rowLabel(int targetIndex) {
        String prefix = (targetIndex + 1) + ". ";
        String target = targets().get(targetIndex);
        int availableWidth = ROW_WIDTH - REMOVE_WIDTH - FOOTER_GAP - font.width(prefix);
        if (font.width(target) <= availableWidth) {
            return prefix + target;
        }
        return prefix + font.plainSubstrByWidth(target, availableWidth - font.width("...")) + "...";
    }

    private void saveAndSync(String message) {
        configManager.save();
        kind.sync(parent);
        statusMessage = Component.literal(message);
        statusColor = 0xFF55FF55;
        page = Math.min(page, maxPage());
        rebuildWidgets();
    }

    private void setFailureStatus(TargetListEditor.Result result) {
        String message = switch (result.status()) {
            case EMPTY_NAME -> "Enter a target name.";
            case NAME_TOO_LONG -> "Name must contain 1-" + TargetNameMatcher.MAX_RULE_LENGTH + " characters.";
            case DUPLICATE -> "Target already exists: " + result.normalizedName();
            case NOT_FOUND -> "Target not found: " + result.normalizedName();
            default -> "Could not update target list.";
        };
        statusMessage = Component.literal(message);
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

    public enum TargetListKind {
        PLAYER_MODEL(
                "Player-Model Targets",
                "Add player-model target names used by Auto Kill.",
                "player-model target",
                "player-model targets",
                "Targets",
                configManager -> configManager.config().namedPlayerTargets
        ) {
            @Override
            void sync(ConfigScreen parent) {
                parent.syncNamedPlayerTargetsFromLiveConfig();
            }
        },
        HIGH_VALUE(
                "High Value Targets",
                "Add external high-value player-model names to track.",
                "high-value target",
                "high-value targets",
                "High Value Targets",
                configManager -> configManager.config().highValueTargets
        ) {
            @Override
            void sync(ConfigScreen parent) {
                parent.syncHighValueTargetsFromLiveConfig();
            }
        };

        private final String title;
        private final String description;
        private final String itemSingular;
        private final String itemPlural;
        private final String confirmTitle;
        private final Function<ConfigManager, List<String>> targets;

        TargetListKind(
                String title,
                String description,
                String itemSingular,
                String itemPlural,
                String confirmTitle,
                Function<ConfigManager, List<String>> targets
        ) {
            this.title = title;
            this.description = description;
            this.itemSingular = itemSingular;
            this.itemPlural = itemPlural;
            this.confirmTitle = confirmTitle;
            this.targets = targets;
        }

        private List<String> targets(ConfigManager configManager) {
            return targets.apply(configManager);
        }

        abstract void sync(ConfigScreen parent);
    }
}
