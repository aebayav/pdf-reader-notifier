@echo off
setlocal
cd /d "%~dp0"

echo PDF Reader Notifier Docker kurulumu baslatiliyor...

where docker >nul 2>&1
if errorlevel 1 (
    echo [HATA] Docker bulunamadi. Docker Desktop'i kurup calistirin.
    pause
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo [HATA] Docker Desktop calismiyor. Lutfen Docker Desktop'i baslatin.
    pause
    exit /b 1
)

if not exist ".env" (
    echo.
    echo .env bulunamadi. Istege bagli GEMINI_API_KEY ve e-posta ayarlari olmadan devam ediliyor.
    echo Gerekirse proje kokunde .env dosyasi olusturun.
)

echo Container image'lari olusturuluyor ve servisler baslatiliyor...
docker compose up --build -d
if errorlevel 1 (
    echo [HATA] Docker kurulumu basarisiz oldu.
    pause
    exit /b 1
)

echo.
echo Kurulum tamamlandi.
echo Uygulama: http://localhost
echo Backend:  http://localhost:8080
echo Loglar:   docker compose logs -f
echo Durdurmak icin: docker compose down
pause