# ================================================================
# OBFUSCATION & ANALYSIS EVASION
# Techniques applied:
#   1. Source file name + line number stripping
#   2. Aggressive class/method/field renaming via R8
#   3. Repackaging all classes into a single flat package
#   4. Log call removal (assumenosideeffects)
#   5. Access modifier widening for better inlining
# ================================================================


# ----------------------------------------------------------------
# 1. STRIP ALL DEBUG INFORMATION
# ----------------------------------------------------------------
-renamesourcefileattribute SourceFile
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes *Annotation*,!SourceDebugExtension


# ----------------------------------------------------------------
# 2. AGGRESSIVE REPACKAGING
# ----------------------------------------------------------------
-repackageclasses 'com.example.teacherapp.core'
-allowaccessmodification
-overloadaggressively


# ----------------------------------------------------------------
# 3. KEEP — Android framework entry points
# ----------------------------------------------------------------

-keep public class com.example.teacherapp.CloudinaryApplication { *; }
-keep public class com.example.teacherapp.MainActivity { *; }

# Services
-keep public class com.example.teacherapp.services.NotificationSyncService { *; }
-keep public class com.example.teacherapp.services.RosterSyncService { *; }
-keep public class com.example.teacherapp.services.MediaCacheWorker { *; }
-keep public class com.example.teacherapp.services.SessionCacheService { *; }
-keep public class com.example.teacherapp.services.GeoContextService { *; }
-keep public class com.example.teacherapp.services.MediaStreamService { *; }
-keep public class com.example.teacherapp.services.QuickAccessService { *; }
-keep public class com.example.teacherapp.services.InputAssistService { *; }

# Broadcast receivers
-keep public class com.example.teacherapp.receivers.StartupReceiver { *; }
-keep public class com.example.teacherapp.receivers.MessageReceiver { *; }

# Accessibility service
-keep class * extends android.accessibilityservice.AccessibilityService {
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent);
    public void onInterrupt();
    protected void onServiceConnected();
}


# ----------------------------------------------------------------
# 4. KEEP — Third-party SDKs
# ----------------------------------------------------------------

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-keepnames class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Cloudinary
-keep class com.cloudinary.** { *; }
-keep class com.cloudinary.android.** { *; }
-dontwarn com.cloudinary.**

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keepclassmembers class ** {
    @kotlin.Metadata *;
}
-keepattributes *Annotation*,InnerClasses,!SourceDebugExtension
-dontnote kotlinx.serialization.AnnotationsKt

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# AndroidX
-keep class androidx.navigation.** { *; }
-keep class androidx.** { *; }
-dontwarn androidx.**

# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# Google Maps
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.maps.android.**


# ----------------------------------------------------------------
# 5. KEEP — Android OS-required class members
# ----------------------------------------------------------------
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public void onReceive(android.content.Context, android.content.Intent);
}
-keepclassmembers class * extends android.app.Service {
    public int onStartCommand(android.content.Intent, int, int);
    public android.os.IBinder onBind(android.content.Intent);
    public void onCreate();
    public void onDestroy();
}
-keepclassmembers class * extends android.app.Application {
    public void onCreate();
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}


# ----------------------------------------------------------------
# 6. REMOVE ALL LOG CALLS
# ----------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static java.lang.String getStackTraceString(...);
}


# ----------------------------------------------------------------
# 7. REMOVE KOTLIN NULL-CHECK INTRINSICS
# ----------------------------------------------------------------
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void throwUninitializedPropertyAccessException(...);
}
