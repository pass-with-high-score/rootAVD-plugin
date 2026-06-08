import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

group = project.findProperty("group").toString()
version = project.findProperty("version").toString()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        androidStudio(project.findProperty("platformVersion").toString())
        bundledPlugins("com.intellij.java", "org.jetbrains.android")
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        name = project.findProperty("pluginName").toString()
        id = project.findProperty("pluginId").toString()
        vendor {
            name = project.findProperty("vendorName").toString()
        }
        ideaVersion {
            sinceBuild = project.findProperty("sinceBuild").toString()
            untilBuild = project.findProperty("untilBuild")?.toString()
        }
        changeNotes = provider {
            changelog.renderItem(
                (changelog.getOrNull(project.version.toString()) ?: changelog.getUnreleased())
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML,
            )
        }
    }

    pluginVerification {
        ides {
            ide(IntelliJPlatformType.AndroidStudio, project.findProperty("platformVersion").toString())
        }
    }
}

changelog {
    groups.empty()
    repositoryUrl = project.findProperty("pluginRepositoryUrl").toString()
}

kotlin {
    jvmToolchain(17)
}
