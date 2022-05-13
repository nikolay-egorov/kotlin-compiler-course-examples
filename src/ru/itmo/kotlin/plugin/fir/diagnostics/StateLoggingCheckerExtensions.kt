package ru.itmo.kotlin.plugin.fir.diagnostics

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class StateLoggingCheckerExtensions(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val annotationCallCheckers: Set<FirAnnotationCallChecker>
            get() = setOf(LoggerAnnotationsChecker)
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirClassChecker>
            get() = setOf(LoggerClassChecker)

        override val propertyCheckers: Set<FirPropertyChecker>
            get() = setOf(LoggerPropertyChecker)

        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
            get() = setOf(LoggerFunctionChecker, LoggerFunctionSemanticsChecker)


    }
}