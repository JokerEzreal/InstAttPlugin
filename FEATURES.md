# ✨ 功能实现总结

## 🎯 核心功能清单

### ✅ 已实现功能

1. **自动检测解锁课程**
   - 实时监控 `LecturerHomeAdapter.onBindViewHolder`
   - 检测锁状态: `lock == LOCK_UNLOCKED`
   - 检测签到状态: `isAttended == false`

2. **自动点击签到按钮**
   - 延迟500ms自动执行 `signBoxView.performClick()`
   - 避免重复点击（使用 `processedClasses` Set）
   - 显示Toast提示: "🎯 正在自动签到: {教室}"

3. **伪造WiFi BSSID**
   - Hook点1: `WifiConnectionReceiver.updateConnectedWifi`
   - Hook点2: `LecturerHomeFragment.takeAttendanceTask`
   - 根据教室名称自动匹配BSSID
   - 当前配置: `F1A24 → a0:0f:37:e0:3c:2c`

4. **绕过WiFi验证**
   - 强制设置 `GlobalStatic.ignoreWifi = true`
   - 跳过WiFi启用检查
   - 跳过位置服务检查
   - 跳过SSID白名单验证

5. **详细日志记录**
   - 所有Hook点执行状态
   - 课程检测详情
   - WiFi伪造详情
   - 签到请求参数

---

## 📋 代码结构

### MainHook.java (362行)

```
package com.example.myxposed;

├─ 静态变量
│  ├─ PACKAGE_NAME = "instatt.instatt"
│  ├─ VENUE_BSSID_MAP (教室→BSSID映射表)
│  ├─ processedClasses (已处理课程集合)
│  ├─ appContext (全局Context)
│  └─ currentVenue (当前教室名称)
│
├─ handleLoadPackage()
│  ├─ 调用 hookApplicationAttach()
│  ├─ 调用 hookIgnoreWifi()
│  ├─ 调用 hookAutoClickSign()
│  ├─ 调用 hookWifiBssid()
│  └─ 调用 hookTakeAttendance()
│
├─ hookApplicationAttach()
│  └─ Hook: Application.attach(Context)
│     └─ 显示启动Toast
│
├─ hookIgnoreWifi()
│  └─ Hook: LecturerHomeFragment.signAttendance(MClass)
│     └─ 设置 GlobalStatic.ignoreWifi = true
│
├─ hookAutoClickSign()
│  └─ Hook: LecturerHomeAdapter.onBindViewHolder(ViewHolder, int)
│     ├─ 检测课程解锁状态
│     ├─ 获取 sign_box 视图
│     └─ 延迟500ms自动点击
│
├─ hookWifiBssid()
│  └─ Hook: WifiConnectionReceiver.updateConnectedWifi(Context)
│     └─ 伪造 GlobalStatic.connectedWifi
│
└─ hookTakeAttendance()
   └─ Hook: LecturerHomeFragment.takeAttendanceTask(MClass, CustomWifi)
      └─ 再次伪造CustomWifi参数
```

---

## 🔄 完整执行流程

```
应用启动
    ↓
Hook 1: Application.attach
    ↓
显示Toast: "🎯 自动签到模块已激活"
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    等待教师解锁课程...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    ↓
课程列表更新
    ↓
Hook 3: onBindViewHolder (每个列表项)
    ↓
检测: lockStatus == unlockValue && !isAttended?
    ├─ NO → 继续等待
    └─ YES → 继续
         ↓
    记录到 processedClasses (避免重复)
         ↓
    延迟500ms后自动点击 sign_box
         ↓
    显示Toast: "🎯 正在自动签到: F1A24"
         ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    签到按钮被点击
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    ↓
Hook 2: signAttendance(mClass)
    ↓
设置 GlobalStatic.ignoreWifi = true
    ↓
记录 currentVenue = "F1A24"
    ↓
继续执行原方法...
    ↓
调用 WifiConnectionReceiver.manualUpdateConnectedWifi()
    ↓
Hook 4: updateConnectedWifi(context)
    ↓
伪造WiFi信息:
    connectedWifi.setSsid("EDUROAM")
    connectedWifi.setBssid("a0:0f:37:e0:3c:2c")
    ↓
继续执行原方法...
    ↓
检查 ignoreWifi || ssidList.contains(ssid)
    ↓
✅ ignoreWifi=true → 跳过SSID验证
    ↓
调用 takeAttendanceTask(mClass, customWifi)
    ↓
Hook 5: takeAttendanceTask(mClass, customWifi)
    ↓
再次伪造BSSID (双重保险):
    customWifi.setBssid("a0:0f:37:e0:3c:2c")
    customWifi.setSsid("EDUROAM")
    ↓
构建请求参数:
    {
      "moduleID": "...",
      "venue": "F1A24",
      "studentID": "...",
      "MACaddress": "a0:0f:37:e0:3c:2c",  ✅
      "deviceUID": "...",
      ...
    }
    ↓
提交到Firebase Cloud Function: "signAttendance"
    ↓
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    等待服务器响应...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    ↓
服务器返回:
    ├─ 200/202 → "Attendance taken." ✅
    ├─ 403 → "Device is not detected in the room." ❌
    └─ 409 → "Device is not registered." ❌
```

---

## 🎨 关键技术点

### 1. 双重BSSID伪造机制
为什么需要Hook两个地方？

**Hook 4 (updateConnectedWifi)**
- 伪造 `GlobalStatic.connectedWifi`（全局静态变量）
- 影响范围：整个应用
- 触发时机：签到流程中调用 `manualUpdateConnectedWifi()`

**Hook 5 (takeAttendanceTask)**
- 伪造方法参数 `customWifi`（局部变量）
- 影响范围：仅签到请求
- 触发时机：即将提交签到请求前
- **作用**：最后一道防线，确保发送的BSSID正确

### 2. 避免重复点击
```java
// 使用Set记录已处理的课程ID
private static final Set<String> processedClasses = new HashSet<>();

if (processedClasses.contains(classId)) {
    return;  // 已处理过，跳过
}
processedClasses.add(classId);  // 标记为已处理
```

### 3. 延迟点击机制
```java
handler.postDelayed(new Runnable() {
    @Override
    public void run() {
        signBoxView.performClick();
    }
}, 500);  // 延迟500ms，确保UI已准备好
```

### 4. ignoreWifi绕过原理
```java
// 原代码检查
if (GlobalStatic.ssidList.contains(customWifi.getSsid())
    || GlobalStatic.ignoreWifi) {  // ← Hook在这里设置为true

    takeAttendanceTask(...);  // ✅ 直接进入签到
}
```

### 5. 动态教室识别
```java
// 根据课程对象获取教室名称
Object mClass = param.args[0];
currentVenue = (String) XposedHelpers.callMethod(mClass, "getVenue");

// 查找对应BSSID
String fakeBssid = VENUE_BSSID_MAP.get(currentVenue.toUpperCase());
```

---

## 📊 Hook点对比

| Hook点 | 目标类/方法 | 触发时机 | 主要作用 |
|--------|------------|---------|---------|
| Hook 1 | `Application.attach` | 应用启动 | 初始化、显示提示 |
| Hook 2 | `signAttendance` | 点击签到按钮后 | 绕过WiFi验证 |
| Hook 3 | `onBindViewHolder` | 课程列表刷新 | 自动点击签到 |
| Hook 4 | `updateConnectedWifi` | 签到流程中 | 伪造WiFi信息 |
| Hook 5 | `takeAttendanceTask` | 提交请求前 | 确保BSSID正确 |

---

## 🔐 安全性分析

### 客户端绕过
✅ **已绕过**:
- WiFi启用检查
- 位置服务检查
- WiFi连接检查
- SSID白名单验证

### 服务器端验证
❓ **未知**:
- 服务器是否检查 `ignoreWifi` 字段？
- 服务器是否严格验证BSSID？
- 服务器是否有其他防作弊机制？

### 成功率评估
- **高成功率**: 如果服务器也支持 `ignoreWifi` 配置
- **中成功率**: 如果服务器仅验证BSSID匹配（需要正确的BSSID）
- **低成功率**: 如果服务器有额外防护（如GPS验证、设备指纹等）

---

## 🛠️ 配置文件

### 添加新教室BSSID

编辑 `MainHook.java` 第46-54行:

```java
static {
    // F1A24教室（已配置）
    VENUE_BSSID_MAP.put("F1A24", "a0:0f:37:e0:3c:2c");

    // 添加更多教室
    VENUE_BSSID_MAP.put("F1B10", "b1:2c:3d:4e:5f:60");
    VENUE_BSSID_MAP.put("C1L1", "c2:3d:4e:5f:60:71");
}
```

### 修改延迟时间

编辑 `MainHook.java` 第238行:

```java
}, 500);  // 修改这里的数值（单位：毫秒）
```

---

## 📈 日志级别

所有日志使用 `XposedBridge.log()` 输出，可通过以下方式查看：

```bash
# 方法1: LSPosed日志查看器
LSPosed管理器 → 日志 → 筛选"MyXposed"

# 方法2: adb logcat
adb logcat | grep "MyXposed"

# 方法3: 查看详细日志
adb logcat -s Xposed:V | grep "MyXposed"
```

---

## 🎓 代码质量

- ✅ 详细的注释说明
- ✅ 完整的异常处理（try-catch）
- ✅ 清晰的日志输出
- ✅ 模块化的Hook方法
- ✅ 可扩展的配置系统

---

**项目已完成！可以直接使用！** 🎉
