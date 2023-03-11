package com.javiersc.semver.settings.gradle.plugin

import com.javiersc.gradle.testkit.test.extensions.GradleTestKitTest
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test
import org.eclipse.jgit.api.Git

internal class MultiProjectTest : GradleTestKitTest() {

    @Test
    fun `multi project test`() {
        gradleTestKitTest("multi-project") {
            val git = Git.init().setDirectory(projectDir).call()
            git.add().addFilepattern(".").call()
            git.commit().setMessage("Initial commit").call()
            git.tag().setName("v1.0.0").call()
            git.tag().setName("o2.0.3").call()
            git.tag().setName("t4.7.5").call()
            gradlew("printSemver", stacktrace())
                .output
                .shouldContain("semver for multi-project: v1.0.0")
                .shouldContain("semver for library-one: o2.0.3")
                .shouldContain("semver for library-two: t4.7.5")
        }
    }
}
