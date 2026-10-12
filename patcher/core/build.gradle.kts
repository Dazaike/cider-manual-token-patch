import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

val sdkDir: String = rootProject.file("local.properties").takeIf { it.isFile }
    ?.let { f -> Properties().also { p -> f.inputStream().use(p::load) }.getProperty("sdk.dir") }
    ?: System.getenv("ANDROID_HOME")
    ?: error("Set sdk.dir in patcher/local.properties or ANDROID_HOME")
val androidJar = file("$sdkDir/platforms/android-34/android.jar")
val d8Jar = file("$sdkDir/build-tools/34.0.0/lib/d8.jar")

// Cloud client that runs inside the patched Cider: plain Java compiled and dexed here, shipped as a resource.
sourceSets {
    create("cloudsid") {
        java.srcDir("src/cloudsid/java")
    }
}

tasks.named<JavaCompile>("compileCloudsidJava") {
    options.release.set(8)
}

val cloudSidDexDir = layout.buildDirectory.dir("generated/cloudsid-dex")

val dexCloudSid by tasks.registering(JavaExec::class) {
    dependsOn("compileCloudsidJava")
    val classDirs = sourceSets["cloudsid"].output.classesDirs
    inputs.files(classDirs)
    outputs.dir(cloudSidDexDir)
    classpath = files(d8Jar)
    mainClass.set("com.android.tools.r8.D8")
    doFirst {
        val out = cloudSidDexDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        val classFiles = classDirs.files.flatMap { dir -> dir.walkTopDown().filter { it.extension == "class" }.toList() }
        args = listOf("--release", "--min-api", "33", "--lib", androidJar.path, "--output", out.path) +
            classFiles.map { it.path }
    }
}

tasks.processResources {
    from(dexCloudSid.map { it.outputs.files }) { into("cloudsid") }
}

dependencies {
    "cloudsidCompileOnly"(files(androidJar))
    implementation("com.android.tools.smali:smali:3.0.10")
    implementation("io.github.reandroid:ARSCLib:1.3.8")
    implementation("com.android.tools.build:apksig:8.5.2")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    testImplementation("junit:junit:4.13.2")
    // On-device org.json is the platform's; JVM tests need the real implementation. android.jar stays off the test classpath (its stubs throw).
    testImplementation(sourceSets["cloudsid"].output)
    testImplementation("org.json:json:20240303")
}
