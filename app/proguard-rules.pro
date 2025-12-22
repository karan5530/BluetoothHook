# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================
# BluetoothHook Xposed Module ProGuard Rules
# ============================================================

# ========== Xposed Framework ==========
# Keep all Xposed API classes and their methods
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }

# Keep the main hook entry point
-keep class com.jingyu233.bluetoothhook.hook.HookEntry {
    public void handleLoadPackage(...);
}

# Keep all hook classes and their methods (accessed via reflection)
-keep class com.jingyu233.bluetoothhook.hook.** { *; }

# ========== Room Database ==========
# Keep Room entities
-keep @androidx.room.Entity class * { *; }
-keep class com.jingyu233.bluetoothhook.data.model.VirtualDevice { *; }

# Keep Room DAOs
-keep interface com.jingyu233.bluetoothhook.data.local.VirtualDeviceDao { *; }

# Keep Room Database implementation
-keep class com.jingyu233.bluetoothhook.data.local.VirtualDeviceDatabase { *; }
-keep class com.jingyu233.bluetoothhook.data.local.VirtualDeviceDatabase_Impl { *; }

# Keep Room TypeConverters
-keep class com.jingyu233.bluetoothhook.data.model.DynamicDataRuleListConverter { *; }

# ========== Kotlin Serialization ==========
# Keep serialized data classes
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
}

# Keep serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

# Keep Kotlin serialization classes
-keep class kotlinx.serialization.** { *; }
-keep interface kotlinx.serialization.** { *; }

# Keep specific serialized models
-keep class com.jingyu233.bluetoothhook.data.model.VirtualDevice { *; }
-keep class com.jingyu233.bluetoothhook.data.model.DynamicDataRule { *; }
-keep class com.jingyu233.bluetoothhook.data.model.RuleType { *; }
-keep class com.jingyu233.bluetoothhook.data.model.AppSettings { *; }
-keep class com.jingyu233.bluetoothhook.hook.VirtualDeviceData { *; }

# ========== Reflection-Accessed Classes ==========
# Keep ScanResultBuilder (uses Android reflection)
-keep class com.jingyu233.bluetoothhook.hook.ScanResultBuilder {
    public <methods>;
}

# Keep VirtualDeviceInjector (accesses system classes via reflection)
-keep class com.jingyu233.bluetoothhook.hook.VirtualDeviceInjector {
    public <methods>;
}

# Keep DynamicDataEngine (reflection-based rule application)
-keep class com.jingyu233.bluetoothhook.engine.DynamicDataEngine {
    public <methods>;
}

# ========== Android Components ==========
# Keep all Activities
-keep class * extends android.app.Activity { *; }
-keep class com.jingyu233.bluetoothhook.MainActivity { *; }

# Keep Application class
-keep class * extends android.app.Application { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class com.jingyu233.bluetoothhook.ui.viewmodel.** { *; }

# ========== Jetpack Compose ==========
# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep Compose Navigation routes
-keep class com.jingyu233.bluetoothhook.ui.navigation.Screen { *; }

# ========== WebDAV (Sardine Library) ==========
# Keep Sardine WebDAV client classes
-keep class com.github.sardine.** { *; }
-dontwarn com.github.sardine.**

# Keep HTTP client classes used by Sardine
-dontwarn org.apache.http.**
-keep class org.apache.http.** { *; }

# ========== Kotlin Coroutines ==========
# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ========== Data Layer ==========
# Keep ConfigBridge (cross-process communication)
-keep class com.jingyu233.bluetoothhook.data.bridge.ConfigBridge {
    public <methods>;
}

# Keep Repository
-keep class com.jingyu233.bluetoothhook.data.repository.VirtualDeviceRepository {
    public <methods>;
}

# ========== Logging ==========
# Keep Logger utility class
-keep class com.jingyu233.bluetoothhook.utils.Logger { *; }
-keep class com.jingyu233.bluetoothhook.utils.Logger$** { *; }

# ========== General Android ==========
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelables
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========== Optimization Settings ==========
# Don't warn about missing classes from other packages
-dontwarn android.bluetooth.**
-dontwarn com.android.bluetooth.**

# Preserve stack traces for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ========== Additional Safety Rules ==========
# Keep class members accessed via SharedPreferences keys
-keepclassmembers class * {
    @android.content.SharedPreferences$* *;
}

# Keep custom exceptions
-keep class * extends java.lang.Exception { *; }

# Suppress notes about reflection
-dontnote com.jingyu233.bluetoothhook.hook.**
