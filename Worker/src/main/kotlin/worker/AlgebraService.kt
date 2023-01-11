package worker

import shared.table.TableDto

object AlgebraService {

    fun intersect(table1: TableDto, table2: TableDto): TableDto {
        checkForm(table1, table2)

        val newRows: MutableList<List<Any>> = mutableListOf()
        for (row1 in table1.rows) {
            for (row2 in table2.rows) {
                if (row1 == row2) {
                    newRows.add(row1)
                    break
                }
            }
        }
        return TableDto("(${table1.name} & ${table2.name})", table1.columns, newRows)
    }

    fun union(table1: TableDto, table2: TableDto): TableDto {
        checkForm(table1, table2)

        val newRows = table1.rows.union(table2.rows.toSet()).toList()
        return TableDto("(${table1.name} | ${table2.name})", table1.columns, newRows)

    }

    fun difference(table1: TableDto, table2: TableDto): TableDto {
        checkForm(table1, table2)

        val newRows = table1.rows.filterNot { table2.rows.contains(it) }
        return TableDto("(${table1.name} \\ ${table2.name})", table1.columns, newRows)

    }

    fun product(table1: TableDto, table2: TableDto): TableDto {
        val newCols = table1.columns + table2.columns

        val newRows: MutableList<List<Any>> = mutableListOf()
        for (row1 in table1.rows) {
            for (row2 in table2.rows) {
                newRows.add(row1 + row2)
            }
        }
        return TableDto("(${table1.name} x ${table2.name})", newCols, newRows)

    }

    // check that tables have same column domains
    private fun checkForm(table1: TableDto, table2: TableDto) {
        if (table1.columns.size != table2.columns.size) {
            throw IllegalArgumentException("Tables '${table1.name}' and '${table2.name}' must have the same number of columns")
        }
        for (colInd in table1.columns.indices) {
            val col1 = table1.columns[colInd]
            val col2 = table2.columns[colInd]
            if (col1.domain != col2.domain) {
                throw IllegalArgumentException(
                    "Tables '${table1.name}' and '${table2.name}' must have same column types : " +
                            "'${col1.name}' was ${col1.domain} and '${col2.name}' was ${col2.domain}"
                )
            }
        }
    }
}


