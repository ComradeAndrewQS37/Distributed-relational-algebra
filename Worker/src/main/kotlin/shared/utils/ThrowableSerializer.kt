package shared.utils

import com.google.gson.*
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.javaType

class ThrowableSerializer : JsonSerializer<Throwable>, JsonDeserializer<Throwable> {
    override fun serialize(throwable: Throwable, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
        val json = JsonObject()

        json.add("class", JsonPrimitive(throwable::class.qualifiedName))
        if (throwable.message != null) {
            json.add("message", JsonPrimitive(throwable.message))
        }
        if (throwable.cause != throwable && throwable.cause != null) {
            json.add("cause", context.serialize(throwable.cause))
        }


        return json
    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): Throwable {
        val jsonObj = json.asJsonObject

        // try to get class of this exception
        val throwableClassName = jsonObj.get("class")?.asString
            ?: throw JsonParseException("Cannot deserialize throwable : json had no 'class' property")

        val throwableClass = try {
            Class.forName(throwableClassName)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException("Cannot deserialize : '$throwableClassName' class not found", e)
        }.kotlin


        // fetch message and cause
        val message = jsonObj.get("message")?.asString

        val cause = jsonObj.get("cause")?.let {
            context.deserialize<Throwable>(it, Throwable::class.java)
        }

        // create instance of this exception
        val result = try {
            constructThrowable(throwableClass, message, cause)
        } catch (e: RuntimeException) {
            throw JsonParseException("Cannot deserialize throwable", e)
        }

        return result
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun constructThrowable(cl: KClass<*>, message: String?, cause: Throwable?): Throwable {
        // try to use constructor with message and cause
        if (message != null && cause != null) {
            val twoArgsConstructor = cl.constructors.find {
                val params = it.parameters
                if (params.size != 2) return@find false
                if (params[0].type.javaType != String::class.java) return@find false
                if (params[1].type.javaType != Throwable::class.java) return@find false
                true
            }
            twoArgsConstructor?.let { return it.call(message, cause) as Throwable }
        }

        // try to find constructor with at least message or cause
        if (message != null) {
            val messageArgConstructor = cl.constructors.find {
                val params = it.parameters
                if (params.size != 1) return@find false
                if (params[0].type.javaType != String::class.java) return@find false
                true
            }
            messageArgConstructor?.let { return it.call(message) as Throwable }
        }
        if (cause != null) {
            val causeArgConstructor = cl.constructors.find {
                val params = it.parameters
                if (params.size != 1) return@find false
                if (params[0].type.javaType != Throwable::class.java) return@find false
                true
            }
            causeArgConstructor?.let { return it.call(cause) as Throwable }
        }

        // use no-arg constructor if nothing else worked
        val noArgConstructor = cl.constructors.find { it.parameters.isEmpty() }
        noArgConstructor?.let { return it.call() as Throwable }

        // if no constructors were found before
        throw RuntimeException("Cannot construct throwable")
    }


}