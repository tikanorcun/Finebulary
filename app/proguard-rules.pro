# Add project specific ProGuard rules here.
# You can specify default rules using 'proguard-android-optimize.txt' in file() of build.gradle.kts.
# For details see:
# http://developer.android.com/tools/app/build.gradle.kts

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Retrofit & Gson
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class com.language.finebulary.data.api.** { *; }
-keep class com.language.finebulary.data.model.** { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# WorkManager
-keep class androidx.work.** { *; }

# Coil
-dontwarn coil.**
