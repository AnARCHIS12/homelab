# Proguard rules for Homelab Android

# Kotlin Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# AndroidX Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class * extends dagger.hilt.internal.ComponentEntryPoint
-keep class * implements dagger.hilt.internal.ComponentEntryPoint
-keep class * implements dagger.hilt.internal.GeneratedComponent
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes Exceptions

# Keep application and data models
-keep class com.homelab.app.data.local.entity.** { *; }
-keep class com.homelab.app.data.remote.dto.** { *; }
-keep class com.homelab.app.domain.model.** { *; }
-keep class com.homelab.app.util.ServiceType { *; }
-keep class com.homelab.app.util.AppIconOption { *; }
