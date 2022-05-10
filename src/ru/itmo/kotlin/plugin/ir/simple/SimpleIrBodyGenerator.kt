package ru.itmo.kotlin.plugin.ir.simple

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import ru.itmo.kotlin.plugin.fir.SimpleClassGenerator
import ru.itmo.kotlin.plugin.ir.AbstractTransformerForGenerator

class SimpleIrBodyGenerator(pluginContext: IrPluginContext) : AbstractTransformerForGenerator(pluginContext) {
    override fun interestedIn(key: FirPluginKey): Boolean {
        return key == SimpleClassGenerator.Key
    }

    override fun generateBodyForFunction(function: IrSimpleFunction, key: FirPluginKey): IrBody {
        require(function.name == SimpleClassGenerator.FOO_ID.callableName)
        val const = IrConstImpl(-1, -1, irBuiltIns.stringType, IrConstKind.String, value = "Hello world")
        val returnStatement = IrReturnImpl(-1, -1, irBuiltIns.nothingType, function.symbol, const)
        return irFactory.createBlockBody(-1, -1, listOf(returnStatement))
    }

    override fun generateBodyForConstructor(constructor: IrConstructor, key: FirPluginKey): IrBody? {
        return generateBodyForDefaultConstructor(constructor)
    }
}
