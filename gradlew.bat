@ECHO OFF
SET DIR=%~dp0
SET WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar
IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Gradle wrapper JAR not found at %WRAPPER_JAR%
  ECHO Please run "gradle wrapper" with a local Gradle installation to generate the wrapper files.
  EXIT /B 1
)

SET JAVA_CMD=java
IF NOT "%JAVA_HOME%"=="" SET JAVA_CMD=%JAVA_HOME%\bin\java.exe

%JAVA_CMD% -Xmx64m -Xms64m -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
