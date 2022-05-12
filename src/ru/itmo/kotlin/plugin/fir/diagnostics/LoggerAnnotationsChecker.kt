package ru.itmo.kotlin.plugin.fir.diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneType

object LoggerAnnotationsChecker : FirAnnotationCallChecker() {
    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotationType = expression.annotationTypeRef.coneType.fullyExpandedType(context.session) as? ConeClassLikeType
            ?: return
        val resolvedAnnotationSymbol = annotationType.lookupTag.toFirRegularClassSymbol(context.session) ?: return
        when (val annotationClassName = resolvedAnnotationSymbol.name.identifier) {
            "StateLogging" -> {
                // checkTypeParcelerUsage(expression, context, reporter)
                // checkDeprecatedAnnotations(expression, annotationClassName, context, reporter, isForbidden = true)
            }

        }
    }



}