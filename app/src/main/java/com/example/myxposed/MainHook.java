package com.example.myxposed;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Xposed模块主Hook类
 * 功能：自动签到，伪造WiFi BSSID
 *
 * 实现功能：
 * 1. 自动检测解锁的课程
 * 2. 自动点击签到按钮
 * 3. 根据教室名称伪造BSSID
 * 4. 绕过WiFi验证
 */
public class MainHook implements IXposedHookLoadPackage {

    // 特征类名 - 用于识别InstAtt应用
    private static final String SIGNATURE_CLASS = "instatt.instatt.main.Fragment.LecturerHomeFragment";

    // 教室与BSSID的映射表
    private static final HashMap<String, String> VENUE_BSSID_MAP = new HashMap<>();

    // 已处理过的课程ID集合，避免重复点击
    private static final Set<String> processedClasses = new HashSet<>();

    // 全局Context
    private static Context appContext = null;

    // 当前正在处理的教室名称
    private static String currentVenue = null;

    static {
        // 配置教室与BSSID的映射关系
        // 格式：教室名称(大写) -> BSSID
        VENUE_BSSID_MAP.put("F3C04", "a0:0f:37:e0:3c:2c");
        VENUE_BSSID_MAP.put("DA08", "14:84:73:40:3d:ec");
        VENUE_BSSID_MAP.put("F4C10", "34:b8:83:56:3e:2c");
        VENUE_BSSID_MAP.put("F1A13", "9c:d5:7d:a5:e2:e3");
        VENUE_BSSID_MAP.put("F3A04", "a0:0f:37:e1:b0:2c");
        VENUE_BSSID_MAP.put("F3A08", "a0:0f:37:e1:b0:2c");
        VENUE_BSSID_MAP.put("BB80", "8c:1e:80:22:e9:2c");
        VENUE_BSSID_MAP.put("F1A02", "a0:0f:37:e0:57:4c");
        VENUE_BSSID_MAP.put("F1A24", "a0:0f:37:e0:57:4c");
        VENUE_BSSID_MAP.put("TCR1", "9c:d5:7d:a5:e4:4a");
        VENUE_BSSID_MAP.put("F3A12", "9c:d5:7d:a5:a8:0c");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 通过检测特征类来判断是否为InstAtt应用（支持多开/共存）
        try {
            Class<?> signatureClass = XposedHelpers.findClass(SIGNATURE_CLASS, lpparam.classLoader);
            if (signatureClass == null) {
                return;  // 不是InstAtt应用，跳过
            }
        } catch (Throwable e) {
            // 找不到特征类，说明不是InstAtt应用
            return;
        }

        XposedBridge.log("========================================");
        XposedBridge.log("MyXposed: 检测到InstAtt应用");
        XposedBridge.log("  - 包名: " + lpparam.packageName);
        XposedBridge.log("  - 进程: " + lpparam.processName);
        XposedBridge.log("========================================");

        // Hook 1: 应用启动
        hookApplicationAttach(lpparam);

        // Hook 2: 强制绕过WiFi验证
        hookIgnoreWifi(lpparam);

        // Hook 3: 自动点击签到按钮
        hookAutoClickSign(lpparam);

        // Hook 4: 伪造WiFi BSSID
        hookWifiBssid(lpparam);

        // Hook 5: 拦截签到请求，确保BSSID正确
        hookTakeAttendance(lpparam);

        // Hook 6: 监听Firebase课程状态变化（后台保活）
        hookFirebaseListener(lpparam);

        XposedBridge.log("MyXposed: 所有Hook设置完成");
    }

    /**
     * Hook 1: 应用启动，显示提示信息
     */
    private void hookApplicationAttach(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(
            Application.class,
            "attach",
            Context.class,
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    appContext = (Context) param.args[0];

                    Handler mainHandler = new Handler(appContext.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(
                                appContext,
                                "🎯 自动签到模块已激活",
                                Toast.LENGTH_LONG
                            ).show();
                        }
                    });

                    XposedBridge.log("MyXposed: 应用已启动，Context已获取");
                }
            }
        );
    }

    /**
     * Hook 2: 强制设置 ignoreWifi = true，绕过WiFi检查
     */
    private void hookIgnoreWifi(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook signAttendance方法，在签到前强制设置ignoreWifi=true
            XposedHelpers.findAndHookMethod(
                "instatt.instatt.main.Fragment.LecturerHomeFragment",
                lpparam.classLoader,
                "signAttendance",
                XposedHelpers.findClass("instatt.instatt.main.Model.MClass", lpparam.classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // 强制设置 ignoreWifi = true
                        Class<?> GlobalStatic = XposedHelpers.findClass(
                            "instatt.instatt.main.Global.GlobalStatic",
                            lpparam.classLoader
                        );
                        XposedHelpers.setStaticBooleanField(GlobalStatic, "ignoreWifi", true);

                        // 获取课程信息
                        Object mClass = param.args[0];
                        currentVenue = (String) XposedHelpers.callMethod(mClass, "getVenue");

                        XposedBridge.log("MyXposed: 已设置 ignoreWifi=true，教室: " + currentVenue);
                    }
                }
            );

            XposedBridge.log("MyXposed: Hook ignoreWifi 成功");
        } catch (Throwable e) {
            XposedBridge.log("MyXposed: Hook ignoreWifi 失败: " + e.getMessage());
        }
    }

    /**
     * Hook 3: 自动检测解锁课程并自动签到
     */
    private void hookAutoClickSign(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook LecturerHomeAdapter的onBindViewHolder方法
            XposedHelpers.findAndHookMethod(
                "instatt.instatt.main.Adapter.LecturerHomeAdapter",
                lpparam.classLoader,
                "onBindViewHolder",
                "androidx.recyclerview.widget.RecyclerView$ViewHolder",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            Object adapter = param.thisObject;
                            int position = (int) param.args[1];

                            // 获取课程列表
                            Object mClasses = XposedHelpers.getObjectField(adapter, "mClasses");
                            Object mClass = XposedHelpers.callMethod(mClasses, "get", position);

                            // 获取课程信息
                            String classId = (String) XposedHelpers.callMethod(mClass, "getId");
                            String venue = (String) XposedHelpers.callMethod(mClass, "getVenue");
                            String code = (String) XposedHelpers.callMethod(mClass, "getCode");

                            // 获取GlobalStatic类
                            Class<?> GlobalStatic = XposedHelpers.findClass(
                                "instatt.instatt.main.Global.GlobalStatic",
                                lpparam.classLoader
                            );

                            // 获取LOCK_UNLOCKED值
                            Object LOCK_UNLOCKED = XposedHelpers.getStaticObjectField(
                                GlobalStatic, "LOCK_UNLOCKED"
                            );
                            int unlockValue = (int) XposedHelpers.callMethod(
                                LOCK_UNLOCKED, "getValue"
                            );

                            // 获取课程的锁状态
                            int lockStatus = (int) XposedHelpers.callMethod(mClass, "getLock");
                            boolean isAttended = (boolean) XposedHelpers.callMethod(
                                mClass, "isAttended"
                            );

                            // 只处理解锁且未签到的课程
                            if (lockStatus == unlockValue && !isAttended) {
                                // 避免重复处理
                                if (processedClasses.contains(classId)) {
                                    return;
                                }

                                processedClasses.add(classId);

                                XposedBridge.log("MyXposed: 发现解锁课程！");
                                XposedBridge.log("  - 课程代码: " + code);
                                XposedBridge.log("  - 教室: " + venue);
                                XposedBridge.log("  - 锁状态: " + lockStatus + " (解锁值: " + unlockValue + ")");

                                // 获取ViewHolder和sign_box视图
                                Object viewHolder = param.args[0];
                                Object sign_box = XposedHelpers.getObjectField(viewHolder, "sign_box");

                                if (sign_box instanceof View) {
                                    final View signBoxView = (View) sign_box;

                                    // 延迟自动点击
                                    Handler handler = new Handler(signBoxView.getContext().getMainLooper());
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                XposedBridge.log("MyXposed: 自动触发签到");

                                                // 直接调用签到回调
                                                Object callback = XposedHelpers.getObjectField(
                                                    adapter, "callback"
                                                );
                                                XposedHelpers.callMethod(
                                                    callback, "signAttendance", mClass
                                                );

                                                Toast.makeText(
                                                    signBoxView.getContext(),
                                                    "🎯 自动签到: " + venue,
                                                    Toast.LENGTH_SHORT
                                                ).show();

                                            } catch (Throwable e) {
                                                XposedBridge.log("MyXposed: 自动签到失败: " + e.getMessage());
                                            }
                                        }
                                    }, 500);  // 延迟500ms
                                }
                            }
                        } catch (Throwable e) {
                            XposedBridge.log("MyXposed: 处理课程列表失败: " + e.getMessage());
                        }
                    }
                }
            );

            XposedBridge.log("MyXposed: Hook 自动签到 成功");
        } catch (Throwable e) {
            XposedBridge.log("MyXposed: Hook 自动签到 失败: " + e.getMessage());
        }
    }

    /**
     * Hook 4: 伪造WiFi BSSID
     */
    private void hookWifiBssid(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook WifiConnectionReceiver.updateConnectedWifi
            XposedHelpers.findAndHookMethod(
                "instatt.instatt.main.Utility.WifiConnectionReceiver",
                lpparam.classLoader,
                "updateConnectedWifi",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (currentVenue == null) {
                            return;
                        }

                        String venueUpper = currentVenue.toUpperCase();
                        String fakeBssid = VENUE_BSSID_MAP.get(venueUpper);

                        if (fakeBssid != null) {
                            // 获取GlobalStatic类
                            Class<?> GlobalStatic = XposedHelpers.findClass(
                                "instatt.instatt.main.Global.GlobalStatic",
                                lpparam.classLoader
                            );

                            // 获取connectedWifi对象
                            Object connectedWifi = XposedHelpers.getStaticObjectField(
                                GlobalStatic, "connectedWifi"
                            );

                            // 伪造BSSID
                            XposedHelpers.callMethod(connectedWifi, "setBssid", fakeBssid);

                            // 设置SSID为允许的WiFi名称（避免某些检查）
                            XposedHelpers.callMethod(connectedWifi, "setSsid", "EDUROAM");

                            XposedBridge.log("MyXposed: 已伪造WiFi信息");
                            XposedBridge.log("  - 教室: " + currentVenue);
                            XposedBridge.log("  - BSSID: " + fakeBssid);
                            XposedBridge.log("  - SSID: EDUROAM");
                        }
                    }
                }
            );

            XposedBridge.log("MyXposed: Hook WiFi BSSID 成功");
        } catch (Throwable e) {
            XposedBridge.log("MyXposed: Hook WiFi BSSID 失败: " + e.getMessage());
        }
    }

    /**
     * Hook 5: 拦截签到请求，确保BSSID正确传递
     */
    private void hookTakeAttendance(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                "instatt.instatt.main.Fragment.LecturerHomeFragment",
                lpparam.classLoader,
                "takeAttendanceTask",
                XposedHelpers.findClass("instatt.instatt.main.Model.MClass", lpparam.classLoader),
                XposedHelpers.findClass("instatt.instatt.main.Model.CustomWifi", lpparam.classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Object mClass = param.args[0];
                        Object customWifi = param.args[1];

                        String venue = (String) XposedHelpers.callMethod(mClass, "getVenue");
                        String venueUpper = venue.toUpperCase();

                        // 根据教室名称伪造BSSID
                        String fakeBssid = VENUE_BSSID_MAP.get(venueUpper);
                        if (fakeBssid != null) {
                            XposedHelpers.callMethod(customWifi, "setBssid", fakeBssid);
                            XposedHelpers.callMethod(customWifi, "setSsid", "EDUROAM");

                            XposedBridge.log("MyXposed: takeAttendanceTask - 已设置BSSID");
                            XposedBridge.log("  - 教室: " + venue);
                            XposedBridge.log("  - BSSID: " + fakeBssid);
                        } else {
                            XposedBridge.log("MyXposed: 教室 " + venue + " 未配置BSSID映射");
                        }

                        // 打印请求参数
                        String bssid = (String) XposedHelpers.callMethod(customWifi, "getBssid");
                        String ssid = (String) XposedHelpers.callMethod(customWifi, "getSsid");
                        XposedBridge.log("MyXposed: 即将提交签到请求");
                        XposedBridge.log("  - SSID: " + ssid);
                        XposedBridge.log("  - BSSID: " + bssid);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("MyXposed: 签到请求已提交，等待服务器响应...");
                    }
                }
            );

            XposedBridge.log("MyXposed: Hook takeAttendance 成功");
        } catch (Throwable e) {
            XposedBridge.log("MyXposed: Hook takeAttendance 失败: " + e.getMessage());
        }
    }

    /**
     * Hook 6: 监听Firebase课程状态变化（后台保活）
     * Hook registerClassListener中的Firebase监听器
     * 当课程状态从locked变为unlocked时，自动触发签到
     * 优势：即使应用在后台或锁屏，Firebase监听器仍然活跃
     */
    private void hookFirebaseListener(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook Firebase监听器的onEvent方法
            XposedHelpers.findAndHookMethod(
                "instatt.instatt.main.Fragment.LecturerHomeFragment$11",
                lpparam.classLoader,
                "onEvent",
                "com.google.firebase.firestore.DocumentSnapshot",
                "com.google.firebase.firestore.FirebaseFirestoreException",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            // 获取错误参数
                            Object firebaseException = param.args[1];
                            if (firebaseException != null) {
                                return;  // 有错误，不处理
                            }

                            // 获取文档快照
                            Object documentSnapshot = param.args[0];
                            if (documentSnapshot == null) {
                                return;
                            }

                            // 检查文档是否存在
                            boolean exists = (boolean) XposedHelpers.callMethod(
                                documentSnapshot, "exists"
                            );
                            if (!exists) {
                                return;
                            }

                            // 获取外部类实例 (LecturerHomeFragment)
                            Object fragmentInstance = XposedHelpers.getObjectField(
                                param.thisObject, "this$0"
                            );

                            // 获取GlobalStatic类
                            Class<?> GlobalStatic = XposedHelpers.findClass(
                                "instatt.instatt.main.Global.GlobalStatic",
                                lpparam.classLoader
                            );

                            // 获取LOCK_UNLOCKED值
                            Object LOCK_UNLOCKED = XposedHelpers.getStaticObjectField(
                                GlobalStatic, "LOCK_UNLOCKED"
                            );
                            int unlockValue = (int) XposedHelpers.callMethod(
                                LOCK_UNLOCKED, "getValue"
                            );

                            // 从文档中读取当前锁状态
                            int newLockStatus;
                            try {
                                Object lockStatusObj = XposedHelpers.callMethod(
                                    documentSnapshot, "get", "lockStatus"
                                );
                                newLockStatus = ((Long) lockStatusObj).intValue();
                            } catch (Exception e) {
                                return;  // 无法读取锁状态
                            }

                            // 检测是否刚刚解锁
                            if (newLockStatus == unlockValue) {
                                // 获取课程索引（从外部类的字段中获取）
                                int courseIndex = XposedHelpers.getIntField(param.thisObject, "val$i");

                                // 获取课程列表
                                Object mClasses = XposedHelpers.getObjectField(
                                    fragmentInstance, "mClasses"
                                );
                                Object mClass = XposedHelpers.callMethod(mClasses, "get", courseIndex);

                                // 检查是否已签到
                                boolean isAttended = (boolean) XposedHelpers.callMethod(
                                    mClass, "isAttended"
                                );

                                if (isAttended) {
                                    return;  // 已签到，跳过
                                }

                                // 获取课程信息
                                String classId = (String) XposedHelpers.callMethod(mClass, "getId");
                                String venue = (String) XposedHelpers.callMethod(mClass, "getVenue");
                                String code = (String) XposedHelpers.callMethod(mClass, "getCode");

                                // 避免重复处理
                                if (processedClasses.contains(classId)) {
                                    return;
                                }

                                processedClasses.add(classId);

                                XposedBridge.log("========================================");
                                XposedBridge.log("MyXposed: 🔥 Firebase监听器检测到课程解锁！");
                                XposedBridge.log("  - 课程代码: " + code);
                                XposedBridge.log("  - 教室: " + venue);
                                XposedBridge.log("  - 触发方式: Firebase实时监听（后台保活）");
                                XposedBridge.log("========================================");

                                // 获取Context
                                Object contextObj = XposedHelpers.callMethod(
                                    fragmentInstance, "getContext"
                                );
                                if (contextObj == null) {
                                    XposedBridge.log("MyXposed: Context为空，无法触发签到");
                                    return;
                                }

                                final Context context = (Context) contextObj;

                                // 延迟触发签到
                                Handler handler = new Handler(context.getMainLooper());
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            // 获取callback对象
                                            Object callback = fragmentInstance;  // Fragment本身实现了callback接口

                                            // 调用signAttendance方法
                                            XposedHelpers.callMethod(
                                                callback, "signAttendance", mClass
                                            );

                                            XposedBridge.log("MyXposed: ✅ Firebase监听器已触发签到");

                                            // 显示Toast
                                            Toast.makeText(
                                                context,
                                                "🔥 后台检测到课程解锁\n自动签到: " + venue,
                                                Toast.LENGTH_LONG
                                            ).show();

                                        } catch (Throwable e) {
                                            XposedBridge.log("MyXposed: Firebase监听器触发签到失败: " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                }, 1000);  // 延迟1秒执行
                            }

                        } catch (Throwable e) {
                            XposedBridge.log("MyXposed: Firebase监听器Hook处理失败: " + e.getMessage());
                        }
                    }
                }
            );

            XposedBridge.log("MyXposed: Hook Firebase监听器 成功（后台保活）");
        } catch (Throwable e) {
            XposedBridge.log("MyXposed: Hook Firebase监听器 失败: " + e.getMessage());
        }
    }
}
