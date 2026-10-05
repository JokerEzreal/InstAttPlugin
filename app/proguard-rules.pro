# Add project specific ProGuard rules here.
# Xposed模块不要混淆
-keep class com.example.myxposed.** { *; }
-keep class de.robv.android.xposed.** { *; }
