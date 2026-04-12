import java.io.File
import java.util.Properties

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Si Cursor utilise un JRE sans jlink : ajoute org.gradle.java.home dans local.properties (JDK complet, ex. jbr d'Android Studio).
val jdkFromLocalProperties: String? = run {
    val f = File(settings.rootDir, "local.properties")
    if (!f.exists()) return@run null
    val p = Properties()
    f.inputStream().use { p.load(it) }
    val home = p.getProperty("org.gradle.java.home")?.trim().orEmpty()
    if (home.isEmpty()) return@run null
    val jlinkWin = File(home, "bin/jlink.exe")
    val jlinkUnix = File(home, "bin/jlink")
    if (jlinkWin.isFile || jlinkUnix.isFile) home else null
}
if (jdkFromLocalProperties != null) {
    System.setProperty("org.gradle.java.home", jdkFromLocalProperties)
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ogoula"
include(":app")
 