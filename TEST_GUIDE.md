# 🧪 测试指南 - 新增点击测试功能

## ✨ 新功能说明

现在**点击任何课程的签到按钮**都会显示详细信息Toast，包括：
- 🔒/🔓 锁状态（已锁定/已解锁）
- 📍 教室名称
- ✅/⏳ 签到状态

**无论课程是否解锁，都可以点击查看信息！**

---

## 📱 测试场景演示

### 场景1: 点击锁定的课程 🔒

**操作**: 点击签到按钮（锁状态）

**显示Toast**:
```
🔒 COMP4088
📍 教室: F1A24
🔐 状态: 已锁定 (Locked)
⏳ 未签到
```

**Xposed日志**:
```log
MyXposed: 点击课程信息
  - 课程: COMP4088 - Software Engineering
  - 教室: F1A24
  - 锁状态: 已锁定 (Locked)
  - 签到状态: ⏳ 未签到
```

**行为**:
- ✅ 显示课程信息
- ❌ 不会触发签到（因为课程锁定）

---

### 场景2: 点击解锁的课程 🔓

**操作**: 点击签到按钮（解锁状态）

**第一个Toast（立即显示）**:
```
🔓 COMP4088
📍 教室: F1A24
🔐 状态: 已解锁 (Unlocked)
⏳ 未签到
```

**第二个Toast（1秒后）**:
```
🎯 正在自动签到: F1A24
```

**Xposed日志**:
```log
MyXposed: 点击课程信息
  - 课程: COMP4088 - Software Engineering
  - 教室: F1A24
  - 锁状态: 已解锁 (Unlocked)
  - 签到状态: ⏳ 未签到
MyXposed: 触发自动签到流程
MyXposed: 已触发签到回调
MyXposed: 已设置 ignoreWifi=true，教室: F1A24
MyXposed: 已伪造WiFi信息
  - 教室: F1A24
  - BSSID: a0:0f:37:e0:3c:2c
  - SSID: EDUROAM
```

**行为**:
- ✅ 显示课程信息
- ✅ 1秒后自动触发签到流程
- ✅ 伪造BSSID并提交签到请求

---

### 场景3: 点击已签到的课程 ✅

**操作**: 点击已签到的课程

**显示Toast**:
```
🔓 COMP4088
📍 教室: F1A24
🔐 状态: 已解锁 (Unlocked)
✅ 已签到
```

**行为**:
- ✅ 显示课程信息
- ❌ 不会重复签到（因为已签到）

---

### 场景4: 重复点击同一解锁课程

**操作**: 第二次点击相同的解锁课程

**第一个Toast**:
```
🔓 COMP4088
📍 教室: F1A24
🔐 状态: 已解锁 (Unlocked)
⏳ 未签到
```

**第二个Toast**:
```
⚠️ 该课程已处理过，避免重复签到
```

**行为**:
- ✅ 显示课程信息
- ❌ 不会重复签到（已在processedClasses中）

---

## 🎯 功能对比表

| 课程状态 | 原始APP行为 | Hook后行为 |
|---------|------------|-----------|
| 🔒 锁定 + 未签到 | 点击无反应 | ✅ 显示教室信息Toast |
| 🔓 解锁 + 未签到 | 触发签到流程 | ✅ 显示信息 + 自动签到 |
| 🔓 解锁 + 已签到 | 点击无反应 | ✅ 显示信息（已签到） |
| 🔓 解锁 + 重复点击 | 触发签到流程 | ✅ 显示信息 + 避免重复 |

---

## 🔍 代码逻辑解析

### 新增的点击监听器

```java
signBoxView.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        // 1️⃣ 判断锁状态
        if (lockStatus == unlockValue) {
            emoji = "🔓";
            lockStatusText = "已解锁 (Unlocked)";
        } else if (lockStatus == lockedValue) {
            emoji = "🔒";
            lockStatusText = "已锁定 (Locked)";
        }

        // 2️⃣ 显示详细信息Toast
        String message = emoji + " " + code + "\n" +
                        "📍 教室: " + venue + "\n" +
                        "🔐 状态: " + lockStatusText + "\n" +
                        attendedText;
        Toast.makeText(v.getContext(), message, Toast.LENGTH_LONG).show();

        // 3️⃣ 如果是解锁状态，触发自动签到
        if (lockStatus == unlockValue && !isAttended) {
            // 检查是否已处理过
            if (processedClasses.contains(classId)) {
                Toast.makeText(..., "⚠️ 该课程已处理过", ...).show();
                return;
            }

            processedClasses.add(classId);

            // 延迟1秒后调用签到
            handler.postDelayed(() -> {
                callback.signAttendance(mClass);
                Toast.makeText(..., "🎯 正在自动签到: " + venue, ...).show();
            }, 1000);
        }
    }
});
```

---

## 📋 测试步骤

### 步骤1: 重新编译安装
```bash
1. 在Android Studio中: Build -> Build APK
2. 安装APK到手机
3. 在LSPosed中确认模块已启用
4. 重启InstAtt应用
```

### 步骤2: 查看启动提示
看到Toast: "🎯 自动签到模块已激活"

### 步骤3: 测试锁定课程
```
1. 找到一个🔒锁定状态的课程
2. 点击签到按钮区域
3. 观察Toast显示的教室信息
```

**预期结果**:
```
🔒 COMP4088
📍 教室: F1A24
🔐 状态: 已锁定 (Locked)
⏳ 未签到
```

### 步骤4: 测试解锁课程
```
1. 等待教师解锁课程（或自己解锁测试）
2. 点击签到按钮区域
3. 观察两个Toast:
   - 第一个: 课程信息
   - 第二个: "正在自动签到"
```

### 步骤5: 查看Xposed日志
```bash
# 使用LSPosed日志查看器
LSPosed管理器 -> 日志 -> 筛选"MyXposed"

# 或使用adb
adb logcat | grep "MyXposed"
```

**预期日志内容**:
```log
MyXposed: 点击课程信息
  - 课程: COMP4088 - Software Engineering
  - 教室: F1A24
  - 锁状态: 已解锁 (Unlocked)
  - 签到状态: ⏳ 未签到
MyXposed: 触发自动签到流程
MyXposed: 已设置 ignoreWifi=true，教室: F1A24
MyXposed: takeAttendanceTask - 已设置BSSID
  - 教室: F1A24
  - BSSID: a0:0f:37:e0:3c:2c
MyXposed: 即将提交签到请求
  - SSID: EDUROAM
  - BSSID: a0:0f:37:e0:3c:2c
```

---

## 🎨 Toast显示效果

### 示例1: 锁定课程（F1A24教室）
```
┌─────────────────────────────┐
│  🔒 COMP4088                │
│  📍 教室: F1A24             │
│  🔐 状态: 已锁定 (Locked)   │
│  ⏳ 未签到                  │
└─────────────────────────────┘
```

### 示例2: 解锁课程（F1B10教室）
```
┌─────────────────────────────┐
│  🔓 MATH2001                │
│  📍 教室: F1B10             │
│  🔐 状态: 已解锁 (Unlocked) │
│  ⏳ 未签到                  │
└─────────────────────────────┘

等待1秒...

┌─────────────────────────────┐
│  🎯 正在自动签到: F1B10      │
└─────────────────────────────┘
```

---

## 🐛 常见测试问题

### 问题1: 点击没有任何反应
**可能原因**:
1. Hook未生效
2. sign_box视图未正确获取

**解决方案**:
```bash
# 查看Xposed日志
adb logcat | grep "MyXposed"

# 检查是否有错误信息
# 应该看到: "MyXposed: Hook 自动点击签到 成功"
```

### 问题2: Toast显示"未知状态"
**原因**: 锁状态值不是0或1

**解决方案**:
查看日志中的实际锁状态值，可能需要更新代码中的判断逻辑

### 问题3: 点击解锁课程没有自动签到
**可能原因**:
1. 课程已在processedClasses中
2. 签到回调失败

**解决方案**:
查看日志中是否有"该课程已处理过"或"签到回调失败"的提示

---

## 📊 测试验证清单

- [ ] 启动应用看到"自动签到模块已激活"Toast
- [ ] 点击锁定课程显示教室信息
- [ ] 点击解锁课程显示信息并自动签到
- [ ] 重复点击显示"已处理过"提示
- [ ] Xposed日志正确记录所有操作
- [ ] 签到成功后课程状态变为"已签到"

---

## 💡 调试技巧

### 1. 实时查看日志
```bash
# 开启终端，实时查看日志
adb logcat -c && adb logcat | grep "MyXposed"
```

### 2. 测试不同教室
修改 `VENUE_BSSID_MAP` 添加更多测试教室：
```java
VENUE_BSSID_MAP.put("F1B10", "test:00:00:00:00:01");
VENUE_BSSID_MAP.put("C1L1", "test:00:00:00:00:02");
```

### 3. 清空已处理列表
如果需要重新测试同一课程，重启应用即可清空 `processedClasses`

---

## 🎓 扩展练习

尝试添加以下功能：

1. **显示课程时间**
   - 在Toast中添加课程开始/结束时间

2. **显示教师信息**
   - 如果课程对象有教师字段，显示教师姓名

3. **点击主区域**
   - Hook `main_v` 的点击事件，显示更详细的课程信息

4. **长按功能**
   - 添加长按监听器，执行不同操作

---

**现在你可以点击任何课程测试了！** 🎉

即使课程是锁定状态，也能看到教室名称等信息！
