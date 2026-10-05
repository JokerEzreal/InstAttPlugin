# InstAttPlugin — InstAtt 自动签到 Xposed 模块

> 语言:[English](README.md) · **简体中文**

一个 Xposed / LSPosed 模块,在已 root 的安卓设备上 hook InstAtt 考勤应用(`instatt.instatt`)实现自动签到:检测课程解锁、自动点击签到按钮、伪造教室对应的 Wi-Fi BSSID,并强制客户端的 `ignoreWifi` 开关。它是服务端方案 [AutoSign](https://github.com/JokerEzreal/AutoSign) 的「端侧」对应实现。

> **⚠️ 仅供学习与安全研究。** 本模块 hook 学校考勤应用,在人未到场的情况下提交出勤。请勿用于真实签到,或任何违反校规、考勤系统条款与当地法律的用途。详见文末[免责声明](#免责声明)。

> 安卓模块/包名仍为 `com.example.myxposed`(「MyXposed」);仅仓库改名为 InstAttPlugin。

## 功能

入口类是 `com.example.myxposed.MainHook`(`IXposedHookLoadPackage`),对 `instatt.instatt` 安装五个 hook:

| Hook | 目标方法 | 效果 |
|---|---|---|
| 启动提示 | `Application.attach(Context)` | 弹 Toast,确认模块已激活 |
| 强制 ignoreWifi | `LecturerHomeFragment.signAttendance(...)` | 签到前把 `GlobalStatic.ignoreWifi` 设为 `true`,跳过客户端 Wi-Fi 预检 |
| 自动点击 | `LecturerHomeAdapter.onBindViewHolder(...)` | 检测到「已解锁且未签」的课程后自动点击签到按钮 |
| 伪造 BSSID | `WifiConnectionReceiver.updateConnectedWifi(...)` | 用内置 `VENUE_BSSID_MAP` 里该教室应有的值替换上报的 BSSID |
| 签到请求 | `LecturerHomeFragment.takeAttendanceTask(...)` | 确保伪造后的 BSSID 被带进签到请求 |

教室→BSSID 映射表(`VENUE_BSSID_MAP`)硬编码在 `MainHook.java` 中,与服务端采集到的 BSSID 一致。上游的位置校验在服务端,但它比对的是客户端自报的 BSSID,因此伪造这个值即可通过。完整原理见 [AutoSign](https://github.com/JokerEzreal/AutoSign) 的安全分析一节。

## 编译与安装

### 1. 放入 Xposed API jar

把 `XposedBridgeApi-82.jar` 放到:

```
app/libs/XposedBridgeApi-82.jar
```

它以 `compileOnly` 方式引用,真正的实现由运行时的 Xposed 框架提供。

### 2. 编译

在 Android Studio 打开项目根目录等待 Gradle 同步,或执行:

```
./gradlew assembleDebug
```

把生成的 APK 安装到已装有 Xposed 框架(LSPosed / EdXposed)的设备上。

### 3. 激活模块

1. 打开 Xposed 管理器(如 LSPosed)。
2. 在模块列表里启用本模块。
3. 作用域勾选 `instatt.instatt`。
4. 强制停止并重新打开目标应用(或重启)。

启动时弹出的 Toast 表示 hook 已加载。

## 项目结构

```
.
├── app/
│   ├── libs/
│   │   └── XposedBridgeApi-82.jar      (需手动添加)
│   ├── src/main/
│   │   ├── assets/xposed_init          (Xposed 入口配置)
│   │   ├── java/com/example/myxposed/
│   │   │   └── MainHook.java           (五个 hook 都在这里)
│   │   ├── res/values/strings.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 要求与说明

- 需要 root + Xposed 框架(推荐 LSPosed,支持 Android 8.0+)。
- 目标应用包名:`instatt.instatt`。
- 更多细节:`FEATURES.md`(逐 hook 讲解)、`USAGE_GUIDE.md`、`TEST_GUIDE.md`、`SETUP_GUIDE.txt`。

## 相关项目

- [AutoSign](https://github.com/JokerEzreal/AutoSign) —— 服务端自动签到服务(Rust + React),以及本模块所依据的完整逆向与安全分析。

## 免责声明

本模块仅用于安全研究、协议分析与学习交流。使用它意味着在人未到场的情况下向学校考勤系统提交出勤,与校规冲突,相关账号可能被处理,风险与责任由使用者自负。作者与本仓库不对由此产生的任何后果负责。

**侵权与下架联系**:若本仓库内容涉及侵权或其他权益问题,请邮件联系 [fs840594947@gmail.com](mailto:fs840594947@gmail.com),我会尽快处理(删除或下架相关内容)。
