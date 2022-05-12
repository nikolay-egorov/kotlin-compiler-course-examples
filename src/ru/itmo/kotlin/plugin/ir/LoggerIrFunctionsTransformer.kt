package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.getValueArgument
import org.jetbrains.kotlin.fir.backend.IrPluginDeclarationOrigin
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.buildStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.interpreter.getAnnotation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.Name
import ru.itmo.kotlin.plugin.AnnotationsNaming.classLogAnnotation
import ru.itmo.kotlin.plugin.AnnotationsNaming.methodLogAnnotation
import ru.itmo.kotlin.plugin.DependencyLocations.loggingMethodName
import ru.itmo.kotlin.plugin.DependencyLocations.loggingReturnMethodAndAnnotationParameterName
import ru.itmo.kotlin.plugin.addAsString
import ru.itmo.kotlin.plugin.asAnnotationFQN
import ru.itmo.kotlin.plugin.defaultBodyOffSet
import ru.itmo.kotlin.plugin.fir.generator.LoggerFieldGenerator
import ru.itmo.kotlin.plugin.logger.CustomLogger


class LoggerIrFunctionsTransformer(context: IrPluginContext): AnnotatedFunctionsIrTransformer(context) {
    private var foundClassWithAnnotation: IrClass? = null

    override fun interestedInClass(irClass: IrClass): Boolean {
        (setOf(irClass) + irClass.getAllSuperclasses())
            .firstOrNull { (it as? IrClass)
            ?.hasAnnotation(classLogAnnotation.asAnnotationFQN()) == true }?.let {

            foundClassWithAnnotation = it
            return true
        }
        return false
    }

    override fun interestedInFunction(function: IrSimpleFunction): Boolean
        = isAnnotatedWithLogger(function)

    override fun generateBodyForFunction(function: IrSimpleFunction): IrBody?
        = transformFunctionBody(function)

    private fun isAnnotatedWithLogger(function: IrSimpleFunction): Boolean
        = function.hasAnnotation(methodLogAnnotation.asAnnotationFQN())


    private fun transformFunctionBody(function: IrSimpleFunction): IrBody? {
        val properAnnotatedClass = foundClassWithAnnotation ?: function.parentClassOrNull ?: return function.body
        val isCallFromParent = foundClassWithAnnotation != function.parentClassOrNull

        return irFactory.createBlockBody(defaultBodyOffSet, defaultBodyOffSet) {
            val originalProperty = properAnnotatedClass.properties
                                .firstOrNull { (it.origin as? IrPluginDeclarationOrigin)?.pluginKey == LoggerFieldGenerator.Key }
                ?: return@createBlockBody
            val loggerField = if (!isCallFromParent) originalProperty
                              else function.parentClassOrNull?.properties
                                ?.filter { it.name == originalProperty.name }
                                ?.firstOrNull { it.overriddenSymbols.contains(originalProperty.symbol) }
                ?: return@createBlockBody

            val loggerClassSymbol = originalProperty.backingField?.type?.classOrNull ?: return@createBlockBody
            val loggerCallSymbol = loggerClassSymbol.getSimpleFunction(loggingMethodName)!!

            val declarationBuilder = DeclarationIrBuilder(context, function.symbol, startOffset, endOffset)
            statements.add(declarationBuilder.createLoggingStatement(
                function, loggerClassSymbol, loggerField, loggerCallSymbol, originalProperty = originalProperty
            ))
            val annotation = function.getAnnotation(methodLogAnnotation.asAnnotationFQN())
            annotation.getValueArgument(name = Name.identifier(loggingReturnMethodAndAnnotationParameterName))?.let {
                val toLog = (it as? IrConst<*>)?.value as? Boolean ?: return@let
                if (toLog) {
                    declarationBuilder.transformReturnStatement(function, loggerClassSymbol)
                }
            }
            statements.addAll(function.body?.statements ?: emptyList())
            statements.add(declarationBuilder.createLoggingStatement(
                function, loggerClassSymbol, loggerField, loggerCallSymbol,
                whenHappened = CustomLogger.HappenedWhen.AFTER,
                originalProperty = originalProperty
            ))
        }
    }

    private fun IrBuilderWithScope.transformReturnStatement(irFunction: IrSimpleFunction,
                                                            loggerSymbol: IrClassSymbol) {
        val properMethodSymbol = loggerSymbol.getSimpleFunction(loggingReturnMethodAndAnnotationParameterName) ?: return
        val builderWithScope = this

        irFunction.body?.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitReturn(expression: IrReturn): IrExpression {
                val wrapped = irCall(properMethodSymbol, irBuiltIns.anyNType).apply {
                    val data = irConcat()
                    data.addAsString(builderWithScope, "\t^${irFunction.name}")
                    putValueArgument(0, data)
                    putValueArgument(1, expression.value)
                    dispatchReceiver = irGetObject(loggerSymbol)
                }
                return super.visitReturn(irReturn(wrapped))
            }
        }, null)
    }


    private fun IrBuilderWithScope.createLoggingStatement(irFunction: IrSimpleFunction, loggerSymbol: IrClassSymbol, loggerField: IrProperty,
                                                          onLogSymbol: IrSimpleFunctionSymbol, logLevel: CustomLogger.InfoLevel = CustomLogger.InfoLevel.INFO,
                                                          whenHappened: CustomLogger.HappenedWhen = CustomLogger.HappenedWhen.BEFORE, originalProperty: IrProperty? = null
    ) = buildStatement(defaultBodyOffSet, defaultBodyOffSet) {
        val isDirectCall = loggerField.backingField != null
        with(irCall(onLogSymbol)) {
            val builderScope = this@buildStatement
            with(irConcat()) {
                addAsString(builderScope, if (whenHappened == CustomLogger.HappenedWhen.BEFORE)
                    "--> ${irFunction.name}"
                else "${whenHappened.time} ${irFunction.name}")
                addAsString(builderScope, "Class state:${getClassState(irFunction).toProperStateFormatting()}")
                addAsString(builderScope, "Arguments:${getFunctionArgs(irFunction).toProperStateFormatting()}")


                putValueArgument(0, this)
            }
            putValueArgument(1, irString(logLevel.name))
            val thisVal = irGet(irFunction.dispatchReceiverParameter!!)
            val originalField = originalProperty?.backingField

            dispatchReceiver = if (isDirectCall) irGetObject(loggerSymbol)
                                else irCall(loggerField.getter!!, origin = IrStatementOrigin.GET_PROPERTY,
                                        superQualifierSymbol=originalField!!.parentClassOrNull!!.symbol).apply {
                                            dispatchReceiver = thisVal
                                      }

            return@with this
        }
    }

    private fun String.toProperStateFormatting(): String
        = ifEmpty { "\t[empty]" }

    private fun IrBuilderWithScope.getFunctionArgs(irFunction: IrSimpleFunction): String {
        return buildString {
            irFunction.valueParameters.forEach {
                append("\t\t${it.name} = ${irGet(it)}\n")
            }
            append("\t")
            append("-".repeat(30))
        }
    }


    private fun IrBuilderWithScope.getClassState(irFunction: IrSimpleFunction): String {
        val classSymbol = irFunction.parentClassOrNull ?: return ""
        if (!classSymbol.fields.iterator().hasNext()) return ""
        val sb = StringBuilder()
        classSymbol.fields.forEach {
            sb.append("\t\t${it.name} = ${irGetField(classSymbol.thisReceiver?.let { it1 -> irGet(it1) }, it)}\n")
        }
        return sb.toString()
    }

}