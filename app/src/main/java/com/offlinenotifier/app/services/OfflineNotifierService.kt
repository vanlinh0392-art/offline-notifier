package com.offlinenotifier.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.offlinenotifier.app.R
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.utils.NetworkStateHelper
import com.offlinenotifier.app.workers.WorkManagerHelper
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OfflineNotifierService : Service() {

    companion object {
        const val CHANNEL_ID = "OfflineNotifierForegroundServiceChannel"
        const val NOTIFICATION_ID = 1001

        var isRunning = false
        private val _isScreenOff = MutableStateFlow(false)
        val isScreenOff: StateFlow<Boolean> = _isScreenOff.asStateFlow()
        var lastScreenOffTime: Long = 0L
    }

    private val tag = "OfflineNotifierSvc"

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    _isScreenOff.value = true
                    lastScreenOffTime = System.currentTimeMillis()
                    updateNotification(isMonitoring = true)
                }

                Intent.ACTION_SCREEN_ON -> {
                    _isScreenOff.value = false
                    updateNotification(isMonitoring = false)
                }
            }
        }
    }

    private var emailLoopJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private var wifiNetworkCallback: ConnectivityManager.NetworkCallback? = null
    private val isStoppingForWifi = AtomicBoolean(false)

    private fun startEmailLoop() {
        emailLoopJob?.cancel()
        emailLoopJob = serviceScope.launch {
            var lastSentTime = 0L
            val repository = SettingsRepository(applicationContext)

            while (isActive) {
                val index = repository.intervalIndexFlow.first()
                val custom = repository.customIntervalFlow.first()

                val intervalMins = when (index) {
                    0 -> 1L
                    1 -> 15L
                    2 -> 30L
                    3 -> 60L
                    4 -> custom.toLong().coerceAtLeast(1L)
                    else -> 15L
                }
                val intervalMillis = intervalMins * 60 * 1000

                // Reuse the existing 30s loop as a fallback so we do not add extra wakeups.
                delay(30000L)

                if (stopForWifiIfNeeded(repository)) {
                    return@launch
                }

                val isDeviceOffline = com.offlinenotifier.app.utils.DeviceStateHelper.isDeviceLockedOrScreenOff(applicationContext)
                if (isDeviceOffline) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSentTime >= intervalMillis) {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val pendingCount = db.eventDao().getAllEvents().size
                        if (pendingCount > 0) {
                            WorkManagerHelper.triggerImmediateSend(applicationContext, isTest = false)
                            lastSentTime = currentTime
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        _isScreenOff.value = com.offlinenotifier.app.utils.DeviceStateHelper.isDeviceLockedOrScreenOff(this)
        if (_isScreenOff.value) {
            lastScreenOffTime = System.currentTimeMillis()
        }

        isRunning = true
        com.offlinenotifier.app.utils.WatchdogManager.startWatchdog(this)
        com.offlinenotifier.app.utils.LauncherIconHelper.setActiveIcon(this, true)
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenStateReceiver, filter)
        refreshWifiAutoStopMonitoring()
        startEmailLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshWifiAutoStopMonitoring()
        val notification = buildNotification(isMonitoring = _isScreenOff.value)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun buildNotification(isMonitoring: Boolean): Notification {
        val icon = if (isMonitoring) R.drawable.ic_notif_active else R.drawable.ic_notif_inactive
        val title = if (isMonitoring) "Dang giam sat" else "Dich vu cho"
        val text = if (isMonitoring) {
            "Ghi nhan thong bao khi may tat man hinh"
        } else {
            "Cho man hinh tat de bat dau giam sat"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setColor(if (isMonitoring) 0xFF4CAF50.toInt() else 0xFF9E9E9E.toInt())
            .setColorized(isMonitoring)
            .setNumber(if (isMonitoring) 1 else 0)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(isMonitoring: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(isMonitoring))
    }

    private fun refreshWifiAutoStopMonitoring() {
        serviceScope.launch {
            val enabled = SettingsRepository(applicationContext).autoStopOnWifiFlow.first()
            if (enabled) {
                registerWifiNetworkCallbackIfNeeded()
                stopForWifiIfNeeded()
            } else {
                unregisterWifiNetworkCallback()
                isStoppingForWifi.set(false)
            }
        }
    }

    private fun registerWifiNetworkCallbackIfNeeded() {
        if (wifiNetworkCallback != null) return

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handleWifiConnectedAutoStop()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    handleWifiConnectedAutoStop()
                }
            }
        }

        try {
            // Default-network callback is more reliable for "current active network became Wi-Fi"
            // than watching any Wi-Fi transport and then re-checking activeNetwork separately.
            connectivityManager.registerDefaultNetworkCallback(callback)
            wifiNetworkCallback = callback
        } catch (e: Exception) {
            Log.w(tag, "Failed to register default network callback", e)
            wifiNetworkCallback = null
        }
    }

    private fun unregisterWifiNetworkCallback() {
        val callback = wifiNetworkCallback ?: return
        try {
            connectivityManager.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            Log.w(tag, "Failed to unregister network callback", e)
        } finally {
            wifiNetworkCallback = null
        }
    }

    private fun handleWifiConnectedAutoStop() {
        serviceScope.launch {
            stopForWifiIfNeeded()
        }
    }

    private suspend fun stopForWifiIfNeeded(
        repository: SettingsRepository = SettingsRepository(applicationContext)
    ): Boolean {
        val shouldAutoStop = repository.autoStopOnWifiFlow.first()
        val isMonitoringEnabled = repository.isActiveFlow.first()

        if (!shouldAutoStop || !isMonitoringEnabled || !NetworkStateHelper.isWifiConnected(applicationContext)) {
            return false
        }

        if (!isStoppingForWifi.compareAndSet(false, true)) {
            return true
        }

        return try {
            Log.d(tag, "Wi-Fi detected while monitoring is active. Auto-stopping service.")
            repository.saveIsActive(false)
            com.offlinenotifier.app.utils.LauncherIconHelper.setActiveIcon(
                applicationContext,
                false,
                force = true
            )
            val db = AppDatabase.getDatabase(applicationContext)
            val events = db.eventDao().getAllEvents()
            events.forEach { event ->
                com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(event.imagePath)
                com.offlinenotifier.app.utils.MediaExtractorHelper.deleteMediaFile(event.audioPath)
            }
            db.eventDao().clearAll()
            WorkManagerHelper.stopEmailWorker(applicationContext)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    applicationContext,
                    "Da tu tat giam sat vi dang ket noi Wi-Fi",
                    Toast.LENGTH_LONG
                ).show()
            }
            stopSelf()
            true
        } catch (e: Exception) {
            isStoppingForWifi.set(false)
            Log.e(tag, "Failed to auto-stop monitoring on Wi-Fi", e)
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        emailLoopJob?.cancel()
        unregisterWifiNetworkCallback()
        serviceScope.cancel()
        isRunning = false
        _isScreenOff.value = false
        com.offlinenotifier.app.utils.WatchdogManager.stopWatchdog(this)
        unregisterReceiver(screenStateReceiver)
        com.offlinenotifier.app.utils.LauncherIconHelper.setActiveIcon(this, false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Offline Notifier - Giam sat",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Dang giam sat thong bao khi man hinh tat"
                setShowBadge(true)
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
