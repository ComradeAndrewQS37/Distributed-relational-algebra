package shared.table

import de.m3y.kformat.table
import shared.utils.Domain
import java.time.LocalDateTime


class Table(var name: String, init: Table.() -> Unit = {}) : TableInterface {

    private var columns: MutableList<Column> = mutableListOf()
    private var rows: MutableList<List<Any>> = mutableListOf()

    private var isInitialised = false

    init {
        init()
        isInitialised = true
    }

    private constructor(name: String, columns: MutableList<Column>, rows: MutableList<List<Any>>) : this(name) {
        this.columns = columns
        this.rows = rows

    }

    fun getRows(): List<List<Any>> = rows.map { it.toList() }

    /**Adds new column of [T] type to `this` table and executes [init] on new column.
     * Available only during initialisation in table init lambda block
     * @throws UnsupportedOperationException when called after initialisation**/
    inline fun <reified T> column(name: String, init: Column.() -> Unit = {}) {
        val col = Column(name, Column.getColumnDomain<T>())
        col.init()
        addColumn(col)
    }

    /**Adds new column [col] to `this` table.
     * Available only during initialisation in table init lambda block
     * @throws UnsupportedOperationException when called after initialisation**/
    fun addColumn(col: Column) {
        if (!isInitialised) {
            columns.add(col)
        } else {
            throw UnsupportedOperationException("Cannot add column: table $name is already initialised")
        }
    }

    /**Inserts one row of [values].
     * @throws IllegalArgumentException if [values] do not match with Table columns**/
    override fun insert(values: Iterable<Any>): Table {
        val expectedTypes = columns.map { it.domain }
        val typesIter = expectedTypes.iterator()
        val newRow = mutableListOf<Any>()

        for (elem in values) {
            if (!typesIter.hasNext()) {
                // too many values passed
                throw IllegalArgumentException("Too many insert values : ${columns.size} expected")
            }
            val nextType = typesIter.next()
            val isValidType =
                when (nextType) {
                    Domain.Int -> elem is Int
                    Domain.Long -> elem is Long
                    Domain.Double -> elem is Double
                    Domain.String -> elem is String
                    Domain.DateTime -> elem is LocalDateTime
                    Domain.Bool -> elem is Boolean
                    Domain.Char -> elem is Char
                }
            if (isValidType) {
                newRow.add(elem)
            } else {
                throw IllegalArgumentException("Invalid insert value type: $nextType required")
            }
        }
        if (typesIter.hasNext()) {
            throw IllegalArgumentException("Not enough insert values : ${columns.size} expected")
        }
        rows.add(newRow)

        return this
    }

    /**Inserts all rows, produced by [dataProvider].
     * @throws IllegalArgumentException if produced values do not match with Table columns**/
    override fun insert(dataProvider: () -> Iterable<Iterable<Any>>): Table {
        val data = dataProvider()
        for (row in data) {
            this.insert(row)
        }
        return this
    }

    /**Inserts one row of [values].
     * @throws IllegalArgumentException if [values] do not match with Table columns**/
    override fun insert(vararg values: Any): Table {
        val valuesList = values.asList()
        return this.insert(valuesList)
    }

    // projection
    override fun get(vararg colNames: String): Table {
        val resultColumns: MutableList<Column> = mutableListOf()
        val resultColumnsIndices: MutableList<Int> = mutableListOf()
        for (col in colNames) {
            columns.find { it.name == col }
                ?.also { resultColumns.add(Column(it.name, it.domain)) }
                ?.also { resultColumnsIndices.add(columns.indexOf(it)) }
                ?: throw IllegalArgumentException("No column with name \'$col\' found in table \'$name\'")
        }

        val resultData = rows.map { oldRow ->
            val newRow = mutableListOf<Any>()
            for (ind in resultColumnsIndices) {
                newRow.add(oldRow[ind])
            }
            newRow.toList()
        }.toMutableList()

        return Table("?$name?", resultColumns, resultData)
    }

    override fun toString(): String {
        // de.m3y.kformat not working when table has 0 rows
        if (rows.size == 0) {
            var res = ""
            for (ind in columns.indices) {
                res += columns[ind].name + " "
                if (ind != columns.size - 1) {
                    res += "|"
                }
            }
            res += "\n" + "-".repeat(res.length) + "\n(0 rows)"
            return res
        }

        var res = table {
            val columnsNames = columns.map { it.name }
            header(columnsNames)

            for (r in this@Table.rows.take(10)) {
                row(*r.toTypedArray())
            }

            hints {
                borderStyle = de.m3y.kformat.Table.BorderStyle.SINGLE_LINE
            }
        }.render(StringBuilder()).toString()

        val rowsNum = rows.size
        if (rowsNum > 10) {
            res += "..."
        }
        return "$res\n($rowsNum rows)"
    }


    fun asDto(): TableDto {
        val dtoColumns = columns.map { Column(it.name, it.domain) }
        val dtoRows = rows.map { it.toList() }

        return TableDto(name, dtoColumns, dtoRows)
    }

    companion object {
        // placed here so TableDto doesn't know about Table (and we can use private constructor)
        fun TableDto.asTable(): Table = Table(name, columns.toMutableList(), rows.toMutableList())
    }

}