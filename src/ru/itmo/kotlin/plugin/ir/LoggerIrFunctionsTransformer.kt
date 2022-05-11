package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.fir.backend.IrPluginDeclarationOrigin
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.buildStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.statements
import ru.itmo.kotlin.plugin.DependencyLocations.loggingMethodName
import ru.itmo.kotlin.plugin.addAsString
import ru.itmo.kotlin.plugin.asAnnotationFQN
import ru.itmo.kotlin.plugin.defaultBodyOffSet
import ru.itmo.kotlin.plugin.fir.generator.LoggerFieldGenerator
import ru.itmo.kotlin.plugin.logger.CustomLogger


class LoggerIrFunctionsTransformer(context: IrPluginContext): AnnotatedFunctionsIrTransformer(context) {
    companion object {
        private const val methodLogAnnotation: String = "ToLogFunction"
        private const val classLogAnnotation: String = "StateLogging"
    }

    override fun interestedInClass(irClass: IrClass): Boolean
        = irClass.hasAnnotation(classLogAnnotation.asAnnotationFQN())

    override fun interestedInFunction(function: IrSimpleFunction): Boolean
        = isAnnotatedWithLogger(function)

    override fun generateBodyForFunction(function: IrSimpleFunction): IrBody? {
        return transformFunctionBody(function)
    }

    private fun isAnnotatedWithLogger(function: IrSimpleFunction): Boolean
        = function.hasAnnotation(methodLogAnnotation.asAnnotationFQN())


    private fun transformFunctionBody(function: IrSimpleFunction): IrBody {
        return irFactory.createBlockBody(defaultBodyOffSet, defaultBodyOffSet) {
            val loggerField = function.parentClassOrNull
                                ?.properties
                                ?.firstOrNull { (it.origin as? IrPluginDeclarationOrigin)?.pluginKey == LoggerFieldGenerator.Key }
                ?: return@createBlockBody

            val declaredClassType = loggerField.backingField?.type?.classOrNull ?: return@createBlockBody
            val loggerCallSymbol = declaredClassType.getSimpleFunction(loggingMethodName)!!

            val declarationBuilder = DeclarationIrBuilder(context, function.symbol, startOffset, endOffset)
            statements.add(declarationBuilder.createLoggingStatement(
                function, declaredClassType, loggerCallSymbol
            ))
            statements.addAll(function.body?.statements ?: emptyList())
            statements.add(declarationBuilder.createLoggingStatement(
                function, declaredClassType, loggerCallSymbol, whenHappened = CustomLogger.HappenedWhen.AFTER
            ))
        }

    }


    private fun IrBuilderWithScope.createLoggingStatement(irFunction: IrSimpleFunction, loggerSymbol: IrClassSymbol,
                                                          onLogSymbol: IrSimpleFunctionSymbol, logLevel: CustomLogger.InfoLevel = CustomLogger.InfoLevel.INFO,
                                                          whenHappened: CustomLogger.HappenedWhen = CustomLogger.HappenedWhen.BEFORE,
    ) = buildStatement(defaultBodyOffSet, defaultBodyOffSet) {
        with(irCall(onLogSymbol)) {
            val builderScope = this@buildStatement
            val stringBuilder = irConcat()
            // addString(stringBuilder, "in ${irFunction.name}")
            with(stringBuilder) {
                addAsString(builderScope, if (whenHappened == CustomLogger.HappenedWhen.BEFORE)
                    "in ${irFunction.name}"
                else "")
                addAsString(builderScope, "Class state ${whenHappened.time}:\n${getClassState(irFunction)}")
                addAsString(builderScope, "Arguments ${whenHappened.time}:\n${getFunctionArgs(irFunction)}")
            }

            putValueArgument(0, stringBuilder)
            putValueArgument(1, irString(logLevel.name))
            dispatchReceiver = irGetObject(loggerSymbol)

            return@with this
        }
    }



    private fun IrBuilderWithScope.getFunctionArgs(irFunction: IrSimpleFunction): String {
        return buildString {
            irFunction.valueParameters.forEach {
                append("\t\t${it.name} = ${irGet(it)}")
            }
            append('\n')
            append("-".repeat(30))
        }
    }


    private fun IrBuilderWithScope.getClassState(irFunction: IrSimpleFunction): String {
        val classSymbol = irFunction.parentClassOrNull ?: return ""
        val sb = StringBuilder()
        classSymbol.fields.forEach {
            sb.append("\t\t${it.name} = ${irGetField(classSymbol.thisReceiver?.let { it1 -> irGet(it1) }, it)}")
        }
        return sb.toString()
    }


}