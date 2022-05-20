package ru.itmo.kotlin.plugin.fir.generator

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId


import org.jetbrains.kotlin.name.Name
import ru.itmo.kotlin.plugin.asAnnotationFQN

class LoggerFieldGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        val LOGGER_NAME = Name.identifier("logger")

        private val PREDICATE: DeclarationPredicate = has("StateLogging".asAnnotationFQN())
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedClasses by lazy {
        predicateBasedProvider.getSymbolsByPredicate(PREDICATE).filterIsInstance<FirRegularClassSymbol>()
    }


    object Key : FirPluginKey() {
        override fun toString(): String = "LoggerFieldGeneratorKey"
    }


    override fun generateProperties(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirPropertySymbol> {
        if (callableId.callableName != LOGGER_NAME) return emptyList()
        val classId = callableId.classId ?: return emptyList()
        val matchedClassSymbol = matchedClasses.firstOrNull { it.classId == classId } ?: return emptyList()

        return listOf(buildLoggerProperty(matchedClassSymbol, callableId, Key).symbol)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return when (classSymbol) {
            in matchedClasses -> setOf(LOGGER_NAME, classSymbol.name)
            else -> emptySet()
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(PREDICATE)
    }
}