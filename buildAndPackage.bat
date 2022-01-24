@echo off

rem This script is for local testing of the packaging process and is not use for the actual build process.

call C:\apache-maven-3.8.1\bin\mvn clean package

"%JAVA_HOME%\bin\jpackage" --verbose --app-version 99.0.1 "@jpackage/jpackage.cfg" "@jpackage/jpackage-windows-image.cfg" 
