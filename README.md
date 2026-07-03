# BatteryMax

BatteryMax is an Android app that keeps a continuous record of your phone's battery — and the batteries of any Bluetooth devices you choose — and visualizes them as daily graphs.

## Features

- **Background monitoring**: a foreground service samples the phone battery (level, charging state, temperature, voltage) continuously, surviving app closure and device reboots.
- **Multiple Bluetooth devices**: track any number of paired Bluetooth devices (headsets, watches, earbuds). Each device is stored and monitored independently, with connected/disconnected status on the Dashboard.
- **Local history**: all samples are stored on-device in a Room database (no network, no accounts). Old data is pruned automatically after ~30 days.
- **Daily graph**: a line chart of battery level over any given day for the phone or any tracked Bluetooth device, with zoom presets and a Now button.
- **Settings**: version info, permission status, and battery-optimization opt-out for reliable background monitoring.

## Screens

| Screen | Purpose |
| --- | --- |
| Dashboard | Live phone and Bluetooth battery status (per device), monitoring on/off toggle |
| Graph | Battery level chart for a selected device and day, with zoom chips and Now |
| Devices | Track or stop tracking any number of paired Bluetooth devices |
| Settings | Version, permissions, and battery optimization |

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM with a repository layer; manual DI via an `Application` class
- Room (with KSP) for persistence
- Navigation Compose
- Vico for charts
- Foreground service + broadcast receivers for battery and Bluetooth events

## Requirements

- minSdk 24, targetSdk 36
- Runtime permissions: notifications (API 33+), Bluetooth connect (API 31+)
- Optional: disable battery optimization for more reliable background sampling

## Building

Open the project in Android Studio and run the `app` configuration, or build from the command line:

```bash
./gradlew assembleDebug
```

Versioning: `versionName` is `1`; `versionCode` is `yyyyMMddHH` at build time (Settings shows e.g. `Version 1 (2026.071010)`).

## Project Documents

- [docs/PLAN.md](docs/PLAN.md) — architecture and implementation plan
