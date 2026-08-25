# Steam Deck 同步功能开发与交付交接文档

## 1. 基本信息

| 属性 | 内容 |
| :--- | :--- |
| **所属分支** | `feature/steam-deck-sync` |
| **功能定位** | Steam Deck 截图（原生快捷键/背键/侧边栏按钮）通过局域网自动同步至 Android 端 YomiLLM 应用并触发日语台词语法解析 |
| **Android 编译产物** | `app/build/outputs/apk/debug/app-debug.apk` (约 60.9 MB) |
| **Decky 插件目录** | `decky-plugin/` |
| **Steam Deck 远端路径** | `/home/deck/homebrew/plugins/decky-yomi-sync/` |
| **Steam Deck 设备环境** | SteamOS 3.6 / Linux 6.16 (x86_64), IP: `192.168.1.16` (DHCP), SSH 用户: `deck`, 密码: `321127` |
| **Decky Loader 环境** | Decky Loader v3.2.6 (以 root 权限运行，PID 9836) |

---

## 2. 接口通信协议规范

所有接口均运行在 Android 端本地 HTTP 服务上，默认端口为 `8765`（支持在设置中自定义）。

### 2.1 服务探测接口
* **URL**: `GET http://<PHONE_IP>:<PORT>/api/v1/ping`
* **请求头**: 无特定要求
* **响应状态码**: `200 OK`
* **响应体 (JSON)**:
  ```json
  {
    "status": "ready",
    "app": "YomiLLM",
    "version": "1.9.2"
  }
  ```

### 2.2 配对认证接口
* **URL**: `POST http://<PHONE_IP>:<PORT>/api/v1/pair`
* **请求头**: `Content-Type: application/json`
* **请求体 (JSON)**:
  ```json
  {
    "pin": "6721"
  }
  ```
* **响应 (PIN 码匹配)**:
  - 状态码: `200 OK`
  - 响应体: `{"status": "paired", "token": "<UUID>"}`
* **响应 (PIN 码错误)**:
  - 状态码: `401 Unauthorized`
  - 响应体: `{"status": "error", "message": "Invalid PIN code"}`

### 2.3 截图上传接口
* **URL**: `POST http://<PHONE_IP>:<PORT>/api/v1/screenshot`
* **请求头**:
  - `Content-Type: multipart/form-data; boundary=...`
  - `X-Auth-Token: <UUID>`
* **请求体**: 包含图片二进制数据的 multipart/form-data（支持 `image/jpeg` 与 `image/png`）
* **响应**:
  - 状态码: `200 OK`
  - 响应体: `{"status": "success", "received_at": <TIMESTAMP>}`

---

## 3. 文件变更与模块清单

### 3.1 Android 端修改与新增文件

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| `app/src/main/java/com/example/japanesegrammarapp/service/DeckSyncServer.kt` | 新增 | 基于原生 Java `ServerSocket` 实现的多线程 HTTP 服务器，监听 `0.0.0.0:8765`，负责处理 ping、pair、screenshot 请求 |
| `app/src/main/java/com/example/japanesegrammarapp/service/DeckSyncForegroundService.kt` | 新增 | 前台服务，持有 `PARTIAL_WAKE_LOCK` 与 `WifiManager.MulticastLock`，管理 `DeckSyncServer` 生命周期并注册 Android NSD (mDNS) 广播 |
| `app/src/main/java/com/example/japanesegrammarapp/service/DeckSyncEventBus.kt` | 新增 | 内存事件总线，用于将接收到的截图 Uri 与服务运行状态分发至 UI 与 MainActivity |
| `app/src/main/java/com/example/japanesegrammarapp/domain/model/DeckSyncSettings.kt` | 新增 | 配对数据模型，定义端口、4 位 PIN 码、Token 等数据结构 |
| `app/src/main/java/com/example/japanesegrammarapp/utils/NetworkUtils.kt` | 新增 | 局域网 IPv4 地址获取工具，过滤虚拟网卡并提取当前 Wi-Fi/以太网 IP |
| `app/src/main/java/com/example/japanesegrammarapp/ui/screens/SettingsDeckSyncSection.kt` | 新增 | 设置界面中的 Steam Deck 投送配置区块，包含服务开关、IP/端口显示、复制按钮、PIN 码管理与控制器映射指引 |
| `app/src/main/java/com/example/japanesegrammarapp/MainActivity.kt` | 修改 | 处理来自 DeckSyncForegroundService 的通知 Intent 跳转与图片 Uri 自动导入逻辑 |
| `app/src/main/java/com/example/japanesegrammarapp/YomiLLMApplication.kt` | 修改 | 维护应用前后台状态标记 `isAppInForeground`，用于判断收到图片时采用应用内拉起还是 Heads-up 通知弹窗 |
| `app/src/main/java/com/example/japanesegrammarapp/ui/SettingsViewModel.kt` | 修改 | 增加投送服务的开启/关闭、端口更新、PIN 码生成与 IP 刷新方法 |
| `app/src/main/java/com/example/japanesegrammarapp/domain/repository/SettingsRepository.kt` | 修改 | 声明 DeckSync 配置存储与读取接口 |
| `app/src/main/java/com/example/japanesegrammarapp/data/repository/SettingsRepositoryImpl.kt` | 修改 | 基于 SharedPreferences 实现 DeckSync 状态、端口、PIN 码、Token 的持久化 |
| `app/src/main/AndroidManifest.xml` | 修改 | 声明 `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_CONNECTED_DEVICE`、`CHANGE_WIFI_MULTICAST_STATE`、`ACCESS_WIFI_STATE` 权限与 Service 组件 |
| `app/proguard-rules.pro` | 修改 | 添加 `-dontwarn java.lang.management.**` 与 `-dontwarn org.slf4j.impl.StaticLoggerBinder` 规则 |
| `app/src/main/res/values*/strings.xml` | 修改 | 添加中、英、日三语的投送功能文本与通知配置文案 |

### 3.2 Steam Deck 端插件文件 (`decky-plugin/`)

| 文件路径 | 变更类型 | 说明 |
| :--- | :--- | :--- |
| `decky-plugin/plugin.json` | 新增 | Decky 插件元数据描述文件（API Version 1，名称 `YomiDeck`） |
| `decky-plugin/package.json` | 新增 | 前端项目依赖与构建脚本定义 |
| `decky-plugin/src/index.tsx` | 新增 | 基于 Decky UI 的侧边栏界面：连接状态、IP/端口配置、自动扫描、PIN 码配对、网络诊断面板、实时截屏发送按钮、历史截图重发按钮，兼容 Decky 2/3 及 `window.DeckyPluginLoader` 桥接 |
| `decky-plugin/build.js` | 新增 | `esbuild` 打包脚本，配置全局依赖映射（`window.DFL`、`window.SP_REACT`、`window.Navigation`），使用经典 JSX 转换模式 |
| `decky-plugin/main.py` | 新增 | Decky 插件 Python 后端守护进程：实现 Watchdog + 0.2s 轮询双模文件监听、局域网并发子网探测、4 级网络链路诊断、多路径配置持久化、Gamescope 官方 Portal API 截图调用与 HTTP POST 上传 |
| `decky-plugin/py_modules/zeroconf_helper.py` | 新增 | mDNS 辅助模块 |
| `decky-plugin/dist/index.js` | 新增 | 前端最终打包生成物（约 24.4 KB），运行于 SteamOS CEF 环境中 |

### 3.3 自动化与辅助脚本 (`scripts/`)

| 脚本路径 | 说明 |
| :--- | :--- |
| `scripts/deploy_to_deck.py` | 基于 Paramiko 的部署脚本：连接 Steam Deck，上传文件到 `/home/deck/homebrew/plugins/decky-yomi-sync/`，设置权限并重启 `plugin_loader` 服务 |
| `scripts/test_portal_plugin.py` | 在 Steam Deck 上直接调用 `Plugin.trigger_live_capture()` 测试 Gamescope Portal 截屏与推流全流程 |
| `scripts/check_live_logs.py` | 读取 SteamOS 端 `journalctl -u plugin_loader` 实时日志 |

---

## 4. 关键问题排查与修复记录

| 阶段 | 出现现象 | 根因分析 | 采取措施 |
| :--- | :--- | :--- | :--- |
| **R8 编译** | `Missing classes detected while running R8` | Ktor 与 SLF4J 引用了 Android 缺失的 `java.lang.management.*` 与 `StaticLoggerBinder` 类 | 在 `app/proguard-rules.pro` 中增加 `-dontwarn` 忽略规则 |
| **Decky 加载** | `TypeError: Failed to resolve module` | 打包产物存在裸模块引用（`from "decky-frontend-lib"`），SteamOS CEF 无法通过 URL 解析裸包名 | 在 `decky-plugin/build.js` 中使用 `esbuild` 插件将依赖重定向到宿主全局对象 `window.DFL` 与 `window.SP_REACT` |
| **React 渲染** | `TypeError: Cannot read properties of undefined (reading 'ReactCurrentOwner')` | `react/jsx-runtime` 试图读取内部私有属性 `ReactCurrentOwner`，宿主 `SP_REACT` 不包含该属性 | 将 TS/Build 切换为经典 JSX 模式（`jsxFactory: "window.SP_REACT.createElement"`） |
| **配对报错** | `Pair error` (URL 畸形) | 用户输入了带 `http://` 和端口的完整地址，导致后端拼接成 `http://http://192.168.1.18:8765:8765/api/v1/pair` | 在前端输入框与 Python 后端加入 `sanitize_ip()` 自动剥离协议头与端口后缀 |
| **扫描失败** | `Scan failed` | SteamOS 未内置 `zeroconf` Python 库，且路由器可能拦截 mDNS 组播 | 在 `main.py` 中增加 80 线程并发子网 HTTP 探测器，1 秒内扫描 `192.168.1.1~254:8765/api/v1/ping` |
| **0字节超时** | TCP 建立连接但 3 秒超时收到 0 字节 | Ktor CIO 引擎在 Android 后台受协程上下文挂起影响，未能向客户端写入响应报文 | 将 `DeckSyncServer.kt` 重构为基于标准 Java `ServerSocket` 的独立守护线程实现 |
| **侧边栏状态丢失** | 每次关闭再打开侧边栏，输入框与连接状态全部重置 | Decky 3.x 中 Loader 传递 1 个位置参数导致 `Plugin.get_status()` 抛出 `TypeError: takes 1 positional argument but 2 were given`；且只保存到单一路径 | 在 `main.py` 所有插件方法增加 `*args, **kwargs` 签名；实现 `get_config_paths()` 多路径同步持久化 |
| **旧图被发送** | 游玩《命运石之门》时同步了历史《符文工房3》截图 | 旧代码在未发生新截屏时，直接将磁盘上修改时间最新的历史文件发送出去 | 将 `PollingWatcher` 轮询间隔优化至 0.2s 实时监听新文件；拆分“实时截图”与“历史重发”两个独立通道 |
| **按键注入死锁** | 点击侧边栏按钮截图报错 | 在子线程中动态 `import evdev` 导致 Python 3.13 对 C 扩展 `_input.so` 报循环导入错误 | 将 `evdev` 提升至 `main.py` 顶层主线程导入并初始化 |
| **截屏黑屏** | 侧边栏按钮抓取到的图片为 12KB 黑屏 | SteamOS 游戏模式采用 Gamescope 合成器与 DRM Direct Scanout（显卡硬件直通），游戏画面不经过 Xwayland（`:0`），`ffmpeg x11grab` 只能抓到空图层 | 接入 SteamOS 官方 `xdg-desktop-portal-gamescope` 接口，通过 DBus 调用直接从 Gamescope Vulkan 渲染后备缓冲区导出 1280x800 全彩无损 PNG（约 2.36MB） |
| **推送死循环** | 截图推送后用户点击取消或确认，图片立即再次强制弹出无法退出 | `DeckSyncEventBus` 错误设置了 `replay = 1` 缓存且监听器绑定在局部 `home_pager`；当从 `CameraScreen` 回退时触发重新订阅，`SharedFlow` 重放旧事件导致图片界面被无限拉起 | 将 `MutableSharedFlow` 调整为 `replay = 0, extraBufferCapacity = 64`，将事件监听提升至 `AppNavigation` 根级全局单次消费 |
| **插件成熟度与自动同步控制** | 插件充满测试痕迹，缺少自动上传接管开关且存在大量 Emoji 符号 | 缺乏正式版场景化状态分流，截图自动上传为硬编码行为无法手动启闭 | 1. 引入 `ToggleField` 开关（`auto_upload_enabled`，默认 ON）；<br>2. 区分已配对卡片视图与未配对向导视图，支持一键解绑；<br>3. 彻底移除所有 Emoji 字符，采用标准 Material/Fa 图标；<br>4. 优化诊断面板为按需折叠。 |

---

## 5. 操作与部署指令

### 5.1 重新编译并部署 Steam Deck 插件
```bash
# 1. 编译前端产物
cd decky-plugin
npm run build

# 2. 远程部署至 Steam Deck 并重启 Decky 服务
cd ..
python scripts/deploy_to_deck.py
```

### 5.2 编译 Android 安装包
```bash
# 运行单元测试
./gradlew testDebugUnitTest

# 编译 Debug APK
./gradlew assembleDebug
# 生成产物路径: app/build/outputs/apk/debug/app-debug.apk

# 编译 Release APK
./gradlew assembleRelease
# 生成产物路径: app/build/outputs/apk/release/app-release.apk
```

---

## 6. Steam Deck 使用方式与按键配置

### 6.1 游戏内快捷键（自动监听模式）
* **系统默认快捷键**：按下 **`Steam + R1`** 触发 Steam 原生截屏。
* **手柄背键映射**：
  1. 打开 Steam 游戏内控制器设置（Controller Settings）。
  2. 将背键（如 **L4** 或 **R4**）分配为 **Take Screenshot**。
* **数据流向**：Steam 客户端将彩色截图保存至 `~/.local/share/Steam/userdata/<USER_ID>/760/remote/<APP_ID>/screenshots/*.jpg`。若开启了“自动上传游戏截图”（默认开启），插件文件监听器在 0.2 秒内检测到新文件并自动推送到手机；若关闭该选项，则仅在本地保存，不推送到手机。

### 6.2 侧边栏实时抓取（Portal 模式）
* 按 `...` 键打开 Quick Access Menu 侧边栏中的 **YomiDeck** 插件。
* 点击 **`Capture & Sync Current Screen (捕获当前画面并同步)`**。
* 前端收起侧边栏，后端通过 DBus 调用 `xdg-desktop-portal-gamescope` 抓取当帧 1280x800 全彩无损画面（PNG 格式），并在 0.5 秒内自动通过 HTTP POST 发送至手机端触发解析。

