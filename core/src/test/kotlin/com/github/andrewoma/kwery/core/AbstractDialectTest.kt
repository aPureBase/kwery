/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.kwery.core

import com.github.andrewoma.kwery.core.dialect.*
import com.zaxxer.hikari.pool.ProxyConnection
import org.junit.Test
import org.postgresql.largeobject.LargeObjectManager
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import javax.sql.DataSource
import kotlin.test.assertEquals

abstract class AbstractDialectTest(dataSource: DataSource, dialect: Dialect) : AbstractSessionTest(dataSource, dialect) {
    abstract val sql: String
    val nullLimit = if (dialect is MysqlDialect || dialect is SqliteDialect) Int.MAX_VALUE else null

    override fun afterSessionSetup() {
        initialise(dialect::class.java.name) {

            for (statement in sql.split(";".toRegex())) {
                session.update(statement)
            }
        }
        session.update("delete from dialect_test")
        session.update("delete from test")
    }

    data class Value(
        val time: Time,
        val date: Date,
        val timestamp: Timestamp,
        val binary: String,
        val varchar: String,
        val blob: String,
        val clob: String,
        val ints: List<Int>
    )

    @Test fun `Array based select should work inlined`() {
        if (!dialect.supportsArrayBasedIn) return

        for (id in listOf("a", "b", "c", "d")) {
            session.update("insert into test(id) values(:id)", mapOf("id" to id))
        }

        val sql = "select id from test where id " + dialect.arrayBasedIn("ids")
        val ids = setOf("b", "c")
        val idsArray = session.connection.createArrayOf("varchar", ids.toTypedArray())
        val bound = session.bindParameters(sql, mapOf("ids" to idsArray))

        val actual = session.select(bound) { row ->
            row.string("id")
        }
        assertEquals(ids, actual.toSet())
    }

    @Test fun `Bindings to blobs and clobs via streams`() {
        if (
            session.dialect is PostgresDialect ||
            session.dialect is SqliteDialect ||
            session.dialect is SqlServerDialect
        ) return

        val now = System.currentTimeMillis()
        val value = Value(Time(now), Date(now), Timestamp(now), "binary",
            "var'char", "blob", "clob", listOf(1, 2, 3))

        val params = createParams(value) + mapOf(
            "id" to "streams",
            "blob_col" to ByteArrayInputStream(value.blob.toByteArray(Charsets.UTF_8)),
            "clob_col" to StringReader(value.clob)
        )

        assertEquals(1, session.update(insertSql, params))

        val (blobStream, clobStream) = session.select("select blob_col, clob_col from dialect_test") { row ->
            row.binaryStream("blob_col") to row.characterStream("clob_col")
        }.single()

        assertEquals(value.blob, String(blobStream.readBytes()))
        assertEquals(value.clob, clobStream.readText())
    }

    //language=SQL
    val insertSql = """
            insert into dialect_test (id, time_col, date_col, timestamp_col, binary_col, varchar_col, blob_col, clob_col, array_col)
            values (:id, :time_col, :date_col, :timestamp_col, :binary_col, :varchar_col, :blob_col, :clob_col, :array_col)
        """

    @Test fun `Bindings to literals should return the same values when fetched`() {
        val now = System.currentTimeMillis()
        val value = Value(Time(now), Date(now), Timestamp(now), "binary",
            "var'char", "blob", "clob", listOf(1, 2, 3))

        session.update(insertSql, createParams(value) + mapOf("id" to "params"))

        val literalSql = session.bindParameters(insertSql, createParams(value) + mapOf("id" to "literal"))
        session.update(literalSql, mapOf())

        val byParams = findById("params")
        val byLiteral = findById("literal")

        assertEqualValues(byParams, value)
        assertEqualValues(byLiteral, value)
    }

    private fun createParams(value: Value) = mapOf(
        "time_col" to value.time,
        "date_col" to value.date,
        "timestamp_col" to value.timestamp,
        "binary_col" to value.binary.toByteArray(Charsets.UTF_8),
        "varchar_col" to value.varchar,
        "blob_col" to toBlob(value.blob),
        "clob_col" to toClob(value.clob),
        "array_col" to if (dialect is MysqlDialect || dialect is SqlServerDialect) "" else session.connection.createArrayOf("int", value.ints.toTypedArray())
    )

    @Test fun `Allocate ids should contain a unique sequence of ids`() {
        if (!dialect.supportsAllocateIds) return

        val count = 100
        val iterations = 5

        val all = hashSetOf<Long>()
        repeat(iterations) {
            val ids = session.select(dialect.allocateIds(count, "test_seq", "id")) { row ->
                row.long("id")
            }

            assertEquals(count, ids.size)
            all.addAll(ids)
        }

        // Check ids are unique across invocations
        assertEquals(count * iterations, all.size)
    }

    @Test open fun `Should apply limit and offset`() {
        for (id in listOf("1", "2", "3", "4")) {
            session.update("insert into test(id) values(:id)", mapOf("id" to id))
        }

        fun assertLimitAndOffset(limit: Int?, offset: Int?, expected: List<String>) {
            val options = session.defaultOptions.copy(limit = limit, offset = offset)
            val actual = session.select("select * from test order by id", mapOf(), options) { row ->
                row.string("id")
            }
            assertEquals(expected, actual)
        }

        assertLimitAndOffset(null, null, listOf("1", "2", "3", "4"))
        assertLimitAndOffset(2, null, listOf("1", "2"))
        assertLimitAndOffset(3, null, listOf("1", "2", "3"))
        assertLimitAndOffset(nullLimit, 2, listOf("3", "4"))
        assertLimitAndOffset(nullLimit, 3, listOf("4"))
        assertLimitAndOffset(2, 1, listOf("2", "3"))
        assertLimitAndOffset(2, 2, listOf("3", "4"))
    }

    @Test open fun `Should apply limit and offset with parameters`() {
        for (id in listOf("1", "2", "3", "4", "5", "6")) {
            session.update("insert into test(id) values(:id)", mapOf("id" to id))
        }

        fun assertLimitAndOffset(limit: Int?, offset: Int?, expected: List<String>) {
            val options = session.defaultOptions.copy(limit = limit, offset = offset)
            val actual = session.select("select * from test where id > :id order by id", mapOf("id" to "2"), options) { row ->
                row.string("id")
            }
            assertEquals(expected, actual)
        }

        assertLimitAndOffset(null, null, listOf("3", "4", "5", "6"))
        assertLimitAndOffset(2, null, listOf("3", "4"))
        assertLimitAndOffset(3, null, listOf("3", "4", "5"))
        assertLimitAndOffset(nullLimit, 2, listOf("5", "6"))
        assertLimitAndOffset(nullLimit, 3, listOf("6"))
        assertLimitAndOffset(2, 1, listOf("4", "5"))
        assertLimitAndOffset(2, 2, listOf("5", "6"))
    }

    private fun findById(id: String): Value {
        return session.select("select * from dialect_test where id = '$id'") { row ->
            Value(row.time("time_col"),
                row.date("date_col"),
                row.timestamp("timestamp_col"),
                String(row.bytes("binary_col"), Charsets.UTF_8),
                row.string("varchar_col"),
                fromBlob(row, "blob_col"),
                fromClob(row, "clob_col"),
                if (dialect is MysqlDialect || dialect is SqliteDialect) listOf() else row.array<Int>("array_col"))
        }.single()
    }

    private fun toBlob(data: String): Any = when (session.dialect) {
        is PostgresDialect -> {
            val manager = (session.connection as ProxyConnection).unwrap(org.postgresql.PGConnection::class.java).largeObjectAPI
            val oid = manager.createLO(LargeObjectManager.READ + LargeObjectManager.WRITE)
            val obj = manager.open(oid, LargeObjectManager.WRITE)
            obj.write(data.toByteArray(Charsets.UTF_8))
            obj.close()
            oid
        }
        is SqliteDialect -> {
            // Don't support createBlob
            data.toByteArray(Charsets.UTF_8)
        }
        else -> session.connection.createBlob().apply { setBytes(1, data.toByteArray(Charsets.UTF_8)) }
    }

    private fun fromBlob(row: Row, column: String) = when (session.dialect) {
        is SqliteDialect -> String(row.bytes(column), Charsets.UTF_8)
        else -> row.blob(column).let { String(it.getBytes(1, it.length().toInt()), Charsets.UTF_8) }
    }

    private fun fromClob(row: Row, column: String): String = when (session.dialect) {
        is PostgresDialect, is SqliteDialect -> row.string(column)
        else -> row.clob(column).let { it.getSubString(1, it.length().toInt()).trim() }
    }

    private fun toClob(data: String): Any = when (session.dialect) {
        is PostgresDialect, is SqliteDialect -> data
        else -> session.connection.createClob().let { it.setString(1, data); it }
    }

    private fun assertEqualValues(actual: Value, expected: Value) {
        println(expected)
        println(actual)

        // Dates and times aren't equal by equality with hsqldb, so test the strings
        assertEquals(expected.time.toString(), actual.time.toString())
        assertEquals(expected.date.toString(), actual.date.toString())
        assertEquals(expected.binary, actual.binary)
        assertEquals(expected.varchar, actual.varchar)
        assertEquals(expected.blob, actual.blob)
        assertEquals(expected.clob, actual.clob)

        if (dialect !is MysqlDialect && dialect !is SqliteDialect) {
            assertEquals(expected.ints, actual.ints) // Arrays not supported
        }

        if (dialect !is MysqlDialect) {
            assertEquals(expected.timestamp.toString(), actual.timestamp.toString()) // travis doesn't support millis
        }
    }
}
