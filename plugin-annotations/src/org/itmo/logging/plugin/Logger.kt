package org.itmo.logging.plugin


class Logger {
    fun logReturn(data: String, value: Any?): Any? {
        println(data + value)
        return value
    }

    fun logState(data: String, level: String) {
        doLog(Pair(data, level))
    }

    private fun doLog(info: OutputInfo) {
        println("[StateLogging] --\t${info.second}\t -- \t${info.first}")
    }
}


object CustomLogger {
    enum class InfoLevel {
        WARN, INFO, ERROR
    }
    enum class HappenedWhen(val time: String) {
        BEFORE("before call"),
        AFTER("after call")
    }

}