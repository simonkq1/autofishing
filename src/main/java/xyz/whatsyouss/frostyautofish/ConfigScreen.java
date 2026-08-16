package xyz.whatsyouss.frostyautofish;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;
import xyz.whatsyouss.frostyautofish.config.ConfigManager;

public final class ConfigScreen extends Screen {
    private static final int BUTTON_WIDTH = 160;
    private static final int FOOTER_BUTTON_WIDTH = 104;
    private static final int BUTTON_HEIGHT = 20;
    private static final int COLUMN_GAP = 8;
    private static final int ROW_GAP = 24;

    private final Screen parent;
    private final ConfigManager configManager;
    private final AutoFishController controller;
    private AutoFishConfig working;

    public ConfigScreen(Screen parent, ConfigManager configManager, AutoFishController controller) {
        super(Component.literal("Frosty AutoFish Settings"));
        this.parent = parent;
        this.configManager = configManager;
        this.controller = controller;
        this.working = configManager.config().copy();
    }

    @Override
    protected void init() {
        int left = width / 2 - BUTTON_WIDTH - COLUMN_GAP / 2;
        int right = width / 2 + COLUMN_GAP / 2;
        int y = height / 2 - 92;

        addRenderableWidget(CycleButton.onOffBuilder(working.autoThrow)
                .create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Auto Throw"),
                        (button, value) -> working.autoThrow = value));
        addRenderableWidget(CycleButton.onOffBuilder(working.antiAfk)
                .create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Anti AFK"),
                        (button, value) -> working.antiAfk = value));

        y += ROW_GAP;
        addRenderableWidget(CycleButton.onOffBuilder(working.backgroundRun)
                .create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Background Run"),
                        (button, value) -> working.backgroundRun = value));
        addRenderableWidget(CycleButton.onOffBuilder(working.lockControls)
                .create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Lock Controls"),
                        (button, value) -> working.lockControls = value));

        y += ROW_GAP;
        addRenderableWidget(new IntSlider(
                left, y, "Dry Timeout", " s", working.dryTimeoutSeconds, 5, 60,
                value -> working.dryTimeoutSeconds = value
        ));
        addRenderableWidget(CycleButton.onOffBuilder(working.autoKill)
                .create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Auto Kill"),
                        (button, value) -> working.autoKill = value));

        y += ROW_GAP;
        addRenderableWidget(new IntSlider(
                left, y, "Trigger Amount", "", working.triggerAmount, 1, 15,
                value -> working.triggerAmount = value
        ));
        addRenderableWidget(CycleButton.onOffBuilder(working.useAbility)
                .create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Use Ability"),
                        (button, value) -> working.useAbility = value));

        y += ROW_GAP;
        addRenderableWidget(CycleButton.builder(
                        value -> Component.literal(value.label()),
                        working.abilityAim
                )
                .withValues(AutoFishConfig.AbilityAim.values())
                .create(left, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.literal("Ability Aim"),
                        (button, value) -> working.abilityAim = value));
        addRenderableWidget(new IntSlider(
                right, y, "Ability Delay", " ms", working.abilityDelayMillis, 50, 1000,
                value -> working.abilityDelayMillis = value
        ));

        y += ROW_GAP;
        addRenderableWidget(new IntSlider(
                left, y, "Weapon Slot", "", working.weaponSlot, 1, 9,
                value -> working.weaponSlot = value
        ));
        addRenderableWidget(CycleButton.builder(
                        value -> Component.literal(value.label()),
                        working.biteDetection
                )
                .withValues(AutoFishConfig.BiteDetection.values())
                .create(right, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                        Component.literal("Bite Detection"),
                        (button, value) -> working.biteDetection = value));

        y += ROW_GAP + 8;
        int footerLeft = width / 2 - FOOTER_BUTTON_WIDTH * 3 / 2 - COLUMN_GAP;
        int footerMiddle = width / 2 - FOOTER_BUTTON_WIDTH / 2;
        int footerRight = width / 2 + FOOTER_BUTTON_WIDTH / 2 + COLUMN_GAP;
        addRenderableWidget(new Button.Builder(Component.literal("Reset Defaults"), button -> {
            working = new AutoFishConfig();
            rebuildWidgets();
        }).bounds(footerLeft, y, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(new Button.Builder(Component.literal("Manage Targets"), button ->
                minecraft.setScreen(new TargetManagementScreen(this, configManager)))
                .bounds(footerMiddle, y, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(new Button.Builder(Component.literal("Done"), button -> onClose())
                .bounds(footerRight, y, FOOTER_BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractMenuBackground(graphics);
        graphics.centeredText(font, title, width / 2, height / 2 - 124, 0xFFFFFFFF);
        graphics.centeredText(
                font,
                Component.literal("Current state: " + controller.stateName()),
                width / 2,
                height / 2 - 108,
                controller.isEnabled() ? 0xFF55FFFF : 0xFFAAAAAA
        );
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        configManager.config().copyFrom(working);
        controller.syncInputLock();
        configManager.save();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    void syncNamedPlayerTargetsFromLiveConfig() {
        working.namedPlayerTargets = configManager.config().copy().namedPlayerTargets;
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final String label;
        private final String suffix;
        private final int minimum;
        private final int maximum;
        private final IntConsumer consumer;

        private IntSlider(
                int x,
                int y,
                String label,
                String suffix,
                int current,
                int minimum,
                int maximum,
                IntConsumer consumer
        ) {
            super(
                    x,
                    y,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    Component.empty(),
                    (current - minimum) / (double) (maximum - minimum)
            );
            this.label = label;
            this.suffix = suffix;
            this.minimum = minimum;
            this.maximum = maximum;
            this.consumer = consumer;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + currentValue() + suffix));
        }

        @Override
        protected void applyValue() {
            consumer.accept(currentValue());
            updateMessage();
        }

        private int currentValue() {
            return minimum + (int) Math.round(value * (maximum - minimum));
        }
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int value);
    }
}
