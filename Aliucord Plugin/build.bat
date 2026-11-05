@echo off
setlocal
where gradle >nul 2>nul
if %errorlevel%==0 (
  gradle %*
  exit /b %errorlevel%
)
set "GRADLE_VERSION=8.13"
set "CACHE_DIR=%USERPROFILE%\.gradle\archive-bootstrap\gradle-%GRADLE_VERSION%"
set "GRADLE_EXE=%CACHE_DIR%\gradle-%GRADLE_VERSION%\bin\gradle.bat"
if exist "%GRADLE_EXE%" goto run
if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"
echo Downloading Gradle %GRADLE_VERSION% from services.gradle.org...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip=Join-Path $env:USERPROFILE '.gradle\archive-bootstrap\gradle.zip'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -LiteralPath $zip -DestinationPath '%CACHE_DIR%' -Force; Remove-Item $zip"
if errorlevel 1 exit /b 1
:run
call "%GRADLE_EXE%" %*
exit /b %errorlevel%
