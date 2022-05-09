package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.buildStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.FqName
import ru.itmo.kotlin.plugin.addAsString
import ru.itmo.kotlin.plugin.defaultBodyOffSet
import ru.itmo.kotlin.plugin.fir.generator.LoggerFieldGenerator
import ru.itmo.kotlin.plugin.toFQN
import ru.itmo.kotlin.plugin.logger.CustomLogger
import ru.itmo.kotlin.plugin.logger.Logger

class LoggerIrFunctionTransformer(pluginContext: IrPluginContext) : AbstractTransformerForGenerator(pluginContext) {
    companion object {
        private const val methodLogAnnotation: String = "ToLogFunction"
    }

    // private val loggerField = context.referenceClass(FqName(Logger::class.java.name))

    override fun interestedIn(key: FirPluginKey): Boolean {
        return key == LoggerFieldGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: FirPluginKey): IrBody? {
        val body = function.body
        return if (isAnnotatedWithLogger(function)) {
            transformFunctionBody(function)
        } else body
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: FirPluginKey): IrBody? {
        return generateBodyForDefaultConstructor(constructor)
    }

    private fun isAnnotatedWithLogger(function: IrSimpleFunction): Boolean
        = function.hasAnnotation(methodLogAnnotation.toFQN())

    private fun transformFunctionBody(function: IrSimpleFunction): IrBody {
        return irFactory.createBlockBody(defaultBodyOffSet, defaultBodyOffSet) {
            val loggerField = context.referenceClass(FqName(Logger::class.java.name))

            val loggerCallSymbol = loggerField!!.owner.getSimpleFunction(Logger::logState.name)!!

            val declarationBuilder = DeclarationIrBuilder(context, function.symbol, startOffset, endOffset)
            statements.add(declarationBuilder.createLoggingStatement(
                function, loggerField.owner.symbol, loggerCallSymbol
            ))
            statements.addAll(function.body?.statements ?: emptyList())
            statements.add(declarationBuilder.createLoggingStatement(
                function, loggerField.owner.symbol, loggerCallSymbol, whenHappened = CustomLogger.HappenedWhen.AFTER
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