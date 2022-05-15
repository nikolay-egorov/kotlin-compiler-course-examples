package ru.itmo.kotlin.plugin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File
import java.io.FilenameFilter

class PluginAnnotationsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        const val ANNOTATIONS_JAR_DIR = "plugin-annotations/build/libs/"
        val ANNOTATIONS_JAR_FILTER = FilenameFilter { _, name -> name.startsWith("plugin-annotations") && name.endsWith(".jar") }

        fun getJarWithAnnotationsOrNull(): File? {
            return File(ANNOTATIONS_JAR_DIR).listFiles(ANNOTATIONS_JAR_FILTER)?.firstOrNull()
        }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val libDir = File(ANNOTATIONS_JAR_DIR)
        testServices.assertions.assertTrue(libDir.exists() && libDir.isDirectory, failMessage)
        val jar = getJarWithAnnotationsOrNull() ?: testServices.assertions.fail(failMessage)
        configuration.addJvmClasspathRoot(jar)
    }

    private val failMessage = { "Jar with annotations does not exist. Please run :plugin-annotations:jar" }
}
