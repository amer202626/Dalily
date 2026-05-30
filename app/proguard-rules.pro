# Proguard rules
-keep class com.yemenservices.app.data.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Firebase rules
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
