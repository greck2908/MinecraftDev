/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.gradle

import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.execution.RunManager
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.application.ApplicationConfigurationType
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

class ForgeRunConfigDataService : AbstractProjectDataService<ProjectData, Project>() {

    override fun getTargetDataKey() = ProjectKeys.PROJECT

    override fun postProcess(toImport: Collection<DataNode<ProjectData>>,
                             projectData: ProjectData?,
                             project: Project,
                             modelsProvider: IdeModifiableModelsProvider) {

        if (projectData == null || projectData.owner != GradleConstants.SYSTEM_ID) {
            return
        }

        val hello = VfsUtil.findRelativeFile(project.baseDir, ".gradle", GradleBuildSystem.HELLO) ?: return
        if (!hello.exists()) {
            return
        }

        val (moduleName, sizeText) = try {
            runReadAction {
                hello.inputStream.bufferedReader().use { it.readText() }.split("\n")
            }
        } finally {
            // Request this file be deleted
            // We only want to do this action one time
            runWriteTaskLater {
                hello.delete(this)
            }
        }

        val size = sizeText.toIntOrNull() ?: return
        val rootModule = ModuleManager.getInstance(project).findModuleByName(moduleName) ?: return

        val moduleMap = modelsProvider.modules.associateBy { it.name }

        invokeLater {
            val mainModule = if (size == 1) {
                moduleMap[rootModule.name + "_main"]
            } else {
                moduleMap[rootModule.name + "-forge_main"]
            }
            val forgeModule = moduleMap[rootModule.name + "-forge"]

            // Client run config
            val runClientConfiguration = ApplicationConfiguration(
                (forgeModule ?: rootModule).name + " run client",
                project,
                ApplicationConfigurationType.getInstance()
            )

            val runningDir = File(project.basePath, "run")
            if (!runningDir.exists()) {
                runningDir.mkdir()
            }

            runClientConfiguration.workingDirectory = project.basePath + File.separator + "run"
            runClientConfiguration.setMainClassName("GradleStart")

            if (size == 1) {
                runClientConfiguration.setModule(mainModule ?: rootModule)
            } else {
                runClientConfiguration.setModule(mainModule ?: forgeModule ?: rootModule)
            }

            val clientSettings = RunnerAndConfigurationSettingsImpl(
                RunManagerImpl.getInstanceImpl(project),
                runClientConfiguration,
                false
            )

            clientSettings.isActivateToolWindowBeforeRun = true
            clientSettings.isSingleton = true

            val runManager = RunManager.getInstance(project)
            runManager.addConfiguration(clientSettings, false)
            runManager.selectedConfiguration = clientSettings

            // Server run config
            val runServerConfiguration = ApplicationConfiguration(
                (forgeModule ?: rootModule).name + " run server",
                project,
                ApplicationConfigurationType.getInstance()
            )

            runServerConfiguration.setMainClassName("GradleStartServer")
            runServerConfiguration.programParameters = "nogui"
            runServerConfiguration.workingDirectory = project.basePath + File.separator + "run"

            if (size == 1) {
                runServerConfiguration.setModule(mainModule ?: rootModule)
            } else {
                runServerConfiguration.setModule(mainModule ?: forgeModule ?: rootModule)
            }

            val serverSettings = RunnerAndConfigurationSettingsImpl(
                RunManagerImpl.getInstanceImpl(project),
                runServerConfiguration,
                false
            )

            serverSettings.isActivateToolWindowBeforeRun = true
            serverSettings.isSingleton = true
            runManager.addConfiguration(serverSettings, false)
        }
    }
}
