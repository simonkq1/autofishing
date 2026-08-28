package xyz.whatsyouss.frostyautofish;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import xyz.whatsyouss.frostyautofish.SettingsTranslations.Key;
import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;
import xyz.whatsyouss.frostyautofish.config.ConfigManager;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class ConfigScreen extends Screen {
    private static final int HEADER_HEIGHT = 46;
    private static final int FOOTER_HEIGHT = 36;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 8;

    private final Screen parent;
    private final ConfigManager configManager;
    private final AutoFishController controller;
    private final EnumMap<Page, Double> pageScroll = new EnumMap<>(Page.class);
    private final EnumMap<Page, String> pageFocusedControl = new EnumMap<>(Page.class);
    private final Map<String, AbstractWidget> controls = new java.util.HashMap<>();
    private final IdentityHashMap<AbstractWidget, String> controlKeys = new IdentityHashMap<>();
    private AutoFishConfig working;
    private Page selectedPage = Page.GENERAL;
    private AbstractScrollArea scrollArea;
    private String focusedControl = "language";
    private StringWidget stateWidget;
    private CycleButton<Boolean> highValueEnabledButton;
    private Screen ownedChildScreen;
    private int widgetGeneration;

    public ConfigScreen(Screen parent, ConfigManager configManager, AutoFishController controller) {
        super(Component.empty());
        this.parent = parent;
        this.configManager = configManager;
        this.controller = controller;
        this.working = configManager.config().copy();
        for (Page page : Page.values()) {
            pageScroll.put(page, 0.0);
        }
    }

    @Override
    public Component getTitle() {
        return component(Key.TITLE);
    }

    @Override
    protected void init() {
        widgetGeneration++;
        captureUiState();
        controls.clear();
        controlKeys.clear();
        scrollArea = null;
        highValueEnabledButton = null;

        SettingsLayoutPolicy.Dimensions dimensions = SettingsLayoutPolicy.forWidth(width);
        HeaderAndFooterLayout frame = new HeaderAndFooterLayout(this, HEADER_HEIGHT, FOOTER_HEIGHT);
        buildHeader(frame);
        buildContents(frame, dimensions);
        buildFooter(frame);
        frame.arrangeElements();
        frame.visitWidgets(this::addRenderableWidget);
    }

    @Override
    protected void setInitialFocus() {
        restoreUiState();
    }

    @Override
    protected void repositionElements() {
        captureUiState();
        super.repositionElements();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        captureUiState();
        AbstractScrollArea previousScrollArea = scrollArea;
        int previousGeneration = widgetGeneration;
        boolean handled = super.mouseClicked(event, doubleClick);
        if (minecraft.screen == this
                && (widgetGeneration != previousGeneration || scrollArea != previousScrollArea)) {
            setFocused(null);
            restoreUiState();
        }
        return handled;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        captureUiState();
        return super.keyPressed(event);
    }

    private void buildHeader(HeaderAndFooterLayout frame) {
        LinearLayout header = LinearLayout.vertical().spacing(2);
        header.addChild(new StringWidget(component(Key.TITLE), font),
                settings -> settings.alignHorizontallyCenter());
        stateWidget = new StringWidget(stateText(), font);
        header.addChild(stateWidget, settings -> settings.alignHorizontallyCenter());
        frame.addToHeader(header, settings -> settings.alignHorizontallyCenter().paddingTop(9));
    }

    private void buildContents(HeaderAndFooterLayout frame, SettingsLayoutPolicy.Dimensions dimensions) {
        GridLayout main = new GridLayout().columnSpacing(SettingsLayoutPolicy.COLUMN_GAP);
        main.addChild(buildNavigation(dimensions.navigationWidth()), 0, 0,
                settings -> settings.alignVerticallyTop());

        Layout page = selectedPage == Page.GENERAL
                ? buildGeneralPage(dimensions)
                : buildFishingPage(dimensions);
        ScrollableLayout scroll = new ScrollableLayout(minecraft, page, frame.getContentHeight());
        scroll.setMinWidth(dimensions.contentWidth());
        scroll.setMinHeight(frame.getContentHeight());
        scroll.setMaxHeight(frame.getContentHeight());
        main.addChild(scroll, 0, 1, settings -> settings.alignVerticallyTop());
        frame.addToContents(main, settings -> settings.alignHorizontallyCenter());

        scroll.visitWidgets(widget -> {
            if (widget instanceof AbstractScrollArea area) {
                scrollArea = area;
            }
        });
    }

    private Layout buildNavigation(int width) {
        LinearLayout navigation = LinearLayout.vertical().spacing(4);
        Button general = register("nav.general", new Button.Builder(component(Key.PAGE_GENERAL),
                button -> switchPage(Page.GENERAL)).size(width, BUTTON_HEIGHT).build());
        Button fishing = register("nav.fishing", new Button.Builder(component(Key.PAGE_FISHING),
                button -> switchPage(Page.FISHING)).size(width, BUTTON_HEIGHT).build());
        general.active = selectedPage != Page.GENERAL;
        fishing.active = selectedPage != Page.FISHING;
        navigation.addChild(general);
        navigation.addChild(fishing);
        return navigation;
    }

    private Layout buildGeneralPage(SettingsLayoutPolicy.Dimensions dimensions) {
        LinearLayout page = LinearLayout.vertical().spacing(ROW_GAP);
        page.addChild(groupHeading(Key.PAGE_GENERAL, dimensions.contentWidth()));
        CycleButton<AutoFishConfig.Language> language = CycleButton.builder(
                        value -> component(value == AutoFishConfig.Language.ENGLISH
                                ? Key.LANGUAGE_ENGLISH : Key.LANGUAGE_TRADITIONAL_CHINESE),
                        working.language)
                .withValues(AutoFishConfig.Language.values())
                .displayOnlyValue()
                .create(0, 0, dimensions.controlWidth(), BUTTON_HEIGHT, component(Key.LANGUAGE),
                        (button, value) -> changeLanguage(value));
        register("language", language);
        page.addChild(settingRow(dimensions, Key.LANGUAGE, Key.LANGUAGE_DESCRIPTION, language));
        return page;
    }

    private Layout buildFishingPage(SettingsLayoutPolicy.Dimensions d) {
        LinearLayout page = LinearLayout.vertical().spacing(ROW_GAP);

        page.addChild(groupHeading(Key.GROUP_FISHING, d.contentWidth()));
        page.addChild(settingRow(d, Key.AUTO_THROW, Key.AUTO_THROW_DESCRIPTION,
                booleanControl("autoThrow", Key.AUTO_THROW, working.autoThrow,
                        value -> working.autoThrow = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.BITE_DETECTION, Key.BITE_DETECTION_DESCRIPTION,
                enumControl("biteDetection", Key.BITE_DETECTION, working.biteDetection,
                        AutoFishConfig.BiteDetection.values(), this::biteDetectionText,
                        value -> working.biteDetection = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.DRY_TIMEOUT, Key.DRY_TIMEOUT_DESCRIPTION,
                intSlider("dryTimeout", Key.DRY_TIMEOUT, Key.UNIT_SECONDS,
                        working.dryTimeoutSeconds, 5, 60,
                        value -> working.dryTimeoutSeconds = value, d.controlWidth())));

        page.addChild(groupHeading(Key.GROUP_CONTROL_SAFETY, d.contentWidth()));
        page.addChild(settingRow(d, Key.ANTI_AFK, Key.ANTI_AFK_DESCRIPTION,
                booleanControl("antiAfk", Key.ANTI_AFK, working.antiAfk,
                        value -> working.antiAfk = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.BACKGROUND_RUN, Key.BACKGROUND_RUN_DESCRIPTION,
                booleanControl("backgroundRun", Key.BACKGROUND_RUN, working.backgroundRun,
                        value -> working.backgroundRun = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.LOCK_CONTROLS, Key.LOCK_CONTROLS_DESCRIPTION,
                booleanControl("lockControls", Key.LOCK_CONTROLS, working.lockControls,
                        value -> working.lockControls = value, d.controlWidth())));

        page.addChild(groupHeading(Key.GROUP_AUTO_KILL, d.contentWidth()));
        page.addChild(settingRow(d, Key.AUTO_KILL, Key.AUTO_KILL_DESCRIPTION,
                booleanControl("autoKill", Key.AUTO_KILL, working.autoKill,
                        value -> working.autoKill = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.TRIGGER_AMOUNT, Key.TRIGGER_AMOUNT_DESCRIPTION,
                intSlider("triggerAmount", Key.TRIGGER_AMOUNT, null, working.triggerAmount, 1, 15,
                        value -> working.triggerAmount = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.TARGETS, Key.TARGETS_DESCRIPTION,
                manageButton("targets", TargetManagementScreen.TargetListKind.PLAYER_MODEL, d.controlWidth())));
        page.addChild(settingRow(d, Key.WEAPON_SLOT, Key.WEAPON_SLOT_DESCRIPTION,
                intSlider("weaponSlot", Key.WEAPON_SLOT, null, working.weaponSlot, 1, 9,
                        value -> working.weaponSlot = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.USE_ABILITY, Key.USE_ABILITY_DESCRIPTION,
                booleanControl("useAbility", Key.USE_ABILITY, working.useAbility,
                        value -> working.useAbility = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.ABILITY_AIM, Key.ABILITY_AIM_DESCRIPTION,
                enumControl("abilityAim", Key.ABILITY_AIM, working.abilityAim,
                        AutoFishConfig.AbilityAim.values(), this::abilityAimText,
                        value -> working.abilityAim = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.ABILITY_DELAY, Key.ABILITY_DELAY_DESCRIPTION,
                intSlider("abilityDelay", Key.ABILITY_DELAY, Key.UNIT_MILLISECONDS,
                        working.abilityDelayMillis, 50, 1000,
                        value -> working.abilityDelayMillis = value, d.controlWidth())));

        page.addChild(groupHeading(Key.GROUP_HIGH_VALUE, d.contentWidth()));
        highValueEnabledButton = booleanControl("highValueEnabled", Key.HIGH_VALUE_ENABLED,
                working.highValueEnabled, this::setHighValueEnabled, d.controlWidth());
        page.addChild(settingRow(d, Key.HIGH_VALUE_ENABLED, Key.HIGH_VALUE_ENABLED_DESCRIPTION,
                highValueEnabledButton));
        page.addChild(settingRow(d, Key.HIGH_VALUE_TARGETS, Key.HIGH_VALUE_TARGETS_DESCRIPTION,
                manageButton("highValueTargets", TargetManagementScreen.TargetListKind.HIGH_VALUE, d.controlWidth())));
        page.addChild(settingRow(d, Key.HIGH_VALUE_BOXES, Key.HIGH_VALUE_BOXES_DESCRIPTION,
                booleanControl("highValueBoxes", Key.HIGH_VALUE_BOXES, working.showHighValueCollision,
                        value -> working.showHighValueCollision = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.HIGH_VALUE_HUD, Key.HIGH_VALUE_HUD_DESCRIPTION,
                booleanControl("highValueHud", Key.HIGH_VALUE_HUD, working.showHighValueHud,
                        value -> working.showHighValueHud = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.HIGH_VALUE_ATTACK, Key.HIGH_VALUE_ATTACK_DESCRIPTION,
                booleanControl("highValueAttack", Key.HIGH_VALUE_ATTACK, working.autoAttackHighValue,
                        value -> working.autoAttackHighValue = value, d.controlWidth())));
        page.addChild(settingRow(d, Key.HIGH_VALUE_HITS, Key.HIGH_VALUE_HITS_DESCRIPTION,
                intSlider("highValueHits", Key.HIGH_VALUE_HITS, null, working.highValueAttackCount, 1, 10,
                        value -> working.highValueAttackCount = value, d.controlWidth())));
        return page;
    }

    private Layout settingRow(SettingsLayoutPolicy.Dimensions d, Key name, Key description, AbstractWidget control) {
        LinearLayout text = LinearLayout.vertical().spacing(2);
        text.addChild(new StringWidget(d.textWidth(), 11, component(name), font));
        text.addChild(new MultiLineTextWidget(component(description), font).setMaxWidth(d.textWidth()));
        if (d.stacked()) {
            LinearLayout row = LinearLayout.vertical().spacing(4);
            row.addChild(text);
            row.addChild(control);
            return row;
        }
        GridLayout row = new GridLayout().columnSpacing(SettingsLayoutPolicy.COLUMN_GAP);
        row.addChild(text, 0, 0, settings -> settings.alignVerticallyTop());
        row.addChild(control, 0, 1, settings -> settings.alignVerticallyMiddle());
        return row;
    }

    private StringWidget groupHeading(Key key, int width) {
        return new StringWidget(width, 16, component(key), font);
    }

    private CycleButton<Boolean> booleanControl(
            String id, Key name, boolean value, BooleanConsumer consumer, int width
    ) {
        CycleButton<Boolean> button = CycleButton.builder(
                        enabled -> component(enabled ? Key.ON : Key.OFF), value)
                .withValues(Boolean.TRUE, Boolean.FALSE)
                .displayOnlyValue()
                .create(0, 0, width, BUTTON_HEIGHT, component(name),
                        (cycle, enabled) -> consumer.accept(enabled));
        return register(id, button);
    }

    private <T> CycleButton<T> enumControl(
            String id, Key name, T current, T[] values,
            java.util.function.Function<T, Component> stringifier,
            java.util.function.Consumer<T> consumer, int width
    ) {
        CycleButton<T> button = CycleButton.builder(stringifier, current)
                .withValues(values)
                .displayOnlyValue()
                .create(0, 0, width, BUTTON_HEIGHT, component(name),
                        (cycle, value) -> consumer.accept(value));
        return register(id, button);
    }

    private IntSlider intSlider(
            String id, Key name, Key suffix, int current, int minimum, int maximum,
            IntConsumer consumer, int width
    ) {
        return register(id, new IntSlider(
                width, component(name), suffix == null ? "" : text(suffix), current, minimum, maximum, consumer
        ));
    }

    private Button manageButton(String id, TargetManagementScreen.TargetListKind kind, int width) {
        Key settingName = kind == TargetManagementScreen.TargetListKind.PLAYER_MODEL
                ? Key.TARGETS : Key.HIGH_VALUE_TARGETS;
        return register(id, new Button.Builder(component(Key.MANAGE), button -> openTargetManager(kind))
                .createNarration(ignored -> Component.literal(
                        component(settingName).getString() + ": " + component(Key.MANAGE).getString()))
                .size(width, BUTTON_HEIGHT).build());
    }

    private void buildFooter(HeaderAndFooterLayout frame) {
        LinearLayout footer = LinearLayout.horizontal().spacing(SettingsLayoutPolicy.COLUMN_GAP);
        footer.addChild(register("reset", new Button.Builder(component(Key.RESET), button -> resetSettings())
                .size(82, BUTTON_HEIGHT).build()));
        footer.addChild(register("done", new Button.Builder(component(Key.DONE), button -> onClose())
                .size(82, BUTTON_HEIGHT).build()));
        frame.addToFooter(footer, settings -> settings.alignHorizontallyCenter().paddingBottom(8));
    }

    @Override
    public void tick() {
        if (stateWidget != null) {
            stateWidget.setMessage(stateText());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractMenuBackground(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        working.highValueEnabled = configManager.config().highValueEnabled;
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

    void syncHighValueTargetsFromLiveConfig() {
        working.highValueTargets = configManager.config().copy().highValueTargets;
    }

    void syncHighValueEnabledFromLiveConfig(boolean visible) {
        working.highValueEnabled = configManager.config().highValueEnabled;
        if (visible && highValueEnabledButton != null) {
            highValueEnabledButton.setValue(working.highValueEnabled);
        }
    }

    AutoFishConfig.Language settingsLanguage() {
        return working.language;
    }

    boolean ownsScreen(Screen screen) {
        return screen != null && screen == ownedChildScreen;
    }

    void setOwnedChildScreen(Screen screen) {
        ownedChildScreen = screen;
    }

    private void setHighValueEnabled(boolean enabled) {
        working.highValueEnabled = enabled;
        configManager.setHighValueEnabled(enabled);
    }

    private void switchPage(Page page) {
        if (page == selectedPage) {
            return;
        }
        captureUiState();
        selectedPage = page;
        focusedControl = pageFocusedControl.get(page);
        discardUiReferences();
        rebuildWidgets();
    }

    private void changeLanguage(AutoFishConfig.Language language) {
        if (working.language == language) {
            return;
        }
        captureUiState();
        working.language = language;
        focusedControl = "language";
        discardUiReferences();
        rebuildWidgets();
    }

    private void resetSettings() {
        working = new AutoFishConfig();
        configManager.config().namedPlayerTargets.clear();
        configManager.config().highValueTargets.clear();
        configManager.setHighValueEnabled(working.highValueEnabled);
        selectedPage = Page.GENERAL;
        pageScroll.replaceAll((page, ignored) -> 0.0);
        pageFocusedControl.clear();
        focusedControl = "language";
        discardUiReferences();
        rebuildWidgets();
    }

    private void openTargetManager(TargetManagementScreen.TargetListKind kind) {
        captureUiState();
        TargetManagementScreen targets = new TargetManagementScreen(this, configManager, kind);
        ownedChildScreen = targets;
        minecraft.setScreen(targets);
    }

    private void captureUiState() {
        if (scrollArea != null) {
            pageScroll.put(selectedPage, scrollArea.scrollAmount());
        }
        for (Map.Entry<AbstractWidget, String> entry : controlKeys.entrySet()) {
            if (entry.getKey().isFocused()) {
                focusedControl = entry.getValue();
                if (scrollArea instanceof AbstractContainerWidget container
                        && container.children().contains(entry.getKey())) {
                    pageFocusedControl.put(selectedPage, focusedControl);
                }
                break;
            }
        }
    }

    private void restoreUiState() {
        if (scrollArea != null) {
            scrollArea.setScrollAmount(pageScroll.getOrDefault(selectedPage, 0.0));
        }
        AbstractWidget focused = controls.get(focusedControl);
        if (focused == null) {
            focusedControl = pageFocusedControl.get(selectedPage);
            focused = controls.get(focusedControl);
        }
        if (focused == null && pageScroll.getOrDefault(selectedPage, 0.0) <= 0.0) {
            focusedControl = selectedPage == Page.GENERAL ? "language" : "autoThrow";
            focused = controls.get(focusedControl);
        }
        if (focused != null && scrollArea instanceof AbstractContainerWidget container
                && container.children().contains(focused)) {
            setFocused(scrollArea);
            container.setFocused(focused);
        } else if (focused != null) {
            setInitialFocus(focused);
        }
    }

    private <T extends AbstractWidget> T register(String key, T widget) {
        controls.put(key, widget);
        controlKeys.put(widget, key);
        return widget;
    }

    private void discardUiReferences() {
        scrollArea = null;
        controls.clear();
        controlKeys.clear();
    }

    private Component stateText() {
        return Component.literal(text(Key.CURRENT_STATE,
                SettingsTranslations.state(working.language, controller.stateName())));
    }

    private Component biteDetectionText(AutoFishConfig.BiteDetection value) {
        return component(switch (value) {
            case BOTH -> Key.BITE_BOTH;
            case HYPIXEL -> Key.BITE_HYPIXEL;
            case VANILLA -> Key.BITE_VANILLA;
        });
    }

    private Component abilityAimText(AutoFishConfig.AbilityAim value) {
        return component(value == AutoFishConfig.AbilityAim.MOB ? Key.AIM_MOB : Key.AIM_DOWN);
    }

    Component component(Key key, Object... arguments) {
        return Component.literal(text(key, arguments));
    }

    String text(Key key, Object... arguments) {
        return SettingsTranslations.text(working.language, key, arguments);
    }

    private enum Page {
        GENERAL,
        FISHING
    }

    private final class IntSlider extends AbstractSliderButton {
        private final Component narrationName;
        private final String suffix;
        private final int minimum;
        private final int maximum;
        private final IntConsumer consumer;

        private IntSlider(
                int width, Component narrationName, String suffix, int current,
                int minimum, int maximum, IntConsumer consumer
        ) {
            super(0, 0, width, BUTTON_HEIGHT, Component.empty(),
                    (current - minimum) / (double) (maximum - minimum));
            this.narrationName = narrationName;
            this.suffix = suffix;
            this.minimum = minimum;
            this.maximum = maximum;
            this.consumer = consumer;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(currentValue() + suffix));
        }

        @Override
        protected void applyValue() {
            consumer.accept(currentValue());
            updateMessage();
        }

        @Override
        protected MutableComponent createNarrationMessage() {
            return Component.literal(narrationName.getString() + ": " + currentValue() + suffix);
        }

        private int currentValue() {
            return minimum + (int) Math.round(value * (maximum - minimum));
        }
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int value);
    }

    @FunctionalInterface
    private interface BooleanConsumer {
        void accept(boolean value);
    }
}
