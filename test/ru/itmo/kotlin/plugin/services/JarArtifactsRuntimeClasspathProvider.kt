package ru.itmo.kotlin.plugin.services

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import ru.itmo.kotlin.plugin.services.PluginAnnotationsProvider.Companion.ANNOTATIONS_JAR_DIR
import java.io.File

class JarArtifactsRuntimeClasspathProvider(testServices: TestServices): RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        val libDir = File(ANNOTATIONS_JAR_DIR)
        testServices.assertions.assertTrue(libDir.exists() && libDir.isDirectory, failMessage)
        val jar = PluginAnnotationsProvider.getJarWithAnnotationsOrNull() ?: testServices.assertions.fail(failMessage)
        return listOf(jar)
    }

    private val failMessage = { "Jar with dependencies does not exist. Please run :plugin-annotations:jar" }

}