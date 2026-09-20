#!/bin/sh
# Gradle Wrapper launcher. The GitHub Actions workflow uses setup-gradle,
# so this project can also be built with: gradle assembleDebug
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
exec gradle -p "$APP_HOME" "$@"
