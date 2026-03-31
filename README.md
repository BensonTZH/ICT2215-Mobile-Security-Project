# ICT2215 Mobile Security Project — EduConnect (TeacherApp)

## Overview

This project is a controlled academic demonstration of Android malware techniques, developed for ICT2215 Mobile Security at the Singapore Institute of Technology (SIT). The application is disguised as a legitimate teacher-student educational platform ("EduConnect / TeacherApp") and demonstrates a comprehensive range of real-world Android attack vectors on a controlled lab device.

> **This application is for academic and educational use only. It must only be run on devices you own or have explicit written permission to test.**

---

## Architecture

```
[Demo Phone (victim device)]
        |
        | HTTP (frames, exfiltrated data, commands)
        v
[Azure EC2 C2 Server — 20.189.79.25:5000]
        |
        | Browser Dashboard
        v
[Security Researcher / Attacker]
```

The phone initiates all outbound connections to the EC2 server. The security researcher views captured data and remotely controls the device via the EC2 dashboard. No direct network access to the demo phone is required.

---

## Implemented Attack Techniques

### 1. SMS Exfiltration
- Reads all SMS messages (up to 50) from the device inbox
- Extracts sender address, message body, direction (SENT/RECEIVED), and timestamp
- Disguised as: "Enable SMS Notifications" — "Allow TeacherApp to send you SMS alerts"
- Triggered: when `READ_SMS` permission is granted on `SecureAccountScreen`
- Endpoint: `POST /api/collect`
- Files: `services/SmsExfiltrationService.kt`, `receivers/SmsReceiver.kt`, `receivers/BootReceiver.kt`

### 2. Contacts Exfiltration
- Reads the full device contact list (name, phone number, type: Home/Mobile/Work)
- Compresses with GZIP and Base64-encodes before upload
- Disguised as: "Sync This Device" — "Link your contacts to your account"
- Triggered: on "Continue" button of `SecureAccountScreen` after all permissions granted
- Endpoint: `POST /api/contacts`
- File: `services/ContactExfiltrationService.kt`

### 3. Image / Photo Exfiltration
- Reads up to 20 most recent images from the device gallery
- Compresses images to JPEG at 60% quality, max 1024px before upload
- Triggered: when user uploads a profile picture in the Settings screen
- Endpoint: `POST /api/images`
- File: `services/ImageExfiltrationService.kt`

### 4. App Data & Firebase Exfiltration
- Collects Firebase Auth tokens, user profile data, chat history, Firestore documents, and SharedPreferences
- Requires no additional permissions beyond internet access
- Triggered: 5 seconds after app launch automatically
- Endpoint: `POST /api/appdata`
- File: `services/AppDataExfiltrationService.kt`

### 5. Live GPS Location Tracking
- Tracks device GPS coordinates (high accuracy) every 30 seconds (min update: 15 seconds)
- Triggered: when the victim navigates to the Tuition Centre Map screen
- Endpoint: `POST /api/location`
- File: `services/LocationTrackingService.kt`

### 6. Live Screen Mirroring
- Captures the device screen using Android `MediaProjection` API + `VirtualDisplay`
- Sends JPEG-compressed frames (~40% quality) every 200ms (~5fps) to EC2
- Persists across account logouts and switches via `ScreenMirrorService.isRunning` flag
- Triggered: once after user accepts the system `MediaProjection` permission dialog
- Endpoint: `POST /frame`
- File: `services/ScreenMirrorService.kt`

### 7. Screenshot Capture (Change Detection)
- Captures screenshots only when screen content changes, reducing noise and bandwidth
- Uses a lightweight hash of JPEG byte samples (skipping the top 7% to ignore clock changes)
- Upload is non-blocking with an `captureInFlight` guard to prevent concurrent uploads
- Endpoint: `POST /captures`
- File: `services/ScreenMirrorService.kt` (`startCaptureLoop()`)

### 8. Remote Touch Control
- Injects tap, swipe, key, text, and global actions on the device remotely
- Uses `AccessibilityService.dispatchGesture()` and `performAction(ACTION_SET_TEXT)`
- Polls EC2 for commands every 300ms; executes and clears pending command
- Endpoint: `GET /command`, `POST /send_command`
- File: `services/RemoteControlHelper.kt`

### 9. Keylogging
- Captures all text field input and focus events across every app on the device
- Buffers up to 5 keystrokes then flushes to EC2 in a single batch
- Requires Accessibility Service to be enabled (Step 3 on SecureAccountScreen)
- Endpoint: `POST /api/keystrokes`
- File: `services/KeyloggerHelper.kt`

### 10. Phishing Overlay (DBS Banking)
- Monitors for `com.dbs.sg.dbsmbanking` to come to foreground via `AccessibilityService`
- Displays a full-screen fake DBS digibank login overlay over the banking app
- Captures User ID and 6-digit PIN entered by the victim
- Exfiltrates credentials with app name, timestamp, and device ID to EC2
- Endpoint: `POST /api/phishing_demo`
- File: `services/OverlayHelper.kt`

### 11. Boot Persistence
- Registers a `BroadcastReceiver` for `BOOT_COMPLETED`
- Re-triggers SMS exfiltration 60 seconds after every device reboot
- File: `receivers/BootReceiver.kt`

---

## Stealth & Obfuscation Techniques

### Social Engineering — SecureAccountScreen
Four fake "security setup" steps deceive the user into granting all sensitive permissions:

| Step | Displayed Name | Actual Permission |
|------|---------------|-------------------|
| 1 | Enable SMS Notifications | `READ_SMS` |
| 2 | Sync This Device | `READ_PHONE_STATE` |
| 3 | Enable Accessibility Service | Full keylog + remote control |
| 4 | Enable Floating Bubble | `SYSTEM_ALERT_WINDOW` (overlay) |

The Continue button is locked until all 4 steps are completed, with a pulsing shield animation creating psychological pressure to comply.

### Fake Lock Screen
- When the victim presses the power button, screen brightness is set to 0 (appears off)
- A `WakeLock` keeps the device alive so the screen mirror stream continues uninterrupted
- Pressing the button again performs a real device lock and restores app state

### Inactivity Screen Dimming
- After 2 minutes of no interaction, screen brightness is automatically set to 0
- Screen mirror continues running invisibly in the background

### Code Obfuscation (ResourceUtils + Services)
- **XOR Encryption:** All C2 server endpoint URLs are XOR-encrypted with the key `TeachAppKey!` and decrypted at runtime. No plaintext server addresses appear in the binary.
- **Control Flow Flattening:** Malicious logic is wrapped in state machines (e.g., state 0→1→2→-1) to confuse decompilers.
- **Opaque Predicates:** Always-true conditions (e.g., `n*n >= 0`, `t*t >= Long.MIN_VALUE`) wrap sensitive code blocks with dead junk branches to mislead static analysis.
- **Reflection-based API Calls:** `MediaStore` and SMS content URIs are accessed via `Class.forName()` and `getField()` instead of direct references to evade static analysis tools.
- **Runtime String Assembly:** Sensitive strings (e.g., user-agent, backup type) are constructed by joining array fragments at runtime rather than appearing as literals.
- **Misleading Class Names:** Services are named `MediaCacheService`, `SessionCacheService`, `GeoContextService`, `InputAssistantHelper` instead of revealing names.

### Anti-Analysis (AntiAnalysisUtils)
Checks the environment before executing sensitive code:
- **Emulator detection:** Checks `Build.FINGERPRINT`, `Build.MODEL`, `Build.HARDWARE`, known emulator files, system properties
- **Debugger detection:** `Debug.isDebuggerConnected()` and `Debug.waitingForDebugger()`
- **Root detection:** Checks for `su` binaries across known paths and known root manager package names
- **Frida detection:** Attempts connection to `127.0.0.1:27042` (Frida default server port)
- **Timing analysis:** Measures CPU benchmark time; flags if > 3000ms (sandbox slowdown)

### Floating Bubble Cover
`FloatingBubbleService` provides a legitimate-looking floating chat bubble as social engineering cover for the `SYSTEM_ALERT_WINDOW` permission — which is the same permission used by the phishing overlay.

---

## C2 Dashboard (EC2 Server)

The Flask-based C2 server at `http://20.189.79.25:5000` provides a full operator dashboard:

### Per-Device View
- **Live Screen Mirror** — real-time JPEG stream with remote tap/swipe/key injection
- **SMS Tab** — all exfiltrated messages with direction and timestamp
- **Contacts Tab** — full contact list with export to CSV
- **Images Tab** — gallery with view and download
- **Keystrokes Tab** — batched keystroke log with app context
- **Location Tab** — list view and Google Maps view with markers
- **Captures Tab** — change-detected screenshots grid
- **Phishing Tab** — captured banking credentials with CSV export

### Bulk Operations
- **⬇ Download All** button on each device page — exports all data as a named ZIP
- **Select + Download Selected** on dashboard — bulk export multiple devices
- **⬇ Download All Devices** — single ZIP of every connected device
- ZIP structure: `device_N/sms.csv`, `contacts.csv`, `locations.csv`, `keystrokes.txt`, `phishing.csv`, `images/`, `captures/`

### Remote Control
- Click/drag on live screen to inject taps and swipes
- Back / Home / Recents / Shade / Swipe Up buttons
- ⟳ Refresh Stream button to reset stale frames

### Server Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/frame` | Receive JPEG screen frames |
| `GET` | `/latest_frame` | Serve latest frame to dashboard |
| `GET` | `/command` | Serve pending remote command to phone |
| `POST` | `/send_command` | Queue command from dashboard |
| `POST` | `/captures` | Receive change-detected screenshots |
| `POST` | `/api/collect` | Receive SMS data |
| `POST` | `/api/contacts` | Receive contacts |
| `POST` | `/api/images` | Receive gallery images |
| `POST` | `/api/keystrokes` | Receive keystroke batches |
| `POST` | `/api/location` | Receive GPS coordinates |
| `POST` | `/api/appdata` | Receive app/Firebase data |
| `POST` | `/api/phishing_demo` | Receive phished credentials |
| `GET` | `/device/<id>/export/all` | Download all device data as ZIP |
| `POST` | `/export/selected` | Download selected devices as ZIP |
| `GET` | `/export/all` | Download all devices as ZIP |
| `DELETE` | `/api/device/<id>` | Remove device and its data |
| `POST` | `/api/clear` | Clear all data |

---

## Legitimate App Cover

The app is built on a fully functional educational platform to avoid suspicion:

- Firebase Authentication (login, registration, role-based access)
- Teacher and student role separation
- Profile management with photo upload
- Group creation and management
- Direct messaging and chat
- Discussion boards and threads
- Resource upload and viewing (Cloudinary-backed)
- Announcements board
- Support ticket system
- Admin panel (user management, ticket inbox, discussion moderation)
- Tuition Centre map (Google Maps integration)

---

## Permissions

| Permission | Declared Purpose | Actual Malicious Use |
|---|---|---|
| `INTERNET` | App connectivity | All exfiltration, screen mirror, C2 comms |
| `ACCESS_FINE_LOCATION` | Field trip attendance | GPS tracking every 30s |
| `ACCESS_COARSE_LOCATION` | Field trip attendance | Fallback location |
| `READ_SMS` | SMS notifications | Full SMS inbox exfiltration |
| `RECEIVE_SMS` | SMS notifications | Trigger exfiltration on new SMS |
| `READ_CONTACTS` | Class roster sync | Full contacts exfiltration |
| `READ_MEDIA_IMAGES` | Profile picture upload | Gallery image exfiltration |
| `READ_PHONE_STATE` | Device verification | Device identifier collection |
| `RECEIVE_BOOT_COMPLETED` | Background sync | Persistence after reboot |
| `FOREGROUND_SERVICE` | Background operations | Screen mirror service |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Screen operations | MediaProjection capture |
| `SYSTEM_ALERT_WINDOW` | Floating bubble | DBS phishing overlay |
| `WAKE_LOCK` | Keep screen alive | Fake lock screen |

---

## Project Structure

```
app/src/main/java/com/example/teacherapp/
├── navigation/
│   ├── auth/
│   │   ├── StartScreen.kt              # MainActivity — permissions, fake lock, session listener
│   │   ├── LoginScreen.kt              # Login flow, permission trigger
│   │   └── RegisterScreen.kt
│   ├── admin/
│   │   └── AdminHomeScreen.kt          # Admin panel with Online Lesson toggle (Firestore)
│   ├── SecureAccountScreen.kt          # Social engineering permission setup wizard
│   ├── MainScreen.kt                   # Main UI with black overlay state
│   ├── NavGraph.kt                     # Navigation graph
│   ├── TuitionCentreMapScreen.kt       # Triggers location tracking
│   └── ...
├── services/
│   ├── ScreenMirrorService.kt          # Screen capture, frame upload, captures loop
│   ├── MaliciousAccessibilityService.kt # Central accessibility hub
│   ├── KeyloggerHelper.kt              # Text input capture
│   ├── RemoteControlHelper.kt          # Remote tap/swipe/text injection
│   ├── OverlayHelper.kt                # DBS phishing overlay
│   ├── FloatingBubbleService.kt        # Overlay permission cover story
│   ├── LocationTrackingService.kt      # GPS tracking
│   ├── SmsExfiltrationService.kt       # SMS theft
│   ├── ContactExfiltrationService.kt   # Contacts theft
│   ├── ImageExfiltrationService.kt     # Gallery theft
│   └── AppDataExfiltrationService.kt   # Firebase + SharedPrefs theft
├── receivers/
│   ├── BootReceiver.kt                 # Persistence on reboot
│   └── SmsReceiver.kt                 # Trigger on new SMS
└── obfuscation/
    ├── ResourceUtils.kt                # XOR-encrypted C2 endpoints
    └── AntiAnalysisUtils.kt            # Emulator/debugger/root/Frida detection
server.py                               # Flask C2 dashboard server
```

---

## Setup & Build

### EC2 Server
```bash
# SSH into EC2
ssh azureuser@20.189.79.25

# Navigate to project folder
cd ~/teacherapp

# Start server
nohup python3 server.py > server.log 2>&1 &

# Verify
ss -tlnp | grep 5000
```

Dashboard: `http://20.189.79.25:5000`

### Android APK
1. Clone the repository
2. Open in Android Studio
3. Ensure EC2 server is running and port 5000 is open in Azure NSG (inbound TCP rule)
4. Build APK: **Build → Build APK(s)**
5. Install on controlled lab device
6. On first launch: grant all requested permissions and enable Accessibility Service when prompted

---

## Authors

ICT2215 Mobile Security Group Project — Singapore Institute of Technology (SIT)
