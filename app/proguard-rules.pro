# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers and source file attributes for actionable crash reporting
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,Deprecated,AnnotationDefault,*Annotation*

# Use R8-specific optimizations or rename source files to save space
-renamesourcefileattribute SourceFile

# --- Kotlin Coroutines & Flow ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- Retrofit ---
# Retain the service interfaces and their methods since they are accessed via reflection dynamic proxies
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# --- Moshi (JSON Serialization) ---
# Prevent Moshi adapters and JSON-reflective model fields from being pruned or renamed
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class * {
    @com.squareup.moshi.JsonClass *;
}
-keep class * extends com.squareup.moshi.JsonAdapter
-dontwarn com.squareup.moshi.**

# --- Gson (Alternative Serialization) ---
-keepattributes Signature,*Annotation*,EnclosingMethod,InnerClasses
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# --- Room Database ---
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomOpenHelper

# --- Keep our internal data models from being obfuscated (critical for Moshi codegen & Room) ---
-keep class com.example.data.model.** { *; }

# --- Firebase & Play Services Proguard Rules ---
# Keep all Firebase classes and their members to prevent R8 from stripping them
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Keep FirebaseInitProvider and associated classes
-keep class com.google.firebase.provider.FirebaseInitProvider { *; }
-keep class com.google.firebase.components.** { *; }
-dontwarn com.google.firebase.components.**

# Keep Google Play Services and messaging classes
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Firebase Messaging Specific Rules
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**
-keep class com.google.firebase.iid.** { *; }
-dontwarn com.google.firebase.iid.**


