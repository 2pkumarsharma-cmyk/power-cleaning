# L3210 Strong Clean

Phone-buildable Android project for Epson L3210 Strong % Cleaning.

## Important
This project downloads the current `android-epson-reset` source from GitHub during the first Gradle build and reuses its proven USB/D4/EPSON-CTRL implementation. It does NOT send a guessed raw `04` byte directly.

The L3200-series database entry is used by `PrinterService.runHeadCleaning(..., "Strong % Cleaning")`.

## Build on Android
Open this folder in AndroidIDE (or another Gradle-capable Android IDE) and build the `app` module. Internet is required for the first build because the upstream source is downloaded.

## Use
1. USB OTG -> Epson L3210.
2. Open the app.
3. Tap CONNECT.
4. Grant USB permission.
5. Confirm printer is detected.
6. Tap STRONG CLEANING.
7. Confirm the warning.
8. After it finishes, use NOZZLE CHECK.

The app deliberately calls the project's documented `Strong % Cleaning` operation. It does not claim that this is identical to Epson's official Windows Power Cleaning.
