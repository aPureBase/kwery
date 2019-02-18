package com.github.andrewoma.kwery.core.builder

import com.github.andrewoma.kwery.core.builder.Join.Type.*

class JoinGroup(internal val builders: MutableList<JoinGroup.() -> Unit> = mutableListOf()) {

    private var counter = 0
    private val joins = mutableMapOf<Int, Join>()
    private val literalJoins = mutableMapOf<Int, String>()

    internal fun addBuilder(builder: JoinGroup.() -> Unit) = builders.add(builder)

    fun Join.add(): Join = if (joins.any { it.value.handler == handler }) {
        copy(handler = this.handler + "1").add()
    } else this.also { joins[counter++] = it }

    fun innerJoin(table: String, handler: String? = null, builder: Join.(String) -> String) =
        join(Inner, table, handler, builder)

    fun leftJoin(table: String, handler: String? = null, builder: Join.(String) -> String) =
        join(Left, table, handler, builder)

    fun rightJoin(table: String, handler: String? = null, builder: Join.(String) -> String) =
        join(Right, table, handler, builder)

    fun join(sql: String) = literalJoins.put(counter++, sql)

    fun join(type: Join.Type, table: String, handler: String?, builder: Join.(String) -> String): Join {
        val join = handler?.let { Join(type, table, it, builder = builder) }
                ?: Join(type, table, builder = builder)
        return join.add()
    }

    fun build(): String {
        builders.forEach { it(this) }
        val joins = (literalJoins + joins.mapValues { it.value.getSql() }).toSortedMap()
        return joins.values.joinToString("\n")
    }
}
