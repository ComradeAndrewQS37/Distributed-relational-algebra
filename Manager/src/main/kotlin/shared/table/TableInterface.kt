package shared.table

interface TableInterface {

    fun insert(values: Iterable<Any>): TableInterface // insert one row
    fun insert(dataProvider: () -> Iterable<Iterable<Any>>): TableInterface // insert multiple rows
    fun insert(vararg values: Any): TableInterface // insert one row

    // projection
    operator fun get(vararg colNames: String): TableInterface

}