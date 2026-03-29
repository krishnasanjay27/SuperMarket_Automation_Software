@echo off
REM ─────────────────────────────────────────────────────────────────────────
REM  Supermarket Automation System – Compile and Launch GUI
REM  Double-click this file from the software\ directory.
REM ─────────────────────────────────────────────────────────────────────────

setlocal enabledelayedexpansion

REM Resolve absolute paths
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"

set "LIBDIR=%ROOT%\lib"
set "OUTDIR=%ROOT%\out"

set "LIBS=%LIBDIR%\javafx.base.jar;%LIBDIR%\javafx.controls.jar;%LIBDIR%\javafx.fxml.jar;%LIBDIR%\javafx.graphics.jar;%LIBDIR%\javafx.media.jar;%LIBDIR%\mysql-connector-j-9.6.0.jar"

REM ── CRITICAL: Add lib\ to PATH so JavaFX DLLs can find each other ──────────
set "PATH=%LIBDIR%;%PATH%"

echo ============================================================
echo   Supermarket Automation System  –  Compile ^& Launch
echo ============================================================
echo.

REM ── Step 1: Compile ────────────────────────────────────────────────────────
echo [1/3] Compiling Java sources...

if exist "%ROOT%\.build_sources.txt" del "%ROOT%\.build_sources.txt" >nul 2>&1

for /R "%ROOT%\src" %%f in (*.java) do (
    set "fname=%%~nf"
    if not "!fname!"=="Main" (
        echo %%f>> "%ROOT%\.build_sources.txt"
    )
)

javac -encoding UTF-8 ^
  -cp "%LIBS%" ^
  -d "%OUTDIR%" ^
  -sourcepath "%ROOT%\src" ^
  @"%ROOT%\.build_sources.txt"

del "%ROOT%\.build_sources.txt" >nul 2>&1

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Compilation failed. Fix errors above and retry.
    pause
    exit /b 1
)
echo   Compilation successful.
echo.

REM ── Step 2: Copy resources ─────────────────────────────────────────────────
echo [2/3] Copying FXML and CSS resources...
xcopy /E /I /Y "%ROOT%\src\app\views\*" "%OUTDIR%\app\views\" >nul
echo   Resources copied.
echo.

REM ── Step 3: Launch GUI ─────────────────────────────────────────────────────
echo [3/3] Launching Supermarket Automation System...
echo.

java ^
  -Djava.library.path=%LIBDIR% ^
  -Dprism.order=d3d,sw ^
  -Dprism.verbose=false ^
  -Dfile.encoding=UTF-8 ^
  --module-path "%LIBDIR%" ^
  --add-modules javafx.controls,javafx.fxml ^
  -cp "%OUTDIR%;%LIBS%" ^
  app.MainApp

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Application exited with an error.
    echo.
    echo Common causes:
    echo   ^(1^) Graphics driver issue  – try updating your display driver.
    echo   ^(2^) Database not running    – start MySQL and verify credentials.
    echo   ^(3^) Wrong Java version      – requires Java 17 with the bundled JavaFX 17 SDK.
)

echo.
pause
