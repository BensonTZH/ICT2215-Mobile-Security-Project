# EduConnect — ICT2215 Mobile Security Project

**Singapore Institute of Technology · ICT2215 Mobile Security · AY2025/26 Trimester 2 · Group 4**

> **Academic Research Disclaimer**
> This application was developed exclusively for the ICT2215 Mobile Security coursework at SIT. All malicious capabilities demonstrated here are for controlled educational analysis only. The APK must only be run on devices you own or have explicit written permission to test. Deploying this application against real users without consent is illegal.

---

## Overview

EduConnect is a dual-purpose Android application that presents itself as a fully functional tutoring marketplace for Singapore students and teachers, while concealing a comprehensive suite of malicious capabilities to demonstrate real-world Android threat vectors.

**The legitimate surface** provides a feature-complete educational platform: student/teacher/admin role separation, real-time Firebase messaging, group management, resource sharing via Cloudinary, tuition centre maps, and a support ticket system.

**The malicious layer** implements seven distinct attack modules covering data exfiltration, real-time surveillance, remote device control, and credential phishing — all coordinated through a Command and Control (C2) server hosted on Microsoft Azure.

**The obfuscation layer** applies five independent techniques to resist static analysis, dynamic sandboxing, and reverse engineering.

---

## Architecture

```
┌────────────────────────────────────────┐
│        Target Android Device           │
│  EduConnect.apk (com.example.teacherapp) │
│                                        │
│  ┌──────────────┐  ┌────────────────┐  │
│  │  Legitimate  │  │    Malicious   │  │
│  │   App Layer  │  │  Service Layer │  │
│  │  (Firebase + │  │ (Exfiltration, │  │
│  │  Compose UI) │  │  Keylog, RAT)  │  │
│  └──────────────┘  └───────┬────────┘  │
└──────────────────────────── │ ──────────┘
                              │ HTTP POST (data, frames)
                              │ HTTP GET  (commands)
                              ▼
              ┌───────────────────────────┐
              │   Azure C2 Server         │
              │   Flask Dashboard         │
              │   Per-device data store   │
              └──────────┬────────────────┘
                         │ Browser
                         ▼
              ┌───────────────────────────┐
              │   Security Researcher     │
              │   Live screen mirror      │
              │   Remote control          │
              │   Exfiltrated data        │
              └───────────────────────────┘
```

All connections are device-initiated outbound. No inbound port to the target device is needed.

---

## Malicious Modules (Part 2)

### 1. Contacts Collection and Exfiltration
- **File:** `services/RosterSyncService.kt`
- **Trigger:** Completion of the SecureAccountScreen onboarding (READ_CONTACTS granted)
- **Method:** Reads full contact database (name, phone, type) via reflection-based access to `ContactsContract`
- **Encoding:** GZIP-compressed, Base64-encoded JSON payload
- **Endpoint:** `POST /api/contacts`

### 2. Image / Gallery Exfiltration
- **File:** `services/MediaCacheWorker.kt`
- **Trigger:** Co-triggered during profile picture selection (READ_MEDIA_IMAGES granted)
- **Method:** Enumerates top 20 most recent gallery images, filters files > 5 MB, resizes to max 1024px, compresses to JPEG at 60% quality
- **Endpoint:** `POST /api/images`

### 3. App and Firebase Data Exfiltration
- **File:** `services/SessionCacheService.kt`
- **Trigger:** Auto-started 5 seconds after app launch — requires only INTERNET permission
- **Method:** Collects Firebase Auth credentials, Firestore user profile, full chat and message history, all SharedPreferences namespaces
- **Endpoint:** `POST /api/appdata`

### 4. GPS Location Tracking
- **File:** `services/GeoContextService.kt`
- **Trigger:** Co-triggered when user opens the Tuition Centre Map screen (ACCESS_FINE_LOCATION granted)
- **Method:** `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`, 30-second polling interval; transmits lat, lon, accuracy, altitude, speed, and timestamp
- **Endpoint:** `POST /api/location`

### 5. Keylogging via Accessibility Service
- **File:** `services/TextSyncHelper.kt` (called from `services/InputAssistService.kt`)
- **Trigger:** InputAssistService enabled during SecureAccountScreen onboarding
- **Method:** Listens for `TYPE_VIEW_TEXT_CHANGED` and `TYPE_VIEW_FOCUSED` AccessibilityEvents system-wide; buffers up to 5 keystrokes then flushes as a JSON batch including package name, class name, and timestamp
- **Endpoint:** `POST /api/keystrokes`

### 6. Screen Mirroring and Remote Control
- **File:** `services/ScreenMirrorService.kt`, `services/GestureHelper.kt`
- **Trigger:** User accepts MediaProjection consent dialog (framed as "Start Online Lesson")
- **Method:**
  - Continuous frame stream via `MediaProjection` + `VirtualDisplay`, JPEG at 40% quality, ~5fps → `/frame`
  - Change-detected screenshot capture using frame-hash delta → `/captures`
  - Command polling every 300ms → dispatches tap, swipe, text, key, and global actions via `AccessibilityService.dispatchGesture()`
- **Endpoints:** `POST /frame`, `POST /captures`, `GET /command`

### 7. DBS Digibank Phishing Overlay
- **File:** `services/UiLayerHelper.kt` (called from `services/InputAssistService.kt`)
- **Trigger:** InputAssistService detects `com.dbs.sg.digibank` coming to foreground via `TYPE_WINDOW_STATE_CHANGED` AccessibilityEvent
- **Method:** Renders a pixel-perfect replica DBS digibank login screen as a full-screen `TYPE_APPLICATION_OVERLAY` window. Captures User ID and 6-digit PIN on form submission.
- **Endpoint:** `POST /api/phishing_demo`

### 8. Boot Persistence
- **File:** `receivers/StartupReceiver.kt`
- **Trigger:** `BOOT_COMPLETED` broadcast, 60-second delay (wrapped in safety gate check)
- **Method:** Re-initialises background services after device reboot via `DeviceCompatUtils.executeIfSafe()`

---

## Obfuscation Techniques (Part 3)

### Layer 1 — R8/ProGuard Compile-time Obfuscation
- All internal class and method names renamed to single/double-character identifiers (`a`, `a0`, `ff`, `uq0`)
- Entire class hierarchy repackaged into `com.example.teacherapp.core` (flat namespace)
- All `android.util.Log` calls stripped via `-assumenosideeffects`
- Source file name metadata replaced with `"SourceFile"` via `-renamesourcefileattribute`
- Kotlin null-check intrinsics stripped to reduce bytecode noise

### Layer 2 — XOR-Encrypted C2 Endpoints (ThemeConfigUtils)
- 14 sensitive strings (all C2 endpoints, phishing target package, runtime tags) stored as raw byte arrays XOR-encrypted with key `"TeachAppKey!"`
- Decryption occurs in memory at point of use only — no plaintext URL appears in DEX
- Three additional strings (auth token, payload type, clipboard endpoint) use runtime `listOf().joinToString("")` assembly
- **File:** `obfuscation/ResourceUtils.kt` (class `ThemeConfigUtils`)

### Layer 3 — Manual Source-level Obfuscation
- **Control flow flattening:** Malicious methods rewritten as `while(true)` integer state machines (states 0→1→2→−1) destroying sequential structure that decompilers rely on
- **Opaque predicates:** All sensitive blocks wrapped in always-true `n² ≥ 0` conditions with dead junk branches (fake `StringBuilder` loops, fake array computations) to force decompiler false-path analysis
- **Junk arithmetic:** Unique XOR expressions (`state * 31 + 7 xor 0xFF` etc.) computed and discarded inside every state loop iteration
- **Reflection-based API access:** `ContactsContract` accessed via `Class.forName().getField()` — hides contacts API references from static call-graph analysis; package comparison in `UiLayerHelper` done via reflected `String.equals()`
- **Decoy file inflation:** 56 production-quality benign classes across `services/` and `obfuscation/` packages (e.g., `AppUpdateChecker`, `ConfigEncryptor`, `BiometricAuthManager`, `RecommendationEngine`) inflate the class list and increase analyst triage burden

### Layer 4 — Anti-analysis Safety Gate (DeviceCompatUtils)
All malicious payloads are gated behind `executeIfSafe()`, which requires all four checks to pass:

| Check | Method | Technique |
|---|---|---|
| Emulator | `isEmulator()` | 17 `Build.*` property checks + 10 known emulator phone numbers + 7 QEMU filesystem paths + 5 `getprop` properties |
| Debugger | `isDebuggerConnected()` | `Debug.isDebuggerConnected()` + `Debug.waitingForDebugger()` |
| Root | `isDeviceRooted()` | 10 `su` binary paths + 12 root manager package names + writable system partition check |
| Frida | `isFridaDetected()` | TCP connect to `127.0.0.1:27042` (Frida server default port) |

- **File:** `obfuscation/DeviceCompatUtils.kt`

### Layer 5 — obfuscapk Bytecode-level String Encryption
- Post-build pass using [obfuscapk](https://github.com/ClaudiuGeorgiu/obfuscapk) `StringEncryption` plugin
- Replaces every `const-string` DEX instruction with a call to an injected `com.decryptstringmanager.DecryptString.decipher()` stub
- Result: zero plaintext strings in the DEX string pool — IP address, endpoint paths, service names, and JSON keys are all absent from `strings` output
- Produces `EduConnect_obfuscated.apk` from `EduConnect.apk`

---

## Permission Map

| Permission | Legitimate Justification | Malicious Use |
|---|---|---|
| `READ_CONTACTS` | ContactSyncScreen — find EduConnect users | `RosterSyncService` exfiltrates full contact list |
| `READ_MEDIA_IMAGES` | Profile picture upload | `MediaCacheWorker` exfiltrates top-20 gallery images |
| `ACCESS_FINE_LOCATION` | Tuition centre map | `GeoContextService` continuous GPS tracking |
| `INTERNET` | Firebase, Cloudinary, maps | All HTTP exfiltration to C2 |
| `RECEIVE_BOOT_COMPLETED` | (not surfaced to users) | `StartupReceiver` reactivates services after reboot |
| `BIND_ACCESSIBILITY_SERVICE` | "Enhanced text input support" | Keylogging + phishing overlay trigger |
| `SYSTEM_ALERT_WINDOW` | "Quick-access chat bubble" | `TYPE_APPLICATION_OVERLAY` phishing screen |
| `FOREGROUND_SERVICE` | Background notifications | Keeps all services alive |
| `READ_PHONE_STATE` | "Device sync" onboarding step | Android ID device fingerprinting |

---

## Social Engineering — SecureAccountScreen

The onboarding screen presents three mandatory steps that must all be completed before the Continue button activates:

| Step | Displayed to User | Actual Purpose |
|---|---|---|
| 1 — Sync This Device | "Link your contacts to find people you know" | Grants `READ_PHONE_STATE` for device fingerprinting |
| 2 — Enable Accessibility Service | "Enhanced text input support for faster communication" | Activates `InputAssistService` — keylogger + phishing trigger |
| 3 — Enable Floating Bubble | "Quick-access chat like Facebook Messenger" | Grants `SYSTEM_ALERT_WINDOW` — enables phishing overlay |

A `DisposableEffect` lifecycle observer re-checks all permission states on every `ON_RESUME` to prevent bypassing any step.

---

## C2 Dashboard

The Flask C2 server provides a per-device dashboard with:

- **Live screen mirror** — real-time JPEG stream with click-to-inject remote control
- **Contacts tab** — full contact list with CSV export
- **Images tab** — exfiltrated gallery with download
- **Keystrokes tab** — timestamped keystroke log with source app context
- **Location tab** — GPS coordinate list + Google Maps view
- **App data tab** — Firebase credentials and session data
- **Phishing tab** — captured banking credentials with CSV export
- **Bulk export** — per-device or all-devices ZIP download

### Server Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/frame` | Receive JPEG screen frames |
| `GET` | `/latest_frame` | Serve latest frame to dashboard |
| `GET` | `/command` | Serve pending remote command to device |
| `POST` | `/send_command` | Queue command from dashboard operator |
| `POST` | `/captures` | Receive change-detected screenshots |
| `POST` | `/api/contacts` | Receive GZIP+Base64 contact payload |
| `POST` | `/api/images` | Receive JPEG image batches |
| `POST` | `/api/keystrokes` | Receive keystroke batches |
| `POST` | `/api/location` | Receive GPS telemetry |
| `POST` | `/api/appdata` | Receive Firebase/app session data |
| `POST` | `/api/phishing_demo` | Receive phished banking credentials |
| `GET` | `/device/<id>/export/all` | Download all data for one device as ZIP |
| `POST` | `/export/selected` | Download selected devices as ZIP |
| `GET` | `/export/all` | Download all devices as ZIP |
| `DELETE` | `/api/device/<id>` | Remove device record |

---

## Project Structure

```
EduConnect/
├── app/
│   └── src/main/java/com/example/teacherapp/
│       ├── CloudinaryApplication.kt          # Application class
│       ├── MainActivity.kt                   # Entry point, MediaProjection launcher
│       ├── navigation/
│       │   ├── auth/
│       │   │   ├── StartScreen.kt            # Permission triggers, session listener
│       │   │   ├── LoginScreen.kt
│       │   │   └── RegisterScreen.kt
│       │   ├── SecureAccountScreen.kt        # Social engineering onboarding wizard
│       │   ├── TuitionCentreMapScreen.kt     # Triggers GPS tracking
│       │   ├── NavGraph.kt
│       │   └── ...                           # All other screens (legitimate)
│       ├── services/
│       │   ├── InputAssistService.kt         # AccessibilityService hub (keylog + phishing)
│       │   ├── TextSyncHelper.kt             # Keylogging logic
│       │   ├── GestureHelper.kt              # Remote tap/swipe/text injection
│       │   ├── UiLayerHelper.kt              # DBS phishing overlay
│       │   ├── QuickAccessService.kt         # Floating bubble (SYSTEM_ALERT_WINDOW cover)
│       │   ├── ScreenMirrorService.kt        # MediaProjection frame stream + captures
│       │   ├── RosterSyncService.kt          # Contacts exfiltration
│       │   ├── MediaCacheWorker.kt           # Gallery image exfiltration
│       │   ├── SessionCacheService.kt        # Firebase/app data exfiltration
│       │   ├── GeoContextService.kt          # GPS location tracking
│       │   ├── NotificationSyncService.kt    # (Boot-triggered background init)
│       │   ├── MediaStreamService.kt         # (Stream session management)
│       │   └── [39 decoy service classes]    # Benign inflation classes
│       ├── receivers/
│       │   ├── StartupReceiver.kt            # BOOT_COMPLETED persistence
│       │   └── MessageReceiver.kt            # (Broadcast handler)
│       └── obfuscation/
│           ├── ResourceUtils.kt              # ThemeConfigUtils — XOR-encrypted endpoints
│           ├── DeviceCompatUtils.kt          # Anti-analysis safety gate
│           └── [17 decoy obfuscation classes] # Benign inflation classes
├── app/proguard-rules.pro                    # R8 aggressive obfuscation config
├── app/build.gradle.kts                      # Release build config (minify + shrink)
├── EduConnect.apk                            # Release APK (pre-obfuscapk)
├── EduConnect_obfuscated.apk                 # Final APK (post-obfuscapk StringEncryption)
├── Final_Report.md                           # Full ICT2215 project report (Parts 1–3)
└── Part3.md                                  # Extended Part 3 obfuscation analysis
```

---

## Build Instructions

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- `google-services.json` placed in `app/` (Firebase project config)
- `app/keystore.jks` (release signing keystore)
- Python 3.9+ with Flask (for C2 server)
- `obfuscapk` installed (`pip install obfuscapk`) for the final obfuscation pass

### Build APK

```bash
# Release build (R8 obfuscation applied automatically)
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
# Rename:
cp app/build/outputs/apk/release/app-release.apk EduConnect.apk
```

### Apply obfuscapk (StringEncryption pass)

```bash
# Install obfuscapk
pip install obfuscapk

# Apply StringEncryption plugin
obfuscapk -o StringEncryption EduConnect.apk -w EduConnect_obfuscated.apk
```

`EduConnect_obfuscated.apk` is the final deliverable. Install this on the test device.

### C2 Server

```bash
# On the Azure VM
cd ~/educonnect-server
pip install flask

# Start server (background)
nohup python3 server.py > server.log 2>&1 &

# Verify listening
ss -tlnp | grep 5000
```

---

## Team

| Role | Name | Student ID |
|---|---|---|
| IS | Wu Wen Jiang | 2401220 |
| IS | Ezra Ho Jincheng | 2403326 |
| IS | Tan Jun An | 2400983 |
| IS | Elgin Ling Jun Hao | 2400885 |
| SE | Deric Allen Bautista Mayores | 2302057 |
| SE | Ng Zheng Wei Dennis | 2301813 |
| SE | Benson Tan Zhong Hao | 2301808 |

---

## Tech Stack

| Component | Technology |
|---|---|
| Android UI | Jetpack Compose + Material 3 |
| Navigation | AndroidX Navigation Component |
| Backend (legitimate) | Firebase Auth + Firestore |
| Media storage | Cloudinary |
| Maps | Google Maps SDK + FusedLocationProviderClient |
| C2 server | Python Flask |
| Compile-time obfuscation | R8 / ProGuard |
| Bytecode obfuscation | obfuscapk (StringEncryption) |
| Min SDK | API 25 (Android 7.1) |
| Target SDK | API 36 |
| Language | Kotlin |
