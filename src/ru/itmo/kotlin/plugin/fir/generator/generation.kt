package ru.itmo.kotlin.plugin.fir.generator

import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.getClassDeclaredConstructors
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import ru.itmo.kotlin.plugin.DependencyLocations


@OptIn(SymbolInternals::class, FirImplementationDetail::class)
fun FirDeclarationGenerationExtension.buildLoggerProperty(
    matchedClassSymbol: FirClassLikeSymbol<*>,
    callableId: CallableId,
    key: FirPluginKey
): FirProperty {
    return buildProperty {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = key.origin
        status = FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.FINAL,
            EffectiveVisibility.Public
        )
        val loggerClassId = ClassId.fromString(DependencyLocations.loggerClassId)
        val resolvedReturnTypeRef = buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                ConeClassLikeLookupTagImpl(loggerClassId),
                emptyArray(),
                isNullable = false
            )
        }
        returnTypeRef = resolvedReturnTypeRef
        name = callableId.callableName
        val propertySymbol = FirPropertySymbol(callableId)
        symbol = propertySymbol
        dispatchReceiverType = callableId.classId?.let {
            val firClass = session.symbolProvider.getClassLikeSymbolByClassId(it)?.fir as? FirClass
            firClass?.defaultType()
        }
        isVar = false
        isLocal = false
        getter = buildPropertyAccessor {
            isGetter = true
            moduleData = session.moduleData
            origin = key.origin
            returnTypeRef = resolvedReturnTypeRef
            status = FirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            )
            symbol = FirPropertyAccessorSymbol()
        }

        initializer = buildFunctionCall {
            typeRef = resolvedReturnTypeRef
            calleeReference = buildResolvedNamedReference {
                val classContractor =
                    session.symbolProvider.getClassDeclaredConstructors(loggerClassId).first { it.isPrimary }
                name = classContractor.name
                resolvedSymbol = classContractor
            }
            argumentList = buildResolvedArgumentList(LinkedHashMap())
        }
    }
}
