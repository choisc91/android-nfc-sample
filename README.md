# NFC Read/Write Example for Android

A lightweight Android app to demonstrate how to read and write NDEF messages to NFC tags using Kotlin and Jetpack Compose.  
This project supports both formatted and formatable NFC tags and includes basic Compose UI for interaction.

---

## 🚀 Getting Started

### Requirements

- Android Studio Meerkat | 2024.3.2 Patch 1 or later
- Kotlin 2.0 (K2 mode enabled)
- Android SDK 35
- Physical Android device with NFC enabled
- Minimum supported Android version: API 26 (Android 8.0)

---

## 📲 Features

- ✅ Read NDEF messages from NFC tags
- ✅ Write plain text to NDEF or NdefFormatable tags
- 🔁 Auto timeout after 5 seconds in write mode
- ⚠️ Prevents write attempts to unsupported tags (`MifareClassic`, `IsoDep`, etc.)
- 🧩 Jetpack Compose UI displaying scanned tag contents

---

## 🧪 Tech Stack

| Layer         | Library / Tool                       |
|---------------|--------------------------------------|
| Language      | Kotlin `2.0.0` (K2 mode)             |
| Android Gradle Plugin | `8.8.1`                     |
| UI            | Jetpack Compose BOM `2024.04.01`     |
| Material      | Compose Material3                    |
| Lifecycle     | `androidx.lifecycle.runtime-ktx:2.8.7` |
| NFC           | Android NFC SDK (`Ndef`, `NdefFormatable`) |
| Build System  | Kotlin DSL + Plugin Aliases          |
| Testing       | JUnit, Espresso, Compose UI Test     |

---

## 📦 Dependencies (`libs.versions.toml`)

```toml
[versions]
agp = "8.8.1"
kotlin = "2.0.0"
coreKtx = "1.15.0"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.10.0"
composeBom = "2024.04.01"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```
---

## 🏷 Supported Tag Types

- ✅ `android.nfc.tech.Ndef`  
   → Tag is already formatted and ready to read/write using NDEF  
- ✅ `android.nfc.tech.NdefFormatable`  
   → Tag is not yet formatted, but can be initialized with NDEF format on first write  
- ❌ Not supported:
  - `android.nfc.tech.MifareClassic`
  - `android.nfc.tech.IsoDep`
  - `android.nfc.tech.Felica`
  - All other secure or non-NDEF tags

- ℹ️ Unsupported tags will trigger a toast warning:  
  - `"This tag is not writable (NDEF not supported)"`

---

## 🧩 App Structure

📁 app/
┣ 📄 MainActivity.kt # NFC read/write dispatch and lifecycle control
┣ 📄 ReadTagDataSource.kt # Parses NDEF messages from incoming tags
┣ 📄 WriteTagDataSource.kt # Writes NDEF or formats NdefFormatable tags
┣ 📄 BuildMain.kt # Jetpack Compose UI for read/write input
┣ 📄 BuildWriteProgress.kt # Optional write-mode progress indicator

---

## 🔧 Development Environment

| Item              | Version                           |
|-------------------|------------------------------------|
| Android Studio    | Meerkat 2024.3.2 Patch 1           |
| Build Number      | AI-243.26053.27.2432.13536105      |
| Runtime           | JetBrains OpenJDK 21.0.6 (K2 mode) |
| OS                | Ubuntu 20.04.6 LTS (glibc 2.31)    |
| Kernel            | Linux 5.15.0-107-generic           |
| Memory            | 8 GB RAM, 16 Cores                 |
| Kotlin Plugin     | Kotlin K2 Compiler enabled         |
| Compose Plugin    | org.jetbrains.compose.intellij.platform:0.1.0 |

---

## 🛡 Permissions

Declare the following in your `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />

