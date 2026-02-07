plugins {
    id("net.labymod.labygradle")
    id("net.labymod.labygradle.addon")
}

val versions = providers.gradleProperty("net.labymod.minecraft-versions").get().split(";")

group = "com.bodywarn.autotranslator"
version = "1.0.0"

tasks.named<Jar>("jar") {
    archiveBaseName.set("autotranslator")
    archiveVersion.set("")
}

labyMod {
    defaultPackageName = "com.bodywarn.autotranslator"

    minecraft {
        registerVersion(versions.toTypedArray()) {
            runs {
                getByName("client") {
                    // devLogin = false
                }
            }
        }
    }

    addonInfo {
        namespace = "autotranslator"
        displayName = "AutoTranslator"
        author = "Bodywarn"
        description = "Hover over, to see a translated word."
        minecraftVersion = "*"
        version = rootProject.version.toString()  // Beholder version internt
    }
}

subprojects {
    plugins.apply("net.labymod.labygradle")
    plugins.apply("net.labymod.labygradle.addon")

    group = rootProject.group
    version = rootProject.version
}