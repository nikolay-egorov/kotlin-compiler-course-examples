package ru.itmo.kotlin.plugin.logger



object CustomLogger {
    enum class InfoLevel {
        BEFORE, AFTER
    }
    enum class HappenedWhen {
        BEFORE,
        AFTER
    }

}