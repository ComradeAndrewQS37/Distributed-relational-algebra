package shared.expression

import shared.table.TableDto

/**Represents one big expression with [rootExpr] as entry point.
 * Used to send expression to manager**/
class ExpressionHolder(
    // list of all (regular) tables used in expression
    val argsList: List<TableDto>,
    val rootExpr: ExpressionDto
)