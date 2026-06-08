package com.github.rootavd.plugin

import com.intellij.execution.configurations.GeneralCommandLine
import java.io.File

class EmulatorManager {
    fun getEmulatorPath(): String? {
        val home = System.getProperty("user.home")
        val paths = listOf(
            "$home/Library/Android/sdk/emulator/emulator",
            "$home/AppData/Local/Android/Sdk/emulator/emulator.exe",
            "/usr/local/bin/emulator"
        )
        return paths.find { File(it).exists() }
    }

    fun coldBoot(avdName: String): String {
        val path = getEmulatorPath() ?: return "Error: Emulator binary not found"
        return try {
            // Run in background as it starts a long-running process
            GeneralCommandLine(path, "-avd", avdName, "-no-snapshot-load").createProcess()
            "Cold boot command sent for $avdName"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun wipeData(avdName: String): String {
        val path = getEmulatorPath() ?: return "Error: Emulator binary not found"
        return try {
            GeneralCommandLine(path, "-avd", avdName, "-wipe-data").createProcess()
            "Wipe data command sent for $avdName"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
