# Add project specific ProGuard rules here.
# By default, the active-set of rules is in .../default-proguard-rules.txt
# None of these are specifically required for a debug build, but let's keep them here.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
