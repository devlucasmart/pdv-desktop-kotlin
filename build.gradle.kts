import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.compose") version "1.5.12"
}

group = "com.pdv"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material)
    implementation(compose.materialIconsExtended)

    // Database
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // PDF Generation
    implementation("org.apache.pdfbox:pdfbox:2.0.30")
    implementation("org.apache.pdfbox:fontbox:2.0.30")

    // Testing
    testImplementation(kotlin("test"))
}


compose.desktop {
    application {
        mainClass = "com.pdv.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)

            packageName = "pdv-desktop"
            packageVersion = "1.0.0"
            description = "Sistema de Ponto de Venda Desktop - Completo com Login e Permissões"
            copyright = "© 2026 PDV Systems. Todos os direitos reservados."
            vendor = "PDV Systems"
            licenseFile.set(project.file("LICENSE.txt"))

            // Configurações Linux
            linux {
                packageName = "pdv-desktop"
                debMaintainer = "contato@pdvsystems.com"
                menuGroup = "Office"
                appCategory = "Office"
                shortcut = true
                packageVersion = "1.0.0"
            }

            // Configurações Windows
            windows {
                console = false
                dirChooser = true
                perUserInstall = false
                menu = true
                shortcut = true
                menuGroup = "PDV Desktop"
                upgradeUuid = "pdv-desktop-upgrade-uuid-12345"
            }

            // Configurações macOS
            macOS {
                bundleID = "com.pdvsystems.pdv.desktop"
                dockName = "PDV Desktop"
                packageVersion = "1.0.0"
                packageBuildVersion = "1"
                dmgPackageVersion = "1.0.0"
                dmgPackageBuildVersion = "1"
            }

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
        }

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}


kotlin {
    jvmToolchain(17)
}

