package com.offlinenotifier.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.data.database.SentLogEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogHistoryScreen(db: AppDatabase) {
    val logs by db.sentLogDao().getAllLogsFlow().collectAsState(initial = emptyList())
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val scope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa toàn bộ lịch sử?") },
            text = { Text("Bạn có chắc chắn muốn xóa sạch toàn bộ lịch sử gửi báo cáo không?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { db.sentLogDao().clearLogs() }
                    showDeleteConfirm = false
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "📋 Lịch Sử Gửi Báo Cáo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            if (logs.isNotEmpty()) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa lịch sử", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Chưa có báo cáo nào được gửi",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    LogCard(log, sdf)
                }
            }
        }
    }
}

@Composable
fun LogCard(log: SentLogEntity, sdf: SimpleDateFormat) {
    val isSuccess = log.status == "SUCCESS"
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sdf.format(Date(log.timeSent)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${log.eventCount} sự kiện",
                    style = MaterialTheme.typography.bodySmall
                )
                if (log.eventSummary.isNotBlank()) {
                    Text(
                        log.eventSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isSuccess && !log.errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Chi tiết lỗi:\n${log.errorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                if (isSuccess) "✅" else "❌",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
