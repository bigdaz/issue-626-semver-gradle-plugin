package com.javiersc.semver.project.gradle.plugin

import com.javiersc.semver.project.gradle.plugin.internal.checkScopeCorrectness
import com.javiersc.semver.project.gradle.plugin.internal.git.hasGit
import com.javiersc.semver.project.gradle.plugin.services.GitBuildService
import com.javiersc.semver.project.gradle.plugin.tasks.CreateSemverTagTask
import com.javiersc.semver.project.gradle.plugin.tasks.PrintSemverTask
import com.javiersc.semver.project.gradle.plugin.tasks.PushSemverTagTask
import com.javiersc.semver.project.gradle.plugin.tasks.WriteSemverTask
import com.javiersc.semver.project.gradle.plugin.valuesources.VersionValueSource
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.apply

public class SemverProjectPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.config()
    }

    private fun Project.config() {
        pluginManager.apply(BasePlugin::class)

        SemverExtension.register(this)

        if (semverExtension.isEnabled.get() && hasGit) {
            val gitTagBuildService = GitBuildService.register(this)
            checkScopeCorrectness()
            configureLazyVersion()
            configureBuildServicesAndTasks(gitTagBuildService)
        }
    }

    private fun Project.configureBuildServicesAndTasks(
        gitTagBuildService: Provider<GitBuildService>
    ) {
        CreateSemverTagTask.register(this, gitTagBuildService)
        PushSemverTagTask.register(this, gitTagBuildService)
        PrintSemverTask.register(this)
        WriteSemverTask.register(this)
    }

    private fun Project.configureLazyVersion() {
        version = LazyVersion(VersionValueSource.register(this))

        // It is possible third party plugin breaks lazy configuration by calling `project.version`
        // too early, applying the calculated version in `afterEvaluate` fix it sometimes.
        afterEvaluate { version = LazyVersion(VersionValueSource.register(this)) }
    }
}
