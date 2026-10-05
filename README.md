# InstAttPlugin — InstAtt Auto Sign-in Xposed Module

> Language: **English** · [简体中文](README.zh-CN.md)

An Xposed / LSPosed module that hooks the InstAtt attendance app (`instatt.instatt`) on a rooted Android device to sign in automatically: it detects unlocked classes, auto-taps the sign-in button, forges the Wi-Fi BSSID the venue expects, and forces the client's `ignoreWifi` flag. It is the on-device counterpart to the server-side [AutoSign](https://github.com/JokerEzreal/AutoSign) service.

> **⚠️ For study and security research only.** This hooks a school attendance app to submit attendance without actually being present. Do not use it for real sign-in or anything that violates school rules, the attendance system's terms, or local law. See the [disclaimer](#disclaimer).

> The Android module/package is still `com.example.myxposed` ("MyXposed"); only the repository is named InstAttPlugin.

## What it does

The entry class is `com.example.myxposed.MainHook` (`IXposedHookLoadPackage`). It installs five hooks on `instatt.instatt`:

| Hook | Target method | Effect |
|---|---|---|
| Startup | `Application.attach(Context)` | Shows a Toast confirming the module is active |
| Force ignoreWifi | `LecturerHomeFragment.signAttendance(...)` | Sets `GlobalStatic.ignoreWifi = true` before sign-in, skipping the client's Wi-Fi pre-check |
| Auto-click | `LecturerHomeAdapter.onBindViewHolder(...)` | Detects an unlocked, not-yet-signed class and auto-taps the sign-in button |
| Forge BSSID | `WifiConnectionReceiver.updateConnectedWifi(...)` | Replaces the reported BSSID with the venue's expected value from a built-in `VENUE_BSSID_MAP` |
| Sign-in request | `LecturerHomeFragment.takeAttendanceTask(...)` | Ensures the forged BSSID is carried into the sign-in request |

The classroom→BSSID table (`VENUE_BSSID_MAP`) is hard-coded in `MainHook.java` and mirrors the BSSIDs surveyed for the server-side service. The upstream location check is server-side, but it compares the BSSID the client reports; forging that value is enough to pass it. The full reasoning is in the [AutoSign](https://github.com/JokerEzreal/AutoSign) security section.

## Build and install

### 1. Add the Xposed API jar

Place `XposedBridgeApi-82.jar` under:

```
app/libs/XposedBridgeApi-82.jar
```

It is referenced as `compileOnly`; the running Xposed framework provides the real implementation.

### 2. Build

Open the project root in Android Studio and let Gradle sync, or run:

```
./gradlew assembleDebug
```

Install the resulting APK on a device that already has an Xposed framework (LSPosed / EdXposed).

### 3. Activate the module

1. Open the Xposed manager (e.g. LSPosed).
2. Enable this module in the module list.
3. Set its scope to `instatt.instatt`.
4. Force-stop and relaunch the target app (or reboot).

A startup Toast confirms the hook loaded.

## Project layout

```
.
├── app/
│   ├── libs/
│   │   └── XposedBridgeApi-82.jar      (add manually)
│   ├── src/main/
│   │   ├── assets/xposed_init          (Xposed entry-point config)
│   │   ├── java/com/example/myxposed/
│   │   │   └── MainHook.java           (all five hooks)
│   │   ├── res/values/strings.xml
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Requirements and notes

- Root plus an Xposed framework (LSPosed recommended, Android 8.0+).
- Target package: `instatt.instatt`.
- More detail: `FEATURES.md` (hook-by-hook walkthrough), `USAGE_GUIDE.md`, `TEST_GUIDE.md`, `SETUP_GUIDE.txt`.

## Related project

- [AutoSign](https://github.com/JokerEzreal/AutoSign) — the server-side auto sign-in service (Rust + React), plus the full reverse-engineering and security write-up this module is based on.

## Disclaimer

This module is for security research, protocol analysis, and study only. Using it means submitting attendance to a school's system without being present, which conflicts with school rules and may get the account penalized; the risk and responsibility rest with the user. The author and this repository accept no liability for any resulting consequences.

**Infringement and takedown contact**: if anything here infringes your rights or raises other concerns, email [fs840594947@gmail.com](mailto:fs840594947@gmail.com) and it will be handled promptly (removed or taken down).
