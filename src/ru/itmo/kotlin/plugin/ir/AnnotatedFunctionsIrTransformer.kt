package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

abstract class AnnotatedFunctionsIrTransformer(protected val context: IrPluginContext): IrElementVisitorVoid {
    protected val irFactory = context.irFactory
    protected val irBuiltIns = context.irBuiltIns

    open fun interestedInClass(irClass: IrClass): Boolean = true

    abstract fun interestedInFunction(function: IrSimpleFunction): Boolean
    abstract fun generateBodyForFunction(function: IrSimpleFunction): IrBody?

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitClass(declaration: IrClass) {
        if (!interestedInClass(declaration)) return
        super.visitClass(declaration)
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        if (!interestedInFunction(declaration)) return
        declaration.body = generateBodyForFunction(declaration)
        super.visitSimpleFunction(declaration)
    }

}