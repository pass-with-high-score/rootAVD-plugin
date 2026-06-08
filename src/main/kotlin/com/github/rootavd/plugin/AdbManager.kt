package com.github.rootavd.plugin

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import java.io.File

class AdbManager(private val project: Project) {

    fun runCommand(vararg args: String): String {
        return try {
            val commandLine = GeneralCommandLine("adb", *args)
            val process = commandLine.createProcess()
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    fun installApk(serial: String, apkPath: String): String {
        return runCommand("-s", serial, "install", "-r", "-d", apkPath)
    }

    fun pushFile(serial: String, localPath: String, remotePath: String): String {
        return runCommand("-s", serial, "push", localPath, remotePath)
    }

    fun pullFile(serial: String, remotePath: String, localPath: String): String {
        return runCommand("-s", serial, "pull", remotePath, localPath)
    }

    fun shell(serial: String, command: String): String {
        return runCommand("-s", serial, "shell", command)
    }

    fun reboot(serial: String, mode: String = ""): String {
        return if (mode.isEmpty()) runCommand("-s", serial, "reboot") 
               else runCommand("-s", serial, "reboot", mode)
    }
}
