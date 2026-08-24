# Frosty AutoFish

Frosty AutoFish 是从 Frosty 客户端中独立重写的 Minecraft 26.1.2 Fabric 客户端 Mod。
它不需要安装 Frosty，也不依赖 Orbit、Mod Menu 或第三方配置库。

## 环境

- Minecraft 26.1.2
- Fabric Loader 0.19.3 或更高兼容版本
- Fabric API 0.152.1+26.1.2 或更高的 26.1.2 兼容版本
- Java 25

将 `FrostyAutoFish-1.6+26.1.2.jar` 和对应版本的 Fabric API 放入客户端
`mods` 目录即可。此 Mod 只能安装在客户端。

## 操作

- `F8`：启用或关闭自动钓鱼。
- `F9`：打开配置页面。
- `F10`：啟用或關閉 High Value 功能。
- 快捷键可以在 Minecraft 的“控制”设置中修改。
- 启用前，主手或副手必须拿着钓鱼竿。

启用状态不会跨游戏重启保存。断线、切换世界、死亡或鱼竿被移走时会自动停用并释放移动键。

## 配置

配置保存在 `config/frosty-autofish.json`：

| 设置 | 默认值 | 说明 |
| --- | --- | --- |
| Auto Throw | 开 | 自动抛竿和重新抛竿 |
| Anti AFK | 开 | 每 10–20 秒轻微转动视角并恢复 |
| Background Run | 开 | AutoFish 启用时切换到其他窗口不触发失焦暂停 |
| Lock Controls | 关 | AutoFish 启用且没有打开 Screen 或 overlay 时锁定物理游戏操作 |
| Dry Timeout | 15 秒 | 首次无咬钩超时重抛，连续第二次超时停用并发送 `/is` |
| Auto Kill | 开 | 累积并自动清理收竿后生成的海怪 |
| Trigger Amount | 3 | 达到该目标数量后开始清理 |
| Use Ability | 关 | 使用指定快捷栏物品的右键技能 |
| Ability Delay | 150 ms | 首次技能延迟，范围 50–1000ms，每个目标随机浮动 ±10% |
| Ability Aim | Mob | 技能朝向目标或正下方 |
| Weapon Slot | 1 | 自动击杀使用的快捷栏槽位 |
| Bite Detection | Hypixel + Vanilla | 使用 `!!!` 标记和原版浮漂 biting 状态 |
| High Value Enabled | 開 | High Value 的總開關；關閉後停止目標匹配、顯示與自動攻擊 |
| High Value Boxes | 開 | 進入世界後常駐顯示外部高價值 player-model 目標的碰撞格 |
| High Value HUD | 開 | 進入世界後在 HUD 常駐顯示最近的外部高價值目標、距離和攻擊進度 |
| High Value Attack | 關 | 僅在 AutoFish 啟用時對外部高價值目標原地瞄準並自動攻擊 |
| High Value Hits | 1 | 每個外部高價值目標最多自動攻擊次數，範圍 1–10 |

近战模式会追击目标、显示路径并在完成后回到启用位置；技能模式会恢复使用技能前的视角。
自动进入战斗时允许 Mod 临时切换到 Weapon Slot，不会被判定为玩家主动切走鱼竿；
清理完成后会自动切回原鱼竿槽位。主手鱼竿与 Weapon Slot 不能使用同一个槽位。

## 注意

- Hypixel 检测依赖浮漂附近名为 `!!!` 的盔甲架，这是服务器特化行为。
- 原版检测读取客户端已同步的浮漂 `biting` 状态，不发送额外网络请求。
- 每次抛竿后咬钩检测会等待 10 tick，并要求信号先出现一次“未咬钩”状态，
  避免残留 `!!!` 标记导致首次抛竿被立即收回。
- 如果浮漂钩住非玩家活体，Mod 会立即收竿；确认旧浮漂消失后再重新抛竿。
  恢复状态绑定本地玩家自己的浮漂实体 ID，不扫描或等待其他玩家的浮漂。
- 自动杀怪在收竿前建立实体快照，并在 20 tick 内按“本地浮漂位置、收竿位置、玩家当前位置”
  三个安全范围收集新生物；玩家和收竿前已有的实体不会被加入目标。
- 技能模式不再依赖准星命中方块，并会每 8 tick 重试一次，直到目标消失或达到超时上限。
- 对于使用玩家模型和皮肤的海怪，可通过客户端指令添加头顶名称关键词：
  - `/frostyautofish target add <名称>`
  - `/frostyautofish target remove <名称>`
  - `/frostyautofish target list`
  - `/frostyautofish target clear`
  也可以在 `F9` 配置页面中通过 `Targets` 管理此名单。
  名称匹配忽略大小写、颜色代码和多余空格，并允许等级、血量等前后缀。只有收竿后新出现且
  位于本地鱼钩/玩家捕获范围内的匹配玩家实体会被攻击。
- 外部高價值 player-model 目標使用獨立名單，不會和普通 Auto Kill target 名單混在一起：
  - `/frostyautofish highvalue add <名稱>`
  - `/frostyautofish highvalue remove <名稱>`
  - `/frostyautofish highvalue list`
  - `/frostyautofish highvalue clear`
  也可以在 `F9` 配置頁面中透過 `High Value` 管理此名單。玩家進入世界後即會持續掃描匹配此名單，
  且不是本輪自己收竿捕獲區或普通 Auto Kill 目標的玩家模型；不需要啟用 AutoFish，按 `F8` 停止後
  碰撞格和 HUD 文字也會繼續顯示。Screen 或 overlay 開啟期間只會暫停顯示，背景追蹤仍會更新，
  關閉介面後自動恢復。碰撞格、HUD 文字和自動攻擊可分別開關。
  `F10`（可在 Minecraft「控制」中重綁）或 `High Value Enabled` 可切換總開關；`F10` 在 Screen、overlay
  及 Lock Controls 生效期間仍可使用。快捷鍵切換會立即儲存至 JSON，並顯示一次
  `High Value: ON/OFF` client status 提示。總開關關閉時會立即隱藏碰撞格和 HUD，停止完整名稱匹配及
  自動攻擊，但保留目前世界的攻擊次數與自己捕獲目標安全紀錄；重新開啟後會先刷新目標再恢復顯示。
  High Value 不會發送目標偵測聊天通知，僅 `F10` 切換總開關時顯示上述狀態提示。
  即使開啟 High Value Attack，也只會在 AutoFish 已啟用、無 Screen/overlay、非 Auto Kill combat、
  目標位於攻擊距離內且符合既有限制時，原地瞄準攻擊設定次數，不接管移動、不追擊、不切換武器。
- 经名称白名单验证的玩家模型实体会在本轮目标生命周期内保留批准状态；技能未能在重试和
  超时上限内消灭目标时，Mod 会丢弃该目标，不会转入近战。
- 每次抛竿后达到 Dry Timeout 仍没有咬钩时会直接收回自己的浮漂并重抛，不进入海怪收集；
  连续第二次超时会再次收竿、停用 Mod，并由玩家向服务器发送 `/is`。正常咬钩会清零连续计数。
- 技能结束后的视角先平滑恢复，并在结束帧精确写回技能前保存的 yaw/pitch，消除鼠标灵敏度
  量化和 1° 完成阈值留下的轻微偏移。
- 技能模式达到 Trigger Amount 后不再等待完整的 20 tick 收集窗口；切换武器后按 Ability Delay
  在主线程计时，并为每个新目标固定生成一次 ±10% 浮动，随后按实时位置精确瞄准并立即使用技能。
- Background Run 只在 AutoFish 正在启用时取消 Minecraft 的失焦暂停；关闭 AutoFish 或关闭该设置后，
  游戏恢复原本的失焦行为。后台帧率仍由 Minecraft 自己的非活动窗口帧率设置决定。
- Lock Controls 启用后，会在 AutoFish 运行且没有打开 Screen 或 overlay 时拦截物理鼠标转向、攻击和
  使用，以及前后左右移动、跳跃、潜行、疾跑、快捷栏 1–9、选取方块槽位和世界内滚轮切换。
  世界中锁定视角时会显示鼠标游标；解除锁定后，会在回到游戏世界时恢复隐藏游标与视角控制。
  `F8`、`F9`、`F10`（包括改键后）、聊天、命令、`Escape`、背包和其他界面快捷键仍可使用。
- 任意 Screen 或 overlay 打开时会暂时解除 Lock Controls，所有输入正常工作；Screen 打开期间，
  AutoFish 会按现有逻辑暂停自动流程并释放移动键。AutoFish 自己的移动、瞄准、攻击、使用物品和
  槽位切换不受此物理输入锁影响。
- 解除锁定时会恢复仍按住的物理键盘状态；若一直按住鼠标攻击键或使用键，则需先松开再重新按下，
  避免合成意外点击。此功能不会阻止其他 Mod 直接调用 `gameMode`、修改 yaw 或设置物品栏槽位。
- 自动化功能可能违反某些服务器规则。使用前请确认目标服务器的规定，风险由使用者承担。
- 不要同时启用 Frosty 原版 AutoFish 和本 Mod，否则两个控制器会互相竞争。

## 构建

在安装 Java 25 后运行：

```powershell
.\gradlew.bat build
```

产物位于 `build\libs\FrostyAutoFish-1.6+26.1.2.jar`。

本目录是 26.1.2 专用兼容变体；请勿将其安装到 26.2 客户端。原始分析文档描述的
Frosty 源码基线仍是 26.2，独立 Mod 的业务逻辑保持一致，仅适配游戏和 Fabric API 版本。

### 与 26.2 版的代码差异

- Screen 的读取和切换改用 26.1.2 的 `Minecraft.screen` 与 `Minecraft.setScreen`。
- 聊天消息改用 26.1.2 的 `Gui.getChat()`。
- 史莱姆实体使用 26.1.2 的 `net.minecraft.world.entity.monster.Slime` 包名。
- 路径和目标渲染通过 26.1.2 的 `LevelRenderer.collectPerFrameGizmos()` 提交。
- 浮漂的私有 `biting` 字段在 26.1.2 中仍然存在，Accessor Mixin 无需改变。

## 许可证与来源

本项目基于 GPL-3.0 发布。AutoFish 行为和地面 NavMesh 实现源自
[WhatsYouss/Frosty](https://github.com/WhatsYouss/Frosty)，独立版重写了模块生命周期、
配置、界面、状态管理、咬钩检测与安全清理逻辑。详见 `NOTICE` 和
`docs/Frosty-analysis.md`。
