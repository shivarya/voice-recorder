# Minification is disabled in the release build (see build.gradle.kts), so these
# rules are only relevant if you later flip isMinifyEnabled = true.

# Google API client / Drive rely on reflection over model classes.
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.drive.** { *; }
-keepclassmembers class * {
    @com.google.api.client.util.Key <fields>;
}
-dontwarn com.google.api.client.**
-dontwarn org.apache.http.**
-dontwarn javax.naming.**
