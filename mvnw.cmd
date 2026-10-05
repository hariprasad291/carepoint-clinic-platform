@echo off
setlocal
set "MAVEN_VERSION=3.9.9"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_EXE=%MAVEN_HOME%\runtime\bin\mvn.cmd"
if not exist "%MAVEN_EXE%" (
  set "MAVEN_EXE=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"
)
if not exist "%MAVEN_EXE%" (
  echo Downloading Apache Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $dir='%MAVEN_HOME%'; New-Item -ItemType Directory -Force -Path $dir | Out-Null; $zip=Join-Path $env:TEMP 'apache-maven-%MAVEN_VERSION%-bin.zip'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Path $zip -DestinationPath $dir -Force; Remove-Item -LiteralPath $zip -Force"
  if errorlevel 1 exit /b 1
  set "MAVEN_EXE=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd"
)
call "%MAVEN_EXE%" %*
exit /b %ERRORLEVEL%
