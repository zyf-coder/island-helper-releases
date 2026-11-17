# 灵动助手 · Island Helper

给**魅族 22**（Flyme AIOS 2）用的灵动岛增强 App。在屏幕顶部绘制一个「药丸」胶囊，实时呈现 **导航 / 音乐 / 支付** 三类信息。

---

## 为什么是自绘，而不是用系统实况通知

魅族官方把这个功能叫 **「实况通知」**（设置路径：系统设置 → 通知和状态栏 → 实况通知），媒体俗称灵动岛。

但 **Flyme 没有开放任何供第三方 App 往实况通知推送内容的公开 API / Intent**。逐篇核查魅族开放平台完整文档树后，ROM 适配类只有两篇：

| 文档 | 内容 | 能否用于实况通知 |
|---|---|---|
| [桌面图标角标](https://open.flyme.cn/docs?id=207) | ContentProvider `content://com.meizu.flyme.launcher.app_extras/badge_extras` | 否，只是角标 |
| [系统媒体控件 & 状态栏歌词](https://open.flyme.cn/docs?id=239) | 反射 `Notification.FLAG_ALWAYS_SHOW_TICKER` | 否，只是歌词 |

魅族推送 SDK（`com.meizu.flyme.push.*`）的公开字段里也只有通知栏下发能力，没有任何胶囊相关项。已接入的实况通知（百度地图、12306 等）全是厂商直连商务合作，第三方无自助通道。

所以本项目走**自绘路线**：用 `WindowManager` 的 `TYPE_APPLICATION_OVERLAY` 在状态栏区域画一个胶囊，视觉与交互对齐系统实况通知。

**这反而是优势**：不依赖厂商白名单、不受「逐 App 实况通知开关」限制，任何 Android 8+ 机型都能用。

---

## 三个场景的实现路径与真实可行性

### 音乐 —— 唯一真正通用的路径 ✅

反向读取系统当前活跃的 `MediaSession`：

```kotlin
MediaSessionManager.getActiveSessions(notifListenerComponent)
```

网易云 / QQ音乐 / 酷狗 / Spotify / YouTube Music 全都遵守 MediaSession 协议，系统媒体控件就是这么来的。播放/暂停/上下曲通过 `MediaController.transportControls` 转发。

> 注意：`MediaButton` / `KeyEvent` 已废弃，必须走 `MediaSession.Callback`。读取他人会话需要用户授予「通知使用权」。

### 导航 —— 没有免费通用接口 ⚠️

第三方**无法**读取高德/百度 App 内部正在进行的导航会话。本项目的做法：

1. `NaviSource.launchNavi()` / `launchBaiduNavi()` —— 由本 App 通过 URI API 发起导航；
2. `NaviSource.parse()` —— 从导航 App 写入通知栏的文本里解析转向、剩余距离/时间、限速。

文本格式随 App 版本变化，解析是容错的：认不出具体转向就降级为「导航中」。这是当前唯一的免费路径，准确率取决于上游 App。

> 用户若愿意，Flyme 系统设置里的「实况通知」可以直接给高德/百度开权限，用系统原生卡片 —— 设置页里做了引导。

### 支付 —— 默认走自有服务端 🔒

**支付真相永远来自你自己的服务端**，App 只做展示：

```json
{"amountCents": 8800, "payer": "张三", "orderId": "xxx", "success": true}
```

在设置页填入服务端地址即可，留空则不接入。

**为什么不监听微信/支付宝的通知？** 技术上可行（社区的「微信免签支付」就是这么做的），但涉及资金信息读取与诱导监听，Flyme 另有《APP 隐私合规检测标准》，上架会被卡。本项目**刻意不实现**这条路径，只在 README 记录这一取舍。

---

## 界面

三种卡片形态，统一的收起/展开两态。

**收起态**（药丸）

```
┌──────────────────────────────────────┐
│  ♪ 平凡之路   朴树   ⏮  ⏸  ⏭       │
└──────────────────────────────────────┘
```

**展开态**

```
┌────────────────────────────────────────┐
│  ┌────┐  平凡之路                      │
│  │封面│  朴树 · 专辑名                 │
│  └────┘  ♀ 02:14 ──○──── 03:57        │
│           ⏮   ⏯   ⏭   ✕                │
└────────────────────────────────────────┘
```

**优先级仲裁**（`IslandState`）

| 优先级 | 场景 | 行为 |
|---|---|---|
| 30 | 导航 | 最高，驾驶中不可错过 |
| 20 | 支付 | 播报 4 秒后自动淡出，恢复上一张 |
| 10 | 音乐 | 常驻，直到播放停止或用户收起 |

导航进行中，音乐卡退化为极简态（只留 ⏯/⏭）挤在胶囊尾部，而不是完全消失。

---

## 使用

### 安装

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 首次使用

打开 App，按提示授予三项权限：

1. **悬浮窗** —— `Settings.canDrawOverlays()`，用来画胶囊
2. **通知使用权** —— 读取 MediaSession（音乐）和导航文本
3. **忽略电池优化** —— 避免后台被杀掉

然后打开「启动灵动岛服务」。

---

## 项目结构

```
app/src/main/java/com/mimo/island/
├── MainActivity.kt              权限引导 + 设置页
├── island/
│   ├── IslandCard.kt            统一卡片模型
│   ├── IslandState.kt           状态总线 + 优先级仲裁
│   └── IslandView.kt            胶囊与展开面板（纯代码构建）
├── service/
│   ├── IslandService.kt         前台服务，驱动数据源轮询
│   ├── FloatWindowController.kt WindowManager 悬浮窗
│   ├── NotifListener.kt         通知监听（导航文本）
│   └── BootReceiver.kt          开机自启 + 偏好存储
└── source/
    ├── MediaSource.kt           音乐：MediaSession
    ├── NaviSource.kt            导航：URI 调起 + 文本解析
    ├── PaySource.kt             支付：服务端轮询
    └── Models.kt                数据结构
```

## 构建环境

- JDK 17
- Android SDK：platform 34、build-tools 34.0.0
- Gradle 8.14.3（wrapper 已附带）
- minSdk 26 / targetSdk 34

---

## 已知限制

- 部分音乐 App 不发布完整 MediaSession，显示为「未知来源」，控制按钮不可用
- 导航文本解析依赖上游 App 的通知格式，可能随时失效
- 悬浮窗在部分 ROM 需要电池优化白名单才能后台保活
- 支付播报依赖自有服务端，无服务端时该场景不工作

## 许可

MIT
