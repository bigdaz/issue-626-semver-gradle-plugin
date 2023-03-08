package com.javiersc.semver.gradle.plugin.valuesources

import com.javiersc.semver.Version
import com.javiersc.semver.gradle.plugin.internal.calculatedVersion
import com.javiersc.semver.gradle.plugin.internal.checkCleanProperty
import com.javiersc.semver.gradle.plugin.internal.checkVersionIsHigherOrSame
import com.javiersc.semver.gradle.plugin.internal.commitsMaxCount
import com.javiersc.semver.gradle.plugin.internal.git.GitCache
import com.javiersc.semver.gradle.plugin.internal.git.GitRef
import com.javiersc.semver.gradle.plugin.internal.projectTagPrefix
import com.javiersc.semver.gradle.plugin.internal.scopeProperty
import com.javiersc.semver.gradle.plugin.internal.stageProperty
import com.javiersc.semver.gradle.plugin.internal.tagPrefixProperty
import com.javiersc.semver.gradle.plugin.semverExtension
import com.javiersc.semver.gradle.plugin.tasks.CreateSemverTagTask
import com.javiersc.semver.gradle.plugin.tasks.PushSemverTagTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.of

internal abstract class VersionValueSource : ValueSource<String, VersionValueSource.Params> {

    override fun obtain(): String =
        with(parameters) {
            val isSamePrefix: Boolean = tagPrefixProperty.get() == projectTagPrefix.get()

            val cache = GitCache(parameters.rootDir.get().asFile, parameters.commitsMaxCount)

            val lastSemver: Version = cache.lastVersionInCurrentBranch(projectTagPrefix.get())
            val lastVersionInCurrentBranch =
                cache.versionsInCurrentBranch(projectTagPrefix.get()).map(Version::toString)

            val lastVersionCommitInCurrentBranch =
                cache.lastVersionCommitInCurrentBranch(projectTagPrefix.get())?.hash

            val version: String =
                calculatedVersion(
                    stageProperty = stageProperty.orNull.takeIf { isSamePrefix },
                    scopeProperty = scopeProperty.orNull.takeIf { isSamePrefix },
                    isCreatingSemverTag = creatingSemverTag.get().takeIf { isSamePrefix } ?: false,
                    lastSemverMajorInCurrentBranch = lastSemver.major,
                    lastSemverMinorInCurrentBranch = lastSemver.minor,
                    lastSemverPatchInCurrentBranch = lastSemver.patch,
                    lastSemverStageInCurrentBranch = lastSemver.stage?.name,
                    lastSemverNumInCurrentBranch = lastSemver.stage?.num,
                    versionTagsInCurrentBranch = lastVersionInCurrentBranch,
                    clean = cache.isClean,
                    checkClean = checkClean.get(),
                    lastCommitInCurrentBranch = cache.lastCommitInCurrentBranch?.hash,
                    commitsInCurrentBranch = cache.commitsInCurrentBranch.map(GitRef.Commit::hash),
                    headCommit = cache.headCommit.commit.hash,
                    lastVersionCommitInCurrentBranch = lastVersionCommitInCurrentBranch,
                )

            checkVersionIsHigherOrSame(version, lastSemver)

            version
        }

    internal interface Params : ValueSourceParameters {
        val rootDir: RegularFileProperty
        val commitsMaxCount: Property<Int>
        val tagPrefixProperty: Property<String>
        val projectTagPrefix: Property<String>
        val stageProperty: Property<String?>
        val scopeProperty: Property<String?>
        val creatingSemverTag: Property<Boolean>
        val checkClean: Property<Boolean>
    }

    companion object {

        fun register(project: Project): Provider<String> =
            project.providers.of(VersionValueSource::class) { valueSourceSpec ->
                valueSourceSpec.parameters.rootDir.set(project.semverExtension.rootDir)
                val commitsMaxCount: Int =
                    project.commitsMaxCount.orNull ?: project.semverExtension.commitsMaxCount.get()
                valueSourceSpec.parameters.commitsMaxCount.set(commitsMaxCount)
                valueSourceSpec.parameters.projectTagPrefix.set(project.projectTagPrefix.get())
                valueSourceSpec.parameters.tagPrefixProperty.set(project.tagPrefixProperty.get())
                valueSourceSpec.parameters.stageProperty.set(project.stageProperty.orNull)
                valueSourceSpec.parameters.scopeProperty.set(project.scopeProperty.orNull)
                valueSourceSpec.parameters.creatingSemverTag.set(project.isCreatingSemverTag)
                valueSourceSpec.parameters.checkClean.set(project.checkCleanProperty.get())
            }
    }
}

private val Project.isCreatingSemverTag: Boolean
    get() =
        gradle.startParameter.taskNames.any { taskName: String ->
            taskName == CreateSemverTagTask.TaskName || taskName == PushSemverTagTask.TaskName
        }
