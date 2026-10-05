# 🎯 MyXposed 自动签到模块 - 使用指南

## ✨ 功能概述

这是一个完整的InstAtt自动签到Xposed模块，实现了以下核心功能：

### 核心功能
1. ✅ **自动检测解锁课程** - 实时监控课程状态
2. ✅ **自动点击签到按钮** - 无需手动操作
3. ✅ **伪造WiFi BSSID** - 根据教室名称自动匹配
4. ✅ **绕过WiFi验证** - 强制设置 `ignoreWifi=true`
5. ✅ **详细日志记录** - 完整的Hook过程记录

---

## 🔧 工作原理

### 完整签到流程

```
应用启动
    ↓
显示提示: "🎯 自动签到模块已激活"
    ↓
监控课程列表（onBindViewHolder）
    ↓
检测到解锁课程？
    ├─ NO → 继续监控
    └─ YES → 继续
         ↓
    获取教室名称（如F1A24）
         ↓
    自动点击签到按钮（延迟500ms）
         ↓
    显示Toast: "🎯 正在自动签到: F1A24"
         ↓
    强制设置 ignoreWifi=true
         ↓
    伪造WiFi信息：
      - SSID: EDUROAM
      - BSSID: a0:0f:37:e0:3c:2c
         ↓
    提交签到请求到服务器
         ↓
    等待服务器响应
         ↓
    签到成功 ✅
```

---

## 📋 5个Hook点详解

### Hook 1: 应用启动监控
**目标**: `Application.attach(Context)`
**功能**:
- 获取应用Context
- 显示模块激活提示
- 初始化全局变量

**日志输出**:
```
MyXposed: 应用已启动，Context已获取
```

---

### Hook 2: 绕过WiFi验证
**目标**: `LecturerHomeFragment.signAttendance(MClass)`
**功能**:
- 强制设置 `GlobalStatic.ignoreWifi = true`
- 绕过WiFi启用检查
- 绕过位置服务检查
- 绕过WiFi连接检查
- 绕过SSID白名单验证

**日志输出**:
```
MyXposed: 已设置 ignoreWifi=true，教室: F1A24
```

**效果对比**:
| 检查项 | 正常流程 | Hook后 |
|--------|---------|--------|
| WiFi是否启用 | ✅ 必须 | ⏭️ 跳过 |
| 位置是否启用 | ✅ 必须 | ⏭️ 跳过 |
| 是否连接WiFi | ✅ 必须 | ⏭️ 跳过 |
| SSID白名单 | ✅ 必须 | ⏭️ 跳过 |

---

### Hook 3: 自动点击签到按钮
**目标**: `LecturerHomeAdapter.onBindViewHolder(ViewHolder, int)`
**功能**:
- 监控课程列表的每一项
- 检测课程锁状态 (`lock == LOCK_UNLOCKED`)
- 检测是否已签到 (`isAttended == false`)
- 自动点击 `sign_box` 视图
- 避免重复点击（使用 `processedClasses` Set）

**检测逻辑**:
```java
if (lockStatus == unlockValue && !isAttended) {
    // 课程已解锁且未签到
    signBoxView.performClick();  // 自动点击
}
```

**日志输出**:
```
MyXposed: 发现解锁课程！
  - 课程ID: MODULE123_20250124_0900
  - 教室: F1A24
  - 锁状态: 1 (解锁值: 1)
MyXposed: 自动点击签到按钮
```

**Toast提示**:
```
🎯 正在自动签到: F1A24
```

---

### Hook 4: 伪造WiFi BSSID (方法1)
**目标**: `WifiConnectionReceiver.updateConnectedWifi(Context)`
**功能**:
- 拦截WiFi信息更新
- 根据 `currentVenue` 查找对应BSSID
- 修改 `GlobalStatic.connectedWifi` 对象

**伪造数据**:
```java
connectedWifi.setSsid("EDUROAM");
connectedWifi.setBssid("a0:0f:37:e0:3c:2c");  // F1A24教室的BSSID
```

**日志输出**:
```
MyXposed: 已伪造WiFi信息
  - 教室: F1A24
  - BSSID: a0:0f:37:e0:3c:2c
  - SSID: EDUROAM
```

---

### Hook 5: 拦截签到请求 (方法2，双重保险)
**目标**: `LecturerHomeFragment.takeAttendanceTask(MClass, CustomWifi)`
**功能**:
- 在签到请求提交前最后一次检查
- 再次根据教室名称伪造BSSID
- 确保发送到服务器的BSSID正确
- 打印完整请求参数

**日志输出**:
```
MyXposed: takeAttendanceTask - 已设置BSSID
  - 教室: F1A24
  - BSSID: a0:0f:37:e0:3c:2c
MyXposed: 即将提交签到请求
  - SSID: EDUROAM
  - BSSID: a0:0f:37:e0:3c:2c
MyXposed: 签到请求已提交，等待服务器响应...
```

---

## 🗺️ 教室BSSID映射配置

### 当前配置
```java
VENUE_BSSID_MAP.put("F1A24", "a0:0f:37:e0:3c:2c");
```

### 添加更多教室
在 `MainHook.java` 的第49行后添加：

```java
static {
    // 已配置的教室
    VENUE_BSSID_MAP.put("F1A24", "a0:0f:37:e0:3c:2c");

    // 添加新教室（示例）
    VENUE_BSSID_MAP.put("F1B10", "aa:bb:cc:dd:ee:ff");
    VENUE_BSSID_MAP.put("C1L1", "11:22:33:44:55:66");
    VENUE_BSSID_MAP.put("D2A15", "12:34:56:78:9a:bc");

    // 注意：教室名称会自动转换为大写
    // "f1a24" 和 "F1A24" 都会匹配到同一个BSSID
}
```

### 获取真实教室BSSID的方法

1. **方法1**: 在真实教室内连接WiFi后查看
```bash
# Android终端
adb shell
dumpsys wifi | grep -i bssid
```

2. **方法2**: 使用WiFi分析APP
   - 下载 "WiFi Analyzer" 应用
   - 在教室内查看连接的WiFi详情
   - 记录BSSID (MAC地址)

3. **方法3**: 通过Xposed日志获取
   - 先不配置BSSID映射
   - 在教室内手动签到一次
   - 查看Xposed日志中的真实BSSID

---

## 📱 完整使用流程

### 步骤1: 安装准备
```bash
1. 将 XposedBridgeApi-82.jar 放到 MyXposed/app/libs/
2. 在Android Studio中打开项目
3. 等待Gradle同步完成
```

### 步骤2: 编译APK
```bash
# 使用Android Studio
Build -> Build Bundle(s) / APK(s) -> Build APK(s)

# 或使用命令行
gradlew assembleDebug
```

### 步骤3: 安装模块
```bash
1. 将APK安装到手机（需要root + LSPosed）
2. 打开LSPosed管理器
3. 进入"模块"页面
4. 勾选"MyXposed"启用
5. 点击模块，设置作用域为"instatt.instatt"
6. 保存并重启应用
```

### 步骤4: 测试验证
```bash
1. 完全关闭InstAtt应用
2. 重新打开InstAtt
3. 查看启动Toast: "🎯 自动签到模块已激活"
4. 等待教师解锁课程
5. 观察自动签到过程
```

### 步骤5: 查看日志
```bash
# 使用LSPosed日志查看器
1. 打开LSPosed管理器
2. 进入"日志"页面
3. 筛选"MyXposed"标签

# 或使用adb logcat
adb logcat | grep "MyXposed"
```

---

## 📊 完整日志示例

```log
========================================
MyXposed: 开始Hook InstAtt应用
========================================
MyXposed: Hook ignoreWifi 成功
MyXposed: Hook 自动点击签到 成功
MyXposed: Hook WiFi BSSID 成功
MyXposed: Hook takeAttendance 成功
MyXposed: 所有Hook设置完成
MyXposed: 应用已启动，Context已获取

--- 等待课程解锁 ---

MyXposed: 发现解锁课程！
  - 课程ID: COMP4088_20250124_0900
  - 教室: F1A24
  - 锁状态: 1 (解锁值: 1)
MyXposed: 自动点击签到按钮
MyXposed: 已设置 ignoreWifi=true，教室: F1A24
MyXposed: 已伪造WiFi信息
  - 教室: F1A24
  - BSSID: a0:0f:37:e0:3c:2c
  - SSID: EDUROAM
MyXposed: takeAttendanceTask - 已设置BSSID
  - 教室: F1A24
  - BSSID: a0:0f:37:e0:3c:2c
MyXposed: 即将提交签到请求
  - SSID: EDUROAM
  - BSSID: a0:0f:37:e0:3c:2c
MyXposed: 签到请求已提交，等待服务器响应...
```

---

## ⚠️ 重要注意事项

### 1. 避免重复点击
模块使用 `processedClasses` Set记录已处理的课程ID，避免重复自动点击。

### 2. 延迟点击机制
自动点击延迟500ms执行，确保UI已完全加载。

### 3. 教室名称大小写
教室名称会自动转换为大写进行匹配：
```java
"f1a24" → "F1A24"
"F1a24" → "F1A24"
```

### 4. 未配置教室
如果教室未在 `VENUE_BSSID_MAP` 中配置，日志会显示：
```
MyXposed: 教室 XXX 未配置BSSID映射
```

### 5. 双重BSSID伪造
模块在两个地方伪造BSSID，确保万无一失：
- Hook 4: `updateConnectedWifi` (第一道防线)
- Hook 5: `takeAttendanceTask` (第二道防线)

---

## 🐛 故障排除

### 问题1: Toast没有显示
**原因**: 模块未激活或作用域设置错误
**解决**:
1. 检查LSPosed中模块是否勾选
2. 确认作用域包含 `instatt.instatt`
3. 完全关闭并重启应用

### 问题2: 没有自动点击
**原因**: 课程未解锁或Hook失败
**解决**:
1. 查看Xposed日志是否有"发现解锁课程"
2. 确认课程锁状态是否为unlocked
3. 检查日志中是否有Hook失败错误

### 问题3: 签到失败（403错误）
**原因**: BSSID配置错误或服务器端仍在验证
**解决**:
1. 在真实教室内查看实际BSSID
2. 更新 `VENUE_BSSID_MAP` 中的配置
3. 重新编译安装模块

### 问题4: 日志中显示"未配置BSSID映射"
**原因**: 教室名称不在映射表中
**解决**:
1. 在日志中查看实际教室名称
2. 添加到 `VENUE_BSSID_MAP`
3. 重新编译安装

---

## 🎓 学习建议

### 扩展功能思路

1. **支持多教室自动选择**
   - 根据GPS位置自动判断当前教室
   - 动态选择对应BSSID

2. **签到结果监控**
   - Hook `processTakeAttendanceResponse`
   - 显示签到成功/失败通知

3. **远程配置BSSID**
   - 从云端获取教室BSSID映射
   - 无需重新编译即可更新配置

4. **签到历史记录**
   - 记录每次自动签到的时间
   - 生成签到统计报告

5. **条件签到**
   - 根据课程名称过滤
   - 仅对指定课程自动签到

---

## 📚 相关资源

- **Xposed官方文档**: https://api.xposed.info/
- **LSPosed项目**: https://github.com/LSPosed/LSPosed
- **XposedBridge API**: https://github.com/rovo89/XposedBridge

---

## ⚖️ 免责声明

**本项目仅供学习和研究Xposed框架使用。**

- ⚠️ 请勿用于实际签到作弊
- ⚠️ 使用本模块可能违反学校规定
- ⚠️ 后果自负，开发者不承担任何责任
- ⚠️ 请尊重课堂纪律，认真出勤

---

**祝你学习愉快！** 🎉
