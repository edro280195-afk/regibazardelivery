# ═══════════════════════════════════════════════════════════════════
#  RegiBazar – ProGuard / R8 Rules
#  Protege las clases que usan reflexión en runtime:
#  Gson/Retrofit data models, Retrofit interfaces, OkHttp, SignalR.
# ═══════════════════════════════════════════════════════════════════

# ── Stack traces legibles en producción ──────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ───────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { <methods>; }
-dontwarn kotlin.**

# ── Kotlin Coroutines ─────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── Gson – Data Models (todos los que usan @SerializedName) ──────
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.example.regibazarapp.data.models.** { *; }

# ── Retrofit ─────────────────────────────────────────────────────
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface com.example.regibazarapp.data.network.** { *; }
-dontwarn retrofit2.**
-dontwarn okio.**

# ── OkHttp ───────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── SignalR (Microsoft) ───────────────────────────────────────────
-keep class com.microsoft.signalr.** { *; }
-dontwarn com.microsoft.signalr.**
-keep class io.reactivex.rxjava3.** { *; }
-dontwarn io.reactivex.rxjava3.**

# ── Google Maps / Play Services ───────────────────────────────────
-keep class com.google.android.gms.** { *; }
-keep class com.google.maps.** { *; }
-dontwarn com.google.android.gms.**

# ── Android DataStore ─────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Jetpack Compose (R8 compatible) ───────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── ViewModel / Lifecycle ─────────────────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.** { *; }

# ── Security Crypto (EncryptedSharedPreferences / Keystore) ──────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ── Coil image loading ────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── Speech & TTS (Android system) ────────────────────────────────
-keep class android.speech.** { *; }
-keep class android.speech.tts.** { *; }

# ── Enums usados en estado UI ─────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Serializable (para Parcelables si se agregan) ─────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
