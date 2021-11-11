package com.javiersc.semver.gradle.plugin

import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

val sandboxPath: Path = Paths.get("build/sandbox").apply { toFile().mkdirs() }

val File.sandboxFile: File
    get() = File("$this/build/sandbox")

val File.git: Git
    get() =
        Git(
            FileRepositoryBuilder()
                .setGitDir(File("$this/.git"))
                .readEnvironment()
                .findGitDir()
                .build()
        )

fun getResource(resource: String): File =
    File(Thread.currentThread().contextClassLoader?.getResource(resource)?.toURI()!!)

infix fun String.copyResourceTo(destination: File) {
    getResource(this).copyRecursively(destination)
}

fun createSandboxFile(prefix: String): File =
    Files.createTempDirectory(sandboxPath, "$prefix - ").toFile()

val File.arguments: List<String>
    get() =
        File("$this/ARGUMENTS.txt").readLines().first().split(" ").map { argument ->
            argument.replace("\"", "")
        }

fun testSandbox(
    sandboxPath: String,
    prefix: String = File(sandboxPath).name,
    beforeTest: File.() -> Unit = {},
    test: (result: BuildResult, testProjectDir: File) -> Unit,
) {
    val testProjectDir: File = createSandboxFile(prefix)
    sandboxPath copyResourceTo testProjectDir

    beforeTest(testProjectDir)

    GradleRunner.create()
        .withDebug(true)
        .withProjectDir(testProjectDir)
        .withArguments(testProjectDir.arguments)
        .withPluginClasspath()
        .run {
            test(if (prefix.contains("buildAndFail")) buildAndFail() else build(), testProjectDir)
        }
}

@Suppress("UNUSED_PARAMETER")
fun testSemVer(result: BuildResult, testProjectDir: File) {
    val versions: List<Pair<File, File>> =
        testProjectDir
            .walkTopDown()
            .mapNotNull {
                if (it.name == "expect-version.txt") {
                    it to File("${it.parentFile}/build/semver/version.txt")
                } else null
            }
            .toList()

    if (!testProjectDir.name.contains("buildAndFail")) {
        check(versions.isNotEmpty()) { "Test wrong, check if there is `expect-version.txt ` files" }
    }

    versions.forEach { (expectVersion, version) ->
        expectVersion.readText() shouldBe version.readText()
    }
}

// `this` is `testProjectDir`
internal fun File.generateInitialCommitAddVersionTagAndAddNewCommit(
    doBefore: Git.() -> Unit = {},
    doAfter: Git.() -> Unit = {},
) {
    File("$this/.gitignore").apply {
        createNewFile()
        writeText(
            """
                .idea/
                build/
                .gradle/
                expect-version.txt

            """.trimIndent(),
        )
    }
    val git: Git = Git.init().setDirectory(this).call()
    doBefore(git)
    git.add().addFilepattern(".").call()
    val commit = git.commit().setMessage("Initial commit").call()
    File("$this/new.txt").createNewFile()
    git.tag().setObjectId(commit).setName(File("$this/last-tag.txt").readLines().first()).call()
    git.add().addFilepattern(".").call()
    git.commit().setMessage("Add new").call()
    File("$this/library/last-tag.txt").apply {
        if (exists()) git.tag().setObjectId(commit).setName(readLines().first()).call()
    }
    File("$this/new2.txt").createNewFile()
    git.add().addFilepattern(".").call()
    git.commit().setMessage("Add new2").call()
    doAfter(git)
}
