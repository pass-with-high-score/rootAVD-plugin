package com.github.rootavd.plugin

import com.intellij.openapi.project.Project
import java.io.File
import java.util.zip.ZipFile

class RootEngine(private val project: Project, private val log: (String) -> Unit) {
    private val adb = AdbManager(project)
    private val rootAVDDir = File(System.getProperty("user.home"), ".rootAVD_plugin")
    private val remoteTmp = "/data/local/tmp/rootAVD"

    fun backup(avd: AVDInfo) {
        if (avd.ramdiskPath == null) return
        val ramdiskFile = File(avd.ramdiskPath)
        val backupFile = File(avd.ramdiskPath + ".bak")
        if (ramdiskFile.exists() && !backupFile.exists()) {
            ramdiskFile.copyTo(backupFile)
            log("Backup created: ${backupFile.name}\n")
        }
    }

    fun restore(avd: AVDInfo) {
        if (avd.ramdiskPath == null) return
        val backupFile = File(avd.ramdiskPath + ".bak")
        if (backupFile.exists()) {
            backupFile.copyTo(File(avd.ramdiskPath), overwrite = true)
            log("Restored original ramdisk from backup.\n")
        } else {
            log("No backup found to restore.\n")
        }
    }

    fun installMagiskApp() {
        val deviceSerial = getActiveDevice() ?: return
        val apk = File(rootAVDDir, "magisk.apk")
        if (apk.exists()) {
            log("Installing Magisk App...\n")
            val result = adb.installApk(deviceSerial, apk.absolutePath)
            log(result + "\n")
        } else {
            log("Note: magisk.apk not found in local plugin storage.\n")
        }
    }

    fun root(avd: AVDInfo) {
        try {
            if (avd.ramdiskPath == null) {
                log("Error: Ramdisk path not found for ${avd.name}\n")
                return
            }

            val ramdiskFile = File(avd.ramdiskPath)
            if (!ramdiskFile.exists()) {
                log("Error: Ramdisk file does not exist at ${avd.ramdiskPath}\n")
                return
            }

            // 1. Backup
            backup(avd)

            // 2. Identify ADB device and ARCH
            val deviceSerial = getActiveDevice() ?: return
            val arch = getDeviceProperty(deviceSerial, "ro.product.cpu.abi") ?: "x86_64"
            val api = getDeviceProperty(deviceSerial, "ro.build.version.sdk")?.toIntOrNull() ?: 30
            
            log("Device: $deviceSerial (Arch: $arch, API: $api)\n")

            // 3. Prepare workspace
            log("Preparing remote workspace...\n")
            adb.runCommand("-s", deviceSerial, "shell", "rm -rf $remoteTmp && mkdir -p $remoteTmp")

            // 4. Extract and Push Binaries from Magisk.zip
            val magiskZipFile = File(rootAVDDir, "Magisk.zip")
            if (!magiskZipFile.exists()) {
                log("Error: Magisk.zip missing.\n")
                return
            }

            log("Extracting and pushing Magisk binaries...\n")
            pushMagiskBinaries(deviceSerial, arch)

            // 5. Push Ramdisk
            log("Pushing ramdisk.img...\n")
            adb.runCommand("-s", deviceSerial, "push", ramdiskFile.absolutePath, "$remoteTmp/ramdisk.img")

            // 6. Execute Rooting Logic
            log("Executing Magisk patch logic...\n")
            val patchScript = generatePatchScript(arch, api)
            val patchScriptFile = File(rootAVDDir, "patch_internal.sh")
            patchScriptFile.writeText(patchScript)
            adb.runCommand("-s", deviceSerial, "push", patchScriptFile.absolutePath, "$remoteTmp/patch.sh")
            
            val patchOutput = adb.runCommand("-s", deviceSerial, "shell", "sh $remoteTmp/patch.sh")
            log(patchOutput)

            // 7. Pull back and Finalize
            log("\nPulling patched ramdisk...\n")
            val patchedLocal = File(ramdiskFile.parent, "ramdisk.img.patched")
            adb.runCommand("-s", deviceSerial, "pull", "$remoteTmp/new-ramdisk.img", patchedLocal.absolutePath)

            if (patchedLocal.exists()) {
                log("\n✅ PATCH SUCCESSFUL!\n")
                log("Replacing original ramdisk.img...\n")
                patchedLocal.copyTo(ramdiskFile, overwrite = true)
                log("Done. Please perform a 'Cold Boot' to apply changes.\n")
                
                installMagiskApp()
            } else {
                log("❌ Error: Patched ramdisk not found.\n")
            }

        } catch (e: Exception) {
            log("Fatal Error: ${e.message}\n")
        }
    }

    private fun getActiveDevice(): String? {
        val devices = adb.runCommand("devices").lines()
            .filter { it.contains("emulator-") && it.contains("device") }
        if (devices.isEmpty()) {
            log("Error: No running emulator found.\n")
            return null
        }
        return devices.first().split("\t")[0]
    }

    private fun getDeviceProperty(serial: String, prop: String): String? {
        return adb.runCommand("-s", serial, "shell", "getprop", prop).trim()
    }

    private fun pushMagiskBinaries(serial: String, arch: String) {
        val magiskZip = ZipFile(File(rootAVDDir, "Magisk.zip"))
        val libPath = when {
            arch.contains("x86_64") -> "lib/x86_64"
            arch.contains("x86") -> "lib/x86"
            arch.contains("arm64") -> "lib/arm64-v8a"
            else -> "lib/armeabi-v7a"
        }

        val binaries = listOf("libmagiskboot.so" to "magiskboot", "libmagiskinit.so" to "magiskinit")
        binaries.forEach { (zipName, remoteName) ->
            val entry = magiskZip.getEntry("$libPath/$zipName")
            if (entry != null) {
                val tempFile = File(rootAVDDir, remoteName)
                magiskZip.getInputStream(entry).use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                adb.runCommand("-s", serial, "push", tempFile.absolutePath, "$remoteTmp/$remoteName")
                adb.runCommand("-s", serial, "shell", "chmod +x $remoteTmp/$remoteName")
            }
        }
    }

    private fun generatePatchScript(arch: String, api: Int): String {
        return """
            cd $remoteTmp
            export PATH=.:${'$'}PATH
            chmod +x magiskboot
            
            echo "KEEPVERITY=false" > config
            echo "KEEPFORCEENCRYPT=false" >> config
            echo "RECOVERYMODE=false" >> config
            [ $api -ge 34 ] && echo "PATCHVBMETAFLAG=false" >> config

            echo "[*] Step 1: Determining ramdisk format..."
            ./magiskboot cpio ramdisk.img test 2>/dev/null
            if [ ${'$'}? -eq 0 ]; then
                cp ramdisk.img ramdisk.cpio
            else
                ./magiskboot decompress ramdisk.img ramdisk.cpio 2>&1
                ./magiskboot cpio ramdisk.cpio test 2>/dev/null
                if [ ${'$'}? -ne 0 ]; then
                    ./magiskboot unpack ramdisk.img 2>&1
                fi
            fi
            
            if [ ! -f ramdisk.cpio ]; then exit 1; fi

            echo "[*] Step 2: Patching..."
            ./magiskboot cpio ramdisk.cpio \
                "add 0750 init magiskinit" \
                "patch" \
                "mkdir 000 .backup" \
                "add 000 .backup/.magisk config" 2>&1
            
            if [ ${'$'}? -ne 0 ]; then exit 1; fi

            echo "[*] Step 3: Compressing..."
            ./magiskboot compress lz4_legacy ramdisk.cpio new-ramdisk.img 2>&1
            if [ ! -f new-ramdisk.img ]; then
                ./magiskboot compress gzip ramdisk.cpio new-ramdisk.img 2>&1
            fi
        """.trimIndent()
    }
}
