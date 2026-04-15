@echo off
setlocal EnableExtensions
cd /d "%~dp0"

REM Keystore : copier keystore.properties.example vers keystore.properties et créer le .jks (voir exemple).
if not exist "keystore.properties" (
  echo [ERREUR] Fichier keystore.properties introuvable.
  echo Copie keystore.properties.example vers keystore.properties et remplis les champs ^(voir commentaires dans l'exemple^).
  exit /b 1
)

REM JDK embarqué Android Studio (adapter si besoin)
if defined JAVA_HOME goto run
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
  set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
  goto run
)
if exist "%LocalAppData%\Programs\Android Studio\jbr\bin\java.exe" (
  set "JAVA_HOME=%LocalAppData%\Programs\Android Studio\jbr"
  goto run
)
REM JDK JetBrains / Cursor : %USERPROFILE%\.jdks\ (ex. ms-17.0.18)
if exist "%USERPROFILE%\.jdks\ms-17.0.18\bin\java.exe" (
  set "JAVA_HOME=%USERPROFILE%\.jdks\ms-17.0.18"
  goto run
)
for /f "delims=" %%J in ('dir /b /ad /o-d "%USERPROFILE%\.jdks" 2^>nul') do (
  if exist "%USERPROFILE%\.jdks\%%J\bin\java.exe" (
    set "JAVA_HOME=%USERPROFILE%\.jdks\%%J"
    goto run
  )
)
echo [ERREUR] JAVA_HOME non défini et aucun JDK trouvé ^(jbr Android Studio ou .jdks dans ton profil^).
echo Définis JAVA_HOME ou ajoute org.gradle.java.home dans local.properties ^(voir local.properties.example^).
exit /b 1

:run
REM Libère les jvm.dll sous .gradle\.tmp\jdks (sinon « fichier utilisé par un autre processus »)
call gradlew.bat --stop 2>nul
if defined JAVA_HOME (
  call gradlew.bat "-Dorg.gradle.java.home=%JAVA_HOME%" bundleRelease --no-daemon
) else (
  call gradlew.bat bundleRelease --no-daemon
)
set "ERR=%ERRORLEVEL%"
if not "%ERR%"=="0" exit /b %ERR%

set "AAB=app\build\outputs\bundle\release\app-release.aab"
if exist "%AAB%" (
  echo.
  echo ===== AAB prêt pour test interne Play Console =====
  echo %CD%\%AAB%
) else (
  echo Fichier AAB attendu introuvable : %AAB%
  exit /b 1
)
endlocal
