package shared.table

import com.google.gson.*
import shared.utils.Domain
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TableDtoSerializer : JsonSerializer<TableDto>, JsonDeserializer<TableDto> {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun serialize(table: TableDto, typeOfSrc: Type?, context: JsonSerializationContext): JsonElement {
        val json = JsonObject()

        // add name
        json.add("name", JsonPrimitive(table.name))

        // add columns
        val columnsJsonArray = JsonArray()
        for (col in table.columns) {
            columnsJsonArray.add(context.serialize(col))
        }
        json.add("columns", columnsJsonArray)

        // add rows
        val rowsJsonArray = JsonArray()
        for (row in table.rows) {
            val rowJsonArray = JsonArray()
            for (elemIndex in row.indices) {
                val elem = row[elemIndex]
                try {
                    val toAdd = when (table.columns[elemIndex].domain) {
                        Domain.Int -> JsonPrimitive(elem as Int)
                        Domain.Long -> JsonPrimitive(elem as Long)
                        Domain.Double -> JsonPrimitive(elem as Double)
                        Domain.String -> JsonPrimitive(elem as String)
                        Domain.DateTime -> JsonPrimitive((elem as LocalDateTime).format(dateFormatter))
                        Domain.Bool -> JsonPrimitive(elem as Boolean)
                        Domain.Char -> JsonPrimitive(elem as Char)
                    }
                    rowJsonArray.add(toAdd)

                } catch (cause: IndexOutOfBoundsException) {
                    // table cannot be in such state if everything is ok
                    throw IllegalStateException(
                        "Cannot serialise table '${table.name}': " +
                                "invalid number of columns (${table.columns.size}) or ${table.rows.indexOf(row)} row length(${row.size})",
                        cause
                    )
                } catch (cause: ClassCastException) {
                    // someone inserted illegal data
                    throw IllegalStateException(
                        "Cannot serialise '${table.name}': " +
                                "illegal object at ${table.rows.indexOf(row)} row at $elemIndex position",
                        cause
                    )
                }
            }
            rowsJsonArray.add(rowJsonArray)
        }
        json.add("rows", rowsJsonArray)

        return json

    }

    override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext): TableDto {
        val jsonObj = json.asJsonObject

        val tableName = jsonObj.get("name")?.asString
            ?: throw JsonParseException("Cannot deserialize table : json had no 'name' property")

        // fetching columns
        val tableColumns = mutableListOf<Column>()
        val columnsJson = jsonObj.get("columns")?.asJsonArray
            ?: throw JsonParseException("Cannot deserialize table $tableName : json had no 'columns' property")
        for (col in columnsJson) {
            try {
                tableColumns.add(context.deserialize(col, Column::class.java))
            } catch (cause: JsonParseException) {
                throw JsonParseException(
                    "Cannot deserialize table $tableName : " +
                            "error while parsing ${columnsJson.indexOf(col)} column", cause
                )
            }
        }

        // fetching rows
        val tableRows = mutableListOf<List<Any>>()
        val rowsJson = jsonObj.get("rows")?.asJsonArray
            ?: throw JsonParseException("Cannot deserialize table $tableName : json had no 'rows' property")

        for (rowJson in rowsJson) {
            val tableRow = mutableListOf<Any>()
            val row = rowJson.asJsonArray
            if (row.size() != tableColumns.size) {
                throw JsonParseException(
                    "Cannot deserialize table $tableName : " +
                            "row ${rowsJson.indexOf(rowJson)} had length ${row.size()}, while ${tableColumns.size} expected"
                )
            }
            for (ind in tableColumns.indices) {
                val elem = row[ind]
                try {
                    when (tableColumns[ind].domain) {
                        Domain.Int -> tableRow.add(elem.asInt)
                        Domain.Long -> tableRow.add(elem.asLong)
                        Domain.Double -> tableRow.add(elem.asDouble)
                        Domain.String -> tableRow.add(elem.asString)
                        Domain.DateTime -> tableRow.add(LocalDateTime.parse(elem.asString, dateFormatter))
                        Domain.Bool -> tableRow.add(elem.asBoolean)
                        Domain.Char -> tableRow.add(elem.asString[0])
                    }
                } catch (cause: UnsupportedOperationException) {
                    throw JsonParseException(
                        "Cannot deserialize table $tableName : " +
                                "row ${rowsJson.indexOf(rowJson)} had illegal object at position $ind", cause
                    )
                } catch (cause: IllegalStateException) {
                    throw JsonParseException(
                        "Cannot deserialize table $tableName : " +
                                "row ${rowsJson.indexOf(rowJson)} had illegal object at position $ind", cause
                    )
                } catch (cause: NumberFormatException) {
                    throw JsonParseException(
                        "Cannot deserialize table $tableName : " +
                                "cannot parse number at row ${rowsJson.indexOf(rowJson)} at position $ind", cause
                    )
                }
            }
            tableRows.add(tableRow)
        }

        return TableDto(tableName, tableColumns, tableRows)
    }

}