package com.offlinenotifier.app.services

import java.util.Properties
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

data class SendResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class InlineImage(
    val contentId: String,
    val filePath: String
)

data class AudioAttachment(
    val fileName: String,
    val filePath: String
)

object SmtpClient {

    fun sendEmailBlocking(
        senderEmail: String,
        recipientEmail: String,
        appPassword: String,
        subject: String,
        bodyHtml: String,
        inlineImages: List<InlineImage> = emptyList(),
        audioAttachments: List<AudioAttachment> = emptyList()
    ): SendResult {
        return try {
            val props = Properties().apply {
                put("mail.smtp.auth", "true")
                put("mail.smtp.starttls.enable", "true")
                put("mail.smtp.host", "smtp.gmail.com")
                put("mail.smtp.port", "587")
                // I/O Timeouts để chống treo luồng khi mạng lag hoặc captive portal
                put("mail.smtp.connectiontimeout", "15000") // 15s kết nối
                put("mail.smtp.timeout", "25000")           // 25s đọc phản hồi
                put("mail.smtp.writetimeout", "25000")      // 25s ghi dữ liệu
            }

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(senderEmail, appPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(senderEmail))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                setSubject(subject, "UTF-8")
            }

            if (inlineImages.isEmpty() && audioAttachments.isEmpty()) {
                message.setContent(bodyHtml, "text/html; charset=utf-8")
            } else {
                // Tạo related multipart chứa HTML và inline images
                val relatedMultipart = MimeMultipart("related")

                val htmlPart = MimeBodyPart()
                htmlPart.setContent(bodyHtml, "text/html; charset=utf-8")
                relatedMultipart.addBodyPart(htmlPart)

                for (img in inlineImages) {
                    val imgPart = MimeBodyPart()
                    val source = FileDataSource(img.filePath)
                    imgPart.dataHandler = DataHandler(source)
                    imgPart.setHeader("Content-ID", "<${img.contentId}>")
                    imgPart.disposition = MimeBodyPart.INLINE
                    relatedMultipart.addBodyPart(imgPart)
                }

                if (audioAttachments.isEmpty()) {
                    message.setContent(relatedMultipart)
                } else {
                    // Tạo mixed multipart chứa related body + các file đính kèm audio
                    val mixedMultipart = MimeMultipart("mixed")

                    val wrapRelatedPart = MimeBodyPart()
                    wrapRelatedPart.setContent(relatedMultipart)
                    mixedMultipart.addBodyPart(wrapRelatedPart)

                    for (audio in audioAttachments) {
                        val audioPart = MimeBodyPart()
                        val source = FileDataSource(audio.filePath)
                        audioPart.dataHandler = DataHandler(source)
                        audioPart.fileName = audio.fileName
                        audioPart.disposition = MimeBodyPart.ATTACHMENT
                        mixedMultipart.addBodyPart(audioPart)
                    }

                    message.setContent(mixedMultipart)
                }
            }

            Transport.send(message)
            SendResult(isSuccess = true)
        } catch (e: Exception) {
            e.printStackTrace()
            SendResult(isSuccess = false, errorMessage = e.localizedMessage ?: e.message ?: e.javaClass.simpleName)
        }
    }
}
