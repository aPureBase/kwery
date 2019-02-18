package com.github.andrewoma.kwery.core.builder

data class Join(
    val type: Type,
    val table: String,
    val handler: String = table.split("_").map { it.first().toLowerCase() }.joinToString(""),
    val andClauses: MutableList<(String) -> String> = mutableListOf(),
    val builder: Join.(String) -> String
) {

    enum class Type(val value: String) {
        Inner("inner"),
        Left("left"),
        Right("right")
    }

    infix fun and(clause: (handler: String) -> String) = apply { andClauses.add(clause) }

    val on = builder(this, handler)

    fun on(clause: String) = clause

    fun getSql(): String {
        val and = andClauses.joinToString(" ") { "and ${it(handler)}" }
        return "${type.value} join $table $handler on $on $and".trim()
    }
}
