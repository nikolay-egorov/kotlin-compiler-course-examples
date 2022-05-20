package ru.itmo.kotlin.plugin.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import org.jetbrains.kotlin.name.ClassId
import ru.itmo.kotlin.plugin.AnnotationsNaming.annotationsNames
import ru.itmo.kotlin.plugin.AnnotationsNaming.classLogAnnotation
import ru.itmo.kotlin.plugin.AnnotationsNaming.classLogAnnotationClassId
import ru.itmo.kotlin.plugin.KtStateLoggingErrors

object LoggerClassChecker: FirClassChecker() {
    @OptIn(LookupTagInternals::class)
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        if (!declaration.symbol.isMarkedOrParentWith(session)) return
        val source = declaration.source ?: return
        if (declaration !is FirRegularClass) {
            reporter.reportOn(source, KtStateLoggingErrors.TARGET_SHOULD_BE_CLASS, context, null)
            return
        }
        val parents = declaration.superConeTypes.filter { typeRef ->
            val localSymbol = typeRef.type.fullyExpandedType(session).toRegularClassSymbol(session) ?: return@filter false
            localSymbol.annotations.any { it.fqName(session)?.shortName()?.identifier == classLogAnnotation }
        }
        val isClassMarked = declaration.isAnnotatedWithLogger()
        if (parents.isNotEmpty() && isClassMarked) {
            reporter.reportOn(source,KtStateLoggingErrors.ANCESTOR_WITH_ANNOTATION, parents.first().lookupTag.toFirRegularClass(session)!!.psi as PsiElement, context)
        }

        when (declaration.classKind) {
            ClassKind.ENUM_CLASS -> reporter.reportOn(source, KtStateLoggingErrors.TARGET_SHOULD_NOT_BE_ENUM_CLASS, context)
            ClassKind.ANNOTATION_CLASS -> reporter.reportOn(source, KtStateLoggingErrors.TARGET_SHOULD_BE_CLASS, context)
            else -> return
        }
    }

    private fun FirClass.isAnnotatedWithLogger(): Boolean
        = hasAnnotation(ClassId.fromString(classLogAnnotationClassId))

    fun FirClassSymbol<*>?.isMarkedOrParentWith(session: FirSession): Boolean {
        if (this == null) return false
        if (this.annotations.any { it.fqName(session)?.shortName()?.identifier in annotationsNames }) return true
        return resolvedSuperTypeRefs.any { typeRef ->
            val symbol = typeRef.type.fullyExpandedType(session).toRegularClassSymbol(session) ?: return@any false
            symbol.annotations.any { it.fqName(session)?.shortName()?.identifier in annotationsNames }
        }
    }
}