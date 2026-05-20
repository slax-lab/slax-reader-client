# ---- Kotlinx Serialization ----
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.slax.reader.**$$serializer { *; }
-keepclassmembers class app.slax.reader.** {
    *** Companion;
}
-keepclasseswithmembers class app.slax.reader.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---- Ktor ----
-dontwarn io.ktor.**

# ---- PowerSync ----
-dontwarn com.powersync.**

# ---- Okio ----
-dontwarn okio.**

# ---- Compose ----
-dontwarn androidx.compose.**
