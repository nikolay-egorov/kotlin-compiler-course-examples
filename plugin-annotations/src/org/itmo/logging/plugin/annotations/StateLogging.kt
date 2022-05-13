package org.itmo.logging.plugin.annotations

@Target(AnnotationTarget.CLASS)
annotation class StateLogging

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class ToLogFunction(val logReturn: Boolean = false)