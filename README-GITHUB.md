# Video Splitter — GitHub Build

This project includes the real `.github/workflows/build-apk.yml` workflow.

If your Windows/file picker hides dot-folders, there is also a visible `GITHUB` folder containing a backup copy of the workflow.
GitHub Actions requires the final path to be exactly:
`.github/workflows/build-apk.yml`

The workflow installs Gradle 8.11.1 and JDK 17 on the GitHub runner, then builds `app/build/outputs/apk/debug/app-debug.apk` and uploads it as an Actions artifact.

The project also includes `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties`. The GitHub workflow intentionally uses the Gradle version installed by `gradle/actions/setup-gradle`, so a Gradle wrapper JAR is not required for the cloud build.
