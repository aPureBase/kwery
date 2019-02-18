package com.github.andrewoma.kwery.core.dialect

import java.sql.Date
import java.sql.Time
import java.sql.Timestamp


open class SqlServerDialect : Dialect {
    override fun bind(value: Any, limit: Int) = when (value) {
        is String -> escapeSingleQuotedString(value.truncate(limit))
        is Timestamp -> timestampFormat.get().format(value)
        is Date -> "'$value'"
        is Time -> "'$value'"
        else -> value.toString()
    }

    override fun arrayBasedIn(name: String) = TODO("Fail")

    override val supportsArrayBasedIn = false

    override val supportsAllocateIds = false

    override fun allocateIds(count: Int, sequence: String, columnName: String) = throw UnsupportedOperationException()

    override val supportsFetchingGeneratedKeysByName = false

    override fun applyLimitAndOffset(limit: Int?, offset: Int?, sql: String) = when {
        limit != null && offset != null -> "$sql OFFSET $offset ROWS FETCH NEXT $limit ROWS ONLY"
        offset != null -> "$sql OFFSET $offset ROWS"
        limit != null -> "$sql OFFSET 0 ROWS FETCH NEXT $limit ROWS ONLY"
        else -> sql
    }
}
