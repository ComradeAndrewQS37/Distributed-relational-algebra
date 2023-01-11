package com.example

import io.ktor.server.application.*
import com.example.plugins.*
import shared.table.TableDto
import shared.table.TableDtoSerializer
import shared.utils.ThrowableSerializer
import com.google.gson.GsonBuilder
import io.ktor.serialization.gson.*
import io.ktor.server.websocket.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(WebSockets) {
        val gson = GsonBuilder()
            .registerTypeAdapter(TableDto::class.java, TableDtoSerializer())
            .registerTypeHierarchyAdapter(Throwable::class.java, ThrowableSerializer())
            .create()
        contentConverter = GsonWebsocketContentConverter(gson)
    }
    configureSerialization()
    configureRouting()

}
