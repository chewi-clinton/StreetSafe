# 🛡️ StreetSafe

**Sensor-driven personal safety app for Android — offline-first, built for Yaoundé, Cameroon**

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24%20(Android%207.0)-orange.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20+%20Clean-purple.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> Your phone becomes a silent bodyguard — detecting falls, crashes, and distress situations through hardware sensors, then automatically alerting your emergency contacts via SMS with your GPS location. No internet required.

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
- [Permissions](#-permissions)
- [Getting Started](#-getting-started)
- [Development Roadmap](#-development-roadmap)
- [Testing Strategy](#-testing-strategy)
- [References](#-references)
- [License](#-license)

---

## 🚨 Problem Statement

Personal safety is a daily concern in **Yaoundé, Cameroon** — a city of over 2.5 million people. Cameroon ranks among the top 15 countries globally on the Numbeo Crime Index (65.7 in 2026), and Yaoundé itself scores a moderate-to-high crime index of 53.6. The UK Foreign Office and U.S. State Department both flag armed robbery, mugging, and violent assault as persistent risks in the capital, particularly in neighborhoods like Briqueterie, Mokolo market, and poorly lit residential areas.

The core problem is not just crime — it is the **inability to call for help when it matters most:**

| Challenge | Context in Yaoundé |
|-----------|-------------------|
| **Victims are incapacitated** | After a motorcycle-taxi ("moto-taxi") accident, the victim may be unconscious or immobilized on the roadside |
| **Active panic buttons are dangerous** | During a mugging or assault, pulling out a phone to press a button can escalate the situation |
| **Emergency response is unreliable** | The U.S. State Department notes that local police "lack the resources to respond effectively to serious criminal incidents." The national emergency number (117) has inconsistent response times |
| **Internet is not guaranteed** | Mobile data is expensive and coverage is unreliable in many neighborhoods, especially at night when incidents are most likely |

Every existing personal safety app shares the same fundamental flaw: they require the user to **actively interact with their phone** at the exact moment when that action is impossible or dangerous. StreetSafe eliminates this dependency entirely.

---

## 💡 Solution Overview

**StreetSafe** reimagines personal safety by turning the smartphone into a **passive detection device**. Instead of waiting for the user to press a button, the app continuously monitors built-in hardware sensors in the background and automatically detects emergency situations — falls, vehicle collisions, phone snatching, and distress events.

When an incident is detected, the app initiates a **15-second countdown with strong vibration**. If the user does not cancel (confirming they need help), StreetSafe automatically sends an **SMS alert** containing their name, the incident type, a timestamp, and a **clickable GPS location link** to up to 5 pre-configured emergency contacts.

The entire pipeline — from detection to alert — operates **completely offline** with no internet connection, no cloud backend, and no user account required. For situations where the user is conscious but unable to openly use their phone (e.g., being followed or threatened), a **customizable shake-to-alert gesture** silently triggers the emergency alert without ever touching the screen.

---

## ✨ Key Features

### Passive Detection — No User Action Required

- **Fall Detection** — Accelerometer-based three-phase algorithm (free-fall → impact → post-impact stillness) achieves 88–98% accuracy based on validated published research
- **Vehicle Collision Detection** — Recognizes extremely high g-force spikes (>4g) followed by sustained stillness, distinguishable from ordinary phone drops by magnitude and duration
- **Phone Separation Detection** — Proximity sensor identifies when the phone is forcefully removed from the user's body, corroborated by a simultaneous accelerometer spike

### Active Safety Tools

- **Shake-to-Alert** — Three rapid shakes within 2 seconds send a silent distress signal without touching the screen — configurable in settings
- **Manual Panic Button** — One-tap emergency alert for situations where active use is possible
- **Walk Mode** — GPS-based route monitoring that triggers a check-in prompt if the user stops moving unexpectedly

### Alert System

- **SMS-Based Alerts** — Emergency contacts receive the user's name, event type, timestamp, and GPS coordinates formatted as a clickable Google Maps link — no internet required
- **Countdown Cancel** — 15-second vibration countdown before dispatching alerts, allowing the user to cancel false positives by tapping or shaking
- **Multi-Contact Support** — Simultaneous delivery to up to 5 emergency contacts

### Offline-First Design

- All sensor processing and incident detection happens entirely on-device
- No cloud backend, no user accounts, no data collection
- All incident history stored locally using Room database
- The only external communication is the SMS alert sent to the user's own chosen contacts

---

## ⚙️ How It Works

```
  Continuous Sensor Monitoring (Background Service)
  Accelerometer · Proximity · GPS · Microphone (optional)
                        │
                        ▼
               Incident Detection Pipeline
               Thresholds calibrated from 15+ validated studies
                        │
                        ▼
               Sensor Fusion Engine
               Cross-references multiple sensors to reduce false positives
                        │
                 Incident Detected
                        │
                        ▼
               15-Second Countdown
               Strong vibration · User can cancel by tapping or shaking
                        │
                 No response received
                        │
                        ▼
               SMS Alert Dispatched
               Name + Event Type + Timestamp + GPS Map Link
               Sent to all configured emergency contacts
                        │
                        ▼
               Incident Logged Locally
               Stored in Room database for history review
```

**Example SMS alert:**

> 🚨 **\[StreetSafe\] EMERGENCY ALERT** \
> From: Jean-Pierre \
> Type: Fall Detected \
> Time: 28/02/2026 21:34 \
> Location: https://maps.google.com/?q=3.8480,11.5021 \
> No response received after fall detection. Please check on them.

---

## 📡 Sensor Integration

StreetSafe uses four hardware sensors as the **primary mechanism** through which the app operates — not as peripheral add-ons.

| Sensor | Role in StreetSafe | Required? |
|--------|-------------------|-----------|
| **Accelerometer** (50Hz) | Fall detection, collision detection, shake-to-alert gesture recognition | ✅ Required |
| **Proximity** | Detects when the phone is forcefully separated from the user's body (pocket → snatched) | ✅ Required |
| **GPS** | Embeds location coordinates in SMS alerts; powers Walk Mode route monitoring | ✅ Required |
| **Microphone** | Detects sustained loud sounds (>90 dB) as a supplementary distress indicator | ⚡ Optional |

### How Each Sensor Contributes

**Accelerometer** is the primary detection engine. It monitors acceleration magnitude against calibrated thresholds. A fall produces a distinctive three-phase signature: brief free-fall, a sharp impact spike, then near-zero movement. Vehicle collisions produce even larger spikes (>4g). The shake-to-alert feature uses jerk calculation (rate of acceleration change) to distinguish deliberate gestures from ordinary movement.

**Proximity Sensor** returns a binary near/far reading. When the phone transitions from NEAR (in pocket, against body) to FAR simultaneously with an accelerometer spike, it strongly indicates theft or assault. The proximity sensor also helps calibrate fall detection — a fall while the phone is in a pocket is more significant than a phone dropped from a table.

**GPS** uses Google Play Services' FusedLocationProvider in balanced-power mode (30-second intervals). On alert dispatch, the most recent GPS fix is formatted as a Google Maps URL embedded in the SMS. In Walk Mode, GPS tracks the user's route and triggers a check-in if movement ceases for a configurable duration (default: 3 minutes).

**Microphone**, when enabled, computes RMS amplitude and converts it to an approximate decibel reading. A sustained loud sound (>90 dB for more than 2 seconds) raises alert priority when corroborated by other sensor signals. **Audio is never recorded or stored** — only the amplitude value is used.

### Graceful Degradation

The app checks sensor availability at runtime and adapts. If the gyroscope is absent (common on budget Tecno/Infinix phones), fall detection operates in accelerometer-only mode with slightly reduced accuracy. If microphone permission is denied, audio monitoring is disabled. The app always informs the user which features are active on their specific device.

---

## 🏗️ System Architecture

StreetSafe follows the **MVVM + Clean Architecture** pattern organized into four layers:

| Layer | Responsibility | Key Components |
|-------|---------------|----------------|
| **Presentation** | UI rendering, user interaction | Jetpack Compose screens, ViewModels, Navigation |
| **Domain** | Business logic and use cases | `DetectFallUseCase`, `SendEmergencyAlertUseCase`, `MonitorRouteUseCase` |
| **Data** | Persistence and sensor abstraction | Room database (DAOs, entities), Repository implementations |
| **Sensor Engine** | Continuous hardware monitoring | Android Foreground Service, sensor processors, SensorFusionEngine |

The **Sensor Engine** runs as an Android Foreground Service (mandatory for continuous sensor access on Android 9+), displaying a persistent notification. Sensor data flows through individual processors into the **SensorFusionEngine**, which cross-references signals from multiple sensors before classifying an event as an incident.

### Sensor Fusion Decision Matrix

| Accelerometer | Proximity | Microphone | Classification | Confidence |
|--------------|-----------|------------|----------------|------------|
| High-g spike → stillness | — | — | Fall | Medium |
| High-g spike → stillness | NEAR → FAR | — | Fall + phone dropped | High |
| Very high-g (>4g) → stillness | Any | Loud sound | Vehicle collision | High |
| — | NEAR → FAR | High amplitude | Phone snatched | Medium |
| Shake pattern (≥3 in 2s) | NEAR | — | Silent alert gesture | High |

This multi-sensor validation is the primary mechanism for reducing false positives — a single sensor signal alone rarely triggers an alert; corroboration from a second sensor elevates the confidence and triggers the countdown.

---

## 🔧 Technical Stack

| Component | Technology | Justification |
|-----------|------------|---------------|
| Language | **Kotlin** | Official Android language; null safety, coroutines for async sensor processing |
| UI Framework | **Jetpack Compose** | Modern declarative UI, significantly less boilerplate than XML |
| Architecture | **MVVM + Clean Architecture** | Separation of concerns; testable and maintainable codebase |
| Local Database | **Room** | SQLite abstraction with compile-time query verification and Flow integration |
| Background Service | **Foreground Service + Coroutines** | Required for continuous sensor access; coroutines for non-blocking processing |
| Location | **FusedLocationProviderClient** | Battery-optimized location via Google Play Services |
| Offline Maps | **OSMDroid** | Open-source; supports offline tile caching; no API key required |
| Dependency Injection | **Hilt** | Android-recommended DI framework with lifecycle awareness |
| SMS | **Android SmsManager** | Native SMS dispatch; works entirely without internet |
| Audio | **AudioRecord** | Low-level PCM access for amplitude-only monitoring |
| Preferences | **DataStore** | Modern, type-safe replacement for SharedPreferences |
| Testing | **JUnit 5 + Mockk + Espresso** | Unit, integration, and UI test coverage |

---

## 🔬 Detection Algorithms

### Fall Detection — Three-Phase Threshold Algorithm

Based on methodology validated across 15+ published studies with reported sensitivities of 60–99% and specificities of 75–100%.

| Phase | What Happens | Threshold | Duration |
|-------|-------------|-----------|----------|
| **1. Free-Fall** | Acceleration magnitude drops below normal gravity | < 3.0 m/s² | > 200 ms |
| **2. Impact** | Sharp acceleration spike immediately following free-fall | > 20.0 m/s² (~2g) | Within 1s of Phase 1 |
| **3. Post-Impact Stillness** | Acceleration stabilizes near gravity (person lying still) | ±1.5 m/s² of 9.8 | > 5 seconds |

All three phases must occur in sequence for the event to be classified as a fall. If a gyroscope is available, an orientation check (vertical → horizontal) further increases confidence.

### Vehicle Collision Detection

Uses the same accelerometer pipeline with a higher impact threshold (**>40 m/s², approximately 4g**) and a shorter countdown window (10 seconds). If GPS data indicates speed was above 10 km/h before the event, collision confidence increases further.

### Shake-to-Alert Gesture Recognition

The algorithm computes **jerk** (rate of acceleration change) to identify deliberate rapid shaking. A shake event registers when jerk exceeds 30 m/s³. If **3 or more shakes** occur within a **2-second window**, the gesture is recognized and a silent alert is dispatched. Both the required shake count and the time window are configurable in settings.

---

## 🗄️ Database Design

StreetSafe uses **Room** with three core tables, all stored locally on-device.

| Table | Purpose | Key Fields |
|-------|---------|------------|
| **incidents** | Records every detected event | `type` (Fall / Collision / Shake / Manual / Sound), `timestamp`, `latitude`, `longitude`, `accelerometerPeak`, `audioPeakDb`, `alertSent`, `cancelledByUser`, `confidenceScore`, `notes` |
| **emergency_contacts** | Stores the user's safety network | `name`, `phoneNumber` (+237 format), `relationship`, `isActive` |
| **monitoring_sessions** | Tracks protection uptime and reliability | `startTime`, `endTime`, `mode` (Passive / Walk / Commute), `incidentCount`, `falsePositiveCount` |

The `monitoring_sessions` table enables the user to review their protection history and helps measure false positive rates during development and testing.

---

## 🎨 User Interface

### Screens

| Screen | Purpose |
|--------|---------|
| **Onboarding** | First-launch setup — grant permissions, add at least 1 emergency contact, run sensor availability check |
| **Home Dashboard** | Real-time monitoring status, sensor health indicators (green/gray dots), quick-access Walk Mode and Panic Button, recent activity log |
| **Incident History** | Chronological list of all detected events — sent alerts, cancelled false positives, manual triggers |
| **Contacts** | Manage up to 5 emergency contacts with name, phone number, and relationship |
| **Settings** | Detection sensitivity, shake gesture configuration, countdown duration, language preference |
| **Countdown Overlay** | Full-screen overlay on incident detection — large countdown timer, event type label, oversized cancel button |

### Design Principles

- **High contrast and large touch targets** — The countdown cancel button is deliberately oversized for use under stress, with trembling hands, or in darkness
- **Zero daily interaction** — After initial setup, the app works entirely in the background with no required user engagement
- **Transparent sensor status** — The home screen always shows which sensors are active so the user knows their exact protection level
- **Persistent notification** — A non-intrusive status bar notification confirms the background service is running

---

## 🌍 Localization

StreetSafe supports **French** (primary) and **English**, reflecting Yaoundé's bilingual population. All user-facing strings, including SMS alert text, are fully localized.

| Element | English | French |
|---------|---------|--------|
| Monitoring status | Monitoring: Active | Surveillance : Active |
| Fall alert | Fall Detected | Chute Détectée |
| Cancel button | I'm OK — Cancel | Je vais bien — Annuler |
| SMS header | EMERGENCY ALERT | ALERTE D'URGENCE |
| Walk mode prompt | Are you OK? Tap to confirm. | Ça va ? Appuyez pour confirmer. |

The app follows the device's system language by default, with a manual override available in Settings.

---

## 📱 Hardware Compatibility

StreetSafe is designed for the budget Android phones that dominate the Cameroonian market.

| Device Family | Accel | Gyro | Proximity | GPS | Mic | StreetSafe Status |
|--------------|:---:|:---:|:---:|:---:|:---:|-------------------|
| **Tecno Spark series** | ✅ | ❌ | ✅ | ✅ | ✅ | All core features functional |
| **Infinix Hot series** | ✅ | ❌ | ✅ | ✅ | ✅ | All core features functional |
| **Samsung Galaxy A series** | ✅ | ✅ | ✅ | ✅ | ✅ | Full feature set with enhanced accuracy |
| **Xiaomi Redmi series** | ✅ | ✅ | ✅ | ✅ | ✅ | Full feature set with enhanced accuracy |

**Key design constraint:** Gyroscope is frequently absent on Tecno and Infinix devices, which hold approximately 40% of the African smartphone market through parent company Transsion. No core StreetSafe feature depends exclusively on the gyroscope — it only serves as an optional accuracy enhancer for fall direction detection.

---

## 🔐 Permissions

| Permission | Rationale |
|-----------|-----------|
| `SEND_SMS` | Dispatch emergency alerts to contacts without internet |
| `ACCESS_FINE_LOCATION` | Embed GPS coordinates in emergency SMS alerts |
| `ACCESS_BACKGROUND_LOCATION` | Continue monitoring when the app is minimized |
| `RECORD_AUDIO` | Detect loud distress sounds — optional, can be denied |
| `FOREGROUND_SERVICE` | Run continuous sensor monitoring in the background |
| `POST_NOTIFICATIONS` | Display persistent monitoring status notification |
| `VIBRATE` | Alert countdown vibration pattern |
| `RECEIVE_BOOT_COMPLETED` | Auto-restart monitoring after device reboot |

**Privacy commitment:** All sensor data is processed on-device. No audio is recorded or stored. No data is sent to any server. The only external communication is the SMS alert sent to the user's own chosen emergency contacts.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 and Android SDK 34
- A **physical Android device** — hardware sensors do not function on emulators

### Quick Start

```bash
git clone https://github.com/yourusername/streetsafe.git
cd streetsafe
```

Open in Android Studio → Sync Gradle → Connect a physical device → Run.

On first launch, the app guides you through granting permissions, adding at least one emergency contact, and running a sensor availability check. Tap **"Start Monitoring"** to begin.

---

## 🗺️ Development Roadmap

| Phase | Timeline | Deliverables |
|-------|----------|-------------|
| **Phase 1 — MVP** | Weeks 1–6 | Foreground service with accelerometer monitoring, fall detection algorithm, shake-to-alert, emergency contact management, SMS alert dispatch with GPS, countdown overlay, home dashboard, French/English localization |
| **Phase 2 — Enhanced Detection** | Weeks 7–10 | Proximity sensor integration, multi-sensor fusion engine, vehicle collision detection, Walk Mode with GPS route monitoring, incident history screen, settings with threshold customization |
| **Phase 3 — Stretch Goals** | Weeks 11–14 | Audio monitoring module, offline map visualization (OSMDroid), battery optimization profiles, auto-start on boot, CSV export of incident history |

---

## 🧪 Testing Strategy

| Test Type | Scope | Tools |
|-----------|-------|-------|
| **Unit Tests** | Detection algorithms, sensor fusion logic, SMS formatting, countdown behavior | JUnit 5, Mockk |
| **Integration Tests** | Room database operations, repository layer, use case pipelines | AndroidX Test, Room in-memory DB |
| **UI Tests** | Screen navigation, countdown overlay interaction, contact management | Espresso, Compose Testing |
| **Manual Device Tests** | Fall simulation on cushioned surfaces, shake gesture trials, false positive measurement during daily activities (walking, stairs, sitting), SMS delivery verification, 4-hour battery consumption tests | Physical device protocol |

Sensor-based features require physical device testing — emulators cannot simulate accelerometer or proximity data. The manual test protocol includes controlled fall simulations, shake gesture trials at various intensities, and extended daily-carry sessions to measure and minimize the false positive rate.

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
8. [OSMDroid Wiki](https://github.com/osmdroid/osmdroid/wiki)

### Safety & Context Data

9. Numbeo Crime Index 2026 — Cameroon: 65.7, Yaoundé: 53.6
10. UK GOV.UK — [Cameroon Safety and Security Advisory](https://www.gov.uk/foreign-travel-advice/cameroon/safety-and-security)
11. U.S. State Department — [Cameroon Travel Advisory](https://travel.state.gov/content/travel/en/traveladvisories/traveladvisories/cameroon-travel-advisory.html)

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Built with ❤️ in Yaoundé, Cameroon 🇨🇲
</p>
