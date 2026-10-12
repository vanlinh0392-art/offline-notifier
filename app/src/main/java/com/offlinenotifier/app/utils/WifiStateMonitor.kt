package com.offlinenotifier.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.shift.ShiftScheduleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WifiStateMonitor {
    private const val TAG = "WifiStateMonitor"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRegistered = false

    fun startMonitoring(context: Context) {
        if (isRegistered) return
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Wi-Fi Connected onAvailable")
                handleWifiChanged(appContext, isConnected = true)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Wi-Fi Disconnected onLost (switched to Mobile Data or Offline)")
                handleWifiChanged(appContext, isConnected = false)
            }
        }

        try {
            cm.registerNetworkCallback(request, callback)
            isRegistered = true
            Log.d(TAG, "WifiStateMonitor registered successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register WifiStateMonitor callback", e)
        }
    }

    private fun handleWifiChanged(context: Context, isConnected: Boolean) {
        scope.launch {
            try {
                val repo = SettingsRepository(context)
                val isScheduleEnabled = repo.scheduleEnabledFlow.first()
                val isAutoStopOnWifi = repo.autoStopOnWifiFlow.first()

                Log.d(TAG, "Wi-Fi state changed: isConnected=$isConnected, scheduleEnabled=$isScheduleEnabled, autoStopOnWifi=$isAutoStopOnWifi")

                // Nếu Lịch kíp đang bật HOẶC tính năng tự tắt khi có Wi-Fi đang bật
                if (isScheduleEnabled || isAutoStopOnWifi) {
                    ShiftScheduleManager.recalculateAndSchedule(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Wi-Fi change in WifiStateMonitor", e)
            }
        }
    }
}
