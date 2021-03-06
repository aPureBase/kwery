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

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

abstract class AbstractSessionDelegationTest {
    companion object {
        var initialised = false
    }

    val insertSql = "insert into delegate_test(value) values (:value)"

    abstract fun <R> withSession(f: (Session) -> R): R

    @Before fun setUp() {
        if (!initialised) {
            initialised = true
            val sql = """
                drop table if exists delegate_test;
                create table delegate_test (
                    key serial,
                    value varchar(1000)
                )
            """
            withSession { session -> for (statement in sql.split(";".toRegex())) session.update(statement) }
        }

        withSession { session -> session.update("delete from delegate_test") }
    }

    @Test fun `Insert with generated keys`() {
        withSession { session ->
            val (count, key) = session.insert(insertSql, mapOf("value" to "hello")) { row -> row.int("key") }
            assertEquals(1, count)
            assertEquals("hello", select(session, key))
        }
    }

    @Test fun `Rollback only should rollback transaction`() {
        val key = withSession { session ->
            session.transaction {
                val sql = "insert into delegate_test(value) values (:value)"
                val (count, key) = session.insert(sql, mapOf("value" to "hello")) { row -> row.int("key") }
                session.currentTransaction?.rollbackOnly = true

                assertNotNull(session.connection)
                assertEquals(1, count)
                key
            }
        }
        assertNull(withSession { session -> select(session, key) })
    }

    @Test fun `Bind parameters should bind values`() {
        assertEquals("select '1'", withSession { session -> session.bindParameters("select ':value'", mapOf("value" to 1)) })
    }

    @Test fun `forEach and stream should process each row`() {
        withSession { session ->
            val sql = "insert into delegate_test(value) values (:value)"
            val parameters = listOf(
                    mapOf("value" to "hello"),
                    mapOf("value" to "there")
            )
            session.batchInsert(sql, parameters) { row -> row.int("key") }

            val values = hashSetOf<String>()
            val select = "select value from delegate_test"
            session.forEach(select) { row ->
                values.add(row.string("value"))
            }

            val expected = setOf("hello", "there")
            assertEquals(expected, values)
            assertEquals(expected, session.asSequence(select) { it.map { it.string("value") }.toSet() })
        }
    }

    @Test fun `Options should be accessible`() {
        withSession { session ->
            assertNotNull(session.dialect)
            assertNotNull(session.defaultOptions)
        }
    }

    @Test fun `Insert with keys followed by batch update`() {
        withSession { session ->
            val insert = "insert into delegate_test(key, value) values (:key, :value)"
            var count = session.update(insert, mapOf("key" to 100, "value" to "v1"))
            assertEquals(1, count)
            assertEquals("v1", select(session, 100))

            count = session.update(insert, mapOf("key" to 101, "value" to "v2"))
            assertEquals(1, count)
            assertEquals("v2", select(session, 101))

            val params = listOf(
                    mapOf("key" to 100, "value" to "newV1"),
                    mapOf("key" to 101, "value" to "newV2")
            )
            val counts = session.batchUpdate("update delegate_test set value = :value where key = :key", params)
            assertEquals(listOf(1, 1), counts)

            assertEquals("newV1", select(session, 100))
            assertEquals("newV2", select(session, 101))
        }
    }

    @Test fun `Transaction blocks should be honoured`() {
        withSession { session ->
            val key1 = session.transaction {
                session.insert(insertSql, mapOf("value" to "v1")) { it.int("key") }
            }

            val key2 = session.transaction {
                session.currentTransaction?.rollbackOnly = true
                session.insert(insertSql, mapOf("value" to "v2")) { it.int("key") }
            }

            assertNotNull(select(session, key1.second))
            assertNull(select(session, key2.second))
        }
    }

    fun select(session: Session, key: Int): String? {
        return session.select("select value from delegate_test where key = :key", mapOf("key" to key)) { row ->
            row.string("value")
        }.firstOrNull()
    }
}
