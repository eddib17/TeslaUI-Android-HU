# TeslaUI-Android-HU
A Tesla style UI for an Android Head Unit. Feel free to fork and replace any parts or code to be more geared towards another car, but my main plan here is for my 1995 Honda Acty. This will be at least 90% "vibe coded" because I'm no software developer, but bear with me as I bring my ideas to life okay. 🫣


## Project docs
- [Master plan](docs/PLAN.md) — features, hardware recommendation, architecture, task list
- [Bridge ↔ app protocol](docs/PROTOCOL.md)
- [Instructions for AI agents](AGENTS.md)

## How to install the APK (on the tablet)

Every time code is pushed, GitHub builds a fresh APK automatically. To get it onto the tablet:

1. **Find the build.** On GitHub, open the repo → **Actions** tab → click the newest **Build** run with a green ✅.
   (You must be signed in to GitHub to download.)
2. **Download it.** Scroll to **Artifacts** at the bottom and tap **acty-hu-apk**. GitHub gives you a `.zip`.
3. **Unzip it.** On the tablet, open **My Files → Downloads**, tap the zip and choose **Extract**.
   Inside is a file like `acty-hu-42-1a2b3c4.apk` (42 = build number).
4. **Allow installing.** Tap the `.apk`. The first time, Android asks to allow installs from this source
   (e.g. *My Files* or *Chrome*) → **Settings → Allow from this source** → go back.
5. **Install.** Tap **Install**. If Play Protect warns about an unknown app, choose **Install anyway**
   (it's warning only because the app isn't from the Play Store).
6. **Updating later:** repeat the same steps. Newer builds install *over* the old one and keep your settings —
   no need to uninstall first.

> **Tip:** you can also download on a computer and copy the APK over USB, or install with
> `adb install -r acty-hu-XX.apk` if you have Android platform-tools.

Firmware for the ESP32-S3 bridge is built the same way and appears under **Artifacts → acty-bridge-firmware**
(flashing instructions come with the firmware tasks, FW-1+).

## Building it yourself (optional)

- **App:** install Android Studio, open this folder, press ▶ Run. Or from a terminal: `./gradlew assembleDebug`
  (needs JDK 17 and the Android SDK; set `ANDROID_HOME` or create `local.properties` with `sdk.dir=...`).
- **Checks CI runs:** `./gradlew spotlessCheck testDebugUnitTest lintDebug assembleDebug`
  (`./gradlew spotlessApply` auto-fixes formatting).
- **Firmware:** install [PlatformIO](https://platformio.org/install), then `pio run -d firmware/acty-bridge`.
