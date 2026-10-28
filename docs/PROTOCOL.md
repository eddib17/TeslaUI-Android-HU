# Bridge ↔ App Protocol (v1)

The contract between the ESP32-S3 bridge firmware, the Python simulator, and the Android app.
Any change: bump `v`, update firmware + simulator + app parser + tests in the same PR.

## Transport

- **USB CDC serial** (ESP32-S3 native USB), 115200 baud nominal (CDC ignores baud).
- **TCP** (simulator / dev only): port `5555`, same bytes.
- Encoding: UTF-8, **one JSON object per line**, terminated by `\n`. Max line length 512 bytes.
- Bridge → app: **state frames at 20 Hz**, plus an immediate frame on any input change.
- App treats the link as **lost** if no frame arrives for **1000 ms**.

## Frames: bridge → app

### `hello` (sent on boot and on app request)
```json
{"type":"hello","v":1,"fw":"0.1.0","board":"esp32s3","features":["rpm","gear","gps"]}
```

### `state` (20 Hz)
```json
{"type":"state","v":1,"seq":1234,"ms":456789,
 "acc":1,"rpm":3250,"gear":3,"rev":0,
 "lo":1,"hi":0,"park":1,"brk":0,"tl":0,"tr":1,
 "clu":0,"shf":0,"dl":0,"dr":0,"awd":0,
 "gps":{"fix":1,"spd":12.4,"lat":35.0,"lon":-80.0,"hdg":91.0,"sats":9}}
```

| Key | Type | Meaning |
|---|---|---|
| `seq` | uint | Increments every frame (detect drops) |
| `ms` | uint | Bridge uptime in ms |
| `acc` | 0/1 | Ignition/ACC on |
| `rpm` | int | Engine RPM (already computed on bridge; 0 if no pulses for 500 ms) |
| `gear` | int \| null | `1`–`5`, `-1` = R, `0` = neutral, `null` = no gear sensors fitted / unknown |
| `rev` | 0/1 | Reverse-light circuit live |
| `lo` / `hi` | 0/1 | Low beam / high beam |
| `park` | 0/1 | Parking/tail lights |
| `brk` | 0/1 | Brake pedal |
| `tl` / `tr` | 0/1 | Left / right blinker lamp currently lit (flashes naturally) |
| `clu` | 0/1 | Clutch pedal pressed |
| `shf` | 0/1 | External shift-light input (optional; app's RPM shift light is used if absent) |
| `dl` / `dr` | 0/1 | Driver(left)/passenger(right) door **open** = 1 |
| `awd` | 0/1 | 4WD engaged (optional) |
| `gps` | object \| absent | Present only if bridge GPS fitted. `spd` in **m/s**, `hdg` degrees true |

Rules for the parser:
- **Unknown keys are ignored**; **missing keys keep their default** (0 / null / absent). This keeps old apps working with newer firmware.
- Malformed lines are dropped and counted (shown on the diagnostics page).
- All "which side is left" values are from the **driver's seat** (Acty is right-hand drive: driver = right door).
  → `dl` = **left** door, `dr` = **right** door, physically. The app labels them by side, not by role.

## Frames: app → bridge

```json
{"type":"req","what":"hello"}
{"type":"cfg","ppr":1.5,"debounceMs":20,"rpmSmooth":0.3}
```

| Command | Effect |
|---|---|
| `req` / `hello` | Bridge replies with `hello` |
| `cfg` | Set pulses-per-rev (`ppr`), debounce (`debounceMs`), RPM smoothing factor; stored in NVS |

## Example session
```
bridge → {"type":"hello","v":1,"fw":"0.1.0","board":"esp32s3","features":["rpm","gear"]}
app    → {"type":"cfg","ppr":1.5}
bridge → {"type":"state","v":1,"seq":1,"ms":812,"acc":1,"rpm":850,"gear":0,...}
```
