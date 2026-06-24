# ========== APKHistory ProGuard 规则 ==========

# ---- 数据模型（Room 实体 + 数据类） ----
-keep class com.apkhistory.downloader.data.model.** { *; }
-keepclassmembers class com.apkhistory.downloader.data.model.** {
    <fields>;
    <methods>;
}

# ---- Room ----
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract <methods>;
}

# ---- Jsoup（HTML 解析） ----
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ---- OkHttp ----
-keep class okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- Coil（图片加载） ----
-keep class coil.** { *; }
-dontwarn coil.**

# ---- Kotlin 协程 ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ---- Kotlin 反射 / Compose ----
-keepattributes *Annotation*, Signature, EnclosingMethod, InnerClasses
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @compose.runtime.Stable <fields>;
}

# ---- 移除日志（Release 优化） ----
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
