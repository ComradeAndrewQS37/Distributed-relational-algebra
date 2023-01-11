package shared.expression

import shared.utils.Operation

/**Represents only one (atomic) expression to store expressions in ExpressionHolder**/
class ExpressionDto(
    val operation: Operation,
    // if (left|right) arg is table then (left|right)Table is not null
    // if (left|right) arg is expression then (left|right)Table is not null
    val leftExpr: ExpressionDto? = null,
    val rightExpr: ExpressionDto? = null,
    // instead of tables use their indexes in argsList (list with all tables used in expression) in ExpressionHolder
    val leftTableIndex: Int? = null,
    val rightTableIndex: Int? = null,
)