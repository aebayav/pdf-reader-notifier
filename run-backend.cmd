@echo off
setlocal
REM ============================================================
REM  PDF Reader Notifier - Backend Calistirici
REM  Derler (gerekirse) ve http://localhost:8080'de baslatir.
REM  Durdurmak icin: bu pencerede CTRL+C
REM ============================================================

REM Java 26 (JDK) - sistemdeki diger java'larla karismasin diye net yol
set "JAVA_HOME=C:\Program Files\Java\jdk-26"

REM 0) Kok dizindeki .env dosyasini yukle (GEMINI_API_KEY vb.)
if exist "%~dp0.env" (
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do set "%%A=%%B"
)

REM 1) Maven'i bul (mvnw tarafindan indirilmis dagitim; PATH'te olmasa da calisir)
set "MVN="
for /f "delims=" %%F in ('dir /s /b "%USERPROFILE%\.m2\wrapper\dists\mvn.cmd" 2^>nul') do if not defined MVN set "MVN=%%F"

if not defined MVN (
    echo [HATA] Maven bulunamadi.
    echo        Ya mvnw.cmd ile bir kez derleme baslatin ^(Maven'i indirecek^)
    echo        ya da Maven kurup PATH'e ekleyin.
    pause
    exit /b 1
)

cd /d "%~dp0pdf-reader-backend"
if errorlevel 1 (
    echo [HATA] pdf-reader-backend klasoru bulunamadi.
    pause
    exit /b 1
)

echo === 1/2 Derleniyor (Maven package) ===
call "%MVN%" -B package -DskipTests
if errorlevel 1 (
    echo.
    echo [HATA] Derleme basarisiz oldu. Yukaridaki ciktiyi kontrol edin.
    pause
    exit /b 1
)

echo === 2/2 Baslatiliyor: http://localhost:8080 ===
echo (Not: PostgreSQL servisi ve Tesseract kurulu olmali)
"%JAVA_HOME%\bin\java" -jar target\backend-0.0.1-SNAPSHOT.jar

REM java kapandiginda kullaniciya haber ver
echo.
echo Backend kapandi. Kapatmak icin bir tusa basin...
pause >nul
