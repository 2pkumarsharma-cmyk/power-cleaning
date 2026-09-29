import java.net.URL
import java.util.zip.ZipInputStream
import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val upstreamDir = layout.buildDirectory.dir("upstream").get().asFile
val upstreamZip = layout.buildDirectory.file("android-epson-reset.zip").get().asFile

fun downloadAndExtractUpstream() {
    if (upstreamDir.resolve("app/src/main/java/info/tekware/aereset/service/PrinterService.kt").exists()) return
    upstreamDir.mkdirs()
    if (!upstreamZip.exists()) {
        URL("https://github.com/tekware-it/android-epson-reset/archive/refs/heads/main.zip").openStream().use { input ->
            upstreamZip.outputStream().use { output -> input.copyTo(output) }
        }
    }
    ZipInputStream(upstreamZip.inputStream().buffered()).use { zis ->
        while (true) {
            val e = zis.nextEntry ?: break
            val name = e.name
            val prefix = name.substringBefore('/', "")
            if (name.startsWith("$prefix/") && name.length > prefix.length + 1) {
                val rel = name.substring(prefix.length + 1)
                val out = upstreamDir.resolve(rel)
                if (e.isDirectory) out.mkdirs()
                else {
                    out.parentFile.mkdirs()
                    out.outputStream().use { zis.copyTo(it) }
                }
            }
        }
    }
    val manifest = upstreamDir.resolve("app/src/main/AndroidManifest.xml")
    var xml = manifest.readText()
    xml = xml.replace("android:name=\".MainActivity\"", "android:name=\"com.l3210.strongclean.StrongCleanActivity\"")
    manifest.writeText(xml)
}

val prepareUpstream = tasks.register("prepareUpstream") {
    doLast { downloadAndExtractUpstream() }
}

android {
    namespace = "info.tekware.aereset"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.l3210.strongclean"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    sourceSets {
        getByName("main") {
            java.srcDir(upstreamDir.resolve("app/src/main/java"))
            assets.srcDir(upstreamDir.resolve("app/src/main/assets"))
            res.srcDir(upstreamDir.resolve("app/src/main/res"))
            manifest.srcFile(upstreamDir.resolve("app/src/main/AndroidManifest.xml"))
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

tasks.named("preBuild").configure { dependsOn(prepareUpstream) }
