@echo off
setlocal
set "CAREERUP_ROOT=%~dp0.."
if exist "%CAREERUP_ROOT%\CareerUp.exe" (
  "%CAREERUP_ROOT%\CareerUp.exe" %*
  exit /b
)
set "CAREERUP_JAVA="
if exist "%CAREERUP_ROOT%\runtime\bin\java.exe" set "CAREERUP_JAVA=%CAREERUP_ROOT%\runtime\bin\java.exe"
if not defined CAREERUP_JAVA if exist "%CAREERUP_ROOT%\..\..\Sprint-4\deliverables\runtime\bin\java.exe" set "CAREERUP_JAVA=%CAREERUP_ROOT%\..\..\Sprint-4\deliverables\runtime\bin\java.exe"
if not defined CAREERUP_JAVA if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "CAREERUP_JAVA=%JAVA_HOME%\bin\java.exe"
if not defined CAREERUP_JAVA for /f "delims=" %%J in ('where.exe java.exe 2^>nul') do if not defined CAREERUP_JAVA set "CAREERUP_JAVA=%%J"
if not defined CAREERUP_JAVA for /d %%J in ("%USERPROFILE%\.jdks\*") do if exist "%%J\bin\java.exe" set "CAREERUP_JAVA=%%J\bin\java.exe"
if not defined CAREERUP_JAVA (
  echo Open the complete CareerUp Windows download, which includes Java.
  pause
  exit /b 1
)
"%CAREERUP_JAVA%" -jar "%CAREERUP_ROOT%\launcher\careerup-launcher.jar" "--root=%CAREERUP_ROOT%" %*
if errorlevel 1 pause
