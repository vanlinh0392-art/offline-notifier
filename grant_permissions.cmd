@echo off
chcp 65001 >nul
title Offline Notifier - ADB Permission Granter
color 0A

echo ===================================================
echo   Offline Notifier - ADB Permission Granter Tool
echo ===================================================
echo.

set /p ip="Nhap IP thiet bi cua ban (vi du: 192.168.1.12) hoac an Enter de dung cap USB: "

if "%ip%"=="" (
    echo.
    echo Dang su dung ket noi USB...
    set adb_target=-d
) else (
    echo.
    echo Dang ket noi toi %ip%:5555 ...
    adb connect %ip%:5555
    set adb_target=-s %ip%:5555
)

echo.
echo Dang cap quyen READ_CALL_LOG...
adb %adb_target% shell pm grant com.offlinenotifier.app android.permission.READ_CALL_LOG

echo Dang cap quyen READ_PHONE_STATE...
adb %adb_target% shell pm grant com.offlinenotifier.app android.permission.READ_PHONE_STATE

echo Dang cap quyen RECEIVE_SMS...
adb %adb_target% shell pm grant com.offlinenotifier.app android.permission.RECEIVE_SMS

echo Dang cap quyen READ_SMS...
adb %adb_target% shell pm grant com.offlinenotifier.app android.permission.READ_SMS

echo Dang cap quyen PACKAGE_USAGE_STATS...
adb %adb_target% shell pm grant com.offlinenotifier.app android.permission.PACKAGE_USAGE_STATS >nul 2>&1

echo Dang cap quyen WRITE_SECURE_SETTINGS (Android 14+)...
adb %adb_target% shell pm grant com.offlinenotifier.app android.permission.WRITE_SECURE_SETTINGS >nul 2>&1

echo Dang cap quyen WRITE_SETTINGS (Sua doi cai dat he thong)...
adb %adb_target% shell appops set com.offlinenotifier.app WRITE_SETTINGS allow
if %errorlevel% neq 0 (
    color 0E
    echo.
    echo [CANH BAO] Quyen WRITE_SETTINGS bi tu choi boi he thong.
    echo Ban phai tu vao Cai dat ^> Ung dung ^> Offline Notifier ^> bat "Sua doi cai dat he thong" bang tay.
    color 0A
)

echo.
echo Dang them ung dung vao danh sach whitelist chong Doze...
adb %adb_target% shell dumpsys deviceidle whitelist +com.offlinenotifier.app

echo.
echo ===================================================
echo KIEM TRA LAI QUYEN DA CAP:
echo ===================================================
echo [Quyen theo file Manifest (yeu cau granted)]
adb %adb_target% shell dumpsys package com.offlinenotifier.app | findstr "android.permission.READ_CALL_LOG android.permission.READ_PHONE_STATE android.permission.RECEIVE_SMS android.permission.READ_SMS"
echo.
echo [Quyen sua doi he thong (yeu cau allow)]
adb %adb_target% shell appops get com.offlinenotifier.app WRITE_SETTINGS
echo.
echo ===================================================
echo HOAN TAT! Hay kiem tra cac thong bao va trang thai quyen o tren.
echo ===================================================
pause
