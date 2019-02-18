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

import com.zaxxer.hikari.HikariDataSource

val hsqlDataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:hsqldb:mem:kwery"
}

val postgresDataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:postgresql://localhost:5432/kwery"
    username = "kwery"
    password = "kwery"
}

val mysqlDataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:mysql://localhost:3306/kwery"
    username = "kwery"
    password = "kwery"
}

val sqliteDataSource = HikariDataSource().apply {
    jdbcUrl = "jdbc:sqlite::memory:"
}
