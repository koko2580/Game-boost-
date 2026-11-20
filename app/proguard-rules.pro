# R8 Obfuscation & Release Hardening for Turbo X
# Note: Do NOT use -repackageclasses; moving AndroidX/Room classes out of their original
# packages breaks Class.forName(pkg + ".WorkDatabase_Impl") inside WorkManagerInitializer.
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod

# Strip debug/verbose/info logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep Manifest Entrypoints (Application, Activities, Services)
-keep public class com.example.TurboGameApplication { *; }
-keep public class com.example.MainActivity { *; }
-keep public class com.example.service.** { *; }

# Keep ViewModels instantiated via reflection
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep AndroidX App Startup, WorkManager & Room Database (used transitively by AdMob SDK)
-keep class androidx.startup.** { *; }
-keep class * implements androidx.startup.Initializer {
    public <init>();
}
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.WorkerFactory { *; }
-keep class * extends androidx.work.InputMerger {
    public <init>();
}
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-keep class androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.model.** { *; }
-keep class androidx.sqlite.** { *; }

# Keep Lifecycle observers & ProcessLifecycleOwner
-keep class * implements androidx.lifecycle.DefaultLifecycleObserver { *; }
-keep class * implements androidx.lifecycle.LifecycleObserver { *; }

# Keep AdMob SDK, UMP consent & Google Play Services integration
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep DataStore & Proto runtime internals
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep Kotlin Coroutines service loaders
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Prevent stripping of security guard constants & reflection targets
-keepclassmembers class com.example.security.AppSecurityGuard { *; }
