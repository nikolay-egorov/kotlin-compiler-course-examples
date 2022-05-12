package ru.itmo.kotlin.plugin.fir.diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import ru.itmo.kotlin.plugin.AnnotationsNaming
import ru.itmo.kotlin.plugin.KtStateLoggingErrors
import ru.itmo.kotlin.plugin.asAnnotationFQN

object LoggerPropertyChecker: FirPropertyChecker() {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val asMethod = declaration.hasAnnotation(ClassId.fromString(AnnotationsNaming.methodLogAnnotation.asAnnotationFQN().asString()))
        val asClass = declaration.hasAnnotation(ClassId.fromString(AnnotationsNaming.classLogAnnotation.asAnnotationFQN().asString()))

        if (asClass || asMethod) {
            reporter.reportOn(declaration.source, KtStateLoggingErrors.TARGET_IS_NOT_ALLOWED, context)
        }
    }
}