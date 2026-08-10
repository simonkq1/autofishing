# Frosty 1.2.1 功能与 AutoFish 实现分析

> 本文分析对象仍是原始 Frosty 26.2 源码；当前目录提供的独立 Mod 已额外适配到
> Minecraft 26.1.2，功能设计与安全重构原则不变。

## 1. 项目概况

Frosty 是面向 Minecraft 26.2、Hypixel SkyBlock 的 Fabric 客户端。项目包含 64 个模块，
使用 Fabric Loader/Fabric API、Mojang 官方命名和 Java 25。主要架构如下：

- `Frosty` 初始化模块管理器、命令管理器、配置目录和 Orbit 事件总线。
- `ModuleManager` 注册所有模块；`Module` 负责开关、快捷键、分类、设置和事件订阅。
- `ButtonSetting`、`SliderSetting`、`SelectSetting` 等设置由自定义 ClickGui 展示并写入 JSON。
- 52 个 Mixin 从 Minecraft 的 tick、输入、网络、渲染、实体和 GUI 流程中产生内部事件。
- `RotationUtils`/`Rotations` 处理可见与静默旋转、鼠标灵敏度步进及移动修正。
- `RenderUtils` 使用 26.2 GPU 管线绘制方框、线条、文字和 HUD。
- `utility.pathfinding` 提供地面 NavMesh、飞行寻路、混合寻路和路径缓存。

## 2. 功能清单

下表覆盖 `modules/impl` 下的全部 64 个模块。“实现方式”描述其主要挂接点与策略，而不是逐行复述。

### Client（6）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| Cape | 自定义披风 | 加载本地纹理并在玩家披风渲染层替换资源 |
| ChatCopier | 点击复制聊天文本 | 修改聊天组件与样式，按设置移除颜色码 |
| Commands | Frosty 客户端命令开关 | 截获聊天输入并交给自定义命令管理器 |
| Title | 自定义窗口标题 | Mixin 修改 Minecraft 窗口标题，可保留原标题 |
| ClickGui/UI | 模块设置界面与中英文 | 自定义 Screen、组件布局、主题色和语言持久化 |
| UngrabMouse | 解除鼠标锁定 | 控制窗口鼠标捕获状态 |

### Combat（3）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| AutoClicker | 自动点击 | 按 CPS/模式生成攻击，支持方块与背包条件 |
| KillAura | 自动攻击附近目标 | 每 tick 选择目标、做距离/视线判断、旋转并攻击 |
| Velocity | 减少击退 | 修改实体速度包和爆炸推力的水平/垂直分量 |

### Farming（5）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| CropNuker | 批量破坏作物 | 扫描范围内成熟作物并按 BPS 发起破坏 |
| FarmingMacro | 自动农场移动 | 状态机控制朝向、行走、换行、停顿、回传与害虫联动 |
| FarmingProtector | 农场异常保护 | 监控移动、方块生成、旋转和物品变化并触发停止/重启 |
| GardenCleaner | 花园清理 | 按模式扫描并以 BPS 清理指定方块 |
| PestCleaner | 自动清理害虫 | 目标搜索、路径移动、交互/攻击并与农场宏协作 |

### Fishing（1）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| AutoFish | 自动抛收竿与清理海怪 | tick 状态机、`!!!` 盔甲架咬钩检测、随机延迟、实体收集、旋转、NavMesh 追击与回位 |

### Foraging（3）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| LushlilacNuker | 自动破坏 Lush Lilac | 范围扫描特定方块并发送破坏操作 |
| SeaLumiesNuker | 自动破坏 Sea Lumies | 范围扫描特定方块并发送破坏操作 |
| WoodNuker | 自动伐木 | 按木材类型筛选、瞄准并支持静默旋转 |

### Fun（5）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| Derp | 自动旋转视角 | 每 tick 按速度和方向改变角度 |
| MurderMystery | 谋杀之谜辅助 | 识别角色/物品实体并提供目标信息 |
| QMaths | 自动回答数学题 | 正则解析聊天算式，延迟后回复并防重复 |
| Spammer | 自动发送文本 | 在多条消息间轮换，按延迟发送并防重复 |
| WBMacro | 连续使用物品 | 按设定延迟执行右键操作 |

### Hunting（2）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| AutoReel | 狩猎套索自动收线 | 找到被玩家牵引的生物，检测附近以 `REEL` 结尾的盔甲架并右键 |
| Shulkers/Hideonleaf | Shulker 狩猎辅助 | ESP、距离筛选、旋转和自动攻击 |

### Mining（4）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| FrozenTreasure | 冰冻宝藏辅助 | 识别目标方块，提供 ESP 与自动破坏 |
| MithrilMacro | 自动挖 Mithril | 方块优先级、智能瞄准、超时与 Titanium 策略组成状态机 |
| NoBreakReset | 防止破坏进度重置 | Mixin 修改客户端持续破坏逻辑 |
| SandNuker | 自动挖沙 | 按模式选择方块并受 BPS 限制 |

### Movement（4）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| Eagle | 边缘自动潜行 | 检查脚下可站立方块并控制潜行键 |
| Fly | 客户端飞行 | 每 tick 重写水平和垂直速度 |
| GuiMove | 打开界面时移动 | 将实际按键状态映射回移动输入 |
| Sprint | 自动疾跑 | 修改疾跑判定事件，支持保持与减速策略 |

### Other（12）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| AntiBot | 机器人过滤 | 根据实体属性、名称和服务器特征排除假玩家 |
| AutoExperiment | 自动完成实验桌 | 读取容器物品状态并按延迟点击正确槽位 |
| AutoGift | 自动赠送礼物 | 搜索目标玩家、旋转并交互 |
| AutoHarp | 自动竖琴 | 读取容器音符位置并按模式/延迟点击 |
| AutoReconnect | 自动重连 | 断线界面倒计时后重新连接服务器 |
| Blink | 暂存网络包 | 拦截并缓存移动包，按时间释放并显示服务端位置 |
| CarnivalHelper | 狂欢节小游戏辅助 | 计分板识别游戏；实现扫雷提示、移动目标预测、自动瞄准与射击/抛竿 |
| DojoHelper | 道场辅助 | 依据道场模式过滤交互、目标或伤害行为 |
| FastPlace | 快速放置 | 修改客户端右键使用冷却 |
| GhostBlock | 幽灵方块 | 射线选块并只在客户端替换方块状态 |
| MoveFix | 旋转移动修正 | 按服务端/客户端朝向重新计算移动输入 |
| NoPlaceInteract | 防止放置时误交互 | 截获方块交互，可保留挥手动画 |

### Render（19）

| 模块 | 功能 | 实现方式 |
| --- | --- | --- |
| AntiDebuff | 移除视觉减益 | 取消恶心等屏幕效果 |
| AntiTexture | 屏蔽指定纹理 | 在资源/模型解析阶段跳过目标纹理 |
| ArmorHider | 隐藏护甲 | 在护甲渲染层按部位和自身条件取消绘制 |
| AxolotlESP | Axolotl 高亮 | 扫描实体并绘制方框 |
| ChestESP | 箱子高亮 | 扫描方块实体并绘制方框 |
| FreeLook | 自由观察 | 分离相机角度和玩家角度，Mixin 替换 Camera 参数 |
| Fullbright | 全亮 | 修改光照贴图或客户端亮度 |
| HUD | 模块列表 HUD | 二维渲染启用模块、渐变、背景、边条与后缀 |
| Nametags | 自定义名牌 | 替换实体名牌渲染并应用缩放 |
| NickHider | 昵称隐藏 | 在文本分解/聊天/HUD 阶段替换用户名 |
| NoBlur | 关闭界面模糊 | 截获后处理模糊调用 |
| NoHudElement | 隐藏 HUD 元素 | 分别取消计分板、Boss Bar 和标题 |
| NoHurtCam | 受伤镜头强度 | 修改 GameRenderer 受伤晃动倍率 |
| NoOverlay | 隐藏覆盖层 | 取消火焰、水下和卡墙覆盖层 |
| PestESP | 害虫高亮 | 搜索 Garden 害虫并绘制提示 |
| PlayerESP | 玩家高亮 | 方框、填充、连线和 Chams 渲染 |
| ScrollableTooltips | 可滚动长提示 | 修改 tooltip 定位并消费滚轮输入 |
| StarredMobESP | 星级怪高亮 | 依据名称盔甲架/实体关联绘制目标 |
| TPS | 服务器 TPS 显示 | 统计时间包间隔并绘制 HUD 文本 |

## 3. AutoFish 原始实现

### 3.1 事件和依赖

`AutoFish` 订阅四类内部事件：

1. `PreUpdateEvent`：由 `MinecraftMixin` 在 `Minecraft.tick()` 开头发布，驱动所有逻辑。
2. `EntityJoinEvent`：由 `ClientLevelMixin#addEntity` 发布，用于收竿后短窗口和史莱姆分裂。
3. `Render3DEvent`：由 `LevelRendererMixin` 发布，绘制目标框、路径点和路径线。
4. `SprintEvent`：强制关闭疾跑，避免寻路移动进入 sprint。

它还直接依赖 `Module`、设置系统、`Utils`、`RotationUtils`、`RenderUtils` 和整套
`PathfindingService`/NavMesh。因此仅复制 `AutoFish.java` 无法成为独立 Mod。

### 3.2 钓鱼状态

原代码用整数 `currentMode` 表示：

- `0`：空闲；Auto Throw 开启后进入抛竿。
- `1`：调用 `gameMode.useItem` 抛竿，等待玩家的 `fishing` 字段出现。
- `2`：扫描浮漂 2 格内盔甲架；名字包含 `!!!` 即视为咬钩。
- 咬钩后生成 10–300ms 随机延迟；独立版默认 15 秒无咬钩时重抛，连续第二次超时则停用并发送 `/is`。
- `3`：右键收竿，并在 Auto Kill 开启时进入战斗状态机。

主手鱼竿会记录快捷栏槽位；副手鱼竿直接使用副手。关闭模块时若浮漂仍存在，会主动收竿。

### 3.3 海怪清理状态

收竿时保存玩家位置，并在 3 tick 内收集距该位置 1 格、距玩家不超过 32 格的活体。
目标数量达到 Trigger Amount 后切到 Weapon Slot：

- Ability：平滑朝向目标或正下方，右键一次，遍历所有目标后恢复视角。
- Chasing：为目标寻找可站立邻格，生成 NavMesh 路径，控制前进/跳跃并在攻击范围内近战。
- Returning：近战清理结束后寻路回到启用位置。
- Restore Rotation：平滑恢复原角度，切回鱼竿并重新开始。

史莱姆目标消失后会记录死亡位置，并在 20 tick 内继续收集 4 格内新生成的分裂体。

## 4. 原实现问题与独立版处理

| 问题 | 原实现 | 独立版 |
| --- | --- | --- |
| 线程安全 | Anti AFK 后台线程直接修改玩家 yaw | 全部改为客户端 tick 定时器 |
| 误伤 | 收竿附近任意 LivingEntity 都可成为目标，包括其他玩家 | 排除所有玩家和盔甲架，并使用收竿前实体快照 |
| 重复攻击 | 同时调用玩家攻击和手工构造 AttackPacket | 只调用标准 `gameMode.attack` |
| 状态可读性 | 钓鱼整数状态和 KillState 并行 | 合并为单一具名状态机 |
| 清理 | 断线/换世界可能留下线程或按键状态 | 自动停用、恢复视角并释放本 Mod 接管的按键 |
| 依赖体积 | 引入整个模块、事件、渲染和寻路框架 | Fabric tick/按键、一个只读 accessor、精简地面 NavMesh |
| 原版支持 | 只识别 Hypixel `!!!` | 增加原版同步 `FishingHook.biting` 回退 |

`AutoReel` 操作的是狩猎套索和 `REEL` 状态盔甲架；`CarnivalHelper` 的钓鱼部分是狂欢节
移动目标小游戏。两者与普通/SkyBlock AutoFish 生命周期不同，因此不纳入独立版。
