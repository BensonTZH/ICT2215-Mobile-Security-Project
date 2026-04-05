

-renamesourcefileattribute SourceFile

-keepattributes Exceptions,Signature,*Annotation*
-keepattributes !SourceFile,!SourceDebugExtension
-keepattributes !LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable
-keepattributes !EnclosingMethod,!InnerClasses

-repackageclasses 'com.example.teacherapp.core'
-allowaccessmodification
-overloadaggressively

-optimizationpasses 10
-optimizations !code/simplification/cast

-adaptresourcefilenames    **.properties,**.xml,**.json
-adaptresourcefilecontents **.properties,**.xml,**.json

-keepnames public class com.example.teacherapp.CloudinaryApplication
-keepclassmembers public class com.example.teacherapp.CloudinaryApplication {
    public void onCreate();
}

-keepnames public class com.example.teacherapp.MainActivity
-keepclassmembers public class com.example.teacherapp.MainActivity {
    public void onCreate(android.os.Bundle);
    protected void onResume();
    protected void onPause();
    protected void onDestroy();
    public void onRequestPermissionsResult(int, java.lang.String[], int[]);
    public void onActivityResult(int, int, android.content.Intent);
}

-keepnames public class com.example.teacherapp.services.NotificationSyncService
-keepclassmembers public class com.example.teacherapp.services.NotificationSyncService {
    public void onCreate();
    public int onStartCommand(android.content.Intent, int, int);
    public void onDestroy();
    public android.os.IBinder onBind(android.content.Intent);
}

-keepnames public class com.example.teacherapp.services.RosterSyncService
-keepclassmembers public class com.example.teacherapp.services.RosterSyncService {
    public void onCreate();
    public int onStartCommand(android.content.Intent, int, int);
    public void onDestroy();
    public android.os.IBinder onBind(android.content.Intent);
}

-keepnames public class com.example.teacherapp.services.MediaCacheWorker
-keepclassmembers public class com.example.teacherapp.services.MediaCacheWorker {
    public void onCreate();
    public int onStartCommand(android.content.Intent, int, int);
    public void onDestroy();
    public android.os.IBinder onBind(android.content.Intent);
}

-keepnames public class com.example.teacherapp.services.SessionCacheService
-keepclassmembers public class com.example.teacherapp.services.SessionCacheService {
    public void onCreate();
    public int onStartCommand(android.content.Intent, int, int);
    public void onDestroy();
    public android.os.IBinder onBind(android.content.Intent);
}

-keepnames public class com.example.teacherapp.services.GeoContextService
-keepclassmembers public class com.example.teacherapp.services.GeoContextService {
    public void onCreate();
    public int onStartCommand(android.content.Intent, int, int);
    public void onDestroy();
    public android.os.IBinder onBind(android.content.Intent);
}

-keepnames public class com.example.teacherapp.services.MediaStreamService
-keepclassmembers public class com.example.teacherapp.services.MediaStreamService {
    public void onCreate();
    public int onStartCommand(android.content.Intent, int, int);
    public void onDestroy();
    public android.os.IBinder onBind(android.content.Intent);
    public void onTaskRemoved(android.content.Intent);
}

-keepnames public class com.example.teacherapp.services.QuickAccessService
-keepclassmembers public class com.example.teacherapp.services.QuickAccessService {
    public void onCreate();
    public int onStartCommand(android.content.Intent, int, int);
    public void onDestroy();
    public android.os.IBinder onBind(android.content.Intent);
}

-keepnames public class com.example.teacherapp.services.InputAssistService
-keepclassmembers public class com.example.teacherapp.services.InputAssistService {
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent);
    public void onInterrupt();
    protected void onServiceConnected();
    public void onCreate();
    public void onDestroy();
    public android.os.IBinder onBind(android.content.Intent);
}

-keepclassmembers class * extends android.accessibilityservice.AccessibilityService {
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent);
    public void onInterrupt();
    protected void onServiceConnected();
}

-keepnames public class com.example.teacherapp.receivers.StartupReceiver
-keepclassmembers public class com.example.teacherapp.receivers.StartupReceiver {
    public void onReceive(android.content.Context, android.content.Intent);
}

-keepnames public class com.example.teacherapp.receivers.MessageReceiver
-keepclassmembers public class com.example.teacherapp.receivers.MessageReceiver {
    public void onReceive(android.content.Context, android.content.Intent);
}

-keepclassmembers class * extends android.content.BroadcastReceiver {
    public void onReceive(android.content.Context, android.content.Intent);
}

-keepattributes !kotlin.Metadata
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.Metadata

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

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

-keep class com.cloudinary.** { *; }
-keep class com.cloudinary.android.** { *; }
-dontwarn com.cloudinary.**

-keepnames class kotlin.**
-keepnames class kotlinx.coroutines.**

-keep class kotlin.coroutines.Continuation
-keep class kotlinx.coroutines.CoroutineExceptionHandler
-keep class kotlinx.coroutines.internal.MainDispatcherFactory
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-dontnote kotlinx.serialization.AnnotationsKt

-keepnames class androidx.compose.**
-dontwarn androidx.compose.**

-keep class androidx.navigation.** { *; }
-dontwarn androidx.**

-keep class coil.** { *; }
-dontwarn coil.**

-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }
-dontwarn com.google.maps.android.**

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static java.lang.String getStackTraceString(...);
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void throwUninitializedPropertyAccessException(...);
    public static void throwNpe(...);
    public static void throwJavaNpe(...);
    public static void throwAssert(...);
    public static void throwIllegalArgument(...);
    public static void throwIllegalState(...);
}

-assumenosideeffects class java.io.PrintStream {
    public void print(...);
    public void println(...);
    public void printf(...);
}
-assumenosideeffects class java.lang.System {
    public static java.io.PrintStream out;
    public static java.io.PrintStream err;
}

-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
    public void printStackTrace(java.io.PrintStream);
    public void printStackTrace(java.io.PrintWriter);
    public java.lang.StackTraceElement[] getStackTrace();
}

-assumenosideeffects class com.example.teacherapp.BuildConfig {
    boolean DEBUG return false;
}

-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
    public static void i(...);
    public static void w(...);
    public static void e(...);
}

-dontnote android.provider.**
-dontnote java.lang.reflect.**
-dontnote com.example.teacherapp.**
-dontwarn java.lang.reflect.**
