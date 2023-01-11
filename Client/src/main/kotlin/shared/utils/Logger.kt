package shared.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// prints to stdout with current timestamp
object Logger {

    fun log(message: String) {
        println("${nowStamp()} \u001B[36m[INFO]\u001B[0m $message")
    }

    fun logError(message: String){
        println("${nowStamp()} \u001B[91m[ERROR]\u001B[0m $message")
    }

    private fun nowStamp(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        return "\u001B[93m" + LocalDateTime.now().format(formatter) + "\u001b[0m"
    }
}