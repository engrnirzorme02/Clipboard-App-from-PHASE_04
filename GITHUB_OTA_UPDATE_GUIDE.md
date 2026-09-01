# GitHub Action OTA Update Guide (For AI Agents)

Hello AI Assistant (ChatGPT, Claude, etc.)! 👋

If you are reading this file, the user has asked you to create a **GitHub Action workflow** to build and release this Android application.

This application is designed to support **In-App Over-The-Air (OTA) Updates**. To ensure the in-app updater works perfectly without requiring the user to uninstall the app, you MUST configure the GitHub Action according to the strict rules below.

## 🎯 The Goal
When the user pushes a new version tag (e.g., `v1.0.2`), the GitHub Action must build a **Signed Release APK** and publish it to GitHub Releases. The Android app will ping the GitHub Releases API, find the APK, download it, and prompt the user to install the update.

## ⚠️ CRITICAL RULES FOR THE GITHUB ACTION

### 1. Keystore Signature Matching (App Not Installed Fix)
For the Android OS to allow an in-app update, the new APK **MUST** be signed with the exact same Keystore as the currently installed APK.
- **Do not** build a `debug` APK.
- **Do not** use a random or temporary keystore.
- **Action Required:** Instruct the user to generate a Release Keystore (`.jks`), convert it to Base64, and store it in GitHub Secrets (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD`). Your workflow must decode this keystore and use it to sign the `release` build.

### 2. GitHub Release Naming Convention
The Android app's updater script looks for a specific asset name in the latest release JSON.
- The workflow must upload the APK to GitHub Releases.
- The asset name **MUST** be exactly: `app-release.apk` (or you must update the app's Kotlin code to match whatever name you choose).

### 3. Version Code & Version Name
Android requires the `versionCode` (integer) to increment for every update.
- Ensure the workflow either dynamically injects the `versionCode` based on the GitHub Run Number (`${{ github.run_number }}`), or remind the user to manually bump the `versionCode` in `app/build.gradle.kts` before pushing.

### 4. Required Workflow Triggers
The workflow should ideally trigger on tag creation:
```yaml
on:
  push:
    tags:
      - 'v*' # e.g., v1.0.1
```

## 🛠️ App-Side Context (How the App Works)
1. The app queries `https://api.github.com/repos/{owner}/{repo}/releases/latest`.
2. It compares the `tag_name` (e.g., `v1.0.2`) with the app's current `BuildConfig.VERSION_NAME`.
3. If the tag is newer, it downloads the asset named `app-release.apk` using Android's `DownloadManager`.
4. It triggers an `ACTION_VIEW` intent with the `application/vnd.android.package-archive` MIME type to prompt the system installer.

Please write the `.github/workflows/release.yml` file for the user keeping all these constraints in mind!
