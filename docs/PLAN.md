# Acty HU — Master Plan

A Tesla-style Android home-screen launcher (APK) that turns a tablet into the head unit
for a **1995 Honda Acty SDX 4WD** (HA4, E07A 3-cyl, 5-speed manual).

This document is written for **AI coding agents** (and humans) to pick up work in pieces.
Read [`AGENTS.md`](../AGENTS.md) first, then this file, then [`PROTOCOL.md`](PROTOCOL.md).

---

## 1. Goals at a glance

| Feature | What it does | Signal source |
|---|---|---|
| Home-screen launcher | App is set as Android's Home app; bottom-dock "app drawer" lists every other installed app | Android `PackageManager` |
| Tesla-style layout | Left "car pane" (gear, speed, RPM bar, telltales, 3D truck) + right map pane + bottom dock | — |
| Gear strip | `1 2 3 4 5 R` replaces Tesla's `P R N D`. Active gear lit **blue**; **none lit = Neutral** | Shifter sensors (bridge) or inferred |
| Speed | Big speed number | **GPS** |
| RPM bar | Replaces Tesla's power/regen bar; fills left→right toward redline | Tach signal (bridge) |
| Telltales | Low/high beam, L/R blinkers, brake, **Clutch** light, **Shift** light, door-ajar, TPMS warning, link-lost | Bridge + app logic |
| TPMS | Per-tire pressure/temp + low-pressure warning | BLE valve-cap sensors |
| 3D truck | White 1995 Acty SDX 4WD; spin/zoom with touch; doors open/close; headlight rays; tail glow (brighter on brake); blinkers front + rear | Bridge |
| Backup cameras | 3 cameras (rear + left + right), shown automatically in `R` | USB cameras |

**Interpretations made (owner can override — see §11 Open Questions):**
- **Clutch light** = lit while the clutch pedal is pressed (needs a pedal switch).
- **Shift light** = lit when RPM ≥ a configurable shift point (computed in the app; no extra wiring).
  The bridge may also send an external shift-light input if the owner prefers.
- Neutral shows **nothing lit** on the gear strip (exactly as requested).

---

## 2. Hardware recommendation

### 2.1 Tablet — Galaxy Tab S10+ is a good choice, with two caveats

Good fit:
- 12.4" 16:10 screen. Tesla Model 3/Y screens are also 16:10, so the layout carries over 1:1.
- Fast chip (handles a 3D view + map + cameras at once), USB-C 3.2 Gen 1 port.

Caveats to check **before buying / wiring**:
1. **GPS on the Wi-Fi model is unclear.** Spec sources disagree on whether the Wi-Fi-only
   S10+ (SM-X820) has GNSS. The 5G model (SM-X826B) definitely does.
   → The plan covers this: the bridge can carry a **u-blox GPS module** (10 Hz,
   which is actually smoother for a speedometer than tablet GPS). The app supports both sources.
2. **Size and heat.** Measure the Acty dash; 12.4" is big for a kei truck. A parked truck in sun
   can cook a tablet battery. Turn on Samsung's *Protect battery* (charge limit) and plan a shade/mount.

Alternatives if it doesn't fit: Galaxy Tab S10 FE / S9 FE (10.9"). The UI will scale to any 16:10
landscape tablet running Android 10+.

### 2.2 Signal bridge — **ESP32-S3** microcontroller (Arduino-compatible), not a Raspberry Pi

**Recommendation: an ESP32-S3 dev board (e.g. ESP32-S3-DevKitC-1), programmed with the Arduino
framework via PlatformIO, plugged into the tablet by USB.**

Why this over the alternatives:

| Option | Verdict |
|---|---|
| **ESP32-S3** ✅ | Boots in under 1 s, no SD card to corrupt when the key turns off, native USB (shows up as a serial port Android can read), hardware pulse counter (perfect for RPM), plenty of inputs, Bluetooth/Wi-Fi as a backup link. ~$15. |
| Arduino Uno/Nano | Works, but slower, fewer pins, needs a USB-serial chip, no wireless fallback. |
| Raspberry Pi Pico 2 | Also a great choice (same reasons). Fine substitute; firmware would need minor porting. |
| Raspberry Pi 4/5 | Overkill for reading switches. Boots in 20–40 s, needs a clean shutdown or the SD card corrupts. **Only** worth it as a *camera server* if direct USB cameras fail (see §2.4). |

### 2.3 Input conditioning (the part that keeps the electronics alive)

Car 12 V is noisy (spikes well above 12 V). **Never wire 12 V straight into the ESP32 (3.3 V max).**

- Use an **8-channel PC817 optocoupler isolation board** (12 V in → 3.3 V-safe out). One channel per signal.
- Tach signal: through its own optocoupler + series resistor + TVS diode; the firmware counts pulses.
- Door switches on most cars switch to **ground** when open (they light the dome lamp) — those are
  active-low and get the ESP32's internal pull-up instead. Polarity is configurable per input in firmware.
- Power the bridge + USB hub from a **12 V→5 V automotive buck converter** on a fused
  (add-a-fuse) **ACC/ignition** circuit. Charge the tablet from a **USB-C PD car charger (45 W)**.

### 2.4 Signals and where they come from

| Signal | How to get it on a 1995 Acty | Input type |
|---|---|---|
| RPM | Tach wire behind the cluster, or ignition coil negative. 3-cyl 4-stroke with one coil = **1.5 pulses/rev** (configurable — verify with a known idle). Note: not every Acty trim has a factory tach; the coil negative works regardless. | Pulse (optocoupled) |
| Gear 1–5, R | **No factory sensor on a manual.** Add a sensor plate under the shift boot: 6 digital hall-effect sensors (e.g. DRV5032) + 1 magnet on the lever. No sensor triggered = Neutral. **Fallback:** "inferred gear" mode — app computes gear from RPM ÷ speed after a calibration drive; reverse taps the **reverse-light switch** wire. | 6 × digital |
| Reverse | Reverse-light circuit (12 V when in R). Used as a cross-check and to trigger cameras even if shifter sensors fail. | 12 V |
| Headlights low / high | Headlight switch output wires | 12 V |
| Parking/tail lights | Tail-light circuit | 12 V |
| Brake | Brake-light switch output | 12 V |
| Blinkers L / R | Turn-signal bulb feeds (already flash — just mirror them). Hazards = both. | 12 V |
| Clutch | Add a pedal microswitch (normally-open) at the clutch pedal stop | Switch to ground |
| Doors L / R | Door-jamb switches (dome-light circuit) | Ground when open |
| Ignition/ACC | ACC wire — tells the app the key is on/off | 12 V |
| 4WD (optional) | 4WD engage switch/indicator, if present | 12 V |
| TPMS | **4 × BLE valve-cap TPMS sensors** read directly by the tablet over Bluetooth (no wiring). | BLE |
| GPS (optional) | u-blox NEO-M9N (or M8N) on the bridge UART, if the tablet has no GPS | UART |

### 2.5 Cameras (3 for the backup view) — biggest technical risk, so we de-risk it first

**Plan A (preferred): 3 × USB (UVC) cameras → powered USB-C hub with PD pass-through → tablet.**
The app talks to the cameras directly through a libuvc-based library (does not depend on Samsung
enabling Android's external-camera support). Use cameras that output **MJPEG at 640×480 or 720p**
to stay within USB bandwidth. The same hub carries the ESP32 bridge and charging.

**Plan B (if 3 simultaneous UVC streams fail on the tablet): hardware "quad-view" combiner.**
3 analog/AHD backup cameras → 4-channel quad splitter → one USB capture dongle → tablet.
App crops the single image into rear/left/right tiles. Very reliable, slightly lower quality.

**Plan C: Raspberry Pi 5 camera server.** Pi captures 3 cameras, streams low-latency H.264 over
Ethernet/Wi-Fi; app plays it. Most flexible, but adds boot time, latency, and shutdown handling.

Task **CAM-0** (spike) decides between A/B/C on the real tablet before the camera UI is built.

### 2.6 Shopping list (starter)

- ESP32-S3-DevKitC-1 (N8R8 or similar)
- 8-ch PC817 optocoupler board ×2 (12+ inputs)
- 6 × DRV5032 (or A3144) hall sensors + small neodymium magnet + perfboard
- Clutch pedal microswitch
- 12 V→5 V 3 A automotive buck converter, add-a-fuse kit, inline fuses, TVS diodes
- Powered USB-C hub with PD pass-through (USB 3, multi-TT if possible)
- 45 W USB-C PD car charger
- 3 × USB UVC cameras (MJPEG), one wide-angle for rear
- 4 × BLE TPMS valve-cap sensors (pick a model whose BLE format is already decoded — see TPMS-0)
- Optional: u-blox NEO-M9N GPS module with antenna

---

## 3. Software architecture

```
┌──────────────── Tablet (Android app "Acty HU") ─────────────────┐
│                                                                │
│  VehicleDataSource ──► VehicleRepository (StateFlow<VehicleState>) ──► Compose UI
│   ├─ UsbSerialSource   (ESP32 bridge, real car)                │   ├─ LeftPane (gear, speed, RPM, telltales)
│   ├─ TcpSource         (dev simulator over Wi-Fi)              │   ├─ CarView3D (Filament/SceneView)
│   └─ DemoSource        (built-in fake data)                    │   ├─ MapPane (MapLibre)
│  SpeedSource: TabletGnss | BridgeGps                           │   ├─ CameraPane (3 cams)
│  TpmsSource: BLE scanner                                       │   ├─ Dock + AppDrawer
│  GearResolver: Sensor | Inferred(rpm/speed)                    │   └─ Settings / Calibration
└────────────────────────────────────────────────────────────────┘
          ▲ USB CDC serial, newline JSON @ 20 Hz (see PROTOCOL.md)
┌─────────┴──────── ESP32-S3 bridge firmware ─────────┐
│ debounced digital inputs, RPM pulse counter, optional GPS │
└────────────────────────────────────────────────────────────┘
```

### Tech stack (decided — don't relitigate without the owner)

| Area | Choice | Why |
|---|---|---|
| Language/UI | Kotlin + Jetpack Compose (Material 3, custom dark theme) | Modern, declarative, easy to mimic a custom UI |
| Build | Gradle (Kotlin DSL), version catalog, AGP current stable | — |
| SDK | `minSdk 29`, `targetSdk`/`compileSdk` = current stable | Tab S10+ ships Android 14+ |
| 3D | **SceneView** (Google Filament under the hood), glTF/GLB model | Real-time PBR, bloom, emissive materials, touch orbit |
| Map | **MapLibre Native Android** + dark vector style (e.g. OpenFreeMap) | No API key/billing required |
| USB serial | `usb-serial-for-android` (mik3y) | Supports ESP32-S3 native CDC-ACM |
| Cameras | libuvc-based UVC library (e.g. `AndroidUSBCamera`) — finalised by CAM-0 | Works without vendor UVC support |
| GPS | `LocationManager` GPS provider (1 Hz) / bridge GPS (10 Hz) | Low latency, no Play Services dependency |
| Media | `NotificationListenerService` + `MediaSessionManager` | "Now playing" card like Tesla |
| DI / state | Hilt (or manual DI if simpler) + `StateFlow` | — |
| Firmware | PlatformIO + Arduino framework for ESP32-S3 | Beginner-friendly, huge ecosystem |
| CI | GitHub Actions: build debug APK + firmware, upload as artifacts | Owner downloads the APK from Actions |

### Repo layout (to be created in Phase 0)

```
app/                         Android app (package io.github.eddib17.actyhu)
firmware/acty-bridge/        PlatformIO project for the ESP32-S3
tools/simulator/             Python script that speaks PROTOCOL.md over TCP or serial
tools/model/                 Blender scripts: placeholder truck + model checker
docs/                        PLAN.md, PROTOCOL.md, WIRING.md, MODEL_SPEC.md, UI_SPEC.md
.github/workflows/           CI
AGENTS.md / CLAUDE.md        Rules for agents
```

---

## 4. UI specification (mimic Tesla Model 3/Y v11+ layout)

Landscape only, dark theme by default, light theme optional. Design at 1920×1200 dp-equivalent and scale.

```
┌───────────────────────────┬──────────────────────────────────────────────┐
│ 1 2 3 4 5 R        ⚠ 🔋 12:41│                                              │
│ ◀            ▶  (blinkers) │                                              │
│            42              │                 MAP (dark)                    │
│           MPH              │        (camera view replaces top of           │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━  │         map when in R)                        │
│  (RPM bar)                 │                                              │
│ 💡 🔦 CLUTCH SHIFT 🚪 TPMS  │                                              │
│                            │                                              │
│        [3D ACTY]           │                                              │
│   (drag to spin, pinch)    │                                              │
│                            │                                              │
├────────────────────────────┴──────────────────────────────────────────────┤
│  🚚   │        ♪ Now playing card        │   📷   ⋯(apps)   │   🔊 vol    │
└───────────────────────────────────────────────────────────────────────────┘
```

Left pane ≈ 33 % width; right map ≈ 67 %; dock ≈ 8 % height — match Tesla's proportions.

### 4.1 Gear strip (`1 2 3 4 5 R`)
- Same position, font weight and spacing as Tesla's `P R N D`.
- Inactive: mid-gray (`#5C5C5C`-ish). Active: **Tesla blue (`#3E6AE1`)** with soft glow.
- Neutral: **no character lit**. Unknown/link lost: all gray (same as neutral) + link-lost telltale.

### 4.2 Speed
- Large thin numerals centered in the left pane, unit label (`MPH` / `km/h`, setting) beneath.
- From GPS; smoothed (low-pass, ~0.5 s). Shows `--` with no fix. < 2 mph shows `0` (GPS jitter).

### 4.3 RPM bar (replaces Tesla power/regen bar)
- Thin horizontal bar under the speed, exact Tesla styling (rounded ends, thin track).
- Fills 0 → redline (configurable). Color: white/light gray; amber from shift point −500; red at shift point+.
- Optional small numeric RPM (setting, default off — Tesla shows no number).

### 4.4 Telltales
Tesla-style flat icons, shown only when active:
low beam (green), high beam (blue), turn arrows (green, mirror input flashing), **CLUTCH** (amber
text badge), **SHIFT** (red/amber text badge, may pulse), door ajar (red), TPMS (amber), parking lights,
4WD (optional), bridge link lost (gray/red).

### 4.5 3D truck view
- Default camera: rear-3/4, slightly above (Tesla default).
- **Touch:** one-finger drag = orbit (yaw unlimited, pitch clamped), pinch = zoom (clamped),
  double-tap = reset. After 5 s idle, ease back to default view (like Tesla).
- **Doors:** open/close animate ~0.4 s around hinge pivots; label "Open" chip appears near door.
- **Headlights:** emissive lens + translucent additive **light-ray cones** in front (longer/brighter on high beam) + faint ground glow.
- **Tail lights:** emissive red when lights on; **brighter + bloom when brake pressed** (brake alone also lights them).
- **Blinkers:** amber emissive front *and* rear on each side, mirroring the input (hazards = both).
- **Reverse:** white reverse lamps lit in R (bonus).
- **TPMS:** tap a tire → pressure/temp bubbles appear around the truck (Tesla style); auto-show if a tire is low.
- Background: Tesla-like gradient floor with soft shadow.

### 4.6 Right pane
- **Map:** dark style, follows GPS heading, recenter button, zoom. (Turn-by-turn navigation is *out of scope* for v1.)
- **Camera view:** when gear = R (or reverse-light input), slides in over the top of the map:
  rear camera large, left and right cameras as two tiles below (Tesla backup layout).
  Optional static guide lines (calibratable). Mirror toggle per camera. Leaves 1 s after leaving R
  (configurable). Can also be opened from the dock camera icon.

### 4.7 Dock (bottom bar) and app drawer
- Left: truck icon → opens "Truck" card (TPMS, door states, signal diagnostics, settings entry).
- Center: media card (now playing, play/pause/next) from any media app.
- Right: camera icon, **apps icon (⋯)**, volume.
- **App drawer:** a Tesla-style sheet that slides up from the dock showing **every installed launchable app**
  (icon + label grid, alphabetical, search box). Tap launches it. Long-press: app info / pin to dock.
  Pinned apps appear in the dock (Tesla lets you customize the dock).

### 4.8 Launcher behaviour
- Manifest: `MAIN` + `HOME` + `DEFAULT` intent filter; `singleTask`; landscape lock.
- Immersive full screen, keep-screen-on while ACC is on.
- Pressing Home returns to the head-unit screen.
- `<queries>` element for `MAIN/LAUNCHER` intents to enumerate apps (no `QUERY_ALL_PACKAGES` needed).

### 4.9 Visual fidelity rules
- Copy the **layout, proportions, spacing, colors, animation feel** of Tesla's UI.
- **Do not** ship Tesla's logo, wordmark, or proprietary fonts/assets. Use a free font with a similar feel
  (e.g. *Inter* or *Manrope*). This is a personal project; keep it clean anyway.
- Agents should keep a `docs/UI_SPEC.md` with measured sizes/colors once the first screen exists.

---

## 5. 3D model specification (summary — full spec goes in `docs/MODEL_SPEC.md`)

- Format **glTF 2.0 binary (`.glb`)**, ≤ 100k triangles, ≤ 8 MB, PBR materials, +Y up, meters, origin at
  ground center between axles, front faces −Z.
- Body: **white** paint (`#F2F2F0`-ish clearcoat), SDX trim details, 4WD stance.
- **Required named nodes** (app finds them by name):
  `door_L`, `door_R` (pivot at hinge), `headlight_L`, `headlight_R`, `taillight_L`, `taillight_R`,
  `blinker_FL`, `blinker_FR`, `blinker_RL`, `blinker_RR`, `reverse_L`, `reverse_R`,
  `wheel_FL`, `wheel_FR`, `wheel_RL`, `wheel_RR`, `body`.
  Plus empty anchor nodes `beam_L`, `beam_R` (where light-ray cones start).
- Lamp nodes use their own material so the app can drive `emissiveFactor`/strength.
- **Sourcing:** an accurate Acty model is a human/artist task (buy/licence from Sketchfab or similar, or commission
  one in Blender). **Agents** build a *placeholder* low-poly truck via a Blender Python script with the exact node
  names above, so the app is complete before the real model arrives. A `tools/model/check_glb.py` validates any model.

---

## 6. Data contract

See [`PROTOCOL.md`](PROTOCOL.md). It is the single contract between firmware, simulator and app.
Changing it requires bumping `"v"` and updating all three in the same PR.

---

## 7. Work breakdown for agents

Each task has an ID, dependencies, deliverables and **acceptance criteria (AC)**. Tasks in the same phase with
no dependency on each other can run **in parallel**. Keep one task per PR where practical.

### Phase 0 — Foundations (do first, one agent)

**P0-1 Repo scaffold** — deps: none — ✅ **DONE** (CI green: `assembleDebug`, `lintDebug`, `spotlessCheck`, `pio run`)
- Create Android project in `app/` (Kotlin, Compose, Gradle KTS, version catalog), package `io.github.eddib17.actyhu`.
- Create empty `firmware/acty-bridge/` PlatformIO project for `esp32-s3-devkitc-1`.
- Add `.gitignore`, `.editorconfig`, ktlint/detekt (or spotless) config.
- AC: `./gradlew assembleDebug` succeeds; `pio run` succeeds; app opens a black screen saying "Acty HU".
- Notes: versions pinned in `gradle/libs.versions.toml` (AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01, Gradle 8.11.1);
  firmware pinned to `espressif32@6.9.0`. ktlint uses the `android_studio` style. A minimal build-only workflow
  (`.github/workflows/build.yml`) was added to verify this task; P0-2 still owns artifacts, tests and the install guide.
  On-device check of the "Acty HU" screen still needs the owner (no emulator in the agent sandbox).

**P0-2 CI** — deps: P0-1
- GitHub Actions: on push/PR build debug APK, run unit tests + lint, build firmware; upload `app-debug.apk` and `firmware.bin` as artifacts.
- AC: green run on the branch; artifacts downloadable. README section "How to install the APK" for the owner.

**P0-3 Core model + data sources** — deps: P0-1
- `VehicleState` data class mirroring PROTOCOL.md; `VehicleDataSource` interface; `VehicleRepository` exposing `StateFlow<VehicleState>`.
- `DemoSource`: scripted drive (idle → 1st → 5th → brake → reverse with doors/lights/blinkers toggling).
- JSON parser for protocol lines with unit tests (valid, partial, malformed, unknown fields ignored).
- AC: unit tests pass; demo mode cycles all signals.

**P0-4 Simulator** — deps: none (parallel with P0-1)
- `tools/simulator/sim.py`: emits PROTOCOL.md frames at 20 Hz over TCP (port 5555) or a serial port; keyboard controls
  (1-5/R/N gears, L/R blinkers, B brake, H headlights, C clutch, D doors, +/- RPM).
- AC: README in folder; app's `TcpSource` (P1-1) can connect to it.

### Phase 1 — Launcher shell and layout (parallel after Phase 0)

**P1-1 Data-source wiring** — deps: P0-3
- `UsbSerialSource` (usb-serial-for-android, auto-connect on attach via `device_filter.xml`, reconnect with backoff),
  `TcpSource` (host:port in settings). Source selector in settings. Link-lost after 1 s without a frame.
- AC: works with simulator over TCP; USB tested on device when hardware available.

**P1-2 Launcher + app drawer** — deps: P0-1
- HOME intent filter, immersive mode, landscape lock, keep-screen-on.
- App drawer sheet: all launchable apps (exclude self), alphabetical, search, launch on tap, long-press menu
  (app info, pin to dock). Refresh on package install/remove.
- AC: can be chosen as default Home app; drawer lists all apps; launching + returning Home works.

**P1-3 Screen layout + theme** — deps: P0-1
- Left pane / right pane / dock scaffold with Tesla proportions, colors, typography, dark/light themes.
- AC: screenshot test (Paparazzi or Compose screenshot) at 2800×1752 and 1920×1200.

### Phase 2 — Left pane instruments (parallel)

**P2-1 Gear strip** — deps: P1-3, P0-3 — `1 2 3 4 5 R`, blue active, none for neutral. AC: screenshot tests for each gear + neutral.
**P2-2 Speed** — deps: P1-3 — `SpeedSource` (tablet GNSS + bridge GPS), smoothing, units, no-fix state, location permission flow. AC: unit tests for smoothing/jitter floor.
**P2-3 RPM bar + shift light** — deps: P1-3, P0-3 — animated bar, redline/shift-point settings, color zones, SHIFT telltale logic (with hysteresis). AC: unit tests for thresholds/hysteresis.
**P2-4 Telltales** — deps: P1-3, P0-3 — all icons from §4.4, blinkers mirror input. AC: screenshot tests.
**P2-5 Gear resolver** — deps: P0-3, P2-2 — `Sensor` mode (from bridge) and `Inferred` mode (RPM/speed ratio vs calibrated table, clutch-in or < 3 mph ⇒ hold/none; reverse input overrides). Calibration flow: "drive in gear N for 10 s" wizard. AC: unit tests with recorded sample data.

### Phase 3 — 3D truck (one agent; P3-1 can start early)

**P3-1 Placeholder model + checker** — deps: none — Blender Python script generating a white low-poly Acty-shaped truck (cab-over, flat bed) with all §5 node names; `check_glb.py` validator. Write `docs/MODEL_SPEC.md`. AC: GLB in `app/src/main/assets/models/`, checker passes.
**P3-2 CarView3D** — deps: P1-3, P3-1 — SceneView in Compose, lighting, floor + shadow, orbit/pinch/double-tap, idle return. AC: 60 fps on device with demo data (log frame time); touch works.
**P3-3 Lights & effects** — deps: P3-2 — emissive lamps, bloom, headlight ray cones (additive, gradient), high-beam variant, tail vs brake intensity, blinkers F+R, reverse lamps. AC: each state visually verified via screenshot set from DemoSource.
**P3-4 Doors** — deps: P3-2 — hinge animation driven by `doorL/doorR`, "Open" chips. AC: doors animate both ways without popping.
**P3-5 TPMS overlay** — deps: P3-2, P5-1 — tire bubbles, low-pressure auto-show.

### Phase 4 — Right pane

**P4-1 Map** — deps: P1-3 — MapLibre dark style, follow-me, heading, recenter. AC: renders and tracks GPS; no API key needed.
**P4-2 Media card + volume** — deps: P1-3 — notification-listener permission flow, now playing, controls, volume slider.

### Phase 5 — TPMS

**P5-0 TPMS spike** — deps: none — pick a cheap BLE valve-cap TPMS model with a documented/decodable advertisement format; write decoder + tests from captured packets. AC: doc in `docs/TPMS.md` naming the exact sensor to buy.
**P5-1 TPMS source** — deps: P5-0, P0-3 — BLE scan service, sensor pairing (assign IDs to FL/FR/RL/RR), pressure/temp/battery, stale-data handling, low/high thresholds (settings, psi/bar/kPa).

### Phase 6 — Cameras (start CAM-0 early — it's the biggest risk)

**CAM-0 Camera spike** — deps: P0-1 — test 3 simultaneous UVC streams through a PD hub on the real tablet. Measure fps/latency. Decide Plan A/B/C (§2.5) and record in `docs/CAMERAS.md`. **Needs the owner's hardware; agent writes the test app screen + instructions, owner runs it and reports.**
**CAM-1 Camera pane** — deps: CAM-0, P1-3, P2-5 — auto-show on R, Tesla layout, per-camera assignment by USB ID, mirror toggle, guide lines, manual open from dock.

### Phase 7 — Bridge firmware (parallel with app work)

**FW-1 Inputs + protocol** — deps: P0-1 — debounced digital inputs with per-pin polarity config, PROTOCOL.md frame @ 20 Hz over USB CDC, `hello` frame with version, `cfg` command handling.
**FW-2 RPM** — deps: FW-1 — PCNT-based pulse counting, configurable pulses/rev, smoothing, 0 when no pulses for 500 ms. Bench-test with a function generator or a second board.
**FW-3 Gear sensors** — deps: FW-1 — 6 hall inputs → gear 1-5/R/0, ambiguity (two active) ⇒ previous value.
**FW-4 Optional GPS** — deps: FW-1 — u-blox UBX/NMEA parser at 10 Hz → `spd`/`lat`/`lon`/`hdg` fields.
**FW-5 Bench test doc** — `docs/WIRING.md` with pin map, optocoupler wiring, fuse/power diagram, bench test steps with a 12 V supply. **Must include safety warnings.**

### Phase 8 — Settings, calibration, polish

**P8-1 Settings screen** — Tesla-style settings: units, source selection, redline/shift point, pulses/rev, gear mode + calibration wizard, camera assignment, TPMS pairing + thresholds, theme, numeric RPM toggle, debug overlay (raw signals).
**P8-2 Diagnostics page** — live raw values of every input, frame rate, link status — used when wiring the truck.

### Phase 9 — In-car hardening

**P9-1 Boot & power** — start on boot, survive Samsung battery optimisation (foreground service for data link), screen dim/off when ACC drops (Device Admin `lockNow`, optional), reconnect after sleep.
**P9-2 Performance** — 60 fps with map + 3D + cameras; thermal check (log CPU/GPU temps if available); reduce 3D quality when cameras are on.
**P9-3 Owner install guide** — step-by-step: enable unknown sources, install APK from Actions, set as Home, grant permissions, Protect battery, mount tips.

### Human-only tasks (owner)
- H-1 Buy hardware (§2.6). H-2 Confirm tablet GPS (Settings → Location test app) or order GPS module.
- H-3 Measure dash for mount. H-4 Wire the truck following `docs/WIRING.md` (or with an auto electrician).
- H-5 Obtain/commission the real Acty 3D model. H-6 Run CAM-0 on the tablet. H-7 Verify RPM pulses/rev at idle.

---

## 8. Milestones

| Milestone | Contents | Proves |
|---|---|---|
| **M1 "It's my home screen"** | P0-*, P1-2, P1-3 | Installable launcher with app drawer |
| **M2 "Dash on the desk"** | P1-1, P2-*, P3-1..4, P4-1, FW-1/2 | Full UI driven by simulator / bench bridge |
| **M3 "Sees and senses"** | CAM-*, P5-*, P3-5, FW-3/4 | Cameras + TPMS + gear sensing |
| **M4 "In the truck"** | P8-*, P9-*, H-* | Installed and reliable in the Acty |

---

## 9. Testing strategy

- **Unit tests:** protocol parser, gear resolver, RPM/shift hysteresis, speed smoothing, TPMS decoder.
- **Screenshot tests** for every left-pane state (gears, telltales, RPM zones), light & dark.
- **Simulator-driven manual test** checklist in `docs/TEST_CHECKLIST.md`.
- **Firmware:** native PlatformIO unit tests for debounce/gear logic; bench test with 12 V supply and switches.
- Never test wiring with the engine running until the bench test passes.

---

## 10. Risks

| Risk | Mitigation |
|---|---|
| 3 UVC cameras won't stream at once on Samsung | CAM-0 first; Plan B quad combiner; Plan C Pi server |
| Wi-Fi tablet has no GPS | Bridge GPS (FW-4) |
| Tablet overheats / battery swells | Protect-battery limit, shade, don't leave in a hot parked truck |
| Electrical noise kills the ESP32 | Optocouplers, TVS, fused buck converter, common ground point |
| No accurate 3D model available | Placeholder ships first; spec lets any artist model drop in |
| Samsung kills background service | Foreground service, battery-optimisation exemption, launcher is always foreground anyway |

---

## 11. Open questions for the owner

1. **Units:** MPH or km/h default? (Assumed **MPH**.)
2. **Clutch light:** "on while clutch pressed" — correct? Or something else (e.g. a reminder to clutch in to start)?
3. **Shift light:** RPM-based in the app (assumed) — what RPM? Or a separate physical switch?
4. **Redline** for your E07A (read it off your tach if fitted; we'll default to a placeholder and make it a setting).
5. **Tablet:** Wi-Fi or 5G Tab S10+? (Decides whether the GPS module is needed.)
6. **Camera positions:** rear + left/right mirrors (Tesla style)? Or rear + bed/side?
7. **Gear sensing:** OK to add hall sensors under the shift boot (recommended), or prefer the no-wiring "inferred" mode?
8. Anything for the right pane besides the map (e.g. a big music view)?
