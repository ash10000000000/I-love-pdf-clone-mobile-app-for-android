# Add project specific ProGuard rules here.
# PDFBox
-keep class com.tom_roush.pdfbox.** { *; }
-keep class org.apache.harmony.awt.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.jempbox.** { *; }
-keep class com.tom_roush.xmpbox.** { *; }

# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.openxmlformats.schemas.** { *; }
-keep class schemasMicrosoftCom.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Zip4j
-keep class net.lingala.zip4j.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
