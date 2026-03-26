# ICT2215 Mobile Security Project — TeacherApp Malware Demo

## Overview

This project is a controlled academic demonstration of Android malware techniques, developed for ICT2215 Mobile Security at SIT. The app is disguised as a legitimate teacher-student platform ("TeacherApp") and demonstrates a range of real-world attack vectors on a controlled lab device.

> **For academic and security research use only. Do not deploy on any device without explicit consent from the device owner.**

---

## Demo Architecture

```
[Demo Phone (victim)]
        |
        | HTTP POST (frames, data)
        v
[EC2 Relay Server — 20.189.79.25:5000]
        |
        | Browser / Dashboard
        v
[Security Tester (attacker)]
```

The phone always initiates outbound connections to the EC2 server. The security tester views and controls the phone via the EC2 relay — no direct network access to the demo phone is required.

---

## Implemented Attack Techniques

### 1. SMS Exfiltration
- Reads all SMS messages from the device
- Triggered: 10 seconds after app permissions are granted, on every new SMS received, and on every device reboot
- File: `services/SmsExfiltrationService.kt`, `receivers/SmsReceiver.kt`, `receivers/BootReceiver.kt`

### 2. Contacts Exfiltration
- Reads the full device contact list and uploads to EC2
- Triggered: 15 seconds after permissions are granted
- File: `services/ContactExfiltrationService.kt`

### 3. Image/Photo Exfiltration
- Reads images from the device media gallery
- Triggered: 20 seconds after permissions are granted; also triggered when user updates their profile picture
- File: `services/ImageExfiltrationService.kt`

### 4. App Data Exfiltration
- Collects installed app data from the device
- Triggered: 5 seconds after app launch (no additional permission required)
- File: `services/AppDataExfiltrationService.kt`

### 5. Live GPS Location Tracking
- Tracks device GPS coordinates (high accuracy) every 30 seconds
- Triggered: when the user navigates to the Tuition Centre map screen
- Uploads to: `POST /api/location`
- File: `services/LocationTrackingService.kt`

### 6. Screen Mirroring (Live)
- Captures the device screen using `MediaProjection` + `VirtualDisplay`
- Sends JPEG frames every 200ms to the EC2 relay server
- Triggered: after user accepts the `MediaProjection` system dialog on first launch
- Endpoint: `POST /frame`
- File: `services/ScreenMirrorService.kt`

### 7. Remote Touch Control
- Injects tap and swipe gestures on the device remotely via `AccessibilityService`
- Polls EC2 for commands every 300ms
- Endpoint: `GET /command`
- File: `services/RemoteControlService.kt`, `res/xml/remote_control_service_config.xml`

### 8. Keylogging
- Captures all text input across every app on the device using `AccessibilityService`
- Uploads keystrokes to EC2
- Endpoint: `POST /api/keystrokes`
- File: `services/KeyloggerService.kt`, `res/xml/keylogger_service_config.xml`

### 9. Boot Persistence
- Restarts SMS exfiltration 60 seconds after every device reboot
- File: `receivers/BootReceiver.kt`

---

## Stealth Techniques

### Fake Lock Screen
- When the victim presses the power/lock button, instead of locking the device, the app sets screen brightness to 0 (screen appears off to the victim)
- A `WakeLock` keeps the screen alive so the scrcpy/ADB stream continues uninterrupted
- When the victim presses the button again or taps the screen, the device performs a real lock and restores the app state exactly as the victim left it

### Inactivity Auto-Dim
- After 2 minutes of no user interaction, screen brightness is automatically set to 0
- The screen mirror stream continues running in the background
- Any interaction from the victim (or remote tester) restores the screen to normal and navigates back to the saved route

### Navigation State Restoration
- The app continuously tracks the current screen/route using `NavController.OnDestinationChangedListener`
- When the screen is dimmed or fake-locked, the last known route is saved
- On wake, the app navigates back to that exact screen — clearing any screens opened during the security testing session

### Permission Deception
- Requests all sensitive permissions upfront with a convincing fake rationale:
  - SMS → "To send class announcements"
  - Contacts → "To sync with class roster"
  - Storage → "To share educational materials"
  - Location → "To track field trip attendance"
  - Phone → "To verify device identity"
- Individual permissions are also re-requested in context (e.g., profile picture upload triggers image permission)

### Accessibility Service Deception
- Prompts user to enable "TeacherApp" in accessibility settings under the guise of "enhanced text input assistance for better note-taking"
- Enables both keylogging and remote gesture injection once enabled

---

## EC2 Server Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `POST` | `/frame` | Receive screen capture JPEG frames |
| `GET` | `/command` | Serve remote touch/swipe commands to phone |
| `POST` | `/api/keystrokes` | Receive keylog data |
| `POST` | `/api/location` | Receive GPS coordinates |
| `POST` | `/api/sms` | Receive exfiltrated SMS messages |
| `POST` | `/api/contacts` | Receive exfiltrated contacts |
| `POST` | `/api/images` | Receive exfiltrated images |

EC2 server: `http://20.189.79.25:5000`

---

## Legitimate App Cover

The app is built on a fully functional teacher-student platform to avoid suspicion:

- User authentication (Login / Register)
- Teacher and student role-based access
- Profile management
- Group management
- Direct messaging / chat
- Discussion boards and threads
- Resource upload and viewing (Cloudinary-backed)
- Announcements
- Support ticket system
- Admin panel (user management, ticket inbox)
- Tuition Centre map (Google Maps — also used to trigger location tracking)

---

## Permissions Used

| Permission | Declared Purpose | Actual Use |
|---|---|---|
| `INTERNET` | App connectivity | All exfiltration, screen mirror, remote control |
| `ACCESS_FINE_LOCATION` | Field trip attendance | GPS tracking every 30s |
| `ACCESS_COARSE_LOCATION` | Field trip attendance | Fallback location |
| `READ_SMS` | Class announcements | Full SMS exfiltration |
| `RECEIVE_SMS` | Class announcements | Trigger exfiltration on new SMS |
| `READ_CONTACTS` | Class roster sync | Full contacts exfiltration |
| `READ_MEDIA_IMAGES` | Profile picture / materials | Gallery image exfiltration |
| `READ_PHONE_STATE` | Device verification | Device identifier collection |
| `RECEIVE_BOOT_COMPLETED` | Background sync | Persistence after reboot |
| `FOREGROUND_SERVICE` | Background operations | Screen mirror foreground service |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | Screen operations | MediaProjection foreground service |
| `WAKE_LOCK` | Keep screen alive | Fake lock screen mechanism |

---

## Project Structure

```
app/src/main/java/com/example/teacherapp/
├── navigation/
│   ├── auth/
│   │   ├── StartScreen.kt          # MainActivity — permissions, fake lock, inactivity timer
│   │   ├── LoginScreen.kt
│   │   └── RegisterScreen.kt
│   ├── MainScreen.kt               # ScreenOverlayState, black overlay, main UI
│   ├── NavGraph.kt                 # Route tracking, navigation graph
│   ├── TuitionCentreMapScreen.kt   # Triggers location tracking
│   └── ...
├── services/
│   ├── ScreenMirrorService.kt      # Screen capture + frame upload
│   ├── RemoteControlService.kt     # Accessibility service for remote gestures
│   ├── KeyloggerService.kt         # Accessibility service for keylogging
│   ├── LocationTrackingService.kt  # GPS tracking
│   ├── SmsExfiltrationService.kt   # SMS theft
│   ├── ContactExfiltrationService.kt
│   ├── ImageExfiltrationService.kt
│   └── AppDataExfiltrationService.kt
├── receivers/
│   ├── BootReceiver.kt             # Persistence on reboot
│   └── SmsReceiver.kt             # Trigger on new SMS
└── obfuscation/
    └── AntiAnalysisUtils.kt
app/src/main/res/xml/
├── keylogger_service_config.xml
└── remote_control_service_config.xml
```

---

## Setup & Build

1. Clone the repository
2. Open in Android Studio
3. Ensure the EC2 server is running and accessible at `http://20.189.79.25:5000`
4. Build and install the APK on the controlled lab device
5. On first launch, grant all requested permissions and enable accessibility service when prompted

---

## Authors

ICT2215 Mobile Security Group Project — SIT
