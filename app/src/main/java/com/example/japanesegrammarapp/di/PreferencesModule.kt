package com.example.japanesegrammarapp.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StandardPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SecurePrefs

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    @StandardPrefs
    fun provideStandardPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @SecurePrefs
    fun provideSecurePreferences(@ApplicationContext context: Context): SharedPreferences {
        // Wrap in a lazy delegate so the heavy Keystore + AES initialization happens
        // on the first actual read/write call (which is always on an IO thread via
        // SettingsRepositoryImpl), NOT on the main thread during Hilt graph construction.
        val lazyPrefs = LazySharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "api_keys_secure",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        // Eagerly trigger the factory on an IO thread RIGHT NOW, so EncryptedSharedPreferences
        // (Keystore + AES256_GCM, 200~500ms) is fully initialized before the user can
        // navigate to Settings. LazySharedPreferences uses double-checked locking, so
        // this warmup thread and any later caller safely share the same singleton instance.
        Thread({
            try { lazyPrefs.getString("__warmup__", null) } catch (_: Exception) {}
        }, "prefs-warmup").also { it.isDaemon = true; it.start() }

        return lazyPrefs
    }
}

/**
 * A [SharedPreferences] proxy that defers the expensive [factory] call to the first
 * actual operation. Thread-safe via double-checked locking.
 */
private class LazySharedPreferences(
    private val factory: () -> SharedPreferences
) : SharedPreferences {

    @Volatile private var delegate: SharedPreferences? = null

    private fun prefs(): SharedPreferences =
        delegate ?: synchronized(this) {
            delegate ?: factory().also { delegate = it }
        }

    override fun getAll(): MutableMap<String, *> = prefs().all
    override fun getString(key: String, defValue: String?) = prefs().getString(key, defValue)
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = prefs().getStringSet(key, defValues)
    override fun getInt(key: String, defValue: Int) = prefs().getInt(key, defValue)
    override fun getLong(key: String, defValue: Long) = prefs().getLong(key, defValue)
    override fun getFloat(key: String, defValue: Float) = prefs().getFloat(key, defValue)
    override fun getBoolean(key: String, defValue: Boolean) = prefs().getBoolean(key, defValue)
    override fun contains(key: String) = prefs().contains(key)
    override fun edit(): SharedPreferences.Editor = prefs().edit()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs().registerOnSharedPreferenceChangeListener(listener)
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs().unregisterOnSharedPreferenceChangeListener(listener)
}
