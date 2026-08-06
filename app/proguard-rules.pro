# Keep Play Billing
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }

# AdMob / Play Services
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.games.** { *; }
-dontwarn com.google.android.gms.**

# Kotlin / coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
