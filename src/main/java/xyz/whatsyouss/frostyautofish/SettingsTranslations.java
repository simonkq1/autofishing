package xyz.whatsyouss.frostyautofish;

import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;

import java.util.EnumMap;
import java.util.Map;

final class SettingsTranslations {
    private static final Map<Key, String> ENGLISH = entries(
            "Frosty AutoFish Settings", "Current state: {0}", "General", "Fishing",
            "Language", "Select the language used by Frosty AutoFish settings.",
            "English", "中文",
            "Fishing", "Control & Safety", "Auto Kill", "High Value",
            "Auto Throw", "Automatically cast again when the fishing hook is missing.",
            "Bite Detection", "Choose which bite signals can trigger an automatic reel.",
            "Dry Timeout", "Recast when no bite is detected within this time.",
            "Anti AFK", "Periodically adjust the view while the macro is active.",
            "Background Run", "Keep the game running while its window is not focused.",
            "Lock Controls", "Block physical gameplay controls while the macro is active.",
            "Auto Kill", "Attack captured player-model fishing creatures.",
            "Trigger Amount", "Start Auto Kill after this many creatures are collected.",
            "Targets", "Manage player-model names recognized by Auto Kill.",
            "Weapon Slot", "Select the hotbar slot used for Auto Kill attacks.",
            "Use Ability", "Use the selected weapon's ability before attacking.",
            "Ability Aim", "Choose where the camera aims while using the ability.",
            "Ability Delay", "Wait this long after using the ability before attacking.",
            "High Value Enabled", "Enable passive High Value tracking and display.",
            "High Value Targets", "Manage external player-model names tracked as High Value.",
            "High Value Boxes", "Draw collision boxes around tracked High Value targets.",
            "High Value HUD", "Show the nearest High Value target in the HUD.",
            "High Value Attack", "Allow automatic attacks on High Value targets while the macro is active.",
            "High Value Hits", "Set the number of attacks used on each High Value target.",
            "Manage…", "Reset", "Done", "On", "Off", "Hypixel + Vanilla", "Hypixel only",
            "Vanilla only", "Mob", "Down", " seconds", " ms",
            "Player-Model Targets", "Add player-model target names used by Auto Kill.",
            "High Value Targets", "Add external high-value player-model names to track.",
            "Target name", "Add", "No player-model targets", "No high-value targets",
            "Page {0} / {1}", "Previous", "Next", "Clear All", "Back", "Cancel", "Remove",
            "Added player-model target: {0}", "Added high-value target: {0}",
            "Removed player-model target: {0}", "Removed high-value target: {0}",
            "Cleared {0} player-model target(s)", "Cleared {0} high-value target(s)",
            "Enter a target name.", "Name must contain 1-{0} characters.",
            "Target already exists: {0}", "Target not found: {0}", "Could not update target list.",
            "Clear All Targets?", "Clear All High Value Targets?",
            "Remove every player-model target from the list?", "Remove every high-value target from the list?",
            "Disabled", "Ready", "Casting", "Waiting", "Clearing hooked entity", "Bite detected",
            "Collecting creatures", "Using ability", "Chasing creatures", "Returning", "Restoring view"
    );

    private static final Map<Key, String> TRADITIONAL_CHINESE = entries(
            "Frosty AutoFish 設定", "目前狀態：{0}", "一般", "釣魚",
            "語言", "選擇 Frosty AutoFish 設定介面使用的語言。",
            "English", "中文",
            "釣魚", "控制與安全", "自動擊殺", "高價值目標",
            "自動拋竿", "魚鉤消失時自動重新拋竿。",
            "咬鉤偵測", "選擇可觸發自動收竿的咬鉤訊號。",
            "乾竿逾時", "在指定時間內未偵測到咬鉤時重新拋竿。",
            "防止掛機", "腳本啟用時定期調整視角。",
            "背景執行", "遊戲視窗失去焦點時仍持續執行。",
            "鎖定操作", "腳本啟用時阻擋實體遊戲操作。",
            "自動擊殺", "攻擊釣起的玩家模型生物。",
            "觸發數量", "收集到指定數量的生物後啟動自動擊殺。",
            "目標名單", "管理自動擊殺辨識的玩家模型名稱。",
            "武器欄位", "選擇自動擊殺攻擊時使用的快捷欄。",
            "使用技能", "攻擊前使用所選武器的技能。",
            "技能瞄準", "選擇使用技能時鏡頭瞄準的位置。",
            "技能延遲", "使用技能後等待指定時間再攻擊。",
            "啟用高價值功能", "啟用高價值目標的被動追蹤與顯示。",
            "高價值目標名單", "管理要追蹤為高價值目標的外部玩家模型名稱。",
            "高價值碰撞格", "在追蹤中的高價值目標周圍繪製碰撞格。",
            "高價值 HUD", "在 HUD 顯示最近的高價值目標。",
            "攻擊高價值目標", "腳本啟用時允許自動攻擊高價值目標。",
            "高價值攻擊次數", "設定每個高價值目標的攻擊次數。",
            "管理…", "重設", "完成", "開啟", "關閉", "Hypixel + 原版", "僅 Hypixel",
            "僅原版", "生物", "向下", " 秒", " 毫秒",
            "玩家模型目標", "新增自動擊殺使用的玩家模型目標名稱。",
            "高價值目標", "新增要追蹤的外部高價值玩家模型名稱。",
            "目標名稱", "新增", "沒有玩家模型目標", "沒有高價值目標",
            "第 {0} / {1} 頁", "上一頁", "下一頁", "全部清除", "返回", "取消", "移除",
            "已新增玩家模型目標：{0}", "已新增高價值目標：{0}",
            "已移除玩家模型目標：{0}", "已移除高價值目標：{0}",
            "已清除 {0} 個玩家模型目標", "已清除 {0} 個高價值目標",
            "請輸入目標名稱。", "名稱長度必須為 1 至 {0} 個字元。",
            "目標已存在：{0}", "找不到目標：{0}", "無法更新目標名單。",
            "清除所有目標？", "清除所有高價值目標？",
            "要從名單中移除所有玩家模型目標嗎？", "要從名單中移除所有高價值目標嗎？",
            "已停用", "準備拋竿", "拋竿中", "等待咬鉤", "清除上鉤實體", "偵測到咬鉤",
            "收集生物", "使用技能", "追蹤生物", "返回原位", "恢復視角"
    );

    private SettingsTranslations() {
    }

    static String text(AutoFishConfig.Language language, Key key, Object... arguments) {
        String value = dictionary(language).get(key);
        for (int index = 0; index < arguments.length; index++) {
            value = value.replace("{" + index + "}", String.valueOf(arguments[index]));
        }
        return value;
    }

    static String state(AutoFishConfig.Language language, String englishState) {
        Key key = switch (englishState) {
            case "Disabled" -> Key.STATE_DISABLED;
            case "Ready" -> Key.STATE_READY;
            case "Casting" -> Key.STATE_CASTING;
            case "Waiting" -> Key.STATE_WAITING;
            case "Clearing hooked entity" -> Key.STATE_CLEARING;
            case "Bite detected" -> Key.STATE_BITE;
            case "Collecting creatures" -> Key.STATE_COLLECTING;
            case "Using ability" -> Key.STATE_ABILITY;
            case "Chasing creatures" -> Key.STATE_CHASING;
            case "Returning" -> Key.STATE_RETURNING;
            case "Restoring view" -> Key.STATE_RESTORING;
            default -> null;
        };
        return key == null ? englishState : text(language, key);
    }

    static Map<Key, String> dictionary(AutoFishConfig.Language language) {
        return language == AutoFishConfig.Language.TRADITIONAL_CHINESE ? TRADITIONAL_CHINESE : ENGLISH;
    }

    private static Map<Key, String> entries(String... values) {
        Key[] keys = Key.values();
        if (values.length != keys.length) {
            throw new IllegalStateException("Translation entry count " + values.length + " != " + keys.length);
        }
        EnumMap<Key, String> result = new EnumMap<>(Key.class);
        for (int index = 0; index < keys.length; index++) {
            result.put(keys[index], values[index]);
        }
        return Map.copyOf(result);
    }

    enum Key {
        TITLE, CURRENT_STATE, PAGE_GENERAL, PAGE_FISHING,
        LANGUAGE, LANGUAGE_DESCRIPTION, LANGUAGE_ENGLISH, LANGUAGE_TRADITIONAL_CHINESE,
        GROUP_FISHING, GROUP_CONTROL_SAFETY, GROUP_AUTO_KILL, GROUP_HIGH_VALUE,
        AUTO_THROW, AUTO_THROW_DESCRIPTION, BITE_DETECTION, BITE_DETECTION_DESCRIPTION,
        DRY_TIMEOUT, DRY_TIMEOUT_DESCRIPTION, ANTI_AFK, ANTI_AFK_DESCRIPTION,
        BACKGROUND_RUN, BACKGROUND_RUN_DESCRIPTION, LOCK_CONTROLS, LOCK_CONTROLS_DESCRIPTION,
        AUTO_KILL, AUTO_KILL_DESCRIPTION, TRIGGER_AMOUNT, TRIGGER_AMOUNT_DESCRIPTION,
        TARGETS, TARGETS_DESCRIPTION, WEAPON_SLOT, WEAPON_SLOT_DESCRIPTION,
        USE_ABILITY, USE_ABILITY_DESCRIPTION, ABILITY_AIM, ABILITY_AIM_DESCRIPTION,
        ABILITY_DELAY, ABILITY_DELAY_DESCRIPTION, HIGH_VALUE_ENABLED, HIGH_VALUE_ENABLED_DESCRIPTION,
        HIGH_VALUE_TARGETS, HIGH_VALUE_TARGETS_DESCRIPTION, HIGH_VALUE_BOXES, HIGH_VALUE_BOXES_DESCRIPTION,
        HIGH_VALUE_HUD, HIGH_VALUE_HUD_DESCRIPTION, HIGH_VALUE_ATTACK, HIGH_VALUE_ATTACK_DESCRIPTION,
        HIGH_VALUE_HITS, HIGH_VALUE_HITS_DESCRIPTION,
        MANAGE, RESET, DONE, ON, OFF, BITE_BOTH, BITE_HYPIXEL, BITE_VANILLA,
        AIM_MOB, AIM_DOWN, UNIT_SECONDS, UNIT_MILLISECONDS,
        PLAYER_TARGETS_TITLE, PLAYER_TARGETS_DESCRIPTION, HIGH_VALUE_TARGETS_TITLE, HIGH_VALUE_TARGETS_MANAGE_DESCRIPTION,
        TARGET_INPUT, ADD, NO_PLAYER_TARGETS, NO_HIGH_VALUE_TARGETS,
        PAGE_NUMBER, PREVIOUS, NEXT, CLEAR_ALL, BACK, CANCEL, REMOVE,
        ADDED_PLAYER_TARGET, ADDED_HIGH_VALUE_TARGET, REMOVED_PLAYER_TARGET, REMOVED_HIGH_VALUE_TARGET,
        CLEARED_PLAYER_TARGETS, CLEARED_HIGH_VALUE_TARGETS,
        ERROR_EMPTY, ERROR_TOO_LONG, ERROR_DUPLICATE, ERROR_NOT_FOUND, ERROR_UPDATE,
        CONFIRM_PLAYER_TITLE, CONFIRM_HIGH_VALUE_TITLE, CONFIRM_PLAYER_BODY, CONFIRM_HIGH_VALUE_BODY,
        STATE_DISABLED, STATE_READY, STATE_CASTING, STATE_WAITING, STATE_CLEARING, STATE_BITE,
        STATE_COLLECTING, STATE_ABILITY, STATE_CHASING, STATE_RETURNING, STATE_RESTORING
    }
}
