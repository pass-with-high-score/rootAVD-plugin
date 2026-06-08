package com.github.rootavd.plugin

import com.intellij.openapi.project.Project
import java.io.File

class RootAVDManager(private val project: Project) {

    private val rootAVDDir = File(System.getProperty("user.home"), ".rootAVD_plugin")

    fun ensureScriptsExtracted(logCallback: (String) -> Unit): Boolean {
        if (!rootAVDDir.exists()) {
            rootAVDDir.mkdirs()
        }

        return try {
            val resourcePath = "/magisk-files/"
            val filesToExtract = listOf("Magisk.zip", "magisk.apk") 
            
            for (fileName in filesToExtract) {
                val inputStream = javaClass.getResourceAsStream("$resourcePath$fileName")
                if (inputStream != null) {
                    val outFile = File(rootAVDDir, fileName)
                    outFile.writeBytes(inputStream.readAllBytes())
                }
            }
            true
        } catch (e: Exception) {
            logCallback("Error extracting resources: ${e.message}\n")
            false
        }
    }
}
