# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# -----------------------------------------------------------------------
# Jetpack Compose
# -----------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# -----------------------------------------------------------------------
# Dagger Hilt
# -----------------------------------------------------------------------
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.AndroidEntryPoint class *
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.HiltAndroidApp class *

# -----------------------------------------------------------------------
# Kotlinx Serialization
# -----------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepnames @kotlinx.serialization.Serializable class *
-keepclassmembers @kotlinx.serialization.Serializable class * {
    <init>(...);
    <fields>;
}
# -----------------------------------------------------------------------
# Kotlin Coroutines
# -----------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile