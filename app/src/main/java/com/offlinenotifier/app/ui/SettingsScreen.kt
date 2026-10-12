package com.offlinenotifier.app.ui

import android.accounts.AccountManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinenotifier.app.R
import com.offlinenotifier.app.utils.ShizukuHelper
import com.offlinenotifier.app.viewmodels.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current

    val senderEmail by viewModel.senderEmail.collectAsState()
    val recipientEmail by viewModel.recipientEmail.collectAsState()
    val emailPass by viewModel.emailPass.collectAsState()
    val intervalIndex by viewModel.intervalIndex.collectAsState()
    val customInterval by viewModel.customInterval.collectAsState()
    val isActive by viewModel.isActive.collectAsState()

    val captureSms by viewModel.captureSms.collectAsState()
    val captureCalls by viewModel.captureCalls.collectAsState()
    val captureApps by viewModel.captureApps.collectAsState()
    val autoStopOnWifi by viewModel.autoStopOnWifi.collectAsState()
    val showTestButton by viewModel.showTestButton.collectAsState()
    val selectedApps by viewModel.selectedApps.collectAsState()
    val filterOngoing by viewModel.filterOngoing.collectAsState()
    val keywordBlacklist by viewModel.keywordBlacklist.collectAsState()

    // Shift schedule flows
    val scheduleEnabled by viewModel.scheduleEnabled.collectAsState()
    val selectedCrew by viewModel.selectedCrew.collectAsState()
    val dayShiftStartHour by viewModel.dayShiftStartHour.collectAsState()
    val dayShiftStartMin by viewModel.dayShiftStartMin.collectAsState()
    val dayShiftEndHour by viewModel.dayShiftEndHour.collectAsState()
    val dayShiftEndMin by viewModel.dayShiftEndMin.collectAsState()
    val nightShiftStartHour by viewModel.nightShiftStartHour.collectAsState()
    val nightShiftStartMin by viewModel.nightShiftStartMin.collectAsState()
    val nightShiftEndHour by viewModel.nightShiftEndHour.collectAsState()
    val nightShiftEndMin by viewModel.nightShiftEndMin.collectAsState()
    val shiftsMap by viewModel.todayAllCrewShifts.collectAsState()
    val shiftDiag by viewModel.shiftDiagnosticInfo.collectAsState()

    var localRecipientEmail by remember { mutableStateOf(recipientEmail) }
    var localEmailPass by remember { mutableStateOf(emailPass) }
    var localCustomInterval by remember { mutableStateOf(customInterval.toString()) }
    var localKeywordBlacklist by remember { mutableStateOf(keywordBlacklist) }
    var showAppFilterDialog by remember { mutableStateOf(false) }

    // Expansion states for accordion layout
    var isEventsExpanded by remember { mutableStateOf(false) }
    var isScheduleExpanded by remember { mutableStateOf(false) }
    var isEmailExpanded by remember { mutableStateOf(false) }
    var isPermissionExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(recipientEmail) { localRecipientEmail = recipientEmail }
    LaunchedEffect(emailPass) { localEmailPass = emailPass }
    LaunchedEffect(customInterval) { localCustomInterval = customInterval.toString() }
    LaunchedEffect(keywordBlacklist) { localKeywordBlacklist = keywordBlacklist }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            viewModel.onSenderEmailChanged(accountName)
            Toast.makeText(context, "Đã chọn: $accountName", Toast.LENGTH_SHORT).show()
        }
    }

    val intervals = listOf("1 Phút (Test)", "15 Phút", "30 Phút", "1 Giờ", "Tùy chỉnh (phút)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Switch Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val statusText = if (isActive) "🟢 ĐANG GIÁM SÁT" else "⭕ DỊCH VỤ TẮT"
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (scheduleEnabled) {
                        Text(
                            text = "Điều khiển tự động bởi Lịch Kíp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { viewModel.onActiveToggled(it) }
                )
            }
        }

        // Wi-Fi Auto-Stop Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "📶 Tự tắt khi có Wi-Fi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tự động tạm dừng giám sát và xóa hàng chờ gửi khi thiết bị kết nối vào mạng Wi-Fi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoStopOnWifi,
                    onCheckedChange = { viewModel.onAutoStopOnWifiToggled(it) }
                )
            }
        }

        // Pending Events Queue Card
        val pendingEvents by viewModel.pendingEvents.collectAsState()
        if (pendingEvents.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📋 Hàng Chờ Gửi (${pendingEvents.size} sự kiện)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val callCount = pendingEvents.count { it.type == "CALL" }
                    val smsCount = pendingEvents.count { it.type == "SMS" }
                    val appCount = pendingEvents.count { it.type == "APP_NOTIFICATION" }
                    if (callCount > 0) Text("📞 $callCount cuộc gọi nhỡ")
                    if (smsCount > 0) Text("💬 $smsCount tin nhắn")
                    if (appCount > 0) Text("🔔 $appCount thông báo ứng dụng")

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearQueue(); Toast.makeText(context, context.getString(R.string.str_x_a_h_ng_i_23), Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.str_x_a_h_ng_i_0))
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.str_kh_ng_c_s_ki_n_n_o_ch_g_i_1), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Test Send Button (Toggleable)
        if (showTestButton) {
            Button(
                onClick = {
                    viewModel.onRecipientEmailChanged(localRecipientEmail)
                    viewModel.onEmailPassChanged(localEmailPass)

                    if (senderEmail.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.str_vui_l_ng_ch_n_t_i_kho_n_g_24), Toast.LENGTH_LONG).show()
                    } else if (localRecipientEmail.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.str_vui_l_ng_nh_p_email_nh_n_25), Toast.LENGTH_LONG).show()
                    } else if (localEmailPass.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.str_vui_l_ng_nh_p_app_passwor_26), Toast.LENGTH_LONG).show()
                    } else {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            viewModel.testSendNow()
                            Toast.makeText(context, context.getString(R.string.str_ang_g_i_th_ki_m_tra_email_27), Toast.LENGTH_LONG).show()
                        }, 500)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(stringResource(R.string.str_test_g_i_ngay_2))
            }
        }

        HorizontalDivider()

        // ===== SECTION 2: EVENT TOGGLES (Accordion) =====
        Card(modifier = Modifier.fillMaxWidth().clickable { isEventsExpanded = !isEventsExpanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.str_l_c_s_ki_n_c_n_l_y_3), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = if (isEventsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Mở rộng"
                )
            }
            AnimatedVisibility(visible = isEventsExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.str_cu_c_g_i_nh_4))
                        Switch(checked = captureCalls, onCheckedChange = { viewModel.onCaptureCallsToggled(it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.str_tin_nh_n_sms_5))
                        Switch(checked = captureSms, onCheckedChange = { viewModel.onCaptureSmsToggled(it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(stringResource(R.string.str_th_ng_b_o_ng_d_ng_6))
                            if (captureApps && selectedApps.isNotBlank()) {
                                Text("Đã lọc: ${selectedApps.split(",").size} app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            } else if (captureApps) {
                                Text(stringResource(R.string.str_l_y_t_t_c_app_7), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        Switch(checked = captureApps, onCheckedChange = { viewModel.onCaptureAppsToggled(it) })
                    }
                    if (captureApps) {
                        OutlinedButton(onClick = { showAppFilterDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.str_ch_n_ng_d_ng_8))
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Smart Filters
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Bỏ qua thông báo tiến trình / phát nhạc", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Tự động lọc bỏ thanh tải file % nhảy liên tục, trình phát nhạc hoặc dịch vụ chạy nền.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = filterOngoing, onCheckedChange = { viewModel.onFilterOngoingToggled(it) })
                        }

                        Spacer(Modifier.height(4.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🚫 Từ khóa loại trừ (Blacklist):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tự động bỏ qua mọi thông báo có chứa từ khóa này (phân cách bằng dấu phẩy).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            OutlinedTextField(
                                value = localKeywordBlacklist,
                                onValueChange = { 
                                    localKeywordBlacklist = it
                                    viewModel.onKeywordBlacklistChanged(it)
                                },
                                label = { Text("Danh sách từ khóa cần chặn") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("quảng cáo, khuyến mãi, voucher, otp, sao lưu...") }
                            )

                            // Quick Keyword Suggestion Chips
                            val sampleKeywords = listOf("quảng cáo", "khuyến mãi", "ưu đãi", "voucher", "mã giảm giá", "otp", "sao lưu", "đồng bộ")
                            Text("Gợi ý thêm nhanh:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                sampleKeywords.forEach { kw ->
                                    val alreadyAdded = localKeywordBlacklist.contains(kw, ignoreCase = true)
                                    AssistChip(
                                        onClick = {
                                            if (!alreadyAdded) {
                                                val updated = if (localKeywordBlacklist.isBlank()) kw else "$localKeywordBlacklist, $kw"
                                                localKeywordBlacklist = updated
                                                viewModel.onKeywordBlacklistChanged(updated)
                                            }
                                        },
                                        label = { Text(if (alreadyAdded) "✓ $kw" else "+ $kw", style = MaterialTheme.typography.bodySmall) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (alreadyAdded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== SECTION 3: AUTO SHIFT SCHEDULE (Accordion) =====
        Card(modifier = Modifier.fillMaxWidth().clickable { isScheduleExpanded = !isScheduleExpanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📅 Lịch Kíp Tự Động", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = if (isScheduleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Mở rộng"
                )
            }
            AnimatedVisibility(visible = isScheduleExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Schedule Switch
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Bật tự động theo kíp")
                            Text("Dịch vụ tự động start/stop theo ca trực đã chọn.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = scheduleEnabled, onCheckedChange = { viewModel.onScheduleEnabledToggled(it) })
                    }

                    if (scheduleEnabled) {
                        // Crew Selection Chips
                        Text("Chọn kíp trực của bạn:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("A", "B", "C").forEach { crew ->
                                val isSelected = selectedCrew == crew
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onSelectedCrewChanged(crew) },
                                    label = { Text("Kíp $crew") }
                                )
                            }
                        }

                        // Shifts Today Info
                        if (shiftsMap.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Lịch kíp hôm nay:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    shiftsMap.forEach { (crew, shiftType) ->
                                        val isMyCrew = crew == selectedCrew
                                        val boldText = if (isMyCrew) " (Kíp của bạn)" else ""
                                        Text(
                                            text = "• Kíp $crew: ${shiftType.emoji} ${shiftType.label}$boldText",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isMyCrew) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isMyCrew) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Day Shift Hours Customizer
                        Text("Cài đặt giờ Ca Ngày:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showTimePicker(context, dayShiftStartHour, dayShiftStartMin) { h, m ->
                                        viewModel.onDayShiftStartChanged(h, m)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(String.format("Bắt đầu: %02d:%02d", dayShiftStartHour, dayShiftStartMin))
                            }
                            OutlinedButton(
                                onClick = {
                                    showTimePicker(context, dayShiftEndHour, dayShiftEndMin) { h, m ->
                                        viewModel.onDayShiftEndChanged(h, m)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(String.format("Kết thúc: %02d:%02d", dayShiftEndHour, dayShiftEndMin))
                            }
                        }

                        // Night Shift Hours Customizer
                        Text("Cài đặt giờ Ca Đêm:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showTimePicker(context, nightShiftStartHour, nightShiftStartMin) { h, m ->
                                        viewModel.onNightShiftStartChanged(h, m)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(String.format("Bắt đầu: %02d:%02d", nightShiftStartHour, nightShiftStartMin))
                            }
                            OutlinedButton(
                                onClick = {
                                    showTimePicker(context, nightShiftEndHour, nightShiftEndMin) { h, m ->
                                        viewModel.onNightShiftEndChanged(h, m)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(String.format("Kết thúc: %02d:%02d", nightShiftEndHour, nightShiftEndMin))
                            }
                        }

                        // Live Shift Diagnostic Card (Chẩn đoán Lịch Kíp)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("📊 Trạng Thái Lịch Kíp Thời Gian Thực:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                
                                val shiftLabel = "${shiftDiag.todayShift.emoji} ${shiftDiag.todayShift.label}"
                                val inShiftText = if (shiftDiag.isCurrentlyInShift) "🟢 Đang trong giờ làm việc" else "⚪ Ngoài giờ làm việc"
                                val serviceStateText = if (shiftDiag.isServiceRunning) "🟢 Dịch vụ đang CHẠY" else "🔴 Dịch vụ đang TẮT"

                                Text("• Ca hôm nay (Kíp ${shiftDiag.crew}): $shiftLabel", style = MaterialTheme.typography.bodySmall)
                                Text("• Tình trạng: $inShiftText", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("• Trạng thái hệ thống: $serviceStateText", style = MaterialTheme.typography.bodySmall)

                                if (shiftDiag.isWifiBlocked) {
                                    Text("⚠️ Đang tạm dừng do kết nối Wi-Fi (Sẽ tự động kích hoạt ngay khi ngắt Wi-Fi)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }

                                Text("• Mốc hẹn giờ tiếp theo: ⏰ ${shiftDiag.nextAlarmFormatted}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.syncAndCheckShiftNow()
                                        Toast.makeText(context, "Đã đồng bộ và kiểm tra ca trực!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🔄 Đồng bộ & Kiểm tra ca ngay")
                                }
                            }
                        }
                    }
                }
            }
        }

        // ===== SECTION 4: EMAIL & INTERVAL (Accordion) =====
        Card(modifier = Modifier.fillMaxWidth().clickable { isEmailExpanded = !isEmailExpanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.str_c_u_h_nh_email_chu_k_9), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = if (isEmailExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Mở rộng"
                )
            }
            AnimatedVisibility(visible = isEmailExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Sender Email
                    Column {
                        Text(stringResource(R.string.str_t_i_kho_n_google_g_i_i_10), fontWeight = FontWeight.SemiBold)
                        if (senderEmail.isNotBlank()) {
                            Text("✅ $senderEmail", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        }
                        Button(
                            onClick = {
                                val intent = AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), null, null, null, null)
                                accountPickerLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (senderEmail.isBlank()) "Chọn Tài Khoản" else "Đổi Tài Khoản")
                        }
                    }

                    // Recipient Email
                    OutlinedTextField(
                        value = localRecipientEmail,
                        onValueChange = { 
                            localRecipientEmail = it 
                            viewModel.onRecipientEmailChanged(it)
                        },
                        label = { Text(stringResource(R.string.str_email_nh_n_b_o_c_o_11)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    // App Password
                    OutlinedTextField(
                        value = localEmailPass,
                        onValueChange = { 
                            localEmailPass = it 
                            viewModel.onEmailPassChanged(it)
                        },
                        label = { Text(stringResource(R.string.str_app_password_16_k_t_12)) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = { Text(stringResource(R.string.str_t_o_t_i_google_account_se_13)) }
                    )

                    // Interval String
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = intervals[intervalIndex],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.str_chu_k_g_i_b_o_c_o_14)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            intervals.forEachIndexed { index, selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = { viewModel.onIntervalChanged(index); expanded = false }
                                )
                            }
                        }
                    }

                    if (intervalIndex == 4) {
                        OutlinedTextField(
                            value = localCustomInterval,
                            onValueChange = { 
                                localCustomInterval = it 
                                viewModel.onCustomIntervalChanged(it.toIntOrNull() ?: 15)
                            },
                            label = { Text(stringResource(R.string.str_s_ph_t_t_y_ch_nh_15_15)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }

        // ===== SECTION 5: PERMISSIONS & ADB (Accordion) =====
        Card(modifier = Modifier.fillMaxWidth().clickable { isPermissionExpanded = !isPermissionExpanded }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.str_quy_n_kh_c_ph_c_l_i_16), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = if (isPermissionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Mở rộng"
                )
            }
            AnimatedVisibility(visible = isPermissionExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val hasCallLog = context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasPhoneState = context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasSms = context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasReadSms = context.checkSelfPermission(android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasNotifListener = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")?.contains(context.packageName) == true
                    val hasWriteSettings = android.provider.Settings.System.canWrite(context)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PermissionRow("Đọc Cuộc Gọi", hasCallLog)
                        PermissionRow("Trạng Thái Điện Thoại", hasPhoneState)
                        PermissionRow("Đọc/Nhận SMS", hasSms && hasReadSms)
                        PermissionRow("Đọc Thông Báo", hasNotifListener)
                        PermissionRow("Ghi Cài Đặt (ADB)", hasWriteSettings)
                    }

                    val isShizukuReady = ShizukuHelper.isShizukuAvailable() && ShizukuHelper.hasShizukuPermission()
                    Card(colors = CardDefaults.cardColors(containerColor = if (isShizukuReady) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isShizukuReady) "⚡ Shizuku: Tự động cấp quyền đang BẬT" else stringResource(R.string.str_c_p_quy_n_t_ng_shizuku_17),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isShizukuReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Hệ thống tự động phát hiện và cấp bù các quyền bị thiếu khi Shizuku chạy (0% tốn pin, không spam).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        if (!ShizukuHelper.isShizukuAvailable()) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, context.getString(R.string.str_shizuku_ch_a_ch_y_ho_c_ch_28), Toast.LENGTH_LONG).show()
                                            }
                                            return@launch
                                        }
                                        if (!ShizukuHelper.hasShizukuPermission()) {
                                            ShizukuHelper.requestShizukuPermission(1001)
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, context.getString(R.string.str_vui_l_ng_cho_ph_p_shizuku_29), Toast.LENGTH_LONG).show()
                                            }
                                            return@launch
                                        }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Đang tự động kiểm tra và cấp quyền qua Shizuku...", Toast.LENGTH_SHORT).show()
                                        }
                                        com.offlinenotifier.app.utils.AutoPermissionManager.checkAndGrantMissingPermissions(context, force = true)
                                        kotlinx.coroutines.delay(1000)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Đã cập nhật trạng thái quyền qua Shizuku!", Toast.LENGTH_SHORT).show()
                                            isPermissionExpanded = false
                                            isPermissionExpanded = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isShizukuReady) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFFFFC107))
                            ) {
                                    Text(
                                        if (isShizukuReady) "Quét & Cấp lại quyền ngay" else stringResource(R.string.str_c_p_quy_n_adb_18),
                                        color = if (isShizukuReady) MaterialTheme.colorScheme.onPrimary else androidx.compose.ui.graphics.Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                            }
                        }
                    }
                }
            }
        }
    }

    // App Filter Dialog
    if (showAppFilterDialog) {
        val pm = context.packageManager
        var installedApps by remember { mutableStateOf<List<android.content.pm.ApplicationInfo>>(emptyList()) }
        LaunchedEffect(Unit) {
            installedApps = withContext(Dispatchers.IO) {
                pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                    .filter { appInfo -> 
                        appInfo.icon != 0 && 
                        (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0    
                    }
                    .sortedBy { pm.getApplicationLabel(it).toString() }
            }
        }

        var localSelectedApps by remember { mutableStateOf(selectedApps.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()) }
        var searchQuery by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAppFilterDialog = false },
            title = { Text(stringResource(R.string.str_l_c_ng_d_ng_th_ng_b_o_19)) },
            text = {
                Column(modifier = Modifier.height(400.dp)) {
                    Text(stringResource(R.string.str_ch_n_c_c_ng_d_ng_b_n_mu_n_20), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))

                    if (installedApps.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        val filteredApps = if (searchQuery.isBlank()) {
                            installedApps
                        } else {
                            installedApps.filter { pm.getApplicationLabel(it).toString().contains(searchQuery, ignoreCase = true) }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text(stringResource(R.string.str_t_m_ki_m_ng_d_ng_21)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )

                        androidx.compose.foundation.lazy.LazyColumn {
                            items(filteredApps.size) { index ->
                                val appInfo = filteredApps[index]
                                val appName = pm.getApplicationLabel(appInfo).toString()
                                val pkgName = appInfo.packageName
                                val isChecked = localSelectedApps.contains(pkgName)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            val newSet = localSelectedApps.toMutableSet()
                                            if (checked) newSet.add(pkgName) else newSet.remove(pkgName)
                                            localSelectedApps = newSet
                                        }
                                    )
                                    Text(appName, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.onSelectedAppsChanged(localSelectedApps.joinToString(","))
                    showAppFilterDialog = false 
                }) {
                    Text("Lưu (${localSelectedApps.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppFilterDialog = false }) {
                    Text(stringResource(R.string.str_h_y_22))
                }
            }
        )
    }
}

private fun showTimePicker(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSelected(hour, minute) },
        initialHour,
        initialMinute,
        true
    ).show()
}

@Composable
fun PermissionRow(name: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        Text(if (granted) "✅" else "❌", style = MaterialTheme.typography.titleMedium)
    }
}
