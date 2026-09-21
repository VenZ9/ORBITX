import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import com.android.build.gradle.tasks.MergeSourceSetFolders
import com.github.megatronking.stringfog.plugin.StringFogExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("stringfog")
}
apply(plugin = "stringfog")

val getCFApiKey = {
    System.getenv("CURSEFORGE_API_KEY") ?: run {
        val curseforgeKeyFile = File(rootDir, "curseforge_key.txt")
        if (curseforgeKeyFile.canRead() && curseforgeKeyFile.isFile) {
            curseforgeKeyFile.readText()
        } else {
            logger.warn("BUILD: You have no CurseForge key, the curseforge api will get disabled !")
            "DUMMY"
        }
    }
}

val getBuildType = {
    val buildType = System.getenv("ZL_BUILD_TYPE") ?: "DEBUG"
    logger.warn("BUILD: Build Type --> $buildType")
    buildType
}

val nameId = "com.movtery.zalithlauncher"
val generatedZalithDir = file("$buildDir/generated/source/zalith/java")
val launcherAPPName = project.findProperty("launcher_app_name") as? String ?: error("The \"launcher_app_name\" property is not set in gradle.properties.")
val launcherName = project.findProperty("launcher_name") as? String ?: error("The \"launcher_name\" property is not set in gradle.properties.")
val launcherVersionCode = (project.findProperty("launcher_version_code") as? String)?.toIntOrNull() ?: error("The \"launcher_version_code\" property is not set as an integer in gradle.properties.")
val launcherVersionName = project.findProperty("launcher_version_name") as? String ?: error("The \"launcher_version_name\" property is not set in gradle.properties.")

configurations {
    create("instrumentedClasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    fogPackages = arrayOf(nameId)
    kg = com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator()
    mode = com.github.megatronking.stringfog.plugin.StringFogMode.bytes
}

android {
    namespace = nameId
    compileSdk = 34

    signingConfigs {
        create("releaseBuild") {
            val pwd = System.getenv("MOVTERY_KEYSTORE_PASSWORD")
            storeFile = file("movtery-key.jks")
            storePassword = pwd
            keyAlias = "mtp"
            keyPassword = pwd
        }
        create("customDebug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        // OrbitX rebrand: only the shipped applicationId changes. The Java package
        // namespaces (com.movtery.zalithlauncher / net.kdt.pojavlaunch) are left
        // intact on purpose - 549 source files reference them and the native code
        // resolves classes by hardcoded path strings, so renaming would break the
        // JNI bindings. See android/LICENSE-THIRD-PARTY.md.
        applicationId = "com.orbitx.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = launcherVersionCode
        versionName = launcherVersionName
        multiDexEnabled = true //important
        manifestPlaceholders["launcher_name"] = launcherAPPName
    }

    buildTypes {
        val storageProviderId = "com.orbitx.launcher.storage_provider"

        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("customDebug")
            resValue("string", "storageProviderAuthorities", "$storageProviderId.debug")
        }
        create("proguard") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            // OrbitX lightweighting: the bundled JRE tarballs are assets copied verbatim
            // into the APK, so nothing here removes them - the per-ABI assets trim above
            // does that.  These flags strip unused code and resources for low-end devices.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("proguardNoDebug") {
            initWith(getByName("proguard"))
            isDebuggable = false
        }
        getByName("release") {
            // Don't set to true or java.awt will be a.a or something similar.
            isMinifyEnabled = false
            // OrbitX note: `isShrinkResources` is deliberately NOT set here.  AGP
            // rejects resource shrinking without code shrinking ("Removing unused
            // resources requires unused code shrinking to be turned on"), and
            // minification must stay off for this build type because java.awt is
            // reached reflectively.  Resource shrinking therefore stays on the
            // `proguard` variant only, which already enables both flags.
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            resValue("string", "storageProviderAuthorities", storageProviderId)
            signingConfig = signingConfigs.getByName("releaseBuild")
        }
    }

    sourceSets["main"].java.srcDirs(generatedZalithDir)

    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                    val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }
                    afterEvaluate {
                        val task = tasks.named("merge${variantName}Assets").get() as MergeSourceSetFolders
                        task.doLast {
                            val arch = System.getProperty("arch", "all")
                            val assetsDir = task.outputDir.get().asFile

                            // OrbitX lightweighting (recorded in LICENSE-THIRD-PARTY.md).
                            //
                            // The bundled Android JREs are the single largest thing in this APK
                            // (~105 MB of the ~150 MB baseline).  Each JRE ships one architecture
                            // independent `universal.tar.xz` plus one `bin-<arch>.tar.xz` per ABI,
                            // and the unpacker only ever reads the pair that matches the device:
                            //   UnpackJreTask -> MultiRTUtils.installRuntimeNamedBinpack(
                            //       .../universal.tar.xz,
                            //       .../bin-<archAsString(DEVICE_ARCHITECTURE)>.tar.xz )
                            // So the tarballs for every other ABI are dead weight in an arm64 APK
                            // (and, because the unpacker copies the whole folder to the data dir,
                            // dead weight on the device too).
                            //
                            // Two fixes over the inherited logic:
                            //   1. The JRE dirs are discovered, not hardcoded.  The inherited list
                            //      was listOf("jre-8", "jre-17", "jre-21") and omitted jre-25, so
                            //      an arm64 build still shipped jre-25's dead bin-arm.tar.xz and
                            //      bin-x86_64.tar.xz (~10 MB).
                            //   2. Only the target ABI's tarball is kept.  `universal.tar.xz`
                            //      (architecture independent) and the `version` marker (read by
                            //      UnpackJreTask.isNeedUnpack) are always kept.
                            //
                            // With arch == "all" nothing is removed.  Otherwise an unsupported
                            // ABI simply has no runtime tarball: UnpackJreTask.isNeedUnpack()
                            // fails its runCatching and the runtime is reported unavailable
                            // rather than crashing, matching the fact that the native .so files
                            // for that ABI are not in the APK either.
                            val componentsDir = File("$assetsDir/components")
                            val jreDirs = componentsDir.listFiles()
                                ?.filter { it.isDirectory && it.name.startsWith("jre-") }
                                ?.sortedBy { it.name }
                                ?: emptyList()
                            println("OrbitX/JRE-trim arch:$arch -> ${jreDirs.map { it.name }}")
                            jreDirs.forEach { runtimeDir ->
                                val keep = "bin-$arch.tar.xz"
                                runtimeDir.listFiles()?.forEach { entry ->
                                    val isArchTarball = entry.name.startsWith("bin-") && entry.name.endsWith(".tar.xz")
                                    // `version` and `universal.tar.xz` are never architecture specific.
                                    if (arch != "all" && isArchTarball && entry.name != keep) {
                                        println("OrbitX/JRE-trim delete:${entry.name} (${entry.length()} bytes) -> ${entry.delete()}")
                                    }
                                }
                            }
                        }
                    }

                    (output.getFilter(ABI)?.identifier ?: "all").let { abi ->
                        val baseName = "$launcherName-${if (variant.buildType == "release") defaultConfig.versionName else "Debug-${defaultConfig.versionName}"}"
                        output.outputFileName = if (abi == "all") "$baseName.apk" else "$baseName-$abi.apk"
                    }
                }
            }
        }
    }

    splits {
        val arch = System.getProperty("arch", "all")
        if (arch != "all") {
            abi {
                isEnable = true
                reset()
                when (arch) {
                    "arm" -> include("armeabi-v7a")
                    "arm64" -> include("arm64-v8a")
                    "x86" -> include("x86")
                    "x86_64" -> include("x86_64")
                }
            }
        }
    }

    ndkVersion = "25.2.9519653"

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf("**/libbytehook.so")
        }
    }

    buildFeatures {
        prefab = true
        buildConfig = true
        viewBinding = true
    }

    buildToolsVersion = "34.0.0"
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

fun generateJavaClass(sourceOutputDir: File, packageName: String, className: String, constantMap: Map<String, String>) {
    val outputDir = File(sourceOutputDir, packageName.replace(".", "/"))
    outputDir.mkdirs()
    val javaFile = File(outputDir, "$className.java")
    val constants = constantMap.entries.joinToString("\n") { (key, value) ->
        "\tpublic static final String $key = \"$value\";"
    }
    javaFile.writeText(
        """
        |/**
        | * Automatically generated file. DO NOT MODIFY
        | */
        |package $packageName;
        |
        |public class $className {
        |$constants
        |}
        """.trimMargin()
    )
    println("Generated Java file: ${javaFile.absolutePath}")
}

tasks.register("generateInfoDistributor") {
    doLast {
        val constantMap = mapOf(
            "CURSEFORGE_API_KEY" to getCFApiKey(),
            "LAUNCHER_NAME" to project.property("launcher_name").toString(),
            "APP_NAME" to project.property("launcher_app_name").toString(),
            "BUILD_TYPE" to getBuildType()
        )
        generateJavaClass(generatedZalithDir, "com.movtery.zalithlauncher", "InfoDistributor", constantMap)
    }
}

tasks.named("preBuild") {
    dependsOn("generateInfoDistributor")
}

dependencies {
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("commons-codec:commons-codec:1.17.1")
    // implementation("com.wu-man:android-bsf-api:3.1.3")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta01")
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    implementation("com.github.duanhong169:checkerboarddrawable:1.0.2")
    implementation("com.github.PojavLauncherTeam:portrait-sdp:ed33e89cbc")
    implementation("com.github.PojavLauncherTeam:portrait-ssp:6c02fd739b")
    implementation("com.github.Mathias-Boulay:ExtendedView:1.0.0")
    implementation("com.github.Mathias-Boulay:android_gamepad_remapper:2.0.3")
    implementation("com.github.Mathias-Boulay:virtual-joystick-android:1.14")
    implementation("com.github.skydoves:powerspinner:1.2.7")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.angcyo.DslTablayout:TabLayout:3.6.5")

    implementation("com.github.megatronking.stringfog:xor:5.0.0")

    implementation("top.fifthlight.touchcontroller:proxy-client-android:0.0.2")

    // implementation("com.intuit.sdp:sdp-android:1.0.5")
    // implementation("com.intuit.ssp:ssp-android:1.0.5")

    implementation("org.tukaani:xz:1.9")
    // Our version of exp4j can be built from source at
    // https://github.com/PojavLauncherTeam/exp4j
    implementation("net.sourceforge.htmlcleaner:htmlcleaner:2.6.1")
    implementation("com.bytedance:bytehook:1.0.10")

    // implementation("net.sourceforge.streamsupport:streamsupport-cfuture:1.7.0")

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.commonmark:commonmark:0.19.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    implementation("com.getkeepsafe.taptargetview:taptargetview:1.14.0")
    implementation("io.github.petterpx:floatingx:2.3.3")
    implementation("org.greenrobot:eventbus:3.3.1")
    implementation("com.moandjiezana.toml:toml4j:0.7.2") {
        exclude(group = "com.google.code.gson", module = "gson")
    }
}
