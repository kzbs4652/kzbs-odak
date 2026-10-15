@echo off
chcp 65001 >nul
echo ======================================================
echo   KZBS ODAK - GitHub'a Proje ve APK Gonderici
echo ======================================================
echo.
echo GitHub kullanici adiniz: kzbs4652
echo Repo: https://github.com/kzbs4652/kzbs-odak.git
echo.
echo Lutfen GitHub Personal Access Token'inizi (PAT) yapistirin
echo (GitHub -> Settings -> Developer Settings -> Personal access tokens -> Tokens (classic))
echo.
set /p TOKEN="Token girin: "
if "%TOKEN%"=="" (
    echo Token girilmedi, islem iptal edildi.
    pause
    exit /b
)

echo.
echo GitHub'a gonderiliyor...
C:\Users\Acer\mingit\cmd\git.exe push -u https://kzbs4652:%TOKEN%@github.com/kzbs4652/kzbs-odak.git main --force

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ======================================================
    echo [BASARILI] Tum kodlar ve v1.0.0 APK GitHub'a yuklendi!
    echo ======================================================
) else (
    echo.
    echo [HATA] Yukleme basarisiz oldu. Token yetkilerini kontrol edin.
)
pause

