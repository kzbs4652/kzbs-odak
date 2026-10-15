@echo off
echo ==============================================
echo   KZBS ODAK - Otomatik APK Derleme Araci
echo ==============================================
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.17.10-hotspot
set ANDROID_HOME=C:\Android\sdk
call gradlew.bat assembleDebug
if %ERRORLEVEL% EQU 0 (
    copy /y app\build\outputs\apk\debug\app-debug.apk KZBS_ODAK_v1.0.0.apk
    echo.
    echo [BASARILI] Yeni APK dosyaniz olusturuldu: KZBS_ODAK_v1.0.0.apk
) else (
    echo.
    echo [HATA] Derleme sirasinda bir hata olustu.
)
pause
