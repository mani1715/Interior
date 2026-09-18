@echo off
setlocal
set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=."
set "APP_HOME=%DIRNAME%"
if "%APP_HOME:~-1%"=="\" set "APP_HOME=%APP_HOME:~0,-1%"

if not "%JAVA_HOME%" == "" (
  set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVACMD=java"
)

set "WRAPPER_JAR=%APP_HOME%\.mvn\wrapper\maven-wrapper.jar"

"%JAVACMD%" "-Dmaven.multiModuleProjectDirectory=%APP_HOME%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
