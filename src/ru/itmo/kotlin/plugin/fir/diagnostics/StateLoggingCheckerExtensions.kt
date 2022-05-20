package ru.itmo.kotlin.plugin.fir.diagnostics

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class StateLoggingCheckerExtensions(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val classCheckers: Set<FirClassChecker>
            get() = setOf(LoggerClassChecker)

        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker>
            get() = setOf(LoggerFunctionChecker, LoggerFunctionSemanticsChecker)
    }
}