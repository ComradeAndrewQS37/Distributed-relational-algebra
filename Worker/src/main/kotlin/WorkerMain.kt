import worker.Worker

fun main(args: Array<String>) {
    val server = Worker()
    server.startConsuming()
}
