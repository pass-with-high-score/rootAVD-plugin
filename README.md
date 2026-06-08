# RootAVD Manager Plugin

[![Build](https://github.com/pass-with-high-score/rootAVD-plugin/workflows/Build/badge.svg)](https://github.com/pass-with-high-score/rootAVD-plugin/actions)
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/pass-with-high-score/rootAVD-plugin/releases)

**RootAVD Manager** is an IntelliJ Platform plugin for Android Studio designed to simplify and automate the process of rooting Android Virtual Devices (AVD). It provides a user-friendly interface to perform complex rooting operations that typically require manual script execution.

![Plugin Icon](src/main/resources/META-INF/pluginIcon.svg)

## Features

- **Device Scanning:** Automatically detects all configured and running Android Virtual Devices.
- **Root Automation:** Automates the Magisk injection process into the AVD's ramdisk.
- **Backup & Restore:** Automatically creates backups of original ramdisks and allows one-click restoration.
- **Cold Boot Integration:** Easily trigger a Cold Boot to apply root changes.
- **Magisk App Installation:** One-click installation of the Magisk manager app to the running emulator.
- **Utility Tools:** Includes "Wipe Data" and detailed "Scan Details" for troubleshooting.

## Installation

### From Marketplace (Coming Soon)
Search for "RootAVD Manager" in the Android Studio Plugin Marketplace (*File > Settings > Plugins > Marketplace*).

### From Disk
1. Download the latest release ZIP from the [Releases](https://github.com/pass-with-high-score/rootAVD-plugin/releases) page.
2. In Android Studio, go to *File > Settings > Plugins*.
3. Click the gear icon and select **Install Plugin from Disk...**.
4. Select the downloaded ZIP file and restart the IDE.

## Usage

1. Open the **RootAVD** tool window on the right-hand side of Android Studio.
2. Select your target AVD from the dropdown list.
3. Ensure the emulator is running.
4. Click **Root Now** to begin the automated patching process.
5. Once completed, use the **Cold Boot** button to restart the emulator with root access.
6. Use **Install Magisk App** to manage root permissions within the device.

## Prerequisites

- **ADB:** Must be installed and available in your system PATH.
- **Android SDK:** Configured within Android Studio.
- **Emulator:** The target AVD should be an "Official" image (Google Play images may have restricted write access).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the [Apache License 2.0](LICENSE).
