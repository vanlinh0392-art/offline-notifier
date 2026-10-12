package com.offlinenotifier.app.workers

import android.content.Context
import androidx.work.*

object WorkManagerHelper {
    private const val SEND_EMAIL_WORK_NAME = "SendOfflineEmailWork"
    private const val TEST_SEND_WORK_NAME = "TestSendEmailWork"

    fun stopEmailWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(SEND_EMAIL_WORK_NAME)
        workManager.cancelUniqueWork(TEST_SEND_WORK_NAME)
    }

    fun triggerImmediateSend(context: Context, isTest: Boolean = true): Operation {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = workDataOf("is_test" to isTest)

        val workRequest = OneTimeWorkRequestBuilder<EmailSenderWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        val workName = if (isTest) TEST_SEND_WORK_NAME else SEND_EMAIL_WORK_NAME

        return WorkManager.getInstance(context)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, workRequest)
    }
}
