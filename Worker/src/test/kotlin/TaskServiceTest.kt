import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import shared.ResultHolder
import shared.TaskHolder
import shared.table.Table
import shared.table.TableDto
import shared.table.TableDtoSerializer
import shared.utils.Operation
import worker.TaskService.processMessage
import kotlin.test.assertEquals

class TaskServiceTest {

    private val gson = GsonBuilder().registerTypeAdapter(TableDto::class.java, TableDtoSerializer()).create()

    @Test
    fun performTaskTest() {
        // trying to process invalid message
        val exceptionalResultHolderJson = processMessage("invalid message")
        val exceptionalResult = gson.fromJson(exceptionalResultHolderJson, ResultHolder::class.java)
        assertEquals(null, exceptionalResult.result)
        assertEquals(false, exceptionalResult.isSuccessful)
        assertEquals("Cannot parse message", exceptionalResult.problem!!.message)

        // create tables
        val t1 = Table("t1") {
            column<Int>("id")
            column<String>("email")
        }.insert {
            (1..100).map { listOf(it, "$it@mail.com") }
        }.asDto()

        val t2 = Table("t2") {
            column<Int>("int_col")
            column<String>("str_col")
        }.insert {
            (30..150).map { listOf(it, "$it@mail.com") }
        }.asDto()

        // expected result of intersection
        val expected = Table("t1 & t2") {
            column<Int>("id")
            column<String>("email")
        }.insert {
            (30..100).map { listOf(it, "$it@mail.com") }
        }.asDto()

        // passing valid message
        val task = TaskHolder(Operation.INTERSECT, t1, t2)
        val taskJson = gson.toJson(task)
        val result = processMessage(taskJson)
        val resultHolder = gson.fromJson(result, ResultHolder::class.java)

        assertEquals(true, resultHolder.isSuccessful)
        assertEquals(null, resultHolder.problem)

        val resultDto = resultHolder.result!!

        assert(areTableRowsEqual(expected, resultDto))

        // table of other form
        val t3 = Table("t3") {
            column<Int>("other_int")
            column<Boolean>("is_working")
        }.insert {
            (15..40).map { listOf(it, it % 2 == 0) }
        }.asDto()

        // passing valid message with illegal operation
        val taskIllegal = TaskHolder(Operation.INTERSECT, t1, t3)
        val taskIllegalJson = gson.toJson(taskIllegal)
        val resultIllegal = processMessage(taskIllegalJson)
        val resultIllegalHolder = gson.fromJson(resultIllegal, ResultHolder::class.java)

        assertEquals(null, resultIllegalHolder.result)
        assertEquals(false, resultIllegalHolder.isSuccessful)
        assertEquals("Tables 't1' and 't3' must have same column types : 'email' was String and 'is_working' was Bool", resultIllegalHolder.problem!!.message)
    }
}