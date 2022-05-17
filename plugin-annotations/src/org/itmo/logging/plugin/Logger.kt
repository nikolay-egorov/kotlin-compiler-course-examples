package org.itmo.logging.plugin


class Logger {
    fun fullLogReturn(dataToLog: String, level:String, isShouldLogReturn: Boolean, funcName: String, value: Any?): Any? {
        logState(dataToLog, level)
        if (!isShouldLogReturn) return value
        return logReturn(funcName, value)
    }

    private fun logReturn(data: String, value: Any?): Any? {
        println("[StateLogging]: $data --> $value")
        println("-".repeat(30))
        println("")
        return value
    }

    fun logState(data: String, level: String) {
        doLog(Pair(data, level))
    }

    private fun doLog(info: OutputInfo) {
        println("[StateLogging] --\t${info.second}\t --${info.first}")
    }
}