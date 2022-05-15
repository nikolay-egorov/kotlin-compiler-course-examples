package ru.itmo.kotlin.plugin.logger



object CustomLogger {
    enum class InfoLevel {
        WARN, INFO, ERROR
    }
    enum class HappenedWhen(val time: String) {
        BEFORE("before call"),
        AFTER("after call")
    }

}