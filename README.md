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

## How background monitoring works

When **Background monitoring** is on, `BatteryMonitorService` runs as a **foreground service** (persistent notification). That keeps the process alive so battery updates can be recorded even when the app UI is closed. After reboot, `BootReceiver` starts the service again if monitoring was left enabled.

Samples are stored in Room as `BatterySampleEntity` rows. The phone uses `source = "phone"`; each Bluetooth device uses `source = <MAC address>`.

### Phone battery

**Event:** the system sticky broadcast `Intent.ACTION_BATTERY_CHANGED`.

The service registers a receiver for that intent. Android delivers it:

- immediately when the receiver is registered (current level), and
- again whenever phone battery state changes (level, charging, temperature, voltage, and so on).

**What is stored:** level percent, charging flag, temperature, voltage, and timestamp.

### Bluetooth battery

For each **tracked** device, `BtBatteryReader` listens for:

1. **`android.bluetooth.device.action.BATTERY_LEVEL_CHANGED`** (hidden system broadcast) — main source of level updates for many headsets and watches.
2. **`ACTION_ACL_CONNECTED` / `ACTION_ACL_DISCONNECTED`** — connection state (Dashboard shows connected vs disconnected). On connect, an immediate level read is attempted.
3. **Reflection** `BluetoothDevice.getBatteryLevel()` — initial / on-connect reading.
4. **BLE GATT** Battery Service (`0x180F` / `0x2A19`) — fallback poll about every 5 minutes while connected.

**What is stored:** level percent and timestamp (no temperature or voltage for Bluetooth).

### When a record is written

Not every event is written to the database. `BatteryRepository.recordSample` only inserts if:

- the **level percent changed**, or
- at least **5 minutes** passed since the last stored sample for that source.

A **5-minute timer** in the service also re-records the last known level for the phone and each tracked Bluetooth device (still subject to the same rules), so the graph keeps points even when the level sits still.

| Source | Trigger events | Stored when |
| --- | --- | --- |
| Phone | `ACTION_BATTERY_CHANGED` (+ 5-minute tick) | Level changed or ≥5 minutes since last sample |
| Bluetooth | `BATTERY_LEVEL_CHANGED`, connect + reflection/GATT, 5-minute tick | Same sampling policy |

In short: the app **reacts to system (and Bluetooth) battery events**, then **filters** writes so the database does not fill with identical levels every few seconds.

## Building

Open the project in Android Studio and run the `app` configuration, or build from the command line:

```bash
./gradlew assembleDebug
```

Versioning: `versionName` is `1`; `versionCode` is `yyyyMMddHH` at build time (Settings shows e.g. `Version 1 (2026.071010)`).

## Project Documents

- [docs/PLAN.md](docs/PLAN.md) — architecture and implementation plan (includes the same monitoring details)
