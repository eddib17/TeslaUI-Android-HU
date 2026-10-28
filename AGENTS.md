# Instructions for AI agents

This repo builds **Acty HU**: a Tesla-style Android launcher + an ESP32-S3 signal bridge for a
1995 Honda Acty SDX 4WD. The owner is new to coding — keep things simple, explained and working.

## Read first
1. [`docs/PLAN.md`](docs/PLAN.md) — goals, hardware, architecture, **task list with IDs and acceptance criteria**.
2. [`docs/PROTOCOL.md`](docs/PROTOCOL.md) — the bridge ↔ app data contract. Don't change it casually.

## How to work
- Pick **one task ID** from PLAN.md §7 whose dependencies are done. Put the ID in your branch name,
  commit messages and PR title (e.g. `P2-1: gear strip`).
- Meet every acceptance criterion in the task. If you can't, say which one and why in the PR.
- Tech stack in PLAN.md §3 is decided. Don't swap libraries without the owner agreeing.
- If the Android SDK isn't available in your environment, rely on CI (`.github/workflows`) to build
  and say so in the PR — never claim a build passed that you didn't see pass.
- Update PLAN.md (tick the task, note decisions) when a task finishes. Record design decisions in `docs/`.

## Conventions
- Kotlin + Jetpack Compose, package `io.github.eddib17.actyhu`. Small, readable files; comments explain *why*.
- All tunable numbers (redline, shift point, pulses/rev, thresholds) are **settings**, not constants.
- Every new signal flows: firmware → PROTOCOL.md → `VehicleState` → UI. Update the simulator and `DemoSource` too.
- No Tesla logos, wordmarks or proprietary fonts/assets — mimic layout, proportions and feel only.
- Firmware: PlatformIO + Arduino framework, `esp32-s3-devkitc-1`. Never assume a pin is 12 V tolerant.
- Wiring docs must include safety notes (fusing, optocouplers, disconnect battery before wiring).

## Things that need the owner (don't fake them)
Hardware tests (cameras CAM-0, USB bridge on the tablet, in-truck wiring), the real 3D model,
and answers to PLAN.md §11 open questions. Leave a clear checklist for the owner instead.
