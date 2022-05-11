package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.name.FqName
import ru.itmo.kotlin.plugin.DependencyLocations.annotationsPath

fun String.asAnnotationFQN(): FqName = FqName("${annotationsPath}.$this")

const val defaultBodyOffSet = -1


fun IrStringConcatenation.addAsString(builder: IrBuilderWithScope, data: String)
    = addArgument(builder.irString(data))

object DependencyLocations {
    const val loggerClassId = "org/itmo/logging/plugin/Logger"
    const val dependencyPath: String = "org.itmo.logging.plugin"
    const val loggerFqName: String = "${dependencyPath}.Logger"
    const val annotationsPath: String = "${dependencyPath}.annotations"

    const val loggingMethodName: String = "logState"
}
