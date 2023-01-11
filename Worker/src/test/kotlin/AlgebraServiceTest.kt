import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import shared.table.Table
import worker.AlgebraService.difference
import worker.AlgebraService.intersect
import worker.AlgebraService.product
import worker.AlgebraService.union
import kotlin.test.assertEquals

class AlgebraServiceTest {

    @Test
    fun intersectTest() {
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

        assert(areTableRowsEqual(expected, intersect(t1, t2)))

        // tables of other form
        val t3 = Table("t3") {
            column<Int>("other_int")
            column<Boolean>("is_working")
        }.insert {
            (15..40).map { listOf(it, it % 2 == 0) }
        }.asDto()

        val e1 = assertThrows<IllegalArgumentException> { intersect(t1, t3) }
        assertEquals(
            "Tables 't1' and 't3' must have same column types : 'email' was String and 'is_working' was Bool",
            e1.message
        )

        val t4 = Table("t4") {
            column<Int>("other_int")
        }.insert {
            (15..40).map { listOf(it) }
        }.asDto()

        val e2 = assertThrows<IllegalArgumentException> { intersect(t2, t4) }
        assertEquals("Tables 't2' and 't4' must have the same number of columns", e2.message)
    }

    @Test
    fun unionTest() {
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

        // expected result of union
        val expected = Table("t1 | t2") {
            column<Int>("id")
            column<String>("email")
        }.insert {
            (1..150).map { listOf(it, "$it@mail.com") }
        }.asDto()

        assert(areTableRowsEqual(expected, union(t1, t2)))

        // tables of other form
        val t3 = Table("t3") {
            column<Int>("other_int")
            column<Boolean>("is_working")
        }.insert {
            (15..40).map { listOf(it, it % 2 == 0) }
        }.asDto()

        val e1 = assertThrows<IllegalArgumentException> { union(t1, t3) }
        assertEquals(
            "Tables 't1' and 't3' must have same column types : 'email' was String and 'is_working' was Bool",
            e1.message
        )

        val t4 = Table("t4") {
            column<Int>("other_int")
        }.insert {
            (15..40).map { listOf(it) }
        }.asDto()

        val e2 = assertThrows<IllegalArgumentException> { union(t2, t4) }
        assertEquals("Tables 't2' and 't4' must have the same number of columns", e2.message)
    }

    @Test
    fun differenceTest() {
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

        // expected result of difference
        val expected = Table("t1 \\ t2") {
            column<Int>("id")
            column<String>("email")
        }.insert {
            (1..29).map { listOf(it, "$it@mail.com") }
        }.asDto()

        assert(areTableRowsEqual(expected, difference(t1, t2)))

        // tables of other form
        val t3 = Table("t3") {
            column<Int>("other_int")
            column<Boolean>("is_working")
        }.insert {
            (15..40).map { listOf(it, it % 2 == 0) }
        }.asDto()

        val e1 = assertThrows<IllegalArgumentException> { difference(t1, t3) }
        assertEquals(
            "Tables 't1' and 't3' must have same column types : 'email' was String and 'is_working' was Bool",
            e1.message
        )

        val t4 = Table("t4") {
            column<Int>("other_int")
        }.insert {
            (15..40).map { listOf(it) }
        }.asDto()

        val e2 = assertThrows<IllegalArgumentException> { difference(t2, t4) }
        assertEquals("Tables 't2' and 't4' must have the same number of columns", e2.message)
    }

    @Test
    fun productTest() {
        // create tables
        val t1 = Table("t1") {
            column<Int>("id")
            column<String>("name")
        }.insert {
            (1..10).map { listOf(it, "person#$it") }
        }.asDto()
        val t2 = Table("t2") {
            column<String>("job")
            column<Boolean>("is_ok")
        }.insert {
            (5..17).map { listOf("job#$it", it % 2 == 0) }
        }.asDto()

        // expected product result
        val expected = Table("t1 x t2") {
            column<Int>("id")
            column<String>("name")
            column<String>("job")
            column<Boolean>("is_ok")
        }.insert {
            val res = mutableListOf<List<Any>>()
            for (i in 1..10) {
                for (j in 5..17) {
                    res.add(listOf(i, "person#$i", "job#$j", j % 2 == 0))
                }
            }
            res
        }.asDto()

        assert(areTableRowsEqual(expected, product(t1, t2)))

    }
}