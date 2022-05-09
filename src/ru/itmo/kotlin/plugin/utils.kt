package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.name.FqName

fun String.toFQN(): FqName = FqName("org.itmo.logging.plugin.$this")

const val defaultBodyOffSet = -1


fun IrStringConcatenation.addAsString(builder: IrBuilderWithScope, data: String)
    = addArgument(builder.irString(data))
