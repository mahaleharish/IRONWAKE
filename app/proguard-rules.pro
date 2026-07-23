# Add project specific ProGuard rules here.
# Keep Room entities and data models
-keep class com.example.data.model.** { *; }
-keep class com.example.data.database.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
