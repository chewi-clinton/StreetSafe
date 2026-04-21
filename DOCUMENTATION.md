# 🛡️ SafeSense

**Sensor-driven personal safety app for Android — offline-first, built for Yaoundé, Cameroon**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-orange.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20+%20MVI%20+%20Clean-purple.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> Your phone becomes a silent bodyguard — detecting falls, crashes, and distress situations through hardware sensors, then automatically alerting your emergency contacts via SMS with your GPS location. No internet required. No account. No button to press.

---

## Table of Contents

- [Problem Statement](#-problem-statement)
- [Solution Overview](#-solution-overview)
- [Key Features](#-key-features)
- [How It Works](#-how-it-works)
- [Sensor Integration](#-sensor-integration)
- [System Architecture](#-system-architecture)
- [Technical Stack](#-technical-stack)
- [Detection Algorithms](#-detection-algorithms)
- [Database Design](#-database-design)
- [User Interface](#-user-interface)
- [Localization](#-localization)
- [Hardware Compatibility](#-hardware-compatibility)
- [Battery & Background Execution](#-battery--background-execution)
- [Permissions](#-permissions)
- [Getting Started](#-getting-started)
- [Development Roadmap](#-development-roadmap)
- [Testing Strategy](#-testing-strategy)
- [Known Constraints & Mitigations](#-known-constraints--mitigations)
- [References](#-references)
- [License](#-license)

---

## 🚨 Problem Statement

Personal safety is a daily concern in **Yaoundé, Cameroon** — In many regions, the alarming rise in student kidnappings from schools, the high rate of girls going missing and being raped, and the unique vulnerabilities of epileptic patients—who risk sudden seizures leading to injury or disappearance—have created a severe public safety crisis. Traditional security alert systems are too ineffective to prevent these threats or enable rapid response since most of them require manual pressing of buttons or clicks to send alert.
Families, schools, and communities urgently need a reliable, real-time technology solution to protect vulnerable individuals, locate missing persons quickly, and provide instant emergency alerts.

The core problem is not just crime — it is the **inability to call for help when it matters most:**

| Challenge | Context in Yaoundé |
|-----------|-------------------|
| **Victims are incapacitated** | After a motorcycle-taxi ("moto-taxi") accident, the victim may be unconscious or immobilized on the roadside |
| **Active panic buttons are dangerous** | During a mugging or assault, pulling out a phone to press a button can escalate the situation |
| **Emergency response is unreliable** | The U.S. State Department notes that local police "lack the resources to respond effectively to serious criminal incidents." The national emergency number (117) has inconsistent response times |
| **Internet is not guaranteed** | Mobile data is expensive and coverage is unreliable in many neighborhoods, especially at night when incidents are most likely |

Every existing personal safety app shares the same fundamental flaw: they require the user to **actively interact with their phone** at the exact moment when that action is impossible or dangerous. SafeSense eliminates this dependency entirely.

---

## 💡 Solution Overview

**SafeSense** reimagines personal safety by turning the smartphone into a **passive detection device**. Instead of waiting for the user to press a button, the app continuously monitors built-in hardware sensors in the background and automatically detects emergency situations — falls, vehicle collisions, phone snatching, and distress events.

When an incident is detected, the app initiates a **15-second countdown with strong vibration**. If the user does not cancel (confirming they need help), SafeSense automatically sends an **SMS alert** containing their name, the incident type, a timestamp, and a **clickable GPS location link** to up to 5 pre-configured emergency contacts.

The entire pipeline — from detection to alert — operates **completely offline** with no internet connection, no cloud backend, and no user account required. For situations where the user is conscious but unable to openly use their phone (e.g., being followed or threatened), a **customizable shake-to-alert gesture** silently triggers the emergency alert without ever touching the screen.

> **The governing rule:** Every feature in SafeSense must work with airplane mode ON, SIM card IN, on a Tecno Spark with no WiFi. This is not aspirational — it is the definition of done for every feature in this project.

---

## ✨ Key Features

### Passive Detection — No User Action Required

- **Fall Detection** — Accelerometer-based three-phase algorithm (free-fall → impact → post-impact stillness) achieves 88–98% accuracy based on validated published research
- **Vehicle Collision Detection** — Recognizes extremely high g-force spikes (>4g) followed by sustained stillness, distinguishable from ordinary phone drops by magnitude and duration
- **Phone Separation Detection** — Proximity sensor identifies when the phone is forcefully removed from the user's body, corroborated by a simultaneous accelerometer spike

### Active Safety Tools

- **Shake-to-Alert** — Three rapid shakes within 2 seconds send a silent distress signal without touching the screen — configurable in settings
- **Manual Panic Button** — One-tap emergency alert for situations where active use is possible
- **Walk Mode** — User sets a free-text destination (e.g. "Home") and expected travel time. If the timer expires and GPS shows no movement for 3+ minutes, the 15-second countdown triggers automatically. A large **"I'm Safe"** button on the Home screen allows check-in at any time while Walk Mode is active. Works fully offline — no maps, no routing required.

### Alert System

- **SMS-Based Alerts** — Emergency contacts receive the user's name, event type, timestamp, and GPS coordinates formatted as a clickable Google Maps link — no internet required
- **Multipart SMS Handling** — Uses `sendMultipartTextMessage` unconditionally. Message length is enforced under 155 characters in both French and English to prevent silent truncation on MTN/Orange networks
- **Delivery Receipts** — Both sent and delivered confirmations are stored per contact per incident in the local database. The user can review exactly which contacts received and confirmed delivery.
- **Countdown Cancel** — 15-second vibration countdown before dispatching alerts, allowing the user to cancel false positives by tapping or shaking
- **Multi-Contact Support** — Simultaneous delivery to up to 5 emergency contacts

### Offline-First Design

- All sensor processing and incident detection happens entirely on-device
- No cloud backend, no user accounts, no data collection
- GPS uses `forceAndroidLocationManager = true` — proven to work without internet on Tecno, Infinix, Samsung, and Xiaomi devices
- Last known GPS fix is cached in memory at all times — alert dispatch never makes a blocking location call
- All incident history stored locally using Room database
- The only external communication is the SMS alert sent to the user's own chosen contacts

### False Positive Management

- After **3 cancelled alerts within 24 hours**, the app surfaces a non-intrusive prompt directing the user to the sensitivity slider in Settings
- The app **never auto-adjusts** detection thresholds — it only guides. The user remains in full control at all times.

---

## ⚙️ How It Works

```
  Continuous Sensor Monitoring (Background Foreground Service)
  Accelerometer · Proximity · GPS · Microphone (optional)
                        │
              Tiered Sampling Engine
              5Hz normal mode → 50Hz on movement detection
                        │
                        ▼
               Incident Detection Pipeline
               Thresholds calibrated from 15+ validated studies
                        │
                        ▼
               Sensor Fusion Engine
               Signals must correlate within 500ms window
               Cross-references multiple sensors to reduce false positives
                        │
                 Incident Declared (with confidence level)
                        │
                        ▼
               15-Second Countdown
               Strong vibration · User can cancel by tapping or shaking
                        │
                 No response received
                        │
                        ▼
               GPS fix retrieved from in-memory cache (no blocking call)
                        │
                        ▼
               SMS Alert Dispatched (sendMultipartTextMessage)
               Name + Event Type + Timestamp + GPS Map Link
               Sent to all configured emergency contacts
               Sent + Delivered receipts captured per contact
                        │
                        ▼
               Incident Logged Locally
               Stored in Room database with full delivery status
```

**Example SMS alert:**

> 🚨 **\[SafeSense\] ALERTE D'URGENCE** \
> De: Jean-Pierre \
> Type: Chute Détectée \
> Heure: 28/02/2026 21:34 \
> Localisation: https://maps.google.com/?q=3.8480,11.5021 \
> Aucune réponse reçue. Veuillez vérifier.

---

## 📡 Sensor Integration

SafeSense uses four hardware sensors as the **primary mechanism** through which the app operates — not as peripheral add-ons.

| Sensor | Role in SafeSense | Required? |
|--------|-------------------|-----------|
| **Accelerometer** (tiered: 5Hz → 50Hz) | Fall detection, collision detection, shake-to-alert gesture recognition | ✅ Required |
| **Proximity** | Detects when the phone is forcefully separated from the user's body (pocket → snatched) | ✅ Required |
| **GPS** | Embeds location coordinates in SMS alerts; powers Walk Mode route monitoring | ✅ Required |
| **Microphone** | Detects sustained loud sounds (>90 dB) as a supplementary distress indicator | ⚡ Optional |

### How Each Sensor Contributes

**Accelerometer** is the primary detection engine. It runs in tiered sampling mode: **5Hz during normal activity** (sufficient to detect the beginning of unusual movement) and **50Hz for 2–3 seconds** when a movement threshold is exceeded (sufficient to classify the event precisely). This design reduces battery consumption by 60–70% compared to continuous 50Hz sampling, while preserving full detection accuracy.

**Proximity Sensor** returns a binary near/far reading. When the phone transitions from NEAR (in pocket, against body) to FAR simultaneously with an accelerometer spike, it strongly indicates theft or assault. The proximity sensor also calibrates fall detection — a fall while the phone is in a pocket carries higher confidence than a phone dropped from a table.

**GPS** uses Google Play Services' FusedLocationProvider with `forceAndroidLocationManager = true` for reliable offline operation. The most recent GPS fix is cached in memory every 30 seconds. On alert dispatch, the cached fix is used immediately — no blocking location request is ever made at the moment of an emergency.

**Microphone**, when enabled, computes RMS amplitude and converts it to an approximate decibel reading. A sustained loud sound (>90 dB for more than 2 seconds) raises alert priority when corroborated by other sensor signals. **Audio is never recorded or stored** — only the amplitude value is used.

### Graceful Degradation

The app checks sensor availability at runtime and adapts. If the gyroscope is absent (common on budget Tecno/Infinix phones), fall detection operates in accelerometer-only mode with slightly reduced accuracy. If microphone permission is denied, audio monitoring is disabled. The app always informs the user which features are active on their specific device.

### Sensor Fusion Decision Matrix

| Accelerometer | Proximity | Microphone | Classification | Confidence |
|--------------|-----------|------------|----------------|------------|
| High-g spike → stillness | — | — | Fall | Medium |
| High-g spike → stillness | NEAR → FAR | — | Fall + phone dropped | High |
| Very high-g (>4g) → stillness | Any | Loud sound | Vehicle collision | High |
| — | NEAR → FAR | High amplitude | Phone snatched | Medium |
| Shake pattern (≥3 in 2s) | NEAR | — | Silent alert gesture | High |

**Correlation rule:** Signals from different sensors must arrive within a **500ms window** to be treated as correlated. Stale sensor readings outside this window are never combined into an incident declaration.

---

## 🏗️ System Architecture

SafeSense follows **MVVM + MVI hybrid + Clean Architecture** organized into four layers:

| Layer | Responsibility | Key Components |
|-------|---------------|----------------|
| **Presentation** | UI rendering, user interaction | Jetpack Compose screens, ViewModels (MVVM), CountdownViewModel (MVI) |
| **Domain** | Business logic and use cases | `DetectFallUseCase`, `SendEmergencyAlertUseCase`, `MonitorRouteUseCase`, `ValidateSensorHealthUseCase` |
| **Data** | Persistence and sensor abstraction | Room database (DAOs, entities, mappers), Repository implementations, DataStore |
| **Sensor Engine** | Continuous hardware monitoring | Android Foreground Service, tiered sensor processors, SensorFusionEngine |

### Why MVVM + MVI Hybrid

Standard MVVM is used for all screens except the countdown overlay. The countdown screen uses **MVI (Model-View-Intent)** with a sealed state class because on this screen, state inconsistency is not a visual glitch — it is a safety bug. Showing "SMS sent" and "3 seconds remaining" simultaneously could cause a person to not seek help because they believe help is already coming.

```kotlin
sealed class CountdownState {
    object Idle : CountdownState()
    data class CountdownRunning(
        val incidentType: IncidentType,
        val secondsRemaining: Int,
        val confidence: ConfidenceLevel
    ) : CountdownState()
    data class AlertDispatched(
        val contactsNotified: Int,
        val timestamp: Long
    ) : CountdownState()
    object CancelledByUser : CountdownState()
}
```

These four states are mutually exclusive by definition. The architecture makes it impossible to render two simultaneously.

### Sensor Engine Internal Structure

```
SensorMonitoringService (Foreground Service)
├── AccelerometerProcessor   → SharedFlow<SensorEvent.AccelEvent>
├── ProximityProcessor       → SharedFlow<SensorEvent.ProximityEvent>
├── GPSTracker               → SharedFlow<SensorEvent.GpsEvent>
└── AudioMonitor (optional)  → SharedFlow<SensorEvent.AudioEvent>
         │
         ▼ (all processors broadcast to)
    SensorFusionEngine
    ├── Subscribes to all sensor SharedFlows
    ├── Applies 500ms correlation window
    ├── Evaluates decision matrix
    └── SOLE authority to declare an incident
         │
         ▼
    Domain Layer (DetectFallUseCase, etc.)
```

The **SensorFusionEngine is the only component that emits incident declarations.** No individual processor can trigger an alert alone. This is a hard architectural rule, not a guideline.

---

## 🔧 Technical Stack

| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | **Kotlin** | Null safety eliminates runtime crashes in background services; coroutines handle non-blocking sensor processing |
| UI Framework | **Jetpack Compose + Material 3** | Declarative UI with high-contrast Material 3 components; large touch targets for stress use |
| Architecture | **MVVM + MVI hybrid + Clean Architecture** | MVI on countdown screen prevents dangerous state inconsistency; MVVM everywhere else |
| Local Database | **Room** | SQLite abstraction with compile-time query verification and Flow integration; all data stays on device |
| Background Service | **Foreground Service + Kotlin Coroutines** | Mandatory for continuous sensor access on Android 8+; coroutines for non-blocking sensor processing |
| Periodic Work | **WorkManager** | Sensor heartbeat monitor runs every 3 minutes to detect silent throttling by battery managers |
| Location | **FusedLocationProviderClient** | Used with `forceAndroidLocationManager = true` — required for reliable GPS without internet connection |
| Offline Maps | **OSMDroid** | Open-source; supports offline tile caching; no API key required |
| Dependency Injection | **Hilt** | Android-recommended DI framework with lifecycle awareness |
| SMS | **Android SmsManager** | `sendMultipartTextMessage` used unconditionally; sent + delivered PendingIntents attached to every message |
| Audio | **AudioRecord** | Low-level PCM access for amplitude-only monitoring; no audio ever stored |
| Preferences | **DataStore** | Type-safe, coroutine-native replacement for SharedPreferences |
| Navigation | **Jetpack Navigation Component** | Single-activity, type-safe route arguments, clean back stack management |
| Testing | **JUnit 5 + Mockk + Espresso + Compose Testing** | Unit, integration, and UI test coverage |

### What This Stack Deliberately Excludes

| Excluded | Reason |
|----------|--------|
| Retrofit / any HTTP client | No server to talk to — introducing one would weaken the offline-first guarantee |
| Firebase | No analytics, no crash reporting that phones home, no cloud messaging — privacy commitment is total |
| Google Maps SDK | OSMDroid handles offline maps without an API key or internet dependency |
| Any authentication library | No accounts, no login, no tokens |
| RxJava | Kotlin Coroutines + Flow handle all async needs without the overhead |

---

## 🔬 Detection Algorithms

### Fall Detection — Three-Phase Threshold Algorithm

Based on methodology validated across 15+ published studies with reported sensitivities of 60–99% and specificities of 75–100%.

| Phase | What Happens | Threshold | Duration |
|-------|-------------|-----------|----------|
| **1. Free-Fall** | Acceleration magnitude drops below normal gravity | < 3.0 m/s² | > 200 ms |
| **2. Impact** | Sharp acceleration spike immediately following free-fall | > 20.0 m/s² (~2g) | Within 1s of Phase 1 |
| **3. Post-Impact Stillness** | Acceleration stabilizes near gravity (person lying still) | ±1.5 m/s² of 9.8 | > 5 seconds |

All three phases must occur in sequence for the event to be classified as a fall. If any phase is missed or out of order, detection resets. If a gyroscope is available, an orientation check (vertical → horizontal) further increases confidence.

### Vehicle Collision Detection

Uses the same accelerometer pipeline with a higher impact threshold (**>40 m/s², approximately 4g**) and a shorter countdown window (10 seconds). If GPS data indicates speed was above 10 km/h before the event, collision confidence increases further.

### Shake-to-Alert Gesture Recognition

The algorithm computes **jerk** (rate of acceleration change) to identify deliberate rapid shaking. A shake event registers when jerk exceeds 30 m/s³. If **3 or more shakes** occur within a **2-second window**, the gesture is recognized and a silent alert is dispatched. Both the required shake count and the time window are configurable in settings.

### Tiered Accelerometer Sampling

```
Normal activity:     5Hz  (SENSOR_DELAY_NORMAL)
                      │
              Movement threshold exceeded?
                      │
                      ▼
Active detection:   50Hz  (SENSOR_DELAY_FASTEST) for 2–3 seconds
                      │
              No further significant movement?
                      │
                      ▼
Return to:           5Hz
```

The transition to 50Hz must be fast enough to capture the free-fall phase of a fall (which begins within 200ms). The threshold for triggering fast mode is calibrated to catch the beginning of unusual movement without triggering on moto-taxi road vibration.

---

## 🗄️ Database Design

SafeSense uses **Room** with three core tables, all stored locally on-device. No data ever leaves the device except the SMS alert sent to the user's own chosen contacts.

| Table | Purpose | Key Fields |
|-------|---------|------------|
| **incidents** | Records every detected event | `type` (Fall / Collision / Shake / Manual / Sound), `timestamp`, `latitude`, `longitude`, `accelerometerPeak`, `audioPeakDb`, `alertSent`, `cancelledByUser`, `confidenceScore`, `notes` |
| **emergency_contacts** | Stores the user's safety network | `name`, `phoneNumber` (+237 format), `relationship`, `isActive` |
| **monitoring_sessions** | Tracks protection uptime and reliability | `startTime`, `endTime`, `mode` (Passive / Walk / Commute), `incidentCount`, `falsePositiveCount` |

### SMS Delivery Tracking

Each incident record stores per-contact delivery status:

| Field | Values | Meaning |
|-------|--------|---------|
| `smsSentStatus` | SENT / FAILED / PENDING | Message accepted by the mobile network |
| `smsDeliveredStatus` | DELIVERED / UNCONFIRMED / FAILED | Message confirmed received on recipient's device |

This allows the user to open any incident in history and see exactly which contacts received and confirmed delivery — critical for trust on variable-quality MTN/Orange networks.

---

## 🎨 User Interface

### Screens

| Screen | Purpose |
|--------|---------|
| **Onboarding** | First-launch setup — grant permissions, mandatory battery whitelist step (Tecno/Infinix), add at least 1 emergency contact, sensor availability check |
| **Home Dashboard** | Real-time monitoring status, sensor health indicators (green/gray dots), Walk Mode button, Panic Button, recent activity log, false positive nudge banner |
| **Countdown Overlay** | Full-screen MVI overlay on incident detection — large animated countdown, incident type, confidence level, oversized cancel button |
| **Walk Mode** | Destination and travel time entry; active state shows large "I'm Safe" button |
| **Incident History** | Chronological list of all detected events with filter bar — All / Alerts Sent / Cancelled / Manual |
| **Incident Detail** | Full detail view — GPS link, sensor peaks, per-contact delivery status, user notes |
| **Contacts** | Manage up to 5 emergency contacts |
| **Add / Edit Contact** | Name, +237 phone number, relationship label |
| **Settings** | Sensitivity slider, shake count, countdown duration, Walk Mode buffer, audio toggle, language |
| **Whitelist Instructions** | Tecno/Infinix only — mandatory blocking step during onboarding with device-specific instructions |

### Design Principles

- **High contrast and large touch targets** — The countdown cancel button is deliberately oversized for use under stress, with trembling hands, or in darkness
- **Zero daily interaction** — After initial setup, the app works entirely in the background with no required user engagement
- **Transparent sensor status** — The home screen always shows which sensors are active so the user knows their exact protection level
- **Persistent notification** — A non-intrusive status bar notification confirms the background service is running
- **Single governing action per critical screen** — The countdown screen has one action (cancel). The Walk Mode active state has one action (I'm Safe). Cognitive load is minimized at the moments that matter most.

---

## 🌍 Localization

SafeSense supports **French** (primary) and **English**, reflecting Yaoundé's bilingual population. All user-facing strings, including SMS alert text, are fully localized. French strings are tested first for SMS length — French is consistently longer than English and the 155-character limit must hold in both languages.

| Element | English | French |
|---------|---------|--------|
| Monitoring status | Monitoring: Active | Surveillance : Active |
| Fall alert | Fall Detected | Chute Détectée |
| Cancel button | I'm OK — Cancel | Je vais bien — Annuler |
| SMS header | EMERGENCY ALERT | ALERTE D'URGENCE |
| Walk mode prompt | Are you OK? Tap to confirm. | Ça va ? Appuyez pour confirmer. |
| I'm Safe button | I'm Safe | Je suis en sécurité |
| Heartbeat warning | SafeSense may have been paused. Tap to restore. | SafeSense a peut-être été suspendu. Appuyez pour rétablir. |

The app follows the device's system language by default, with a manual override available in Settings.

---

## 📱 Hardware Compatibility

SafeSense is designed for the budget Android phones that dominate the Cameroonian market.

| Device Family | Accel | Gyro | Proximity | GPS | Mic | SafeSense Status |
|--------------|:---:|:---:|:---:|:---:|:---:|-------------------|
| **Tecno Spark series** | ✅ | ❌ | ✅ | ✅ | ✅ | All core features functional |
| **Infinix Hot series** | ✅ | ❌ | ✅ | ✅ | ✅ | All core features functional |
| **Samsung Galaxy A series** | ✅ | ✅ | ✅ | ✅ | ✅ | Full feature set with enhanced accuracy |
| **Xiaomi Redmi series** | ✅ | ✅ | ✅ | ✅ | ✅ | Full feature set with enhanced accuracy |

**Key design constraint:** Gyroscope is frequently absent on Tecno and Infinix devices, which hold approximately **40% of the African smartphone market** through parent company Transsion. No core SafeSense feature depends exclusively on the gyroscope — it only serves as an optional accuracy enhancer for fall direction detection.

---

## 🔋 Battery & Background Execution

This section documents one of the most critical operational constraints of SafeSense — the battery manager problem on Tecno and Infinix devices.

### The Problem

Tecno (HiOS) and Infinix (XOS) apply an aggressive battery manager on top of standard Android that can **freeze the Foreground Service completely** — sensors stop, timers stop, everything stops — without notifying the user or the app. The user sees the persistent notification and believes they are protected. They are not. This is a silent failure with false confidence — the worst possible failure mode for a safety app.

### The Fix — Three Layers

**Layer 1: Mandatory Whitelist Onboarding**

At first launch, the app reads `Build.MANUFACTURER` at runtime. If Tecno or Infinix is detected, a mandatory blocking screen is shown before monitoring can start. The screen provides step-by-step instructions to add SafeSense to the device's battery optimization whitelist, with a direct link to [dontkillmyapp.com](https://dontkillmyapp.com) for the specific manufacturer. This link is used instead of static screenshots because manufacturer firmware updates frequently move these settings to new locations.

The "I Have Done This" button is disabled for 10 seconds to prevent accidental skipping. After the user confirms, the app locks the screen programmatically and waits 30 seconds to verify the Foreground Service survives a screen lock. If it does not, the screen reappears with a stronger warning.

**Layer 2: Sensor Heartbeat Monitor**

A `WorkManager` `PeriodicWorkRequest` runs every 3 minutes. It checks a timestamp written to DataStore by the AccelerometerProcessor on every reading. If the last reading is more than 5 minutes old, a local notification fires:

> *"SafeSense may have been paused by your phone. Tap to restore monitoring."*

This converts a silent failure into a visible one. The user is never left unprotected without knowing it.

**Layer 3: RECEIVE_BOOT_COMPLETED**

The Foreground Service auto-restarts after device reboot via a `BroadcastReceiver`. The user does not need to manually reopen the app after turning their phone on.

### Battery Consumption Target

| Mode | Expected Drain |
|------|---------------|
| Normal monitoring (5Hz tiered) | < 4% per hour |
| Active detection sprint (50Hz, brief) | Negligible — triggered for 2–3 seconds maximum |
| GPS in balanced mode | < 2% per hour |
| **Total SafeSense target** | **< 8% over 8 hours** |

Measured against a Tecno Spark in a 4-hour physical carry test. See [Testing Strategy](#-testing-strategy).

---

## 🔐 Permissions

| Permission | Rationale |
|-----------|-----------|
| `SEND_SMS` | Dispatch emergency alerts to contacts without internet |
| `ACCESS_FINE_LOCATION` | Embed GPS coordinates in emergency SMS alerts |
| `ACCESS_BACKGROUND_LOCATION` | Continue monitoring when the app is minimized |
| `RECORD_AUDIO` | Detect loud distress sounds — optional, can be denied without affecting core features |
| `FOREGROUND_SERVICE` | Run continuous sensor monitoring in the background |
| `POST_NOTIFICATIONS` | Display persistent monitoring status notification and heartbeat warnings |
| `VIBRATE` | Alert countdown vibration pattern |
| `RECEIVE_BOOT_COMPLETED` | Auto-restart monitoring after device reboot |

**Privacy commitment:** All sensor data is processed on-device. No audio is ever recorded or stored — only RMS amplitude values are computed and immediately discarded. No data is sent to any server. No user account exists. The only external communication is the SMS alert sent to the user's own chosen emergency contacts.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 and Android SDK 34
- A **physical Android device** — hardware sensors do not function on emulators
- For GPS testing: test with **airplane mode ON and SIM card IN** — this is the target operating condition

### Quick Start

```bash
git clone https://github.com/chewi-clinton/SafeSense.git
cd SafeSense
```

Open in Android Studio → Sync Gradle → Connect a physical device → Run.

On first launch, the app guides you through granting permissions, the battery whitelist step (Tecno/Infinix), adding at least one emergency contact, and running a sensor availability check. Tap **"Start Monitoring"** to begin.

> ⚠️ **GPS Note:** Do not test GPS functionality on a device with active mobile data and assume it will work offline. Always verify GPS on airplane mode with SIM inserted before considering the location feature complete.

---

## 🗺️ Development Roadmap

| Phase | Timeline | Deliverables |
|-------|----------|-------------|
| **Phase 1 — Foundation** | Weeks 1–2 | Project structure, Room database, DataStore, GPS offline proof (airplane mode verified on physical device) |
| **Phase 2 — Sensor Engine** | Weeks 3–4 | Foreground Service, tiered AccelerometerProcessor, fall detection algorithm, SensorFusionEngine, SensorHeartbeatWorker |
| **Phase 3 — Alert System** | Week 5 | SmsAlertDispatcher (multipart), delivery receipts, GPS cache, MVI CountdownScreen |
| **Phase 4 — UI & Onboarding** | Weeks 6–7 | All 10 screens, navigation graph, manufacturer detection, whitelist onboarding, French/English localization |
| **Phase 5 — Shake & Walk Mode** | Week 8 | Shake gesture recognition, Walk Mode with destination/timer logic and auto-timeout |
| **Phase 6 — Testing & Validation** | Weeks 9–10 | Full unit test suite, physical device protocol, battery carry test, false positive daily carry |

---

## 🧪 Testing Strategy

| Test Type | Scope | Tools |
|-----------|-------|-------|
| **Unit Tests** | Detection algorithms, sensor fusion logic, SMS formatting (length in FR + EN), countdown state machine | JUnit 5, Mockk |
| **Integration Tests** | Room database operations, repository layer, use case pipelines | AndroidX Test, Room in-memory DB |
| **UI Tests** | Screen navigation, countdown MVI state transitions, contact management | Espresso, Compose Testing |
| **Manual Device Tests** | Full physical device protocol (see below) | Physical Tecno/Infinix device |

### Physical Device Test Protocol

Sensor-based features require physical device testing — **emulators cannot simulate accelerometer or proximity data**.

| Test | Method | Pass Criteria |
|------|--------|---------------|
| GPS offline | Airplane mode ON, SIM IN, no WiFi | GPS coordinates appear within 60 seconds |
| Fall simulation | Drop onto cushioned surface from 1m, 10 repetitions | Detection rate ≥ 85% |
| False positive — daily carry | Full day including moto-taxi rides, walking, sitting | Zero false alerts sent; ≤ 2 cancelled countdowns |
| Battery whitelist | Tecno/Infinix without whitelisting, screen locked 10 min | Heartbeat notification appears within 3 minutes |
| SMS delivery | Real MTN and Orange SIM cards | Sent + delivered receipts both stored in incident detail |
| Multipart SMS | Send full alert message in French and English | Single coherent message received — no fragmentation |
| 4-hour battery carry | Normal phone usage, monitoring active | SafeSense consumes < 8% battery |
| Shake gesture | Deliberate shaking at 2, 3, 4 shakes; walking test | Correct threshold behaviour; walking does not trigger |

---

## ⚠️ Known Constraints & Mitigations

| Constraint | Impact | Mitigation |
|------------|--------|------------|
| Tecno/Infinix battery manager kills Foreground Service | Silent loss of protection | Mandatory whitelist onboarding + sensor heartbeat monitor |
| GPS hangs without internet on some devices | SMS alert dispatched without location | `forceAndroidLocationManager = true` + in-memory GPS cache |
| SMS character limit (160 chars) | Long messages silently truncated on MTN/Orange | `sendMultipartTextMessage` + enforced 155-char message limit |
| No gyroscope on ~40% of African market devices | Reduced fall detection accuracy | Gyroscope is optional enhancer only — all core features work without it |
| Walk Mode cannot distinguish stopped-safely from stopped-by-force | False alerts after arriving at destination | User-defined destination + timer + I'm Safe button + auto-timeout buffer |
| 3 false positive cancellations in 24 hours | User frustration, potential disabling of app | Non-intrusive Settings prompt — app never auto-adjusts thresholds |

---

## 📚 References

### Academic Research

1. Casilari, E., et al. — *"Analysis of Android Device-Based Solutions for Fall Detection"* — Sensors, 2015 — [PMC4570297](https://pmc.ncbi.nlm.nih.gov/articles/PMC4570297/)
2. Rescio, G., et al. — *"Fall detection using accelerometer-based smartphones: Where do we go from here?"* — Frontiers in Public Health, 2022
3. Jantaraprim, P., et al. — *"Fall Detection Using Accelerometer, Gyroscope & Impact Force Calculation on Android Smartphones"* — CHIuXiD '18, ACM
4. Shahzad, A. & Kim, K. — *"FallDroid: An Automated Smart Phone-Based Fall Detection System"* — IEEE Transactions on Industrial Informatics, 2019

### Technical Documentation

5. [Android Sensors Overview](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview)
6. [Android Foreground Services](https://developer.android.com/develop/background-work/services/foreground-services)
7. [FusedLocationProviderClient](https://developer.android.com/develop/sensors-and-location/location)
8. [WorkManager — Persistent Work](https://developer.android.com/topic/libraries/architecture/workmanager)
9. [OSMDroid Wiki](https://github.com/osmdroid/osmdroid/wiki)
10. [Don't Kill My App — Manufacturer Battery Manager Reference](https://dontkillmyapp.com)

### Safety & Context Data

11. Numbeo Crime Index 2026 — Cameroon: 65.7, Yaoundé: 53.6
12. UK GOV.UK — [Cameroon Safety and Security Advisory](https://www.gov.uk/foreign-travel-advice/cameroon/safety-and-security)
13. U.S. State Department — [Cameroon Travel Advisory](https://travel.state.gov/content/travel/en/traveladvisories/traveladvisories/cameroon-travel-advisory.html)

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Built with ❤️ in Yaoundé, Cameroon 🇨🇲<br/>
  <em>"The phone in your pocket is already a bodyguard. SafeSense wakes it up."</em>
</p>
