package com.example.japanesegrammarapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.japanesegrammarapp.MainActivity
import com.example.japanesegrammarapp.R
import com.example.japanesegrammarapp.YomiLLMApplication
import com.example.japanesegrammarapp.domain.model.DeckSyncSettings
import com.example.japanesegrammarapp.domain.repository.SettingsRepository
import com.example.japanesegrammarapp.utils.AppLogger
import com.example.japanesegrammarapp.utils.NetworkUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeckSyncForegroundService : Service() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var deckSyncServer: DeckSyncServer? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopDeckSync()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startDeckSync()
            }
        }
        return START_STICKY
    }

    private fun startDeckSync() {
        val settings = settingsRepository.getDeckSyncSettings()
        val port = settings.port
        val ip = NetworkUtils.getLocalIpAddress(this)

        isServiceRunning = true

        val notification = buildForegroundNotification(ip, port)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(SERVICE_NOTIFICATION_ID, notification)
        }

        startServer(port)
        registerMdns(port)

        DeckSyncEventBus.emit(
            DeckSyncEvent.StatusChanged(
                isRunning = true,
                ipAddress = ip,
                port = port,
                isMdnsBroadcasting = true
            )
        )
    }

    private fun startServer(port: Int) {
        if (deckSyncServer != null && deckSyncServer!!.isRunning) return

        deckSyncServer = DeckSyncServer(
            appContext = applicationContext,
            settingsRepository = settingsRepository,
            onScreenshotReceived = { uri ->
                handleIncomingScreenshot(uri)
            }
        )

        serviceScope.launch {
            try {
                deckSyncServer?.start(port)
            } catch (e: Exception) {
                AppLogger.e("DECK_SYNC_SVC", "Failed to start server on port $port", e)
            }
        }
    }

    private fun handleIncomingScreenshot(uri: Uri) {
        AppLogger.d("DECK_SYNC_SVC", "Incoming screenshot received: $uri, app in foreground: ${YomiLLMApplication.isAppInForeground}")

        // Pulse wake lock briefly so CPU remains awake during image processing & notification dispatch
        wakeLock?.acquire(3000)

        // Broadcast through memory event bus
        DeckSyncEventBus.emit(DeckSyncEvent.ScreenshotReceived(uri))

        // If the app is in the background or screen is locked, pop a high-priority Heads-up notification
        if (!YomiLLMApplication.isAppInForeground) {
            showScreenshotHeadsUpNotification(uri)
        }
    }

    private fun showScreenshotHeadsUpNotification(imageUri: Uri) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "camera")
            putExtra("image_uri", imageUri.toString())
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, SCREENSHOT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.deck_sync_notif_title))
            .setContentText(getString(R.string.deck_sync_notif_desc))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(SCREENSHOT_NOTIFICATION_ID, notification)
    }

    private fun registerMdns(port: Int) {
        try {
            nsdManager = getSystemService(Context.NSD_SERVICE) as? NsdManager
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = DeckSyncSettings.MDNS_SERVICE_NAME
                serviceType = DeckSyncSettings.MDNS_SERVICE_TYPE
                setPort(port)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAttribute("version", "1.10.0")
                    setAttribute("app", "YomiLLM")
                }
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(serviceInfo: NsdServiceInfo?) {
                    AppLogger.d("DECK_SYNC_SVC", "mDNS service registered: ${serviceInfo?.serviceName}")
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    AppLogger.e("DECK_SYNC_SVC", "mDNS registration failed with code: $errorCode")
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo?) {
                    AppLogger.d("DECK_SYNC_SVC", "mDNS service unregistered")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    AppLogger.e("DECK_SYNC_SVC", "mDNS unregistration failed with code: $errorCode")
                }
            }

            nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            AppLogger.e("DECK_SYNC_SVC", "Error registering mDNS service", e)
        }
    }

    private fun stopDeckSync() {
        isServiceRunning = false
        try {
            deckSyncServer?.stop()
            deckSyncServer = null
        } catch (e: Exception) {
            AppLogger.e("DECK_SYNC_SVC", "Error stopping server", e)
        }

        try {
            registrationListener?.let { nsdManager?.unregisterService(it) }
            registrationListener = null
        } catch (e: Exception) {
            AppLogger.e("DECK_SYNC_SVC", "Error unregistering mDNS", e)
        }

        DeckSyncEventBus.emit(
            DeckSyncEvent.StatusChanged(
                isRunning = false,
                ipAddress = "",
                port = DeckSyncSettings.DEFAULT_PORT,
                isMdnsBroadcasting = false
            )
        )
    }

    private fun acquireLocks() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("YomiLLM_DeckSync_Multicast")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YomiLLM:DeckSyncWakeLock")
        } catch (e: Exception) {
            AppLogger.e("DECK_SYNC_SVC", "Error acquiring network/wake locks", e)
        }
    }

    private fun releaseLocks() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            AppLogger.e("DECK_SYNC_SVC", "Error releasing locks", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val serviceChannel = NotificationChannel(
                SERVICE_NOTIFICATION_CHANNEL_ID,
                getString(R.string.deck_sync_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.deck_sync_service_channel_desc)
                setShowBadge(false)
            }

            val screenshotChannel = NotificationChannel(
                SCREENSHOT_NOTIFICATION_CHANNEL_ID,
                getString(R.string.deck_sync_screenshot_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.deck_sync_screenshot_channel_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(screenshotChannel)
        }
    }

    private fun buildForegroundNotification(ip: String, port: Int): Notification {
        val stopIntent = Intent(this, DeckSyncForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SERVICE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.deck_sync_service_running_title))
            .setContentText(getString(R.string.deck_sync_service_running_desc, ip, port))
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.deck_sync_stop_service),
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopDeckSync()
        releaseLocks()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.japanesegrammarapp.action.START_DECK_SYNC"
        const val ACTION_STOP = "com.example.japanesegrammarapp.action.STOP_DECK_SYNC"

        const val SERVICE_NOTIFICATION_CHANNEL_ID = "deck_sync_service_channel"
        const val SCREENSHOT_NOTIFICATION_CHANNEL_ID = "deck_sync_screenshot_channel"

        const val SERVICE_NOTIFICATION_ID = 8848
        const val SCREENSHOT_NOTIFICATION_ID = 8849

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, DeckSyncForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DeckSyncForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
