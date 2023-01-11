package worker

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import shared.ResultHolder
import shared.utils.Operation
import shared.TaskHolder
import shared.table.TableDto
import shared.table.TableDtoSerializer
import shared.utils.Logger
import shared.utils.ThrowableSerializer

object TaskService {

    fun processMessage(message: String): String {
        val gson = GsonBuilder()
            .registerTypeAdapter(TableDto::class.java, TableDtoSerializer())
            .registerTypeHierarchyAdapter(Throwable::class.java, ThrowableSerializer())
            .create()
        // try to process message
        try {
            // try to deserialize message
            val taskHolder = try {
                gson.fromJson(message, TaskHolder::class.java)
            } catch (e: Exception) {
                // wrap all possible gson errors
                throw JsonParseException("Cannot parse message", e)
            }

            Logger.log(
                "received task: ${taskHolder.operation.name} for ${taskHolder.arg1?.name}" +
                        " and ${taskHolder.arg2?.name}"
            )

            // perform operation on arguments
            val result = when (taskHolder.operation) {
                Operation.INTERSECT -> AlgebraService.intersect(taskHolder.arg1!!, taskHolder.arg2!!)
                Operation.UNION -> AlgebraService.union(taskHolder.arg1!!, taskHolder.arg2!!)
                Operation.DIFFERENCE -> AlgebraService.difference(taskHolder.arg1!!, taskHolder.arg2!!)
                Operation.PRODUCT -> AlgebraService.product(taskHolder.arg1!!, taskHolder.arg2!!)
                else -> throw IllegalArgumentException("Invalid operation ${taskHolder.operation}")
            }

            // received valid result
            return gson.toJson(ResultHolder(result))

        } catch (e: Exception) {
            // something went wrong during processing the message
            Logger.logError("could not process received message : ${e.message}")
            return gson.toJson(ResultHolder(e))
        }

    }

}