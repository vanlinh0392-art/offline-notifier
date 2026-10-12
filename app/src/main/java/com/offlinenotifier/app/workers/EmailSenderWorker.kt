package com.offlinenotifier.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.offlinenotifier.app.data.SettingsRepository
import com.offlinenotifier.app.data.database.AppDatabase
import com.offlinenotifier.app.data.database.SentLogEntity
import com.offlinenotifier.app.services.SmtpClient
import com.offlinenotifier.app.services.InlineImage
import com.offlinenotifier.app.services.AudioAttachment
import com.offlinenotifier.app.utils.MediaExtractorHelper
import com.offlinenotifier.app.utils.NetworkStateHelper
import java.io.File
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class EmailSenderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "EmailSenderWorker"
    private val MAX_TOTAL_ATTACHMENT_BYTES = 20 * 1024 * 1024L // 20 MB safe threshold for Gmail SMTP

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting EmailSenderWorker...")
        
        val isTest = inputData.getBoolean("is_test", false)
        
        val settingsRepo = SettingsRepository(context)
        val isActive = settingsRepo.isActiveFlow.first()
        if (!isActive && !isTest) {
            Log.d(TAG, "Service is turned OFF. Skipping email sending.")
            return Result.success()
        }

        // Kiểm tra kết nối Internet thực tế
        if (!NetworkStateHelper.isInternetValidated(context)) {
            Log.w(TAG, "Internet connection is not validated yet. Will retry when connection improves.")
            return Result.retry()
        }

        val db = AppDatabase.getDatabase(context)
        val events = db.eventDao().getAllEvents()

        if (events.isEmpty() && !isTest) {
            Log.d(TAG, "No offline events in database. Nothing to send.")
            return Result.success()
        }

        val senderEmail = settingsRepo.senderEmailFlow.first()
        val recipientEmail = settingsRepo.recipientEmailFlow.first()
        val emailPass = settingsRepo.emailPassFlow.first()

        if (senderEmail.isBlank() || recipientEmail.isBlank() || emailPass.isBlank()) {
            Log.e(TAG, "Sender, Recipient email or Password is not configured properly.")
            return Result.failure()
        }

        // Prepare HTML Body
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val callCount = events.count { it.type == "CALL" }
        val smsCount = events.count { it.type == "SMS" }
        val appCount = events.count { it.type == "APP_NOTIFICATION" }
        
        val sb = StringBuilder()
        if (isTest) {
            sb.append("<h2>🧪 Test - Offline Notifier</h2>")
            sb.append("<p>Đây là email test từ Offline Notifier.</p>")
        } else {
            sb.append("<h2>Báo Cáo Sự Kiện (Offline Notifier)</h2>")
        }
        sb.append("<p><b>📞 Cuộc gọi nhỡ:</b> $callCount</p>")
        sb.append("<p><b>💬 Tin nhắn SMS:</b> $smsCount</p>")
        sb.append("<p><b>🔔 Thông báo app:</b> $appCount</p>")
        sb.append("<p><b>Tổng:</b> ${events.size} sự kiện</p>")
        
        val inlineImages = mutableListOf<InlineImage>()
        val audioAttachments = mutableListOf<AudioAttachment>()
        var currentPayloadBytes = 0L
        var imgIndex = 0

        if (events.isNotEmpty()) {
            sb.append("<hr>")
            sb.append("<table border='1' cellpadding='5' style='border-collapse: collapse; width:100%;'>")
            sb.append("<tr style='background-color:#f2f2f2'><th>Thời Gian</th><th>Loại</th><th>Người Gửi / App</th><th>Nội Dung</th></tr>")
            for (event in events) {
                sb.append("<tr>")
                sb.append("<td style='white-space:nowrap;'>${sdf.format(Date(event.timestamp))}</td>")
                sb.append("<td><b>${event.type}</b></td>")
                sb.append("<td>${event.title}</td>")
                
                var bodyContent = event.body
                
                // Xử lý đính kèm ảnh Inline (có kiểm tra giới hạn an toàn 20MB)
                if (!event.imagePath.isNullOrBlank()) {
                    val file = File(event.imagePath)
                    if (file.exists()) {
                        val fileSize = file.length()
                        if (currentPayloadBytes + fileSize <= MAX_TOTAL_ATTACHMENT_BYTES) {
                            val cid = "noti_img_$imgIndex"
                            inlineImages.add(InlineImage(contentId = cid, filePath = event.imagePath))
                            currentPayloadBytes += fileSize
                            bodyContent = "$bodyContent<br/><img src=\"cid:$cid\" style=\"max-width:300px; margin-top:6px; border-radius:8px; display:block;\"/>"
                            imgIndex++
                        } else {
                            bodyContent = "$bodyContent<br/><span style='color:#888; font-size:12px;'>[Ảnh đính kèm đã được lược bỏ vì vượt quá giới hạn 20MB]</span>"
                        }
                    }
                }

                // Xử lý đính kèm file âm thanh Audio (có kiểm tra giới hạn an toàn 20MB)
                if (!event.audioPath.isNullOrBlank()) {
                    val audioFile = File(event.audioPath)
                    if (audioFile.exists()) {
                        val fileSize = audioFile.length()
                        val fileName = audioFile.name
                        val fileSizeKb = (fileSize / 1024).coerceAtLeast(1)
                        if (currentPayloadBytes + fileSize <= MAX_TOTAL_ATTACHMENT_BYTES) {
                            audioAttachments.add(AudioAttachment(fileName = fileName, filePath = event.audioPath))
                            currentPayloadBytes += fileSize
                            bodyContent = "$bodyContent<br/><div style=\"margin-top:6px; padding:6px 10px; background-color:#e8f0fe; color:#1a73e8; border-radius:6px; font-size:13px; display:inline-block;\">🎵 <b>File âm thanh đính kèm:</b> $fileName ($fileSizeKb KB)</div>"
                        } else {
                            bodyContent = "$bodyContent<br/><span style='color:#888; font-size:12px;'>[File âm thanh $fileName ($fileSizeKb KB) đã được lược bỏ vì vượt quá giới hạn 20MB]</span>"
                        }
                    }
                }

                sb.append("<td>$bodyContent</td>")
                sb.append("</tr>")
            }
            sb.append("</table>")
        }

        val htmlBody = sb.toString()
        val subject = if (isTest) {
            "[Offline Notifier] 🧪 Test: $callCount cuộc gọi, $smsCount SMS, $appCount thông báo"
        } else {
            "[Offline Notifier] Cập nhật: ${events.size} thông báo chưa đọc"
        }

        // Execute block sending
        val result = SmtpClient.sendEmailBlocking(
            senderEmail = senderEmail,
            recipientEmail = recipientEmail,
            appPassword = emailPass,
            subject = subject,
            bodyHtml = htmlBody,
            inlineImages = inlineImages,
            audioAttachments = audioAttachments
        )

        // Build event summary breakdown (reusing callCount/smsCount/appCount from above)
        val summaryParts = mutableListOf<String>()
        if (callCount > 0) summaryParts.add("📞 $callCount cuộc gọi")
        if (smsCount > 0) summaryParts.add("📬 $smsCount tin nhắn")
        if (appCount > 0) summaryParts.add("🔔 $appCount thông báo")
        val eventSummary = summaryParts.joinToString(" · ")

        // Record sent log
        db.sentLogDao().insertLog(SentLogEntity(
            timeSent = System.currentTimeMillis(),
            eventCount = events.size,
            status = if (result.isSuccess) "SUCCESS" else "FAILED",
            eventSummary = eventSummary,
            errorMessage = result.errorMessage
        ))
        // Tự động giữ tối đa 100 log gần nhất để bảo vệ bộ nhớ DB
        try {
            db.sentLogDao().trimOldLogs(100)
        } catch (e: Exception) {
            Log.w(TAG, "Failed trimming old logs", e)
        }

        return if (result.isSuccess) {
            Log.d(TAG, "Email sent successfully!")
            if (events.isNotEmpty() && !isTest) {
                events.forEach { event ->
                    MediaExtractorHelper.deleteMediaFile(event.imagePath)
                    MediaExtractorHelper.deleteMediaFile(event.audioPath)
                }
                val idsToDelete = events.map { it.id }
                db.eventDao().deleteEvents(idsToDelete)
            }
            Result.success()
        } else {
            Log.e(TAG, "Failed to send email: ${result.errorMessage}. Will retry on next interval.")
            Result.retry()
        }
    }
}
