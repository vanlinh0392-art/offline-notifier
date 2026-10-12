package com.offlinenotifier.app.utils

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.util.Log

object DeviceStateHelper {
    private const val TAG = "DeviceStateHelper"

    /**
     * Kiểm tra xem thiết bị có đang ở trạng thái KHÔNG SỬ DỤNG (Màn hình tắt HOẶC đang bị khóa màn hình).
     *
     * Quy tắc nghiêm ngặt:
     * - Nếu màn hình ĐANG BẬT (Interactive) VÀ KHÔNG BỊ KHÓA (Keyguard not locked / Unlocked)
     *   -> Người dùng ĐANG SỬ DỤNG MÁY -> Trả về FALSE (KHÔNG bắt sự kiện trong mọi trường hợp).
     * - Nếu màn hình TẮT (!Interactive) -> Trả về TRUE (Thiết bị đang nghỉ).
     * - Nếu màn hình SÁNG nhưng ĐANG BỊ KHÓA (Keyguard locked) -> Trả về TRUE (Thiết bị chưa mở khóa, ví dụ có cuộc gọi đến sáng màn hình khóa).
     */
    fun isDeviceLockedOrScreenOff(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

        val isInteractive = powerManager?.isInteractive ?: true
        val isKeyguardLocked = keyguardManager?.isKeyguardLocked ?: false

        // Màn hình tắt hoàn toàn -> Đang offline
        if (!isInteractive) {
            Log.d(TAG, "Screen is OFF (non-interactive) -> Device is offline/locked")
            return true
        }

        // Màn hình sáng: Bắt buộc phải đang bị khóa màn hình mới coi là offline
        val isOffline = isKeyguardLocked
        Log.d(TAG, "Screen is ON (interactive) -> keyguardLocked=$isKeyguardLocked, isOffline=$isOffline")
        return isOffline
    }
}
