package ru.itmo.kotlin.plugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
import org.jetbrains.kotlin.diagnostics.Severity.WARNING
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.NAME_IDENTIFIER
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.OVERRIDE_MODIFIER
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.RENDER_CLASS_OR_OBJECT
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.psi.KtClassOrObject
import ru.itmo.kotlin.plugin.StateLoggingErrors.ANCESTOR_WITH_ANNOTATION
import ru.itmo.kotlin.plugin.StateLoggingErrors.DUPLICATING_ANNOTATIONS
import ru.itmo.kotlin.plugin.StateLoggingErrors.GETTER_OVERRIDE_IS_NOT_ALLOWED
import ru.itmo.kotlin.plugin.StateLoggingErrors.NO_LOGGER_CLASS_ANNOTATION
import ru.itmo.kotlin.plugin.StateLoggingErrors.RETURN_HAS_NO_EFFECT
import ru.itmo.kotlin.plugin.StateLoggingErrors.TARGET_DELEGATE_IS_NOT_ALLOWED
import ru.itmo.kotlin.plugin.StateLoggingErrors.TARGET_IS_NOT_ALLOWED
import ru.itmo.kotlin.plugin.StateLoggingErrors.TARGET_SHOULD_BE_CLASS
import ru.itmo.kotlin.plugin.StateLoggingErrors.TARGET_SHOULD_NOT_BE_ENUM_CLASS


object KtStateLoggingErrors {
    val TARGET_SHOULD_BE_CLASS by error0<PsiElement>(NAME_IDENTIFIER)
    val TARGET_DELEGATE_IS_NOT_ALLOWED by error0<PsiElement>(NAME_IDENTIFIER)
    val TARGET_SHOULD_NOT_BE_ENUM_CLASS by error0<PsiElement>(NAME_IDENTIFIER)
    val TARGET_SHOULD_BE_INSTANTIABLE = DiagnosticFactory0.create<PsiElement>(ERROR)
    val GETTER_OVERRIDE_IS_NOT_ALLOWED by error0<PsiElement>(OVERRIDE_MODIFIER)
    val NO_LOGGER_CLASS_ANNOTATION  by error0<PsiElement>(NAME_IDENTIFIER)
    val TARGET_IS_NOT_ALLOWED  by error0<PsiElement>(NAME_IDENTIFIER)
    val RETURN_HAS_NO_EFFECT by warning0<PsiElement>(NAME_IDENTIFIER)
    val DUPLICATING_ANNOTATIONS  by warning0<PsiElement>(NAME_IDENTIFIER)
    val ANCESTOR_WITH_ANNOTATION  by warning1<PsiElement, KtClassOrObject>(NAME_IDENTIFIER)
}


object StateLoggingErrors {
    var TARGET_SHOULD_BE_CLASS = DiagnosticFactory0.create<PsiElement>(ERROR)
    var TARGET_DELEGATE_IS_NOT_ALLOWED = DiagnosticFactory0.create<PsiElement>(ERROR)
    var TARGET_SHOULD_NOT_BE_ENUM_CLASS = DiagnosticFactory0.create<PsiElement>(ERROR)
    var TARGET_SHOULD_BE_INSTANTIABLE = DiagnosticFactory0.create<PsiElement>(ERROR)
    var GETTER_OVERRIDE_IS_NOT_ALLOWED = DiagnosticFactory0.create<PsiElement>(ERROR)
    var TARGET_IS_NOT_ALLOWED = DiagnosticFactory0.create<PsiElement>(ERROR)
    var NO_LOGGER_CLASS_ANNOTATION = DiagnosticFactory0.create<PsiElement>(ERROR)
    var RETURN_HAS_NO_EFFECT = DiagnosticFactory0.create<PsiElement>(WARNING)
    var DUPLICATING_ANNOTATIONS = DiagnosticFactory0.create<PsiElement>(WARNING)
    var ANCESTOR_WITH_ANNOTATION = DiagnosticFactory1.create<PsiElement, KtClassOrObject>(WARNING)
}


object DefaultLoggerErrorMessages : DefaultErrorMessages.Extension {
    private val MAP = DiagnosticFactoryToRendererMap("StateLogging")
    override fun getMap() = MAP

    init {
        MAP.put(
            RETURN_HAS_NO_EFFECT,
            "'logReturn=true' option has no effect on a method with Unit type"
        )

        MAP.put(
            NO_LOGGER_CLASS_ANNOTATION,
            "'ToLogFunction' could be applied only on classes or ancestors with '@StateLogging'"
        )

        MAP.put(
            TARGET_SHOULD_BE_CLASS,
            "'StateLogging' should be placed on a class"
        )

        MAP.put(
            TARGET_DELEGATE_IS_NOT_ALLOWED,
            "Delegating 'ToLogFunction' is not allowed"
        )

        MAP.put(
            TARGET_SHOULD_NOT_BE_ENUM_CLASS,
            "'StateLogging' should not be placed on a 'enum class'"
        )

        MAP.put(
            GETTER_OVERRIDE_IS_NOT_ALLOWED,
            "Overriding 'get-logger' is not allowed"
        )

        MAP.put(
            ANCESTOR_WITH_ANNOTATION,
            "Class {0} has already been annotated with '@StateLogging'",
            RENDER_CLASS_OR_OBJECT
        )

        MAP.put(
            DUPLICATING_ANNOTATIONS,
            "Duplicating ''ToLogFunction'' annotations"
        )

        MAP.put(
            TARGET_IS_NOT_ALLOWED,
            "Placing annotations is only allowed on methods or classes"
        )

    }


    var _initializer: Any = object : Any() {
        init {
            Errors.Initializer.initializeFactoryNamesAndDefaultErrorMessages(
                StateLoggingErrors::class.java,
                DefaultLoggerErrorMessages
            )
        }
    }
}