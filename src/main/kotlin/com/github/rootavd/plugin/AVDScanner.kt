package com.github.rootavd.plugin

import java.io.File
import java.util.Properties

data class AVDInfo(
    val name: String,
    val iniFile: File,
    val properties: Properties,
    val systemImagePath: String?,
    val ramdiskPath: String?
)

class AVDScanner {
    fun getAvds(): List<AVDInfo> {
        val avdHome = File(System.getProperty("user.home"), ".android/avd")
        if (!avdHome.exists()) return emptyList()

        return avdHome.listFiles { file -> file.extension == "ini" }?.mapNotNull { iniFile ->
            val props = Properties()
            try {
                iniFile.inputStream().use { props.load(it) }
                val name = iniFile.nameWithoutExtension
                val path = props.getProperty("path")
                
                var systemImagePath: String? = null
                var ramdiskPath: String? = null
                
                if (path != null) {
                    val configIni = File(path, "config.ini")
                    if (configIni.exists()) {
                        val configProps = Properties()
                        configIni.inputStream().use { configProps.load(it) }
                        systemImagePath = configProps.getProperty("image.sysdir.1")
                        
                        if (systemImagePath != null) {
                            // Convert relative to absolute if needed
                            val sdkHome = findSdkHome()
                            val fullSystemImagePath = if (systemImagePath.startsWith("system-images")) {
                                File(sdkHome, systemImagePath).absolutePath
                            } else {
                                systemImagePath
                            }
                            ramdiskPath = File(fullSystemImagePath, "ramdisk.img").absolutePath
                        }
                    }
                }

                AVDInfo(name, iniFile, props, systemImagePath, ramdiskPath)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    private fun findSdkHome(): String {
        // Try common locations
        val envSdk = System.getenv("ANDROID_SDK_ROOT") ?: System.getenv("ANDROID_HOME")
        if (envSdk != null) return envSdk
        
        val home = System.getProperty("user.home")
        val macPath = "$home/Library/Android/sdk"
        if (File(macPath).exists()) return macPath
        
        val winPath = "$home/AppData/Local/Android/Sdk"
        if (File(winPath).exists()) return winPath
        
        return ""
    }
}
