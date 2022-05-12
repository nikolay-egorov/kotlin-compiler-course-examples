package ru.itmo.kotlin.plugin.fir.diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.PsiSourceNavigator.getRawName
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import ru.itmo.kotlin.plugin.AnnotationsNaming.methodLogAnnotationClassId
import ru.itmo.kotlin.plugin.DependencyLocations.loggerClassId
import ru.itmo.kotlin.plugin.KtStateLoggingErrors
import ru.itmo.kotlin.plugin.fir.diagnostics.LoggerClassChecker.isMarkedOrParentWith

object LoggerFunctionChecker: FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val containingClassSymbol = declaration.dispatchReceiverType?.toRegularClassSymbol(session)

        if (declaration.origin != FirDeclarationOrigin.Source) return
        val parentOrClassMarked = containingClassSymbol.isMarkedOrParentWith(session)
        val methodMarked = declaration.isAnnotated()

        if (declaration.isWrongAnnotation()) {
            reporter.reportOn(declaration.source, KtStateLoggingErrors.TARGET_SHOULD_BE_CLASS, context)
        }

        if (!parentOrClassMarked && methodMarked) {
            reporter.reportOn(declaration.source, KtStateLoggingErrors.NO_LOGGER_CLASS_ANNOTATION, context)
        }
        val loggerClass = session.symbolProvider.getClassLikeSymbolByClassId(ClassId.fromString(loggerClassId))
        loggerClass?.let {
            if (declaration.isGetter(it) && declaration.isOverride) {
                reporter.reportOn(declaration.source, KtStateLoggingErrors.GETTER_OVERRIDE_IS_NOT_ALLOWED, context)
            }
        }
    }

    private fun FirFunction.isWrongAnnotation(): Boolean
        = hasAnnotation(ClassId.fromString(loggerClassId))

    private fun FirFunction.isAnnotated(): Boolean
        = hasAnnotation(ClassId.fromString(methodLogAnnotationClassId))

    private fun FirFunction.isGetter(loggerClass: FirClassLikeSymbol<*>): Boolean
        = this.returnTypeRef == loggerClass && getRawName() == "<get-logger>"
}