@echo off
rem Gradle launcher for environments where Gradle is installed.
rem GitHub Actions installs Gradle automatically via setup-gradle.
gradle -p "%~dp0" %*
