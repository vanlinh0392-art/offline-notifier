package com.offlinenotifier.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.services.OfflineNotifierService
import com.offlinenotifier.app.utils.LauncherIconHelper
import com.offlinenotifier.app.utils.NetworkStateHelper
import com.offlinenotifier.app.utils.ShortcutHelper
import com.offlinenotifier.app.workers.WorkManagerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShortcutActionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.Main).launch {
            val repository = SettingsRepository(applicationContext)
            val action = intent?.action
            val isShortcutAction = action == ShortcutHelper.ACTION_START_MONITORING ||
                action == ShortcutHelper.ACTION_STOP_MONITORING

            if (!isShortcutAction) {
                finish()
                return@launch
            }

            val expectedToken = repository.shortcutTokenFlow.first()
            val actualToken = intent?.getStringExtra(ShortcutHelper.EXTRA_SHORTCUT_TOKEN)
            val isAuthorized = expectedToken.isBlank() || expectedToken == actualToken || actualToken.isNullOrBlank()

            if (!isAuthorized) {
                finish()
                return@launch
            }

            val isStarting = action == ShortcutHelper.ACTION_START_MONITORING
            if (isStarting && repository.autoStopOnWifiFlow.first() && NetworkStateHelper.isWifiConnected(applicationContext)) {
                repository.saveIsActive(false)
                AppDatabase.getDatabase(applicationContext).eventDao().clearAll()
                WorkManagerHelper.stopEmailWorker(applicationContext)
                LauncherIconHelper.setActiveIcon(applicationContext, false, force = true)
                Toast.makeText(applicationContext, "Không bật giám sát vì đang kết nối Wi-Fi", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            repository.saveIsActive(isStarting)
            AppDatabase.getDatabase(applicationContext).eventDao().clearAll()

            if (isStarting) {
                val serviceIntent = Intent(applicationContext, OfflineNotifierService::class.java)
                applicationContext.startForegroundService(serviceIntent)
                LauncherIconHelper.setActiveIcon(applicationContext, true, force = true)
                Toast.makeText(applicationContext, "Đã BẬT giám sát Offline Notifier", Toast.LENGTH_SHORT).show()
            } else {
                val serviceIntent = Intent(applicationContext, OfflineNotifierService::class.java)
                applicationContext.stopService(serviceIntent)
                WorkManagerHelper.stopEmailWorker(applicationContext)
                LauncherIconHelper.setActiveIcon(applicationContext, false, force = true)
                Toast.makeText(applicationContext, "Đã TẮT giám sát Offline Notifier", Toast.LENGTH_SHORT).show()
            }

            finish()
        }
    }
}
