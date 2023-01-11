import shared.table.TableDto

fun areTableRowsEqual(t1: TableDto, t2: TableDto): Boolean {

    // check columns
    if (t1.columns.size != t2.columns.size) return false
    for (ind in t1.columns.indices) {
        if (t1.columns[ind].name != t2.columns[ind].name) return false
        if (t1.columns[ind].domain != t2.columns[ind].domain) return false
    }

    // check data
    if (t1.rows.size != t2.rows.size) return false
    for (rowInd in t1.rows.indices) {
        if (t1.rows[rowInd].size != t2.rows[rowInd].size) return false
        for (elemInd in t1.rows[rowInd].indices) {
            if (t1.rows[rowInd][elemInd] != t2.rows[rowInd][elemInd]) return false
        }
    }

    return true
}