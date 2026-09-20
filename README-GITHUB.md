# Build the APK on GitHub

1. Create a new GitHub repository.
2. Upload **the contents of this `VideoSplitter` folder** to the repository root (so `settings.gradle` is at the root).
3. Open the **Actions** tab.
4. Select **Build Android APK** and run it, or push a commit to trigger it automatically.
5. After the workflow finishes, open the workflow run and download the **VideoSplitter-debug-apk** artifact.
6. Extract the artifact and install the APK on your Android phone.

The workflow uses JDK 17, Gradle 8.11.1, and Android SDK tooling on GitHub's Ubuntu runner.
