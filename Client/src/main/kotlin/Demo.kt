import client.compute
import kotlinx.coroutines.*
import shared.expression.Expression
import shared.table.Table
import shared.table.Table.Companion.asTable
import shared.utils.Logger
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun runDemo() {
    // starting
    Logger.log("Starting demo...")
    Thread.sleep(1000)
    Logger.log("Initial tables:")

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // initializing tables
    val t1 = Table("users1") {
        column<Int>("id")
        column<String>("email")
        column<LocalDateTime>("hire_date")
    }.insert {
        (1..9).map {
            listOf<Any>(
                it, "$it@mail.com",
                LocalDateTime.parse("2023-01-0$it 00:00:00", formatter)
            )
        }
    }
    println("\n" + t1.name)
    println(t1)

    val t2 = Table("users2") {
        column<Int>("id")
        column<String>("email")
        column<LocalDateTime>("hire_date")
    }.insert {
        (7..25).map {
            listOf<Any>(
                it, "$it@mail.com",
                LocalDateTime.parse("2023-01-${if (it < 10) "0$it" else it} 00:00:00", formatter)
            )
        }
    }
    println("\n" + t2.name)
    println(t2)

    val t3 = Table("users3") {
        column<Int>("id")
        column<String>("email")
        column<LocalDateTime>("hire_date")
    }.insert {
        (20..31).map {
            listOf<Any>(
                it, "$it@mail.com",
                LocalDateTime.parse("2023-01-$it 00:00:00", formatter)
            )
        }
    }
    println("\n" + t3.name)
    println(t3)

    // building expressions
    val expr1 = Expression {
        (t1 intersect t2) intersect (t2 union t3) union (t1 intersect t2 intersect t3) product t2
    }
    val expr2 = Expression {
        (t1 product t2) intersect (t2 product t3)
    }
    val expr3 = Expression {
        (t1 union t2 union t3) intersect t1
    }

    // sending expressions and printing results
    val expressions = listOf(expr1, expr2, expr3)
    runBlocking {
        expressions.mapIndexed { index, expr ->
            launch {
                val res =
                    withContext(Dispatchers.Default) {
                        Logger.log("starting calculating expr${index + 1}")
                        expr.compute().get().asTable()
                    }
                println("\nexpr${index + 1} result: " + res.name)
                println(res)
            }
        }

    }

    // the end
    Logger.log("finished demo")

}