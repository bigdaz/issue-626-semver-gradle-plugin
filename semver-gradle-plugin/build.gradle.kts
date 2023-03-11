plugins { alias(libs.plugins.javiersc.hubdle) }

hubdle {
    config {
        documentation { api() }
        explicitApi()
        publishing()
    }
    kotlin {
        jvm {
            features {
                gradle {
                    plugin {
                        gradlePlugin {
                            plugins {
                                create("SemverPlugin") {
                                    id = "com.javiersc.semver"
                                    displayName = "Semver"
                                    description =
                                        "Manage project versions automatically with git tags"
                                    implementationClass =
                                        "com.javiersc.semver.gradle.plugin.SemverPlugin"
                                    tags.set(
                                        listOf(
                                            "semver",
                                            "semantic versioning",
                                            "semantic version",
                                            "git tags",
                                            "git version",
                                        )
                                    )
                                }
                            }
                        }

                        pluginUnderTestDependencies(
                            androidToolsBuildGradle(),
                            jetbrainsKotlinGradlePlugin(),
                        )
                    }
                }
            }

            main {
                dependencies {
                    api(projects.semverProjectGradlePlugin)
                    api(projects.semverSettingsGradlePlugin)
                }
            }
        }
    }
}
