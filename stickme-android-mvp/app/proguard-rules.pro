# StickMe release shrink rules.
# Keep Android IME service and FileProvider entry points stable for signed releases.

-keep class com.stickme.app.keyboard.StickMeKeyboardService { *; }
-keep class androidx.core.content.FileProvider { *; }

# Keep Compose runtime metadata safe through release optimization.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
