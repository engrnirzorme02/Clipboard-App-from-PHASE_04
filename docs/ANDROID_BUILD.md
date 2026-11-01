# Android integration baseline

This is step 5.1 of the Clipboard consolidation. The integration branch starts
at `719672fff58fbfe4bbc9814f46b24977a4743fac`, preserving the checksum fix from
draft PR #2. Voice, database migration and authenticated cloud integration are
subsequent steps in the agreed sequence.

## Build environment

The development environment used for this baseline is Linux x64:

- A complete JDK 21 installation, including `javac`; a Java runtime alone is insufficient.
  AGP requires JDK 17 or newer, while the existing Robolectric SDK 36 tests
  require Java 21. JDK 17 can build the APK but cannot run that full test suite.
- Android SDK Platform 35 and Build Tools 35.0.0.
- AGP 8.10.1 and the checked-in Gradle 8.11.1 wrapper.
- Kotlin Android and Compose plugins 2.2.10, with Java/Kotlin bytecode targets aligned to 11.

AGP's documented compatibility requirements are available at
[Android Developers](https://developer.android.com/build/releases/agp-8-10-0-release-notes).
The Gradle distribution SHA-256 is pinned in the wrapper properties.

With Android command-line tools available, install the required SDK packages:

```bash
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

Set `JAVA_HOME` to the complete JDK and `ANDROID_HOME` to the SDK, or configure
the SDK path in the ignored `local.properties` file. Then run:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`.
JUnit XML results: `app/build/test-results/testDebugUnitTest/`.

## Optional services and signing

The Gradle build supplies an empty `GEMINI_API_KEY` string,
so a build can compile without a real key. Configure the ignored `.env` file
when enabling AI calls. Never commit API keys. Missing `google-services.json`
uses the existing Google Services warning strategy; production Firebase
ownership, security rules and account isolation remain later integration work.

Debug builds use Android Gradle Plugin's normal generated debug keystore.
Release signing still uses the existing release configuration and its external
keystore/password inputs. A newly generated debug key is not evidence that an
APK can update an older installed build: compare signing identities and protect
existing device data before an installation or update test.

## Focused regression tests

```bash
./gradlew testDebugUnitTest --tests com.example.BackupRepositoryChecksumTest
./gradlew testDebugUnitTest --tests com.example.GeminiApiLoggingTest
```

The checksum contract covers unchanged exports, modified data, malformed input
and legacy backups. The logging regression sends synthetic credentials and
payloads to a local MockWebServer through the actual production HTTP client,
then checks captured HTTP logs for those markers. It makes no Gemini API call.
HTTP content logging is disabled in the production client for every build type.

Passing these tests and building a debug APK do not verify physical-device
clipboard behavior, background service survival, full data restoration, live
AI model support, or authenticated cloud isolation. Those checks remain in the
later consolidation steps. The existing encrypted-backup key/recovery and
full-fidelity database migration findings remain open.
