package shared

import org.junit.jupiter.api.Test
import shared.expression.Expression
import shared.table.Table
import shared.table.TableDto
import shared.utils.Operation
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ExpressionTest {

    private fun areTableDtosEqual(t1 : TableDto, t2 : TableDto) : Boolean{
        // check names
        if(t1.name != t2.name) return false

        // check columns
        if(t1.columns.size != t2.columns.size) return false
        for (ind in t1.columns.indices){
            if(t1.columns[ind].name != t2.columns[ind].name) return false
            if(t1.columns[ind].domain != t2.columns[ind].domain) return false
        }

        // check data
        if(t1.rows.size != t2.rows.size) return false
        for (rowInd in t1.rows.indices){
            if(t1.rows[rowInd].size != t2.rows[rowInd].size) return false
            for (elemInd in t1.rows[rowInd].indices){
                if(t1.rows[rowInd][elemInd] != t2.rows[rowInd][elemInd]) return false
            }
        }

        return true
    }

    @Test
    fun expressionConstructTest() {
        // create two tables for test
        val t1 = Table("t1") {
            column<Int>("int_col")
            column<String>("str_col")
        }.insert {
            (1..100).map { listOf(it, "string#$it") }
        }

        val t2 = Table("t2") {
            column<Int>("int_col")
            column<String>("str_col")
        }.insert {
            (30..150).map { listOf(it, "string#$it") }
        }

        // create test expression
        val expr = Expression {
            t2 product ((t1 intersect t2) union t1)
        }

        // check expression structure
        var currExpr = expr
        assertEquals(Operation.PRODUCT, currExpr.operation)
        assertEquals(null, currExpr.leftExpr)
        assertEquals(t2, currExpr.leftTable)
        assertNotEquals(null, currExpr.rightExpr)
        assertEquals(null, currExpr.rightTable)

        currExpr = currExpr.rightExpr!!
        assertEquals(Operation.UNION, currExpr.operation)
        assertNotEquals(null, currExpr.leftExpr)
        assertEquals(null, currExpr.leftTable)
        assertEquals(null, currExpr.rightExpr)
        assertEquals(t1, currExpr.rightTable)

        currExpr = currExpr.leftExpr!!
        assertEquals(Operation.INTERSECT, currExpr.operation)
        assertEquals(null, currExpr.leftExpr)
        assertEquals(t1, currExpr.leftTable)
        assertEquals(null, currExpr.rightExpr)
        assertEquals(t2, currExpr.rightTable)
    }

    fun expressionHolderTest(){
        // create two tables for test
        val t1 = Table("t1") {
            column<Int>("int_col")
            column<String>("str_col")
        }.insert {
            (1..100).map { listOf(it, "string#$it") }
        }

        val t2 = Table("t2") {
            column<Int>("int_col")
            column<String>("str_col")
        }.insert {
            (30..150).map { listOf(it, "string#$it") }
        }

        // create test expression
        val expr = Expression {
            t2 product ((t1 intersect t2) union t1)
        }

        // turn into ExpressionHolder
        val holder = expr.asExpressionHolder()

        // check args list (tables are added to argsList according to their appearance
        // during traversing the expression 'tree' starting from root
        assertEquals(2, holder.argsList.size)
        assert(areTableDtosEqual(t2.asDto(), holder.argsList[0]))
        assert(areTableDtosEqual(t1.asDto(), holder.argsList[1]))

        // check that expression structure is preserved after turning into ExpressionHolder
        var currExpr = holder.rootExpr
        assertEquals(Operation.PRODUCT, currExpr.operation)
        assertEquals(null, currExpr.leftExpr)
        assert(areTableDtosEqual(t2.asDto(), holder.argsList[currExpr.leftTableIndex!!]))
        assertNotEquals(null, currExpr.rightExpr)
        assertEquals(null, currExpr.rightTableIndex)

        currExpr = currExpr.rightExpr!!
        assertEquals(Operation.UNION, currExpr.operation)
        assertNotEquals(null, currExpr.leftExpr)
        assertEquals(null, currExpr.leftTableIndex)
        assertEquals(null, currExpr.rightExpr)
        assert(areTableDtosEqual(t1.asDto(), holder.argsList[currExpr.rightTableIndex!!]))

        currExpr = currExpr.leftExpr!!
        assertEquals(Operation.INTERSECT, currExpr.operation)
        assertEquals(null, currExpr.leftExpr)
        assert(areTableDtosEqual(t1.asDto(), holder.argsList[currExpr.leftTableIndex!!]))
        assertEquals(null, currExpr.rightExpr)
        assert(areTableDtosEqual(t2.asDto(), holder.argsList[currExpr.rightTableIndex!!]))
    }
}