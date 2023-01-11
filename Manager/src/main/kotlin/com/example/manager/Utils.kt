package com.example.manager

import shared.ResultHolder
import shared.utils.Logger
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ClosedSendChannelException


suspend fun DefaultWebSocketServerSession.sendExceptionalResult(e: Throwable) {
    try {
        sendSerialized(ResultHolder(e))
    } catch (e: ClosedSendChannelException) {
        Logger.logError("cannot send exceptional result, channel was already closed")
    }

}

suspend fun DefaultWebSocketServerSession.waitForClose() {

    try {
        // server is not expecting any other frames, so receive them and discard until channel is closed
        for (frame in incoming) {
            val text = (frame as? Frame.Text)?.readText() ?: "unknown format"
            Logger.log("unexpected frame was received: $text")
        }
    } catch (e: ClosedReceiveChannelException) {
        return
    }
}