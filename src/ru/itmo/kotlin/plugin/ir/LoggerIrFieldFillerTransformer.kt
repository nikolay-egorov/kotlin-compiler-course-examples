package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.fir.backend.IrPluginDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.properties
import ru.itmo.kotlin.plugin.defaultBodyOffSet
import ru.itmo.kotlin.plugin.fir.generator.LoggerFieldGenerator
import ru.itmo.kotlin.plugin.fir.generator.LoggerFieldGenerator.Companion.LOGGER_NAME

class LoggerIrFieldFillerTransformer(pluginContext: IrPluginContext) : AbstractTransformerForGenerator(pluginContext) {
    companion object {
        private val nameOfInterest = "<get-${LOGGER_NAME}>"
    }

    override fun interestedIn(key: FirPluginKey): Boolean {
        return key == LoggerFieldGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: FirPluginKey): IrBody? {
        return if (function.name.asString() != nameOfInterest) function.body
        else initGetterBody(function)
    }


    private fun initGetterBody(function: IrSimpleFunction): IrBody {
        return irFactory.createBlockBody(defaultBodyOffSet, defaultBodyOffSet) {
            val parentClass = function.parentClassOrNull ?: return@createBlockBody
            val generatedProperty = parentClass.properties.firstOrNull { (it.origin as? IrPluginDeclarationOrigin)?.pluginKey == LoggerFieldGenerator.Key }!!
            val r = function.returnType
            val declarationBuilder = DeclarationIrBuilder(context, function.symbol, startOffset, endOffset)
            val properReceiver = if (function.dispatchReceiverParameter == null) null
                                else declarationBuilder.irGet(function.dispatchReceiverParameter!!, type = function.dispatchReceiverParameter!!.type)

            statements.add(
                IrReturnImpl(startOffset, endOffset, r, function.symbol,
                    IrGetFieldImpl(startOffset, endOffset, generatedProperty.backingField!!.symbol, generatedProperty.backingField!!.type,
                    receiver = properReceiver, origin = IrStatementOrigin.GET_PROPERTY)
                )
            )
        }
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: FirPluginKey): IrBody? {
        return generateBodyForDefaultConstructor(constructor)
    }
}