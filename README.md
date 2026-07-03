# BatteryMax

BatteryMax is an Android app that keeps a continuous record of your phone's battery — and the battery of a Bluetooth device you choose — and visualizes it as a daily graph.

## Features

- **Background monitoring**: a foreground service samples the phone battery (level, charging state, temperature, voltage) continuously, surviving app closure and device reboots.
- **Bluetooth device battery**: pick any paired Bluetooth device (headset, watch, earbuds) and BatteryMax will track and store its battery level alongside the phone's.
- **Local history**: all samples are stored on-device in a Room database (no network, no accounts). Old data is pruned automatically after ~30 days.
- **Daily graph**: a line chart of battery level over any given day, with separate series for the phone and the tracked Bluetooth device.

## Screens

| Screen | Purpose |
| --- | --- |
| Dashboard | Live phone and Bluetooth battery status, monitoring on/off toggle, permission prompts |
| Daily Graph | Battery level chart for a selected day with day-by-day navigation |
| Devices | Choose which paired Bluetooth device to track |

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- MVVM with a repository layer; manual DI via an `AppContainer`
- Room (with KSP) for persistence
- Navigation Compose
- Vico for charts
- Foreground service + broadcast receivers for battery and Bluetooth events

## Requirements

- minSdk 24, targetSdk 36
- Runtime permissions: notifications (API 33+), Bluetooth connect (API 31+)

## Building

Open the project in Android Studio and run the `app` configuration, or build from the command line:

```bash
./gradlew assembleDebug
```

## Project Documents

- [docs/PLAN.md](docs/PLAN.md) — full architecture and implementation plan
