plugins {
    java
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.org/repository/maven-releases/")
        maven("https://repo.codemc.org/repository/maven-snapshots/")
        maven("https://repo.opencollab.dev/main/")
    }

    group = "com.jtprince.coordinateoffset"

    version = "6.1.8-folia"
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}
