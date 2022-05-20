package ru.itmo.kotlin.plugin.fir.diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.name.Name
import ru.itmo.kotlin.plugin.DependencyLocations
import ru.itmo.kotlin.plugin.KtStateLoggingErrors
import ru.itmo.kotlin.plugin.fir.diagnostics.LoggerFunctionChecker.getFunctionLogAnnotation
import ru.itmo.kotlin.plugin.fir.diagnostics.LoggerFunctionChecker.isAnnotated

object LoggerFunctionSemanticsChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.origin != FirDeclarationOrigin.Source) return
        val methodMarked = declaration.isAnnotated()

        if (methodMarked && declaration.returnTypeRef.coneType.isUnit) {
            declaration.getFunctionLogAnnotation().argumentMapping
                .mapping[Name.identifier(DependencyLocations.loggingReturnMethodAndAnnotationParameterName)]
                ?.let {
                    val toLog = (it as FirConstExpression<*>).value as? Boolean ?: return@let
                    if (toLog) {
                        reporter.reportOn(declaration.source, KtStateLoggingErrors.RETURN_HAS_NO_EFFECT, context)
                    }
                }
        }

    }

}