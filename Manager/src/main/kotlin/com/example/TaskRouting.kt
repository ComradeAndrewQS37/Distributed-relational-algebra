package com.example

import com.example.manager.TaskManager
import com.example.manager.sendExceptionalResult
import com.example.manager.waitForClose
import shared.ResultHolder
import shared.exception.ComputationException
import shared.expression.ExpressionHolder
import shared.table.TableDto
import shared.utils.Logger
import io.ktor.serialization.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException

fun Route.taskRouting() {

    webSocket("/task") {
        // try to receive expression
        val expr = try {
            receiveDeserialized<ExpressionHolder>()
        } catch (e: WebsocketDeserializeException) {
            Logger.logError("cannot resolve client message")
            sendExceptionalResult(e)
            close(CloseReason(CloseReason.Codes.NOT_CONSISTENT, "cannot deserialize message"))
            return@webSocket
        } catch (e: ClosedReceiveChannelException) {
            Logger.logError("cannot receive result : the websocket channel was closed")
            return@webSocket
        } catch (e: Throwable) {
            Logger.logError("cannot receive result because of unknown error")
            sendExceptionalResult(e)
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "cannot deserialize message"))
            return@webSocket
        }
        Logger.log("received new task")

        // starting computing the expression and waiting for result
        val deferredRes: Deferred<TableDto> =
            withContext(Dispatchers.Default) {
                TaskManager.processExpressionAsync(expr)
            }

        // control if client closed channel (i.e. cancelled expression computation)
        val waiteForCloseJob = launch {
            waitForClose() // finishes if client closed connection
            deferredRes.cancel() // cancel all computing tasks
        }

        // wait for result and send it
        try {
            val result = deferredRes.await()

            waiteForCloseJob.cancel()
            // everything completed successfully and client hasn't cancelled computation
            sendSerialized(ResultHolder(result))
            close(CloseReason(CloseReason.Codes.NORMAL, "finished computation"))

            Logger.log("finished computation successfully")

        } catch (e: ComputationException) {
            // final task was completed exceptionally
            waiteForCloseJob.cancel()
            sendExceptionalResult(e)
            close(CloseReason(CloseReason.Codes.NORMAL, "cannot compute expression, check for errors in it"))
            Logger.logError("error during computation : ${e.cause?.message ?: e.message}")
        } catch (e: CancellationException) {
            // client cancelled computation
            Logger.log("client cancelled computation")
        } catch (e: Throwable) {
            // unknown error
            waiteForCloseJob.cancel()
            sendExceptionalResult(RuntimeException("Unknown error during computation", e))
            close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "unknown error"))
            Logger.logError("unknown error during computation : ${e.message}")

        }
    }

}


