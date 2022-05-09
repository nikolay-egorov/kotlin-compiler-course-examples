package ru.itmo.kotlin.plugin

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import ru.itmo.kotlin.plugin.fir.generator.LoggerFieldGenerator
import ru.itmo.kotlin.plugin.ir.LoggerIrFunctionTransformer
import ru.itmo.kotlin.plugin.ir.transformers.LoggerIrFieldFillerTransformer

class LoggingPluginFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::LoggerFieldGenerator
        // todo: add
        // +::AdditionalSupertypeGenerator
    }
}

class LoggingPluginIrExtensionRegistrar : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val transformers = listOf(
            LoggerIrFieldFillerTransformer(pluginContext),
            LoggerIrFunctionTransformer(pluginContext),
        )

        for (transformer in transformers) {
            moduleFragment.accept(transformer, null)
        }
    }
}


class FirLoggingPluginComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(project, LoggingPluginFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(project, LoggingPluginIrExtensionRegistrar())
    }
}