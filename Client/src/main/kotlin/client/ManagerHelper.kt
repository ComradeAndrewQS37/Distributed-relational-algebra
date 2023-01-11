package client

import com.google.gson.GsonBuilder
import shared.expression.Expression
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.serialization.*
import io.ktor.serialization.gson.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import shared.ResultHolder
import shared.table.TableDto
import shared.table.TableDtoSerializer
import shared.utils.Logger
import shared.utils.ThrowableSerializer

object ManagerHelper {
    // configure client
    private val client: HttpClient = HttpClient(CIO) {
        install(WebSockets) {
            val gson = GsonBuilder()
                .registerTypeAdapter(TableDto::class.java, TableDtoSerializer())
                .registerTypeHierarchyAdapter(Throwable::class.java, ThrowableSerializer())
                .create()
            contentConverter = GsonWebsocketContentConverter(gson)
        }
    }


    // send expression to manager
    suspend fun sendExpression(expr: Expression): ExpressionResult {
        // pack Expression into ExpressionHolder
        val exprHolder = expr.asExpressionHolder()

        // connect to manager
        val currentSession: DefaultClientWebSocketSession =
            client.webSocketSession(method = HttpMethod.Get, host = "127.0.0.1", port = 8080, path = "/task")

        // send expression to manager
        currentSession.sendSerialized(exprHolder)
        Logger.log("sent expression to manager")

        return ExpressionResult(currentSession)
    }

    // receive result from manager
    suspend fun getResult(session: DefaultClientWebSocketSession): TableDto {

        Logger.log("started getting expression result")
        val resultHolder = try {
            session.receiveDeserialized<ResultHolder>()
        }catch (e : WebsocketDeserializeException){
            Logger.logError("cannot deserialize server response")
            throw e
        }catch (e : ClosedReceiveChannelException){
            Logger.logError("cannot receive result : the websocket channel was closed")
            throw e
        }catch (e : Throwable){
            Logger.logError("cannot receive result because of unknown error")
            throw e
        }

        // channel should be closed by server normally after sending result, but send close frame just in case
        session.close(CloseReason(CloseReason.Codes.NORMAL, "received result"))

        if (resultHolder.isSuccessful) {
            Logger.log("received result successfully")
            return resultHolder.result!!
        } else {
            Logger.log("received exceptional result")
            throw resultHolder.problem!!
        }
    }

    suspend fun cancelComputation(session: DefaultClientWebSocketSession){
        session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "computation was cancelled"))
    }

}


