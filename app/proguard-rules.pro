-keepattributes *Annotation*
-keep class com.example.browser.data.model.** { *; }
-keep class com.example.browser.data.local.entity.** { *; }
-keep class com.example.browser.data.local.dao.** { *; }
-keep class com.example.browser.data.local.BrowserDatabase { *; }
-keep class com.example.browser.manager.** { *; }
-keep class com.example.browser.ui.viewmodel.** { *; }
-keep class com.example.browser.player.** { *; }

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# GeckoView
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**
