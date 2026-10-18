# Proguard rules for CHECKLIST
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep serialization companions and serializers
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * implements kotlinx.serialization.internal.GeneratedSerializer {
    <fields>;
    <methods>;
}

# Keep app data models
-keep class com.example.checklist.data.** { *; }
