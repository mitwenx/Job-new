# ============================================================
# JobAlerts — Production ProGuard / R8 Rules
# ============================================================

# ==================== Retrofit & OkHttp ====================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ==================== Gson ====================
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ==================== Room ====================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ==================== Kotlin Coroutines ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ==================== WorkManager ====================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ==================== App Models (never obfuscate) ====================
-keep class com.jobalerts.app.domain.models.** { *; }
-keep class com.jobalerts.app.data.local.** { *; }

# ==================== Suppress common warnings ====================
-dontwarn java.lang.invoke.*
-dontwarn **$$Lambda$*
-dontwarn kotlin.reflect.jvm.internal.**
