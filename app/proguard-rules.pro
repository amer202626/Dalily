# Jetpack Compose rules
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Kotlinx Serialization Keep original model and data signatures intact
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    *** Companion;
}
-keep class com.yemenservices.app.data.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# Firebase Keep rule signatures for Firestore/Auth/Storage reflection
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Retrofit, OkHttp and Okio keep
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coil images loading
-keep class coil.** { *; }
-dontwarn coil.**
