package shared.expression

import shared.utils.Operation
import shared.table.Table
import shared.table.TableDto

class Expression private constructor() {
    /* IMPORTANT:
    Validity of expression is not checked here,
    i.e. one can create expression like (t1 union t2) where t1 and t2 may have different columns
    All checks are made during computations
     */

    var operation: Operation = Operation.NONE
        private set

    // if (left|right) arg is table then (left|right)Table is not null
    // if (left|right) arg is expression then (left|right)Table is not null
    var leftExpr: Expression? = null
        private set
    var rightExpr: Expression? = null
        private set
    var leftTable: Table? = null
        private set
    var rightTable: Table? = null
        private set

    // the only public constructor
    constructor(init: Expression.() -> Expression) : this() {
        val expr = init()
        this.operation = expr.operation
        this.leftExpr = expr.leftExpr
        this.rightExpr = expr.rightExpr
        this.leftTable = expr.leftTable
        this.rightTable = expr.rightTable
    }

    private constructor(operation: Operation, leftExpr: Expression, rightExpr: Expression) : this() {
        this.operation = operation
        this.leftExpr = leftExpr
        this.rightExpr = rightExpr
    }

    private constructor(operation: Operation, leftTable: Table, rightTable: Table) : this() {
        this.operation = operation
        this.leftTable = leftTable
        this.rightTable = rightTable
    }

    private constructor(operation: Operation, leftExpr: Expression, rightTable: Table) : this() {
        this.operation = operation
        this.leftExpr = leftExpr
        this.rightTable = rightTable
    }

    private constructor(operation: Operation, leftTable: Table, rightExpr: Expression) : this() {
        this.operation = operation
        this.leftTable = leftTable
        this.rightExpr = rightExpr
    }

    /*
    table operation table
    expression operation table
    table operation expression
    expression operation expression
     */

    infix fun Table.intersect(table: Table) = Expression(Operation.INTERSECT, this@intersect, table)
    infix fun intersect(table: Table) = Expression(Operation.INTERSECT, this, table)
    infix fun Table.intersect(expr: Expression) = Expression(Operation.INTERSECT, this@intersect, expr)
    infix fun intersect(expr: Expression) = Expression(Operation.INTERSECT, this, expr)

    infix fun Table.union(table: Table) = Expression(Operation.UNION, this@union, table)
    infix fun union(table: Table) = Expression(Operation.UNION, this, table)
    infix fun Table.union(expr: Expression) = Expression(Operation.UNION, this@union, expr)
    infix fun union(expr: Expression) = Expression(Operation.UNION, this, expr)

    infix fun Table.difference(table: Table) = Expression(Operation.DIFFERENCE, this@difference, table)
    infix fun difference(table: Table) = Expression(Operation.DIFFERENCE, this, table)
    infix fun Table.difference(expr: Expression) = Expression(Operation.DIFFERENCE, this@difference, expr)
    infix fun difference(expr: Expression) = Expression(Operation.DIFFERENCE, this, expr)

    infix fun Table.product(table: Table) = Expression(Operation.PRODUCT, this@product, table)
    infix fun product(table: Table) = Expression(Operation.PRODUCT, this, table)
    infix fun Table.product(expr: Expression) = Expression(Operation.PRODUCT, this@product, expr)
    infix fun product(expr: Expression) = Expression(Operation.PRODUCT, this, expr)


    // pack Expression into ExpressionHolder
    fun asExpressionHolder(): ExpressionHolder {
        val argsList = mutableListOf<TableDto>()
        val rootExpr = traverseExprRec(this, argsList)

        return ExpressionHolder(argsList, rootExpr)
    }

    // traverse the "tree" of expression
    private fun traverseExprRec(
        expr: Expression,
        argsList: MutableList<TableDto>,
    ): ExpressionDto {
        // getting components of ExpressionDto
        val leftExprDto = expr.leftExpr?.let { traverseExprRec(it, argsList) }
        val rightExprDto = expr.rightExpr?.let { traverseExprRec(it, argsList) }

        val leftTableDto = expr.leftTable?.asDto()?.let {
            if (it in argsList) {
                argsList.indexOf(it)
            } else {
                argsList.add(it)
                argsList.size - 1
            }
        }
        val rightTableDto = expr.rightTable?.asDto()?.let {
            if (it in argsList) {
                argsList.indexOf(it)
            } else {
                argsList.add(it)
                argsList.size - 1
            }
        }

        return ExpressionDto(expr.operation, leftExprDto, rightExprDto, leftTableDto, rightTableDto)
    }


}