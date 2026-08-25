package com.example.japanesegrammarapp

import android.app.Application
import com.example.japanesegrammarapp.domain.ApplicationScope
import com.example.japanesegrammarapp.utils.AppLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@HiltAndroidApp
class YomiLLMApplication : Application() {
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this, applicationScope)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var resumedActivities = 0
            override fun onActivityResumed(activity: android.app.Activity) {
                resumedActivities++
                isAppInForeground = resumedActivities > 0
            }
            override fun onActivityPaused(activity: android.app.Activity) {
                resumedActivities--
                isAppInForeground = resumedActivities > 0
            }
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                AppLogger.logCrashSync(throwable)
            } catch (e: Exception) {
                // Ignore exception to prevent infinite loop
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
