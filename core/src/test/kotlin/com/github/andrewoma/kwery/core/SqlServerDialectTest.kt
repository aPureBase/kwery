package com.github.andrewoma.kwery.core

import com.github.andrewoma.kwery.core.dialect.SqlServerDialect

class SqlServerDialectTest : AbstractDialectTest(sqlserverDataSource, SqlServerDialect()) {
    //language=tsql
    override val sql = """
        drop table if exists dialect_test;

        create table dialect_test (
          id            varchar(255),
          time_col      time,
          date_col      date,
          timestamp_col timestamp,
          --              timestamp_col timestamp(3), Waiting on travis to support this
          binary_col    binary(50),
          varchar_col   varchar(1000),
          blob_col      binary(50),
          clob_col      text,
          array_col     text -- Not supported
        );

        drop table if exists test;

        create table test (
          id            varchar(255),
          value         varchar(255)
        )
    """.trimIndent()

}
