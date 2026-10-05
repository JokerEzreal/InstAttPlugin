# MyXposed - InstAtt Hook模块

这是一个用于学习Xposed框架的示例项目，用于Hook InstAtt签到应用。

## 功能
- 在InstAtt应用启动时显示Toast "Hello World"

## 使用步骤

### 1. 准备工作
将 `XposedBridgeApi-82.jar` 文件复制到以下目录：
```
MyXposed/app/libs/XposedBridgeApi-82.jar
```

### 2. 在Android Studio中打开项目
1. 打开Android Studio
2. 选择 File -> Open
3. 选择 `MyXposed` 目录
4. 等待Gradle同步完成

### 3. 编译安装
1. 连接手机（需要已安装Xposed框架，如LSPosed、EdXposed等）
2. 点击 Run 按钮或执行 `./gradlew assembleDebug`
3. 安装生成的APK

### 4. 激活模块
1. 打开Xposed管理器（如LSPosed）
2. 在模块列表中找到"MyXposed"
3. 勾选启用
4. 在作用域中选择"instatt.instatt"
5. 重启目标应用或系统

### 5. 测试
启动InstAtt应用，如果看到"Hello World - Xposed Hook 成功!"的Toast提示，说明Hook成功！

## 项目结构
```
MyXposed/
├── app/
│   ├── libs/
│   │   └── XposedBridgeApi-82.jar  (需要手动添加)
│   ├── src/
│   │   └── main/
│   │       ├── assets/
│   │       │   └── xposed_init     (Xposed入口配置)
│   │       ├── java/
│   │       │   └── com/example/myxposed/
│   │       │       └── MainHook.java  (主Hook类)
│   │       ├── res/
│   │       │   └── values/
│   │       │       └── strings.xml
│   │       └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 注意事项
1. 需要root权限和Xposed框架
2. 推荐使用LSPosed（支持Android 8.0+）
3. 目标应用包名：`instatt.instatt`
4. Hook点：`Application.attach(Context)`方法

## 下一步学习
成功运行后，你可以尝试：
1. Hook签到验证逻辑
2. 修改WiFi SSID/BSSID检查
3. 绕过位置权限检查
4. 修改服务器响应

## 相关文件说明
- `xposed_init`: Xposed模块入口点配置文件
- `MainHook.java`: 主Hook逻辑实现
- `AndroidManifest.xml`: 包含Xposed模块的元数据配置
