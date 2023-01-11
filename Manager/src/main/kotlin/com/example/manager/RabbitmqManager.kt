package com.example.manager

import shared.ResultHolder
import com.google.gson.GsonBuilder
import com.rabbitmq.client.*
import shared.TaskHolder
import shared.exception.ComputationException
import shared.table.TableDto
import shared.table.TableDtoSerializer
import shared.utils.ThrowableSerializer
import kotlinx.coroutines.CompletableDeferred
import java.util.*

object RabbitmqManager {

    private val connection: Connection
    private val channel: Channel
    private const val taskQueueName = "task_queue"

    private val gson = GsonBuilder()
        .registerTypeAdapter(TableDto::class.java, TableDtoSerializer())
        .registerTypeHierarchyAdapter(Throwable::class.java, ThrowableSerializer())
        .create()


    init {
        val factory = ConnectionFactory()
        factory.host = "localhost" // changed to "host.docker.internal" in docker version
        factory.port = 5672
        connection = factory.newConnection()
        channel = connection.createChannel()

        channel.queuePurge(taskQueueName) // clear queue

    }

    // not returning anything, just sending task
    suspend fun sendTask(taskHolder: TaskHolder) {

        // if depends on other task, wait for (i.e. block until) their completion
        try {
            taskHolder.arg1Future?.let { taskHolder.arg1 = it.await() }
            taskHolder.arg2Future?.let { taskHolder.arg2 = it.await() }
        } catch (e: ComputationException) {
            // one or both args were completed exceptionally, so can't compute result of current task
            taskHolder.resultTo?.completeExceptionally(e)
            return
        }

        // sending messages to rabbitmq queue
        val taskHolderJson = gson.toJson(taskHolder)
        sendMessage(taskHolderJson, taskHolder.resultTo)
    }

    // sending message to rabbitmq queue
    private fun sendMessage(message: String, resultTo: CompletableDeferred<TableDto>?) {
        val corrId = UUID.randomUUID().toString() // task identifier
        val replyQueueName = channel.queueDeclare().queue // define new queue for reply for this message

        val props = AMQP.BasicProperties.Builder()
            .correlationId(corrId)
            .replyTo(replyQueueName)
            .build()

        // publish message
        channel.basicPublish("", taskQueueName, props, message.toByteArray(charset("UTF-8")))

        // register consumer for response from worker
        channel.basicConsume(replyQueueName, true,
            { _: String?, delivery: Delivery ->
                // verify task id
                if (delivery.properties.correlationId == corrId) {
                    try {
                        // receive result form worker
                        val resultHolder = gson.fromJson(
                            String(delivery.body, charset("UTF-8")),
                            ResultHolder::class.java
                        )
                        if (resultHolder.isSuccessful) {
                            resultTo?.complete(resultHolder.result!!)
                        } else {
                            resultTo?.completeExceptionally(
                                ComputationException("Error during computation", resultHolder.problem!!)
                            )
                        }
                    } catch (e: Exception) {
                        resultTo?.completeExceptionally(e)
                    }
                }
            }
        ) { _: String? -> }
    }
}



