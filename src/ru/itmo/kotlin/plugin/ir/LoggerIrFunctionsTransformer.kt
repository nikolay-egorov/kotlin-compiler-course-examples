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
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.interpreter.getAnnotation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classOrNull
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
        var shouldLogReturn = false

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
                function, loggerField, loggerCallSymbol, originalProperty = originalProperty
            ))
            val annotation = function.getAnnotation(methodLogAnnotation.asAnnotationFQN())
            annotation.getValueArgument(name = Name.identifier(loggingReturnMethodAndAnnotationParameterName))?.let {
                val toLog = (it as? IrConst<*>)?.value as? Boolean ?: return@let
                if (toLog) {
                    shouldLogReturn = true
                }
            }
            declarationBuilder.transformReturnStatement(function, this, loggerClassSymbol,
                                                        loggerField, originalProperty, shouldLogReturn)
        }
    }

    private fun IrBuilderWithScope.transformReturnStatement(irFunction: IrSimpleFunction,
                                                            irCreatingFunctionBody: IrBlockBody,
                                                            loggerSymbol: IrClassSymbol,
                                                            loggerField: IrProperty,
                                                            originalProperty: IrProperty?,
                                                            shouldLogReturn: Boolean) {
        val loggingMethod = loggerSymbol.getSimpleFunction(loggingMethodName) ?: return
        val returnMethodWrapper = loggerSymbol.getSimpleFunction(loggingReturnMethodAndAnnotationParameterName) ?: return
        val builderWithScope = this
        val functionStatements = irFunction.body?.statements?.toSet() ?: emptySet()
        val adjustmentIndex = mutableMapOf<Int, IrCall>()
        val receiver = builderWithScope.irGet(irFunction.dispatchReceiverParameter!!)

        irFunction.body?.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitReturn(expression: IrReturn): IrExpression {
                val foundAt = functionStatements.indexOf(expression)
                adjustmentIndex[foundAt] = createLoggingStatement(
                    irFunction, loggerField, loggingMethod,
                    whenHappened = CustomLogger.HappenedWhen.AFTER,
                    originalProperty = originalProperty
                )

                if (!shouldLogReturn) return super.visitReturn(expression)

                val wrapped = irCall(returnMethodWrapper, irBuiltIns.anyNType).apply {
                    val data = irConcat()
                    data.addAsString(builderWithScope, "\t^${irFunction.name}()")
                    putValueArgument(0, data)
                    putValueArgument(1, expression.value)
                    dispatchReceiver = getProperLoggerGetterCall(loggerField.backingField != null,
                        loggerField, loggerField.backingField, receiver) //irGetObject(loggerSymbol)
                }
                return super.visitReturn(irReturn(wrapped))
            }
        }, null)

        val interestingIndexes = adjustmentIndex.keys
        val newStatements = irCreatingFunctionBody.statements
        irFunction.body?.statements?.forEachIndexed { index, irStatement ->
            if (index in interestingIndexes) {
                newStatements.add(adjustmentIndex[index]!!)
            }
            newStatements.add(irStatement)
        }
    }


    private fun IrBuilderWithScope.createLoggingStatement(irFunction: IrSimpleFunction, loggerField: IrProperty,
                                                          onLogSymbol: IrSimpleFunctionSymbol, logLevel: CustomLogger.InfoLevel = CustomLogger.InfoLevel.INFO,
                                                          whenHappened: CustomLogger.HappenedWhen = CustomLogger.HappenedWhen.BEFORE, originalProperty: IrProperty? = null
    ) = buildStatement(defaultBodyOffSet, defaultBodyOffSet) {
        val isDirectCall = loggerField.backingField != null
        with(irCall(onLogSymbol)) {
            val builderScope = this@buildStatement
            with(irConcat()) {
                addAsString(builderScope, if (whenHappened == CustomLogger.HappenedWhen.BEFORE) "--> ${irFunction.name}()"
                else " ${whenHappened.time} ${irFunction.name}()")
                addAsString(builderScope, "\n")
                addAsString(builderScope, "\tClass state:")
                getClassState(builderScope, irFunction)
                addAsString(builderScope, "\n")
                addAsString(builderScope, "\tArguments:")
                getFunctionArgs(builderScope, irFunction)

                putValueArgument(0, this)
            }
            putValueArgument(1, irString(logLevel.name))
            val thisVal = irGet(irFunction.dispatchReceiverParameter!!)
            val originalField = originalProperty?.backingField

            dispatchReceiver = getProperLoggerGetterCall(isDirectCall, loggerField, originalField, thisVal)

            return@with this
        }
    }


    private fun IrStringConcatenation.getFunctionArgs(builder: IrBuilderWithScope, irFunction: IrSimpleFunction) {
        if (irFunction.valueParameters.isEmpty()) {
            onEmptyState(builder)
            return
        }
        val totalSz = irFunction.valueParameters.size
        irFunction.valueParameters.forEachIndexed { index, it ->
            addAsString(builder, "\t${it.name} :${it.type.classFqName} = ")
            addArgument(builder.irGet(it))
            if (index < totalSz - 1) {
                addAsString(builder, ", ")
            }
        }
        addAsString(builder, "\n")
        addAsString(builder, "-".repeat(30))
    }

    private fun IrBuilderWithScope.getProperLoggerGetterCall(isDirectCall: Boolean, loggerField: IrProperty, originalField: IrField?, receiver: IrExpression): IrDeclarationReference {
        val call = if (isDirectCall) irGetField(receiver, loggerField.backingField!!)
        else irCall(loggerField.getter!!, origin = IrStatementOrigin.GET_PROPERTY,
            superQualifierSymbol=originalField!!.parentClassOrNull!!.symbol).apply {
            dispatchReceiver = receiver
        }
        return call
    }


    private fun IrStringConcatenation.getClassState(builder: IrBuilderWithScope, irFunction: IrSimpleFunction) {
        val classSymbol = irFunction.parentClassOrNull
        if (classSymbol == null) {
            onEmptyState(builder, true)
            return
        }
        val properties = classSymbol.properties
        if (!properties.iterator().hasNext()) {
            onEmptyState(builder)
            return
        }
        val instanceReceiver = if (irFunction.dispatchReceiverParameter == null) null else builder.irGet(irFunction.dispatchReceiverParameter!!)
        val filteredProperties = properties.filter { !it.isFakeOverride && !it.origin.isSynthetic && it.origin !is IrPluginDeclarationOrigin }
        if (!filteredProperties.iterator().hasNext()) {
            onEmptyState(builder)
            return
        }
        filteredProperties.forEach {
            addAsString(builder, "\t${it.name} :${it.backingField!!.type.classFqName} = ")
            addArgument(builder.irGetField(instanceReceiver, it.backingField!!))
            addAsString(builder, ", ")
        }
    }

    private fun IrStringConcatenation.onEmptyState(builder: IrBuilderWithScope, noState: Boolean = false) {
        addArgument(builder.irString(if (noState) "\t[static]" else "\t[empty]"))
    }

}