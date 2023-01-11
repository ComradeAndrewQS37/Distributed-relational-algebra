package shared.table

import shared.utils.Domain
import java.lang.IllegalArgumentException
import java.time.LocalDateTime


// stores information about column
class Column(var name: String, val domain: Domain) {

    // actually doesn't use column, just checks the type of reified T
    companion object {
        inline fun <reified T> getColumnDomain(): Domain {
            return when (T::class) {
                Int::class -> Domain.Int
                Long::class -> Domain.Long
                Double::class -> Domain.Double
                String::class -> Domain.String
                LocalDateTime::class -> Domain.DateTime
                Boolean::class -> Domain.Bool
                Char::class -> Domain.Char
                else -> throw IllegalArgumentException("Unsupported column type")
            }
        }
    }
}