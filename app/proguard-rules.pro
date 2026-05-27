# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Strict optimizations and aggressive obfuscation parameters
-repackageclasses ''
-allowaccessmodification
-optimizationpasses 5

# Remove log entries completely in compiled release so hackers cannot follow operation logs
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# Keep our Room database and retrofit classes from breaking, but obfuscate implementation details
-keep class com.example.data.Category { *; }
-keep class com.example.data.ServiceProvider { *; }
-keep class com.example.data.Admin { *; }
-keep class com.example.data.CategoryDao { *; }
-keep class com.example.data.ServiceProviderDao { *; }
-keep interface com.example.data.SupabaseService { *; }
-keep class * implements com.squareup.moshi.JsonAdapter { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

