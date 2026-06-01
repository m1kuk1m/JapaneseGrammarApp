# Protect domain models used by Gson from being obfuscated
-keep class com.example.japanesegrammarapp.domain.model.** { *; }

# Protect network models (OpenAiResponse, GeminiResponse, etc.)
-keep class com.example.japanesegrammarapp.network.** { *; }

# Protect Retrofit interfaces
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# MUST keep generic signatures for Retrofit's suspend functions to work properly
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes Exceptions
-keepattributes *Annotation*

# Keep Continuation class for Retrofit suspend functions
-keep class kotlin.coroutines.Continuation

# Protect Room entities (just in case)
-keep class com.example.japanesegrammarapp.data.** { *; }

# Coil and Coroutines usually have their own bundled proguard rules
