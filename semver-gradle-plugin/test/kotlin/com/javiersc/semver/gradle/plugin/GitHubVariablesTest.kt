@file:Suppress("LocalVariableName")

package com.javiersc.semver.gradle.plugin

import com.javiersc.gradle.testkit.test.extensions.GradleTest
import com.javiersc.semver.gradle.plugin.setup.generateInitialCommitAddVersionTagAndAddNewCommit
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class GitHubVariablesTest : GradleTest() {

    @Test
    fun `GitHub variables`() {
        gradleTestKitTest("github-variables") {
            projectDir.generateInitialCommitAddVersionTagAndAddNewCommit()

            gradlew(
                    "printSemver",
                    "-Psemver.tagPrefix=v",
                    "-Psemver.stage=final",
                    "--githubOnlyRoot",
                    "--githubOutputTag",
                    "--githubOutputVersion",
                    "--githubOutput",
                    stacktrace(),
                )
                .outputTrimmed
                .shouldContain("Setting v as `semver-tag` output:")
                .shouldContain("::set-output name=semver-tag::v")
                .shouldContain("Setting 0.9.1 as `semver-version` output:")
                .shouldContain("::set-output name=semver-version::0.9.1")
                .shouldContain("Setting v0.9.1 as `semver` output:")
                .shouldContain("::set-output name=semver::v0.9.1")
            gradlew(
                    "printSemver",
                    "-Psemver.tagPrefix=w",
                    "-Psemver.stage=final",
                    "--githubOutputTag",
                    "--githubOutputVersion",
                    "--githubOutput",
                    stacktrace(),
                )
                .outputTrimmed
                .shouldContain("Setting v as `semver-tag` output:")
                .shouldContain("::set-output name=semver-tag::v")
                .shouldContain("as `semver-version` output:")
                .shouldContain("::set-output name=semver-version::")
                .shouldContain("as `semver` output:")
                .shouldContain("::set-output name=semver::")
                .shouldContain("Setting w as `semver-tag-library` output:")
                .shouldContain("::set-output name=semver-tag-library::w")
                .shouldContain("Setting 0.1.1 as `semver-version-library` output:")
                .shouldContain("::set-output name=semver-version-library::0.1.1")
                .shouldContain("Setting w0.1.1 as `semver-library` output:")
                .shouldContain("::set-output name=semver-library::w0.1.1")
        }
    }
}
