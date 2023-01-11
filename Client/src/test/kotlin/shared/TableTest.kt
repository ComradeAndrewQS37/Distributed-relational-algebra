package shared

import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import shared.table.Column
import shared.table.Table
import shared.table.TableDto
import shared.table.TableDtoSerializer
import shared.utils.Domain
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


class TableTest {

    @Test
    fun columnTest() {
        // getColumnDomain<T> test
        assertEquals(Domain.Int, Column.getColumnDomain<Int>())
        assertEquals(Domain.DateTime, Column.getColumnDomain<LocalDateTime>())
        assertThrows<IllegalArgumentException> { Column.getColumnDomain<Column>() }

        // column creation test
        val col = Column("id", Column.getColumnDomain<Int>())
        assertEquals("id", col.name)
        assertEquals(Domain.Int, col.domain)
    }

    @Test
    fun tableCreationTest() {
        val t1 = Table("t1") {
            column<Int>("id")
            column<String>("name")
        }

        // check name
        assertEquals("t1", t1.name)
        // check empty table rows
        assertEquals(listOf<List<Any>>(), t1.getRows())

        // check columns
        val t1Dto = t1.asDto()// convert to dto to access private fields
        assertEquals("id", t1Dto.columns[0].name)
        assertEquals(Domain.Int, t1Dto.columns[0].domain)
        assertEquals("name", t1Dto.columns[1].name)
        assertEquals(Domain.String, t1Dto.columns[1].domain)
        assertEquals(listOf<List<Any>>(), t1.getRows())

        // check that cannot add columns after initialisation
        assertThrows<UnsupportedOperationException> { t1.column<Int>("fake_id") }
        assertThrows<UnsupportedOperationException> { t1.addColumn(Column("fake_name", Domain.String)) }

        // check that can use only allowed column types
        assertThrows<IllegalArgumentException> {
            Table("name") {
                column<List<Int>>("unavailable_column")
            }
        }
    }

    @Test
    fun tableInsertTest() {
        val t1 = Table("t1") {
            column<Int>("id")
            column<String>("name")
        }
        // size before insertion
        assertEquals(0, t1.getRows().size)

        // insert one row via vararg
        t1.insert(1, "John")
        assertEquals(1, t1.getRows().size)
        assertContentEquals(listOf<Any>(1, "John"), t1.getRows()[0])

        // insert one row via passing list
        t1.insert(listOf(2, "Sally"))
        assertEquals(2, t1.getRows().size)
        assertContentEquals(listOf<Any>(1, "John"), t1.getRows()[0])
        assertContentEquals(listOf<Any>(2, "Sally"), t1.getRows()[1])

        // insert via lambda dataProvider
        val t2 = Table("t2") {
            column<Int>("id")
            column<String>("email")
        }
        assertEquals(0, t2.getRows().size)

        t2.insert {
            (1..10).map { listOf<Any>(it, "example$it@test.com") }
        }
        assertEquals(10, t2.getRows().size)
        for (i in 1..10) {
            assertContentEquals(listOf(i, "example$i@test.com"), t2.getRows()[i - 1])
        }

        // being a bad user
        val e1 = assertThrows<IllegalArgumentException> { t1.insert() }
        assertEquals("Not enough insert values : 2 expected", e1.message)

        val e2 = assertThrows<IllegalArgumentException> {
            t1.insert(3, "Michael", true)
        }
        assertEquals("Too many insert values : 2 expected", e2.message)

        val e3 = assertThrows<IllegalArgumentException> {
            t1.insert("Michael", 3)
        }
        assertEquals("Invalid insert value type: Int required", e3.message)

        val e4 = assertThrows<IllegalArgumentException> {
            t1.insert(3, false)
        }
        assertEquals("Invalid insert value type: String required", e4.message)

        val e5 = assertThrows<IllegalArgumentException> {
            t2.insert {
                (11..20).map { listOf<Any>(it, "example$it@test.com", it + 10) }
            }
        }
        assertEquals("Too many insert values : 2 expected", e5.message)

        val e6 = assertThrows<IllegalArgumentException> {
            t2.insert {
                (11..20).map { listOf<Any>("$it", "example$it@test.com") }
            }
        }
        assertEquals("Invalid insert value type: Int required", e6.message)

    }

    @Test
    fun tableProjectionTest() {
        val t1 = Table("t1") {
            column<Int>("id")
            column<String>("email")
            column<Boolean>("is_working")
        }
        t1.insert {
            (1..15).map { listOf<Any>(it, "example$it@test.com", it % 2 == 0) }
        }

        // take only one column
        val t2 = t1["id"]
        assertEquals(1, t2.asDto().columns.size)
        assertEquals("id", t2.asDto().columns[0].name)
        assertEquals(Domain.Int, t2.asDto().columns[0].domain)
        assertEquals(15, t2.getRows().size)
        assertContentEquals(List<List<Any>>(15) { listOf(it + 1) }, t2.getRows())

        // take several columns (that can repeat)
        val t3 = t1["is_working", "email", "email"]
        val t3Dto = t3.asDto()
        assertEquals(3, t3Dto.columns.size)
        assertEquals("is_working", t3Dto.columns[0].name)
        assertEquals(Domain.Bool, t3Dto.columns[0].domain)
        assertEquals("email", t3Dto.columns[1].name)
        assertEquals(Domain.String, t3Dto.columns[1].domain)
        assertEquals("email", t3Dto.columns[2].name)
        assertEquals(Domain.String, t3Dto.columns[2].domain)
        assertEquals(15, t3.getRows().size)
        assertContentEquals(
            List<List<Any>>(15) { listOf((it + 1) % 2 == 0, "example${it + 1}@test.com", "example${it + 1}@test.com") },
            t3.getRows()
        )

        // take columns that do not exist
        val e = assertThrows<IllegalArgumentException> {
            t1["id", "fake_column"]
        }
        assertEquals("No column with name 'fake_column' found in table 't1'", e.message)
    }

    @Test
    fun tableDtoTest() {
        // test that we can convert to dto being secure that no one changes the original table data
        val t1 = Table("t1") {
            column<Int>("id")
            column<String>("email")
            column<LocalDateTime>("birth_date")
        }
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        t1.insert {
            (1..15).map {
                listOf<Any>(
                    it, "example$it@test.com", LocalDateTime.parse(
                        "2000-01-03 00:00:00", formatter
                    )
                )
            }
        }

        // trying to change original columns through dto
        val t1Dto = t1.asDto()
        t1Dto.columns[0].name = "not_id"
        assertEquals("id", t1.asDto().columns[0].name)

        // trying to change original rows data through dto
        (t1Dto.rows[0][2] as LocalDateTime).minusDays(1)
        assertEquals(
            LocalDateTime.parse(
                "2000-01-03 00:00:00", formatter
            ), t1.getRows()[0][2]
        )

    }

    @Test
    fun serializerTest() {
        // create table
        val t1 = Table("t1") {
            column<Int>("numeric")
            column<String>("string")
            column<LocalDateTime>("time")
            column<Boolean>("boolean")
            column<Char>("char")
        }

        // insert values
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val insValues = listOf<List<Any>>(
            listOf(1, "One", LocalDateTime.parse("2000-01-01 00:00:00", formatter), true, 'a'),
            listOf(2, "Two", LocalDateTime.parse("2000-02-02 00:00:00", formatter), true, 'b'),
            listOf(3, "Three", LocalDateTime.parse("2000-03-03 00:00:00", formatter), false, 'c'),
            listOf(4, "Four", LocalDateTime.parse("2000-04-04 00:00:00", formatter), false, 'd'),
            listOf(5, "Five", LocalDateTime.parse("2000-05-05 00:00:00", formatter), true, 'e')
        )
        t1.insert { insValues }

        // serialize
        val gson = GsonBuilder().registerTypeAdapter(TableDto::class.java, TableDtoSerializer()).create()
        val t1Dto = t1.asDto()
        val t1Json = gson.toJson(t1Dto)

        // deserialize
        val t2Dto = gson.fromJson(t1Json, TableDto::class.java)

        // assert tables equal
        assertEquals(t1Dto.name, t2Dto.name)

        assertEquals(t1Dto.columns.size, t2Dto.columns.size)
        for (ind in t1Dto.columns.indices) {
            val col1 = t1Dto.columns[ind]
            val col2 = t2Dto.columns[ind]
            assertEquals(col1.name, col2.name)
            assertEquals(col1.domain, col2.domain)
        }

        assertEquals(t1Dto.rows.size, t2Dto.rows.size)
        for (rowInd in t1Dto.rows.indices) {
            assertEquals(t1Dto.rows[rowInd], t2Dto.rows[rowInd])
        }

    }
}