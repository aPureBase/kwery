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

import com.github.andrewoma.kwery.core.dialect.Dialect
import com.github.andrewoma.kwery.core.dialect.HsqlDialect
import com.github.andrewoma.kwery.core.interceptor.LoggingInterceptor
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import javax.sql.DataSource
import kotlin.properties.Delegates

abstract class AbstractSessionTest(val dataSource: DataSource = hsqlDataSource, val dialect: Dialect = HsqlDialect()) {
    companion object {
        val initialised: MutableSet<String> = hashSetOf()
    }

    var transaction: ManualTransaction by Delegates.notNull()
    var session: Session by Delegates.notNull()

    open var startTransactionByDefault: Boolean = true
    open var rollbackTransactionByDefault: Boolean = false

    val name = TestName()
    @Rule fun name(): TestName = name // Annotating val directly doesn't work

    @Before fun setUp() {
        session = DefaultSession(dataSource.connection, dialect, LoggingInterceptor())
        if (startTransactionByDefault) {
            transaction = session.manualTransaction()
        }

        afterSessionSetup()
        println("\n==== Starting '${name.methodName}'")
    }

    open fun afterSessionSetup() {}

    fun initialise(token: String, f: () -> Unit) {
        if (initialised.contains(token)) return
        initialised.add(token)
        f()
    }

    @After fun tearDown() {
        try {
            if (startTransactionByDefault) {
                if (rollbackTransactionByDefault || transaction.rollbackOnly) {
                    transaction.rollback()
                } else {
                    transaction.commit()
                }
            }
        } finally {
            session.connection.close()
        }
    }
}

val dbNameSql = """
    select character_value as name from information_schema.sql_implementation_info
    where implementation_info_name = 'DBMS NAME'
"""
