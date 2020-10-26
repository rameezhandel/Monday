import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform") version "1.4.10"
    id("com.android.library")
    id("kotlin-android-extensions")

    id("base")
    `maven-publish`

    id("com.jfrog.bintray") version "1.8.4"
}

repositories {
    gradlePluginPortal()
    google()
    jcenter()
    mavenCentral()
}

group = "me.rameez"
version "0.0.1"

val artifactName = project.name
val artifactGroup = project.group.toString()
val artifactVersion = project.version.toString()

val pomUrl = "https://github.com/rameezhandel/Monday"
val pomScmUrl = "https://github.com/rameezhandel/Monday.git"
val pomIssueUrl = "https://github.com/rameezhandel/Monday/issues"
val pomDesc = "https://github.com/rameezhandel/Monday"

val githubRepoUrl = "rameezhandel/Monday"
val githubReadme = "README.md"

val pomLicenseName = "MIT"
val pomLicenseUrl = "https://opensource.org/licenses/mit-license.php"
val pomLicenseDist = "repo"

val pomDeveloperId = "rameezhandel"
val pomDeveloperName = "Rameez Handel"

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets.getByName("main").java.srcDirs)
}


publishing {
    publications {
        create<MavenPublication>("Monday") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion

            artifact("$buildDir/outputs/aar/${artifactId}-release.aar")

            pom.withXml {
                asNode().apply {
                    appendNode("description", pomDesc)
                    appendNode("name", rootProject.name)
                    appendNode("url", pomUrl)
                    appendNode("licenses").appendNode("license").apply {
                        appendNode("name", pomLicenseName)
                        appendNode("url", pomLicenseUrl)
                        appendNode("distribution", pomLicenseDist)
                    }
                    appendNode("developers").appendNode("developer").apply {
                        appendNode("id", pomDeveloperId)
                        appendNode("name", pomDeveloperName)
                    }
                    appendNode("scm").apply {
                        appendNode("url", pomScmUrl)
                    }
                }
            }
        }
    }
}

bintray {
    user = project.findProperty("bintrayUser").toString()
    key = project.findProperty("bintrayKey").toString()
    publish = false

    setPublications("Monday")

    pkg.apply {
        repo = "samplemaven"
        name = artifactName
        userOrg = "rameezhandel"
        githubRepo = githubRepoUrl
        vcsUrl = pomScmUrl
        description = "Sample Kotlin library"
        setLabels("kotlin", "sample", "testing", "data", "generation")
        setLicenses("MIT")
        desc = description
        websiteUrl = pomUrl
        issueTrackerUrl = pomIssueUrl
        githubReleaseNotesFile = githubReadme

        version.apply {
            name = artifactVersion
            desc = pomDesc
            released = "26-10-2020"//Date().toString()
            vcsTag = artifactVersion
        }
    }
}

kotlin {
    android()
    ios("ios") {
        binaries {
            framework {
                baseName = "library"
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.2.0")
            }
        }
        val androidTest by getting
        val iosMain by getting
        val iosTest by getting
    }
}

android {
    compileSdkVersion(29)
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

val packForXcode by tasks.creating(Sync::class) {
    val targetDir = File(buildDir, "xcode-frameworks")

    /// selecting the right configuration for the iOS
    /// framework depending on the environment
    /// variables set by Xcode build
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName: String? = System.getenv("SDK_NAME")
    val isiOSDevice = sdkName.orEmpty().startsWith("iphoneos")
    val framework = kotlin.targets
        .getByName<KotlinNativeTarget>(
            if(isiOSDevice) {
                "iosArm64"
            } else {
                "iosX64"
            }
        )
        .binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)

    from({ framework.outputDirectory })
    into(targetDir)

    /// generate a helpful ./gradlew wrapper with embedded Java path
    doLast {
        val gradlew = File(targetDir, "gradlew")
        gradlew.writeText("#!/bin/bash\n"
                + "export 'JAVA_HOME=${System.getProperty("java.home")}'\n"
                + "cd '${rootProject.rootDir}'\n"
                + "./gradlew \$@\n")
        gradlew.setExecutable(true)
    }

}
