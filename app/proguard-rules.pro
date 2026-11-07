# Project-specific ProGuard/R8 rules for play-my-music

# Preserve annotations and attributes
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,Exceptions

# Gson reflection rules for data models and settings
-keep class de.kindermaenner.playmymusic.data.model.** { *; }
-keep class de.kindermaenner.playmymusic.settings.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# JCIFS-NG rules (SMB client)
-keep class jcifs.** { *; }
-dontwarn jcifs.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**

# ML Kit Barcode scanning
-keep class com.google.mlkit.vision.barcode.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.mlkit.common.** { *; }
-dontwarn com.google.mlkit.**
