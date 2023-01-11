package worker

import com.rabbitmq.client.*
import shared.utils.Logger

class Worker {

    private val connection: Connection
    private val channel: Channel
    private val taskQueueName = "task_queue"


    init {
        // configuring connection
        val factory = ConnectionFactory()
        factory.host = "localhost" // changed to "host.docker.internal" in docker version
        factory.port = 5672
        connection = factory.newConnection()
        channel = connection.createChannel()

        // configuring queue for tasks
        channel.queueDeclare(taskQueueName, false, false, false, null)
        channel.basicQos(1) // worker consumes new task only after completing the previous one

        Logger.log("worker connected to RabbitMQ successfully")

    }


    fun startConsuming() {
        // callback for receiving message
        val deliverCallback = DeliverCallback { _: String?, delivery: Delivery ->
            val replyProps = AMQP.BasicProperties.Builder()
                .correlationId(delivery.properties.correlationId)
                .build()

            // received TaskHolder message
            val message = String(delivery.body, charset("UTF-8"))
            // ResultHolder message
            val response = TaskService.processMessage(message) // exception handling in this method

            // send message
            channel.basicPublish(
                "",
                delivery.properties.replyTo, // queue for reply
                replyProps,
                response.toByteArray(charset("UTF-8"))
            )
            channel.basicAck(delivery.envelope.deliveryTag, false)

        }

        // subscribe for receiving messages from task queue
        channel.basicConsume(taskQueueName, false, deliverCallback) { _ -> }
        Logger.log("worker is ready and waiting for task requests")

    }

}


