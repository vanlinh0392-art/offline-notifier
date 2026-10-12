package com.offlinenotifier.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.ui.LogHistoryScreen
import com.offlinenotifier.app.ui.SettingsScreen
import com.offlinenotifier.app.utils.AutoPermissionManager
import com.offlinenotifier.app.utils.LauncherIconHelper
import com.offlinenotifier.app.viewmodels.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo và kích hoạt tự động cấp quyền qua Shizuku nếu có
        AutoPermissionManager.init(this)

        // Khởi động lắng nghe kết nối/ngắt kết nối Wi-Fi
        com.offlinenotifier.app.utils.WifiStateMonitor.startMonitoring(this)

        // Session GC: Tự động dọn dẹp các file media tạm thời cũ > 24h
        kotlinx.coroutines.MainScope().launch(kotlinx.coroutines.Dispatchers.IO) {
            com.offlinenotifier.app.utils.MediaExtractorHelper.purgeOrphanMediaFiles(applicationContext)
        }

        // Tự động kiểm tra và đồng bộ Lịch kíp
        com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(this)

        val db = AppDatabase.getDatabase(this)
        
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) } // Default to true for dark theme
            
            MaterialTheme(
                colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
            ) {
                MainScreenWithDrawer(viewModel = viewModel, db = db, isDarkTheme = isDarkTheme, onThemeToggle = { isDarkTheme = !isDarkTheme })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Tự động kiểm tra và bù quyền thiếu khi mở lại app
        AutoPermissionManager.checkAndGrantMissingPermissions(this, force = false)
        // Đồng bộ lại lịch kíp khi mở app
        com.offlinenotifier.app.shift.ShiftScheduleManager.recalculateAndSchedule(this)
    }

    override fun onStop() {
        super.onStop()
        // Sync icon safely when leaving app to avoid crashing while in foreground
        val repository = com.offlinenotifier.app.data.SettingsRepository(applicationContext)
        kotlinx.coroutines.MainScope().launch {
            try {
                val isActive = repository.isActiveFlow.first()
                LauncherIconHelper.setActiveIcon(applicationContext, isActive, force = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithDrawer(viewModel: SettingsViewModel, db: AppDatabase, isDarkTheme: Boolean, onThemeToggle: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedScreen by remember { mutableStateOf("settings") }
    val showTestButton by viewModel.showTestButton.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.str_offline_notifier_33),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = { Text(stringResource(R.string.txt_34)) },
                    label = { Text(stringResource(R.string.c_i_t_35)) },
                    selected = selectedScreen == "settings",
                    onClick = {
                        selectedScreen = "settings"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Text("📋") },
                    label = { Text(stringResource(R.string.l_ch_s_g_i_36)) },
                    selected = selectedScreen == "logs",
                    onClick = {
                        selectedScreen = "logs"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                
                Spacer(Modifier.weight(1f))
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.hi_n_n_t_test_g_i_ngay_37))
                    Switch(
                        checked = showTestButton,
                        onCheckedChange = { viewModel.onShowTestButtonToggled(it) }
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (selectedScreen == "settings") "Offline Notifier" else "Lịch Sử Gửi",
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onThemeToggle) {
                            Text(
                                text = if (isDarkTheme) "☀️" else "🌙",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedScreen) {
                    "settings" -> SettingsScreen(viewModel = viewModel)
                    "logs" -> LogHistoryScreen(db = db)
                }
            }
        }
    }
}
