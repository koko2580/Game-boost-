# R8 Obfuscation & Anti-Reverse-Engineering Hardening for Turbo X
-repackageclasses 'tx'
-allowaccessmodification
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod

# Strip debug/verbose/info logs in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep Manifest Entrypoints
-keep public class com.example.TurboGameApplication { *; }
-keep public class com.example.MainActivity { *; }
-keep public class com.example.service.** { *; }

# Keep ViewModel instantiated via reflection
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Keep AdMob SDK callbacks & Google Play Services integration
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep DataStore & Proto runtime internals
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Prevent stripping of security guard constants & reflection targets
-keepclassmembers class com.example.security.AppSecurityGuard { *; }
