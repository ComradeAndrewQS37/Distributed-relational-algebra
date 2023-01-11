package shared.table


data class TableDto(
    val name: String,
    val columns: List<Column>,
    val rows: List<List<Any>>
)
