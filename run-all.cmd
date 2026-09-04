@echo off
setlocal
REM PDF Reader Notifier - Frontend ve backend'i birlikte baslatir.

echo Backend baslatiliyor...
start "PDF Reader Backend" "%ComSpec%" /k call "%~dp0run-backend.cmd"

echo Frontend baslatiliyor...
pushd "%~dp0pdf-reader-frontend"
start "PDF Reader Frontend" "%ComSpec%" /k npm run dev
popd

echo Frontend ve backend ayri pencerelerde baslatildi.