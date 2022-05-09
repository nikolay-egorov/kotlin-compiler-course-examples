package ru.itmo.kotlin.plugin.logger

object CustomLogger {
    enum class InfoLevel {
        WARN, INFO, ERROR
    }
    enum class HappenedWhen(val time: String) {
        BEFORE("before call"),
        AFTER("after call")
    }




    class Logger {
        fun logState(data: String, level: InfoLevel) {
            doLog(Pair(data, level))
        }

        private fun doLog(info: OutputInfo) {
            println("[StateLogging] --\t${info.second}\t -- \t\t${info.first}")
        }
    }


}