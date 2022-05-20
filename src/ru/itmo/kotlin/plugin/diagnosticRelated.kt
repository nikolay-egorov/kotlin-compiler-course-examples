package ru.itmo.kotlin.plugin

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.NAME_IDENTIFIER
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.OVERRIDE_MODIFIER
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1


object KtStateLoggingErrors {
    val TARGET_SHOULD_BE_CLASS by error0<PsiElement>(NAME_IDENTIFIER)
    val TARGET_SHOULD_NOT_BE_ENUM_CLASS by error0<PsiElement>(NAME_IDENTIFIER)
    val GETTER_OVERRIDE_IS_NOT_ALLOWED by error0<PsiElement>(OVERRIDE_MODIFIER)
    val NO_LOGGER_CLASS_ANNOTATION by error0<PsiElement>(NAME_IDENTIFIER)
    val RETURN_HAS_NO_EFFECT by warning0<PsiElement>(NAME_IDENTIFIER)
    val ANCESTOR_WITH_ANNOTATION by warning1<PsiElement, PsiElement>(NAME_IDENTIFIER)
}
