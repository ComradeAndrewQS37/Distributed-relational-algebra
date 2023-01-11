package client

import io.ktor.client.plugins.websocket.*


/** Returned as result of [Expression.compute()][client.compute], allows to receive result later by [get()][get]**/
class ExpressionResult(private val session : DefaultClientWebSocketSession) {
    suspend fun get() = ManagerHelper.getResult(session)
    suspend fun cancel() = ManagerHelper.cancelComputation(session)
}
